package com.example.skyblockbestiary.hud;

import com.example.skyblockbestiary.data.BestiaryScanner;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ChestMenu;

import java.util.List;

public final class BestiaryHUD {
    private static final int PANEL_WIDTH = 190;
    private static final int VISIBLE_ROWS = 10;
    private static final int ROW_HEIGHT = 14;
    private static final int TITLE_HEIGHT = 12;
    private static final int SUMMARY_HEIGHT = 12;
    private static final int BUTTONS_HEIGHT = 12;
    private static final int BUTTONS_GAP = 2;
    private static final BestiaryScanner SCANNER = new BestiaryScanner();
    private static boolean dragging;
    private static int dragOffsetX;
    private static int dragOffsetY;

    private BestiaryHUD() {
    }

    public static void init() {
        SCANNER.load();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) SCANNER.receiveMessage(message.getString());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SCANNER.resetChat());
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("skyblock-bestiary-tracker", "tracker"),
            (graphics, tickCounter) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (SCANNER.isHudEnabled() && minecraft.screen == null) {
                    clampHudPosition(graphics.guiWidth(), graphics.guiHeight());
                    renderPanel(graphics, SCANNER.hudX(), SCANNER.hudY(), false, panelHeight(false));
                }
            }
        );

        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            if (!(screen instanceof ContainerScreen containerScreen)) return;
            ChestMenu menu = containerScreen.getMenu();

            ScreenEvents.afterTick(screen).register(ignored -> {
                if (SCANNER.scan(menu, screen.getTitle().getString())) {
                    containerScreen.leftPos = Math.max(16, Math.min(screen.width - 184,
                        Math.max(containerScreen.leftPos, PANEL_WIDTH + 16)));
                }
            });
            ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, tickDelta) -> {
                if (!SCANNER.isBestiaryMenu(menu, screen.getTitle().getString())) {
                    if (SCANNER.isHudEnabled()) {
                        clampHudPosition(screen.width, screen.height);
                        renderPanel(graphics, SCANNER.hudX(), SCANNER.hudY(), false, panelHeight(false));
                    }
                    return;
                }
                float scale = sideScale(containerScreen);
                graphics.pose().pushMatrix();
                graphics.pose().translate(sideX(containerScreen), containerScreen.topPos);
                graphics.pose().scale(scale, scale);
                renderPanel(graphics, 0, 0, true, (int) (inventoryHeight(menu) / scale));
                graphics.pose().popMatrix();
                if (SCANNER.isHudEnabled()) {
                    clampHudPosition(screen.width, screen.height);
                    renderPanel(graphics, SCANNER.hudX(), SCANNER.hudY(), false, panelHeight(false));
                }
                if (!dragging && !(SCANNER.isHudEnabled()
                    && inside(mouseX, mouseY, SCANNER.hudX(), SCANNER.hudY(), PANEL_WIDTH, panelHeight(false)))) {
                    double localX = (mouseX - sideX(containerScreen)) / scale;
                    double localY = (mouseY - containerScreen.topPos) / scale;
                    int rows = visibleRows(true, (int) (inventoryHeight(menu) / scale));
                    if (inside(localX, localY, 0, listTop(true), PANEL_WIDTH, rows * ROW_HEIGHT)) {
                        int row = (int) ((localY - listTop(true)) / ROW_HEIGHT);
                        List<BestiaryScanner.MobEntry> ranked = SCANNER.ranked();
                        int index = SCANNER.scrollOffset(rows) + row;
                        if (index < ranked.size()) {
                            Component hint = Component.literal(
                                "Click to open " + ranked.get(index).name() + " Bestiary"
                                    + (SCANNER.confirmedTier(ranked.get(index)) > 0 ? " (refresh progress)" : ""));
                            // afterExtract runs after Minecraft's deferred tooltip pass.
                            graphics.nextStratum();
                            graphics.tooltip(minecraft.font, List.of(ClientTooltipComponent.create(hint.getVisualOrderText())),
                                mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
                        }
                    }
                }
            });
            ScreenEvents.remove(screen).register(ignored -> {
                dragging = false;
                SCANNER.save();
            });

            ScreenMouseEvents.allowMouseClick(screen).register((ignored, event) -> {
                if (event.button() != 0) return true;
                if (SCANNER.isHudEnabled()) {
                    clampHudPosition(screen.width, screen.height);
                    if (inside(event.x(), event.y(), SCANNER.hudX(), SCANNER.hudY(), PANEL_WIDTH, TITLE_HEIGHT)) {
                        dragging = true;
                        dragOffsetX = (int) event.x() - SCANNER.hudX();
                        dragOffsetY = (int) event.y() - SCANNER.hudY();
                        return false;
                    }
                    if (inside(event.x(), event.y(), SCANNER.hudX(), SCANNER.hudY(), PANEL_WIDTH, panelHeight(false))) return false;
                }
                if (!SCANNER.isBestiaryMenu(menu, screen.getTitle().getString())) return true;
                float scale = sideScale(containerScreen);
                double mouseX = (event.x() - sideX(containerScreen)) / scale;
                double mouseY = (event.y() - containerScreen.topPos) / scale;
                int buttonY = buttonTop();
                if (inside(mouseX, mouseY, 5, buttonY, 88, BUTTONS_HEIGHT)) {
                    SCANNER.toggleMode();
                    return false;
                }
                if (inside(mouseX, mouseY, 97, buttonY, 88, BUTTONS_HEIGHT)) {
                    SCANNER.toggleHud();
                    return false;
                }
                int rows = visibleRows(true, (int) (inventoryHeight(menu) / scale));
                if (inside(mouseX, mouseY, 0, listTop(true), PANEL_WIDTH, rows * ROW_HEIGHT)) {
                    int row = (int) ((mouseY - listTop(true)) / ROW_HEIGHT);
                    String command = BestiaryRowAction.commandForRow(SCANNER.ranked(), row, SCANNER.scrollOffset(rows), rows);
                    if (command != null && minecraft.getConnection() != null && minecraft.player != null) {
                        minecraft.player.closeContainer();
                        minecraft.getConnection().sendCommand(command);
                    }
                    return false;
                }
                return true;
            });
            ScreenMouseEvents.allowMouseDrag(screen).register((ignored, event, deltaX, deltaY) -> {
                if (!dragging) return true;
                SCANNER.setHudPosition(
                    Math.min(screen.width - PANEL_WIDTH, (int) event.x() - dragOffsetX),
                    Math.min(screen.height - panelHeight(false), (int) event.y() - dragOffsetY)
                );
                return false;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((ignored, event) -> {
                if (!dragging) return true;
                dragging = false;
                SCANNER.save();
                return false;
            });
            ScreenMouseEvents.allowMouseScroll(screen).register((ignored, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                if (!SCANNER.isBestiaryMenu(menu, screen.getTitle().getString())) return true;
                float scale = sideScale(containerScreen);
                int x = sideX(containerScreen);
                int y = containerScreen.topPos;
                int invHeight = inventoryHeight(menu);
                if (!inside(mouseX, mouseY, x, y, (int) (PANEL_WIDTH * scale), invHeight) || verticalAmount == 0) return true;
                SCANNER.scroll(verticalAmount > 0 ? -1 : 1, visibleRows(true, (int) (invHeight / scale)));
                return false;
            });
        });
    }

    private static int sideX(ContainerScreen screen) {
        return screen.leftPos - 8 - (int) Math.ceil(PANEL_WIDTH * sideScale(screen));
    }

    private static float sideScale(ContainerScreen screen) {
        return Math.min(1f, Math.max(1, screen.leftPos - 16) / (float) PANEL_WIDTH);
    }

    private static int inventoryHeight(ChestMenu menu) {
        return 114 + menu.getRowCount() * 18;
    }

    private static int buttonTop() {
        return TITLE_HEIGHT + SUMMARY_HEIGHT + BUTTONS_GAP;
    }

    private static int listTop(boolean controls) {
        return controls
            ? buttonTop() + BUTTONS_HEIGHT + BUTTONS_GAP
            : TITLE_HEIGHT + SUMMARY_HEIGHT + BUTTONS_GAP;
    }

    private static int panelHeight(boolean controls) {
        return listTop(controls) + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private static int visibleRows(boolean controls, int height) {
        return Math.max(1, (height - listTop(controls)) / ROW_HEIGHT);
    }

    private static void clampHudPosition(int width, int height) {
        SCANNER.setHudPosition(Math.min(SCANNER.hudX(), Math.max(0, width - PANEL_WIDTH)),
            Math.min(SCANNER.hudY(), Math.max(0, height - panelHeight(false))));
    }

    private static void renderPanel(GuiGraphicsExtractor graphics, int x, int y, boolean controls, int explicitHeight) {
        Font font = Minecraft.getInstance().font;
        int height = explicitHeight > 0 ? explicitHeight : panelHeight(controls);
        graphics.text(font, !controls && Minecraft.getInstance().screen != null ? "Bestiary HUD (drag)" : "Bestiary",
            x + 5, y + 2, 0xFFFFFF55, true);

        int unlocked = SCANNER.unlockedCount();
        int total = SCANNER.totalCount();
        int maxed = SCANNER.maxedCount();
        String summary = controls ? unlocked + "/" + total + " unlocked  " + maxed + " maxed" : unlocked + "/" + total;
        graphics.text(font, summary, x + 5, y + TITLE_HEIGHT, 0xFFFFFFFF, true);

        if (controls) {
            int buttonY = y + buttonTop();
            graphics.text(font, SCANNER.isNextTier() ? "[Next Tier]" : "[Completion]", x + 5, buttonY + 1, 0xFFFFFF55, true);
            graphics.text(font, SCANNER.isHudEnabled() ? "[HUD: ON]" : "[HUD: OFF]", x + 97, buttonY + 1, 0xFFFFFF55, true);
        }

        int listY = y + listTop(controls);
        int visibleRows = visibleRows(controls, height);

        List<BestiaryScanner.MobEntry> ranked = SCANNER.ranked();
        if (ranked.isEmpty()) {
            graphics.text(font, "Open a Bestiary mob page", x + 5, listY + 2, 0xFFFFFFFF, true);
            return;
        }

        int offset = SCANNER.scrollOffset(visibleRows);
        int limit = Math.min(visibleRows, ranked.size() - offset);
        for (int i = 0; i < limit; i++) {
            BestiaryScanner.MobEntry mob = ranked.get(offset + i);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 4, listY + i * ROW_HEIGHT + 1);
            graphics.pose().scale(0.75f, 0.75f);
            graphics.item(SCANNER.icon(mob), 0, 0);
            graphics.pose().popMatrix();
            int tier = SCANNER.confirmedTier(mob);
            long current = SCANNER.current(mob);
            long target = SCANNER.target(mob);
            String progress = tier > 0 ? "Tier " + tier + "*" : current + "/" + target;
            String name = font.plainSubstrByWidth(mob.name(), Math.max(0, PANEL_WIDTH - 26 - font.width(progress)));
            graphics.text(font, name, x + 19, listY + i * ROW_HEIGHT + 3, 0xFFFFFFFF, true);
            graphics.text(font, progress, x + PANEL_WIDTH - 5 - font.width(progress), listY + i * ROW_HEIGHT + 3, 0xFFFFFFFF, true);
        }

    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}

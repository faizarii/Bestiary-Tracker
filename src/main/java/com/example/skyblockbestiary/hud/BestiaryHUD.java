package com.example.skyblockbestiary.hud;

import com.example.skyblockbestiary.data.BestiaryScanner;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ChestMenu;

import java.util.List;

public final class BestiaryHUD {
    private static final int PANEL_WIDTH = 190;
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 22;
    private static final int TITLE_HEIGHT = 12;
    private static final int SUMMARY_HEIGHT = 12;
    private static final int BUTTONS_HEIGHT = 15;
    private static final int BUTTONS_GAP = 6;
    private static final int BOTTOM_PADDING = 4;
    private static final int SCROLL_HINT_HEIGHT = 10;
    private static final BestiaryScanner SCANNER = new BestiaryScanner();
    private static boolean dragging;
    private static int dragOffsetX;
    private static int dragOffsetY;

    private BestiaryHUD() {
    }

    public static void init() {
        SCANNER.load();
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("skyblock-bestiary-tracker", "tracker"),
            (graphics, tickCounter) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (SCANNER.isHudEnabled() && minecraft.screen == null) {
                    renderPanel(graphics, SCANNER.hudX(), SCANNER.hudY(), false, panelHeight(false));
                }
            }
        );

        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            if (!(screen instanceof ContainerScreen containerScreen)) return;
            ChestMenu menu = containerScreen.getMenu();

            ScreenEvents.afterTick(screen).register(ignored -> SCANNER.scan(menu, screen.getTitle().getString()));
            ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, tickDelta) -> {
                if (!SCANNER.isBestiaryMenu(menu, screen.getTitle().getString())) return;
                int x = sideX(screen);
                int y = sideY(screen, menu);
                renderPanel(graphics, x, y, true, inventoryHeight(menu));
                if (SCANNER.isHudEnabled()) renderPanel(graphics, SCANNER.hudX(), SCANNER.hudY(), false, panelHeight(false));
            });
            ScreenEvents.remove(screen).register(ignored -> {
                dragging = false;
                SCANNER.save();
            });

            ScreenMouseEvents.allowMouseClick(screen).register((ignored, event) -> {
                if (event.button() != 0) return true;
                int x = sideX(screen);
                int y = sideY(screen, menu);
                int buttonY = y + buttonTop();
                if (inside(event.x(), event.y(), x + 5, buttonY, 88, BUTTONS_HEIGHT)) {
                    SCANNER.toggleMode();
                    return false;
                }
                if (inside(event.x(), event.y(), x + 97, buttonY, 88, BUTTONS_HEIGHT)) {
                    SCANNER.toggleHud();
                    return false;
                }
                if (SCANNER.isHudEnabled() && inside(event.x(), event.y(), SCANNER.hudX(), SCANNER.hudY(), PANEL_WIDTH, TITLE_HEIGHT)) {
                    dragging = true;
                    dragOffsetX = (int) event.x() - SCANNER.hudX();
                    dragOffsetY = (int) event.y() - SCANNER.hudY();
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
                int x = sideX(screen);
                int y = sideY(screen, menu);
                int invHeight = inventoryHeight(menu);
                if (!inside(mouseX, mouseY, x, y, PANEL_WIDTH, invHeight)) return true;
                SCANNER.scroll(verticalAmount > 0 ? -1 : 1, (invHeight - listTop(true)) / ROW_HEIGHT);
                return false;
            });
        });
    }

    private static int sideX(Screen screen) {
        int inventoryLeft = (screen.width - 176) / 2;
        int right = inventoryLeft + 184;
        return right + PANEL_WIDTH <= screen.width ? right : Math.max(0, inventoryLeft - PANEL_WIDTH - 8);
    }

    private static int sideY(Screen screen, ChestMenu menu) {
        return Math.max(0, (screen.height - inventoryHeight(menu)) / 2);
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
        return listTop(controls) + VISIBLE_ROWS * ROW_HEIGHT + (controls ? SCROLL_HINT_HEIGHT : 0) + BOTTOM_PADDING;
    }

    private static void renderPanel(GuiGraphicsExtractor graphics, int x, int y, boolean controls, int explicitHeight) {
        Font font = Minecraft.getInstance().font;
        int height = explicitHeight > 0 ? explicitHeight : panelHeight(controls);
        graphics.fill(x, y, x + PANEL_WIDTH, y + height, 0xE00A0A0A);
        graphics.text(font, controls ? "Bestiary" : "Bestiary (drag)", x + 5, y + 4, 0xFFFFFFFF, true);

        int unlocked = SCANNER.unlockedCount();
        int total = SCANNER.totalCount();
        int maxed = SCANNER.maxedCount();
        String summary = controls ? unlocked + "/" + total + " unlocked  " + maxed + " maxed" : unlocked + "/" + total;
        graphics.text(font, summary, x + 5, y + TITLE_HEIGHT + 2, 0xFFFFFFFF, false);

        if (controls) {
            int buttonY = y + buttonTop();
            graphics.fill(x + 5, buttonY, x + 93, buttonY + BUTTONS_HEIGHT, 0xFF303030);
            graphics.fill(x + 97, buttonY, x + 185, buttonY + BUTTONS_HEIGHT, 0xFF303030);
            graphics.centeredText(font, SCANNER.isNextTier() ? "Next Tier" : "Completion", x + 49, buttonY + 3, 0xFFFFFFFF);
            graphics.centeredText(font, SCANNER.isHudEnabled() ? "HUD: ON" : "HUD: OFF", x + 141, buttonY + 3, 0xFFFFFFFF);
        }

        int listY = y + listTop(controls);
        int maxHeight = explicitHeight > 0 ? explicitHeight : height;
        int availableListHeight = maxHeight - listTop(controls);
        int visibleRows = availableListHeight / ROW_HEIGHT;

        List<BestiaryScanner.MobEntry> ranked = SCANNER.ranked();
        if (ranked.isEmpty()) {
            graphics.text(font, "Open a Bestiary mob page", x + 5, listY + 6, 0xFFFFFFFF, false);
            return;
        }

        int offset = SCANNER.scrollOffset(visibleRows);
        int limit = Math.min(visibleRows, ranked.size() - offset);
        for (int i = 0; i < limit; i++) {
            BestiaryScanner.MobEntry mob = ranked.get(offset + i);
            graphics.item(SCANNER.icon(mob), x + 4, listY + i * ROW_HEIGHT + 2);
            String name = font.plainSubstrByWidth((offset + i + 1) + ". " + mob.name(), PANEL_WIDTH - 60);
            graphics.text(font, name, x + 24, listY + i * ROW_HEIGHT + 6, 0xFFFFFFFF, false);
            long current = SCANNER.current(mob);
            long target = SCANNER.target(mob);
            String progress = current + "/" + target;
            graphics.text(font, progress, x + PANEL_WIDTH - 5 - font.width(progress), listY + i * ROW_HEIGHT + 6, 0xFFAAAAAA, false);
        }

        if (controls && ranked.size() > visibleRows) {
            int bottom = listY + limit * ROW_HEIGHT;
            String scrollHint = (offset + 1) + "-" + (offset + limit) + "/" + ranked.size();
            graphics.text(font, scrollHint, x + PANEL_WIDTH - 5 - font.width(scrollHint), bottom, 0xFF888888, false);
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}

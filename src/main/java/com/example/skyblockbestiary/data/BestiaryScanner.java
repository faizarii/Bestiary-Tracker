package com.example.skyblockbestiary.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BestiaryScanner {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Data>() {}.getType();
    private static final Path DATA_FILE = FabricLoader.getInstance().getConfigDir().resolve("skyblock-bestiary-tracker.json");
    private static final Pattern NAME_PATTERN = Pattern.compile("^(.+?)(?: ([IVXLCDM]+|\\d+))?$");
    private static final Pattern KILLS_PATTERN = Pattern.compile("Kills: ([0-9,.]+)");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("([0-9kKmMbB,.]+)/([0-9kKmMbB,.]+)$");

    private final Map<String, ItemStack> icons = new LinkedHashMap<>();
    private Data data = new Data();

    public boolean isBestiaryMenu(ChestMenu menu, String title) {
        if (title.equals("Bestiary") || title.contains("Bestiary ➜") || title.equals("Search Results")) return true;
        int containerSlots = menu.getRowCount() * 9;
        for (int i = 0; i < containerSlots; i++) {
            ItemLore lore = menu.getSlot(i).getItem().get(DataComponents.LORE);
            if (lore == null) continue;
            List<String> lines = lore.lines().stream().map(component -> component.getString()).toList();
            if (lines.stream().anyMatch(line -> line.contains("Families Found") || line.contains("Progress to Tier"))) return true;
        }
        return false;
    }

    public boolean scan(ChestMenu menu, String title) {
        if (!isBestiaryMenu(menu, title)) return false;
        boolean changed = title.equals("Bestiary") && scanRootTotals(menu);
        String category = title.contains("➜") ? title.substring(title.lastIndexOf('➜') + 1).trim() : title;
        int containerSlots = menu.getRowCount() * 9;

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            MobEntry entry = parseMob(stack, category);
            if (entry == null) continue;
            icons.put(entry.key(), stack.copy());
            if (!entry.equals(data.mobs.put(entry.key(), entry))) changed = true;
        }
        if (changed) save();
        return true;
    }

    public ItemStack icon(MobEntry mob) {
        ItemStack stack = icons.get(mob.key());
        return stack != null ? stack : new ItemStack(Items.PLAYER_HEAD);
    }

    public List<MobEntry> ranked() {
        return data.mobs.values().stream()
            .filter(mob -> remaining(mob) > 0)
            .sorted(Comparator.comparingLong(this::remaining).thenComparing(MobEntry::name))
            .toList();
    }

    public long remaining(MobEntry mob) {
        return Math.max(0, target(mob) - current(mob));
    }

    public long current(MobEntry mob) {
        return data.nextTier ? mob.nextCurrent : mob.kills;
    }

    public long target(MobEntry mob) {
        return data.nextTier ? mob.nextNeeded : mob.maxNeeded;
    }

    public int unlockedCount() {
        return data.unlocked;
    }

    public int totalCount() {
        return data.total;
    }

    public int maxedCount() {
        return data.maxed;
    }

    public boolean isNextTier() {
        return data.nextTier;
    }

    public void toggleMode() {
        data.nextTier = !data.nextTier;
        data.scrollOffset = 0;
        save();
    }

    public boolean isHudEnabled() {
        return data.hudEnabled;
    }

    public void toggleHud() {
        data.hudEnabled = !data.hudEnabled;
        save();
    }

    public int hudX() {
        return data.hudX;
    }

    public int hudY() {
        return data.hudY;
    }

    public void setHudPosition(int x, int y) {
        data.hudX = Math.max(0, x);
        data.hudY = Math.max(0, y);
    }

    public int scrollOffset(int visibleRows) {
        return Math.min(data.scrollOffset, Math.max(0, ranked().size() - visibleRows));
    }

    public void scroll(int amount, int visibleRows) {
        data.scrollOffset = Math.max(0, Math.min(Math.max(0, ranked().size() - visibleRows), scrollOffset(visibleRows) + amount));
    }

    public void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(DATA_FILE)) {
                GSON.toJson(data, DATA_TYPE, writer);
            }
        } catch (IOException exception) {
            System.err.println("Failed to save bestiary data: " + exception.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(DATA_FILE)) return;
        try (Reader reader = Files.newBufferedReader(DATA_FILE)) {
            Data loaded = GSON.fromJson(reader, DATA_TYPE);
            if (loaded != null) data = loaded;
            if (data.mobs == null) data.mobs = new LinkedHashMap<>();
        } catch (IOException | RuntimeException exception) {
            System.err.println("Failed to load bestiary data: " + exception.getMessage());
        }
    }

    private boolean scanRootTotals(ChestMenu menu) {
        int unlocked = 0;
        int total = 0;
        int maxed = 0;
        int containerSlots = menu.getRowCount() * 9;

        for (int i = 0; i < containerSlots; i++) {
            ItemLore lore = menu.getSlot(i).getItem().get(DataComponents.LORE);
            if (lore == null) continue;
            List<String> lines = lore.lines().stream().map(component -> component.getString().trim()).toList();
            for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
                Matcher progress = PROGRESS_PATTERN.matcher(lines.get(lineIndex));
                if (!progress.find()) continue;
                String previous = lines.get(lineIndex - 1);
                if (previous.startsWith("Families Found")) {
                    unlocked += parseNumber(progress.group(1));
                    total += parseNumber(progress.group(2));
                } else if (previous.startsWith("Families Completed")) {
                    maxed += parseNumber(progress.group(1));
                }
            }
        }

        if (total == 0 || data.unlocked == unlocked && data.total == total && data.maxed == maxed) return false;
        data.unlocked = unlocked;
        data.total = total;
        data.maxed = maxed;
        return true;
    }

    private static MobEntry parseMob(ItemStack stack, String category) {
        if (stack.isEmpty()) return null;
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        List<String> lines = lore.lines().stream().map(component -> component.getString().trim()).toList();
        long kills = 0;
        long nextCurrent = 0;
        long nextNeeded = 0;
        long maxNeeded = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher killsMatcher = KILLS_PATTERN.matcher(line);
            if (killsMatcher.find()) kills = parseNumber(killsMatcher.group(1));

            Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
            if (!progressMatcher.find() || i == 0) continue;
            String previous = lines.get(i - 1);
            if (previous.startsWith("Progress to Tier")) {
                nextCurrent = parseNumber(progressMatcher.group(1));
                nextNeeded = parseNumber(progressMatcher.group(2));
            } else if (previous.startsWith("Overall Progress")) {
                maxNeeded = parseNumber(progressMatcher.group(2));
            }
        }

        if (nextNeeded == 0 && maxNeeded == 0) return null;
        Matcher nameMatcher = NAME_PATTERN.matcher(stack.getHoverName().getString().trim());
        if (!nameMatcher.matches()) return null;
        String name = nameMatcher.group(1);
        return new MobEntry(category + "/" + name, name, category, kills, nextCurrent, nextNeeded, maxNeeded);
    }

    static long parseNumber(String input) {
        String value = input.replace(",", "").toLowerCase(Locale.ROOT);
        long multiplier = 1;
        if (value.endsWith("k")) multiplier = 1_000;
        if (value.endsWith("m")) multiplier = 1_000_000;
        if (value.endsWith("b")) multiplier = 1_000_000_000;
        if (multiplier > 1) value = value.substring(0, value.length() - 1);
        return Math.round(Double.parseDouble(value) * multiplier);
    }

    public record MobEntry(String key, String name, String category, long kills, long nextCurrent, long nextNeeded, long maxNeeded) {}

    private static final class Data {
        private Map<String, MobEntry> mobs = new LinkedHashMap<>();
        private boolean nextTier = true;
        private boolean hudEnabled;
        private int hudX = 8;
        private int hudY = 8;
        private int scrollOffset;
        private int unlocked;
        private int total;
        private int maxed;
    }
}

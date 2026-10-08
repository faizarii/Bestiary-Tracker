package com.example.skyblockbestiary.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

final class BestiaryScannerCheck {
    static void run() throws IOException {
        var directory = Files.createTempDirectory("bestiary-scanner-check");
        var file = directory.resolve("progress.json");
        try {
            checkBrowsing(directory.resolve("browsing.json"));
            checkHiddenProgress(directory.resolve("hidden.json"));
            Files.writeString(file, """
                {"mobs": {
                  "Fishing (1/2)/Beehemoth": {"key":"Fishing (1/2)/Beehemoth","name":"Beehemoth",
                    "category":"Fishing (1/2)","kills":8,"nextCurrent":8,"nextNeeded":10,"maxNeeded":100,"tier":1},
                  "Fishing (2/2)/Beehemoth": {"key":"Fishing (2/2)/Beehemoth","name":"Beehemoth",
                    "category":"Fishing (2/2)","kills":9,"nextCurrent":9,"nextNeeded":10,"maxNeeded":100,"tier":1},
                  "Beehemoth/Beehemoth": {"key":"Beehemoth/Beehemoth","name":"Beehemoth",
                    "category":"Beehemoth","kills":7,"nextCurrent":7,"nextNeeded":10,"maxNeeded":100,"tier":1},
                  "Search Results/Beehemoth": {"key":"Search Results/Beehemoth","name":"Beehemoth",
                    "category":"Search Results","kills":6,"nextCurrent":6,"nextNeeded":10,"maxNeeded":0,"tier":1}},
                 "icons":{"Fishing (2/2)/Beehemoth":{"item":"minecraft:dragon_egg"}},
                 "confirmedTiers":{"Fishing (1/2)/Beehemoth":2},"hudEnabled":true,"hudX":30}
                """);
            var scanner = new BestiaryScanner(file);
            String original = Files.readString(file);
            scanner.load();
            var backup = file.resolveSibling("progress.json.before-family-keys.bak");
            assert Files.readString(backup).equals(original) : "Original saved data must be backed up before migration";
            assert scanner.ranked().size() == 1 : "Saved page, detail, and hidden-progress aliases should merge";
            var mob = scanner.ranked().getFirst();
            assert mob.key().equals("Beehemoth/100") && mob.kills() == 9;
            assert scanner.confirmedTier(mob) == 2 : "Pending tier must survive migration";
            assert scanner.icon(mob).is(Items.DRAGON_EGG) : "Icon must survive migration";
            assert scanner.isHudEnabled() && scanner.hudX() == 30 : "Settings must survive migration";

            var reloaded = new BestiaryScanner(file);
            reloaded.load();
            assert Files.readString(backup).equals(original) : "Reload must not overwrite the original backup";
            assert reloaded.ranked().equals(scanner.ranked()) : "Migration must persist";
            assert reloaded.confirmedTier(reloaded.ranked().getFirst()) == 2;

            var menu = ChestMenu.threeRows(0, new Inventory(null, null));
            menu.getContainer().setItem(0, mobItem("Beehemoth I", 9, 9, 10));
            scanner.scan(menu, "Bestiary ➜ Fishing (1/2)");
            assert scanner.confirmedTier(scanner.ranked().getFirst()) == 2 : "Stale menu must not undo chat";
            menu.getContainer().setItem(0, mobItem("Beehemoth II", 10, 0, 15));
            scanner.scan(menu, "Bestiary ➜ Fishing (2/2)");
            assert scanner.ranked().size() == 1 : "Switching pages must not duplicate a family";
            mob = scanner.ranked().getFirst();
            assert mob.tier() == 2 && mob.nextNeeded() == 15 && scanner.confirmedTier(mob) == 0;

            scanner.receiveMessage("BESTIARY\n▬▬▬▬▬▬\nBeehemoth II ➡ III\nREWARDS");
            assert scanner.confirmedTier(mob) == 3 : "Migrated family must accept milestone updates";
            scanner.receiveMessage("BESTIARY\nBeehemoth 1 ➡ 2");
            assert scanner.confirmedTier(mob) == 3 : "Late messages must not downgrade progress";
            menu.getContainer().setItem(0, mobItem("Beehemoth III", 25, 0, 20));
            scanner.scan(menu, "Search Results");
            assert scanner.ranked().size() == 1;
            mob = scanner.ranked().getFirst();
            assert mob.kills() == 25 && mob.tier() == 3 && scanner.confirmedTier(mob) == 0
                : "Search refresh must replace pending progress";
            menu.getContainer().setItem(0, mobItem("Beehemoth II", 10, 0, 15));
            scanner.scan(menu, "Bestiary ➜ Fishing (Page 1)");
            assert scanner.ranked().getFirst().equals(mob) : "Stale page must not roll back refreshed progress";
            menu.getContainer().setItem(0, mobItem("Beehemoth III", 25, 0, 20));

            scanner.scan(menu, "Bestiary ➜ Other Area");
            assert scanner.ranked().size() == 1 : "Another menu title must not create a new family";
            scanner.receiveMessage("BESTIARY\nBeehemoth 3 ➡ 4");
            assert scanner.confirmedTier(scanner.ranked().getFirst()) == 4;
            System.out.println("Bestiary scanner checks passed");
        } finally {
            Files.deleteIfExists(directory.resolve("browsing.json"));
            Files.deleteIfExists(directory.resolve("hidden.json"));
            Files.deleteIfExists(directory.resolve("progress.json.before-family-keys.bak"));
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void checkBrowsing(java.nio.file.Path file) {
        var scanner = new BestiaryScanner(file);
        var menu = ChestMenu.sixRows(0, new Inventory(null, null));
        menu.getContainer().setItem(10, mobItem("Enderman I", 3, 3, 10));
        scanner.scan(menu, "Bestiary ➜ The End");
        menu.getContainer().clearContent();
        menu.getContainer().setItem(4, mobItem("Enderman I", 3, 3, 10));
        scanner.scan(menu, "Bestiary ➜ Enderman");
        menu.getContainer().clearContent();
        menu.getContainer().setItem(10, mobItem("Enderman I", 3, 3, 10));
        scanner.scan(menu, "(1/2) Search Results");
        assert scanner.ranked().size() == 1 : "Area, detail, and paginated search must update the same family";

        menu.getContainer().setItem(11, mobItem("Hewer I", 2, 2, 10));
        scanner.scan(menu, "Bestiary ➜ Galatea");
        scanner.scan(menu, "(2/3) Bestiary ➜ Fishing");
        assert scanner.ranked().size() == 2 : "Browsing another category must not duplicate known families";
        scanner.receiveMessage("BESTIARY\nEnderman 1 ➡ 2\nHewer 1 ➡ 2\nREWARDS");
        assert scanner.ranked().stream().allMatch(entry -> scanner.confirmedTier(entry) == 2)
            : "Browsing aliases must not make milestone messages ambiguous";

        var differentFamily = mobItem("Enderman I", 3, 3, 10);
        differentFamily.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Kills: 3"), Component.literal("Progress to Tier"), Component.literal("3/10"),
            Component.literal("Overall Progress"), Component.literal("3/50"))));
        menu.getContainer().clearContent();
        menu.getContainer().setItem(10, differentFamily);
        scanner.scan(menu, "Bestiary ➜ Private Island");
        assert scanner.ranked().size() == 3 : "Genuine same-name families with different caps must remain separate";
        var beforeAmbiguousMessage = scanner.ranked().stream().map(scanner::confirmedTier).toList();
        scanner.receiveMessage("BESTIARY\nEnderman 2 ➡ 3\nREWARDS");
        assert scanner.ranked().stream().map(scanner::confirmedTier).toList().equals(beforeAmbiguousMessage)
            : "A same-name message must not update both genuine families";
        scanner.save();
        var reloaded = new BestiaryScanner(file);
        reloaded.load();
        assert reloaded.ranked().equals(scanner.ranked()) : "Browsing identities must survive restart";
    }

    private static void checkHiddenProgress(java.nio.file.Path file) {
        var scanner = new BestiaryScanner(file);
        var menu = ChestMenu.sixRows(0, new Inventory(null, null));
        var hidden = mobItem("Hewer I", 2, 2, 10);
        hidden.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Kills: 2"), Component.literal("Progress to Tier"), Component.literal("2/10"),
            Component.literal("Overall Progress: HIDDEN"))));
        menu.getContainer().setItem(10, hidden);
        scanner.scan(menu, "Bestiary ➜ Galatea");
        scanner.scan(menu, "Bestiary ➜ Hewer");
        assert scanner.ranked().size() == 1 : "Hidden overall progress must not duplicate a family";
        scanner.receiveMessage("BESTIARY\nHewer 1 ➡ 2\nREWARDS");
        menu.getContainer().setItem(10, mobItem("Hewer II", 10, 0, 15));
        scanner.scan(menu, "Search Results");
        var mob = scanner.ranked().getFirst();
        assert scanner.ranked().size() == 1 && mob.key().equals("Hewer/100") && mob.tier() == 2
            && scanner.confirmedTier(mob) == 0 : "Revealing the cap must upgrade the existing identity";
        menu.getContainer().setItem(10, hidden);
        scanner.scan(menu, "(1/2) Search Results");
        assert scanner.ranked().getFirst().equals(mob) : "Hidden stale data must not erase the known cap or progress";
    }

    private static ItemStack mobItem(String name, long kills, long current, long needed) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("Kills: " + kills),
            Component.literal("Progress to Tier"),
            Component.literal(current + "/" + needed),
            Component.literal("Overall Progress"),
            Component.literal(kills + "/100"))));
        return stack;
    }
}

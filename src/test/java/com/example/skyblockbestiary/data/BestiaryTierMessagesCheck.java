package com.example.skyblockbestiary.data;

import com.google.gson.Gson;

final class BestiaryTierMessagesCheck {
    static void run() {
        BestiaryTierMessages parser = new BestiaryTierMessages();
        String honeyhog = "§f                             §b§lHoneyhog §7§81 §8➡§b §b2";
        assert parser.accept(honeyhog, 0) == null : "Tier text without a Bestiary header must be ignored";
        assert parser.accept("§f                                  §6§lBESTIARY", 10) == null;
        assert parser.accept(" ", 11) == null;
        var event = parser.accept(honeyhog, 12);
        assert event != null && event.name().equals("Honeyhog") && event.previous() == 1 && event.tier() == 2;
        assert parser.accept(honeyhog, 13) == null : "Only one tier line per banner";

        parser.accept("BESTIARY", 20);
        event = parser.accept("  Giant Isopod 9 ➡️ 10  ", 21);
        assert event != null && event.name().equals("Giant Isopod") && event.tier() == 10;
        parser.accept("BESTIARY", 30);
        assert parser.accept("Honeyhog 1 ➡ 2", 10_031) == null : "Expired banner";
        parser.accept("BESTIARY", 40);
        assert parser.accept("REWARDS", 41) == null;
        assert parser.accept(honeyhog, 42) == null : "Reward section ends the tier window";
        parser.accept("BESTIARY", 50);
        assert parser.accept("Honeyhog 2 ➡ 1", 51) == null : "Do not downgrade";
        parser.accept("BESTIARY", 60);
        assert parser.accept("Honeyhog 2 ➡ 2", 61) == null : "Not a tier-up";
        parser.accept("BESTIARY", 70);
        parser.reset();
        assert parser.accept(honeyhog, 71) == null : "Disconnect clears the banner";
        parser.accept("BESTIARY", 80);
        assert parser.accept("Honeyhog 999999999999999 ➡ 2", 81) == null;

        assert BestiaryTierMessages.menuTier(null) == 0;
        assert BestiaryTierMessages.menuTier("12") == 12;
        assert BestiaryTierMessages.menuTier("IX") == 9;
        assert BestiaryTierMessages.menuTier("XIV") == 14;
        var legacy = new Gson().fromJson("""
            {"key":"Fishing/Honeyhog","name":"Honeyhog","category":"Fishing",
             "kills":3,"nextCurrent":3,"nextNeeded":4,"maxNeeded":100}
            """, BestiaryScanner.MobEntry.class);
        assert legacy.tier() == 0 && legacy.kills() == 3 : "Older progress files remain readable";
        System.out.println("Bestiary tier message checks passed");
    }
}

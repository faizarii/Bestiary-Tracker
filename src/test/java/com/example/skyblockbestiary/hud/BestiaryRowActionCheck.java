package com.example.skyblockbestiary.hud;

import com.example.skyblockbestiary.data.BestiaryScanner.MobEntry;

import java.util.List;

public final class BestiaryRowActionCheck {
    public static void run() {
        var mobs = List.of(mob("Honeyhog"), mob("Giant Isopod"), mob("Poisonous Water Worm"));
        assert "bestiary Honeyhog".equals(BestiaryRowAction.commandForRow(mobs, 0, 0, 2));
        assert "bestiary Poisonous Water Worm".equals(BestiaryRowAction.commandForRow(mobs, 1, 1, 2));
        assert BestiaryRowAction.commandForRow(mobs, -1, 0, 2) == null;
        assert BestiaryRowAction.commandForRow(mobs, 2, 0, 2) == null;
        assert BestiaryRowAction.commandForRow(mobs, 1, 2, 2) == null;
        assert BestiaryRowAction.commandForRow(List.of(), 0, 0, 12) == null;
        assert BestiaryRowAction.commandForRow(mobs, 0, Integer.MAX_VALUE, 12) == null;
        assert BestiaryRowAction.commandForRow(List.of(mob("Honeyhog\nwarp hub")), 0, 0, 1) == null;
        assert BestiaryRowAction.commandForRow(List.of(mob(" ")), 0, 0, 1) == null;
        System.out.println("Bestiary row action checks passed");
    }

    private static MobEntry mob(String name) {
        return new MobEntry("Fishing/" + name, name, "Fishing", 0, 0, 1, 100, 0);
    }
}

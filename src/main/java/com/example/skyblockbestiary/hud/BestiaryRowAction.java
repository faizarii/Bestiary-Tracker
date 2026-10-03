package com.example.skyblockbestiary.hud;

import com.example.skyblockbestiary.data.BestiaryScanner.MobEntry;

import java.util.List;

final class BestiaryRowAction {
    static String commandForRow(List<MobEntry> mobs, int row, int offset, int visibleRows) {
        long index = (long) offset + row;
        if (row < 0 || row >= visibleRows || offset < 0 || index >= mobs.size()) return null;
        String name = mobs.get((int) index).name().trim();
        if (name.isEmpty() || name.length() > 247 || name.codePoints().anyMatch(Character::isISOControl)) return null;
        return "bestiary " + name;
    }
}

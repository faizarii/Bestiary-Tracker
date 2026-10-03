package com.example.skyblockbestiary.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BestiaryTierMessages {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern TIER_UP = Pattern.compile("^(.+?)\\s+(\\d{1,4})\\s*➡\\uFE0F?\\s*(\\d{1,4})$");
    private long headerTime;
    private boolean awaitingTier;

    TierUp accept(String message, long nowMillis) {
        String text = FORMATTING.matcher(message).replaceAll("").trim();
        if (text.equals("BESTIARY")) {
            headerTime = nowMillis;
            awaitingTier = true;
            return null;
        }
        if (!awaitingTier) return null;
        if (nowMillis - headerTime > 10_000 || nowMillis < headerTime) {
            reset();
            return null;
        }
        if (text.isEmpty()) return null;
        reset();
        Matcher matcher = TIER_UP.matcher(text);
        if (!matcher.matches()) return null;
        int previous = Integer.parseInt(matcher.group(2));
        int tier = Integer.parseInt(matcher.group(3));
        return tier > previous ? new TierUp(matcher.group(1), previous, tier) : null;
    }

    void reset() {
        awaitingTier = false;
    }

    static int menuTier(String suffix) {
        if (suffix == null) return 0;
        if (suffix.chars().allMatch(Character::isDigit)) return Integer.parseInt(suffix);
        int result = 0;
        int previous = 0;
        for (int i = suffix.length() - 1; i >= 0; i--) {
            int value = switch (suffix.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> throw new IllegalArgumentException("Invalid tier: " + suffix);
            };
            result += value < previous ? -value : value;
            previous = value;
        }
        return result;
    }

    record TierUp(String name, int previous, int tier) {}
}

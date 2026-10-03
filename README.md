# Skyblock Bestiary Tracker

A Fabric client mod for Hypixel Skyblock that reads your Bestiary menu and shows a HUD with the mobs closest to their next tier (or full completion).

## What it does

- Scans the Bestiary chest GUI as you browse it and remembers kill counts per mob.
- Shows a draggable HUD panel (toggleable) ranking mobs by how many kills are left.
- Toggle between "next tier" and "full completion" ranking modes from the in-menu panel.
- Saves progress and mob icons (including head textures) to `config/skyblock-bestiary-tracker.json` across restarts.
- Recognizes Bestiary tier-up announcements and saves the confirmed tier for previously scanned mobs.

## Requirements

- Minecraft 26.1.2
- Fabric Loader >= 0.19.5
- Fabric API >= 0.155.3+26.1.2
- Java 25

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.

## Usage

Open the Bestiary menu in-game. A transparent tracker appears to the left of the inventory, showing your unlocked/maxed counts and the mobs nearest completion. Compact rows fit 13 mobs beside a six-row inventory at normal tracker scale. Scroll over the list to see more. The inventory moves right when necessary, and the tracker scales down if the window is too narrow.

Click `[HUD: OFF]` to enable the standalone ten-row HUD. With the HUD enabled, a preview stays visible in chest menus at its saved position. Drag its `Bestiary HUD (drag)` title to reposition it. Positions are clamped to the screen so the HUD stays reachable after changing window size or GUI scale.

Click a mob row in the menu tracker to run `/bestiary <mob name>` and open Bestiary filtered to that mob.

If you used a version that did not save icons, visit the mob pages once to capture their heads. After that, they load with your saved progress on startup.

Bestiary level-up chat updates the confirmed tier without opening a menu. Those rows show `Tier N*`, with a hover hint to refresh progress, and sort below rows with known remaining kills. Opening the mob page refreshes its exact counts and next target. Messages for unscanned mobs or names shared by multiple cached families are ignored because the announcement does not identify the area. Kill counts between tiers and overall unlocked/maxed totals still need menu scans.

## Notes

Parsing relies on the exact Bestiary lore text format Hypixel currently uses. If Hypixel changes the item lore layout, the regexes in `BestiaryScanner` will need updating.

API sync is not implemented. [Hypixel's API policy](https://developer.hypixel.net/policies) prohibits entering API keys into public mods. Automatic API updates would require a registered backend that keeps the application's key server-side, caches responses, and respects the API's usage rules.

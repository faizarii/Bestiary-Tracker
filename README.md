# Skyblock Bestiary Tracker

A Fabric client mod for Hypixel Skyblock that reads your Bestiary menu and shows a HUD with the mobs closest to their next tier (or full completion).

## What it does

- Scans the Bestiary chest GUI as you browse it and remembers kill counts per mob.
- Shows a draggable HUD panel (toggleable) ranking mobs by how many kills are left.
- Toggle between "next tier" and "full completion" ranking modes from the in-menu panel.
- Persists data to `config/skyblock-bestiary-tracker.json` so progress survives restarts.

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

Open the Bestiary menu in-game. A panel appears next to the inventory showing your unlocked/maxed counts and the mobs nearest completion. Click "HUD: ON" to pin a smaller version of the panel to your screen outside menus; drag it by the title bar to reposition.

## Notes

Parsing relies on the exact Bestiary lore text format Hypixel currently uses. If Hypixel changes the item lore layout, the regexes in `BestiaryScanner` will need updating.

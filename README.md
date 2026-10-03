# Bestiary Tracker

![GUI Example](https://cdn.modrinth.com/data/cached_images/e3def2165e352e5d08a91762fc1fa08d331916b9.jpeg)

A client-side Fabric mod for Hypixel SkyBlock that shows which Bestiary mobs need the fewest kills to reach their next tier or full completion.

## Features

- Transparent, compact overlay beside the Bestiary menu.
- Mob icons and current/target progress.
- Switch between next-tier and completion sorting.
- Click a mob row to open Bestiary filtered to that mob.
- Hover hints showing what each row opens.
- Optional draggable HUD outside menus.
- Saved progress across restarts.
- Bestiary level-up messages update confirmed tiers automatically.

## Requirements

- Minecraft **26.1.2**
- Fabric Loader **0.19.5 or newer**
- Fabric API **0.155.3+26.1.2** or a compatible newer version
- Java **25**

## Installation

Install Fabric Loader, then place this mod’s `.jar` and Fabric API in your Minecraft instance’s `mods` folder.

## Getting started

Open `/bestiary` and browse the mob pages to populate the tracker.

Use `[Next Tier]` to switch sorting modes and `[HUD: OFF]` to enable the standalone HUD. With the HUD enabled, open a chest or Bestiary menu and drag the **Bestiary HUD (drag)** title to move it.

Scroll over the menu overlay to see more mobs. Click a row to open its filtered Bestiary menu.

## How progress updates

The tracker reads kill counts from the Bestiary pages you open. It does not count individual kills or fetch your profile through the Hypixel API.

Bestiary level-up messages update confirmed tiers for previously scanned mobs. A row marked `Tier N*` needs a menu refresh to obtain its exact kill count and next target.

Saved progress loads automatically when you restart Minecraft.

## Issues and source

Report bugs on [GitHub](https://github.com/faizarii/Bestiary-Tracker/issues). Include your Minecraft version, mod version, and steps to reproduce the problem.

[Source code](https://github.com/faizarii/Bestiary-Tracker)

Not affiliated with or endorsed by Hypixel.

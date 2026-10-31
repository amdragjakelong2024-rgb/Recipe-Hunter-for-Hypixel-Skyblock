# Recipe Hunter 0.4.4

**Minecraft 26.1.2 · Fabric · Java 25**

Recipe Hunter 0.4.4 is the public release of the feature set previously available as **0.4.3 Test Build 2**. The project version and Fabric metadata have been updated to 0.4.4 and the release has been rebuilt and checked from its release commit.

## What's New

### Settings and Themes
- Dedicated **SETTINGS** screen, accessible through the book's gear button or `/rh settings`.
- Persistent General, Forge, Notifications and Display options with Back navigation.
- Light and Dark parchment themes applied consistently to the Recipe Book, saved goals, Settings, Diary, Calculator, Forge screens, HUDs and tooltips.
- Theme selection and supported preferences persist between sessions.

### Forge Dashboard
- Added `/rh forge` to inspect up to seven observed Forge slots.
- Added `/rh forge hide` and `/rh forge show`; hiding the HUD does not stop tracking.
- Added `/rh forge slot1 info` through `/rh forge slot7 info`.
- Displays observed item names and Busy / Ready / Empty / Locked / Unknown states.
- Parallel-job wait time uses the longest remaining observed timer.
- Timer expiry is not treated as server-confirmed collection readiness. Unknown slots remain unknown.

### Next-Craft Preview and Notifications
- Hover a tracked ingredient for three continuous seconds to inspect the next catalogue-defined craft.
- Shows required ingredients, known ownership, missing quantities and Ready to Craft / Not Ready.
- Active unfinished saved goals can notify: `You can craft now <ITEM_NAME>`.
- Craft Ready / Goal Reached / Both, notification sound and Forge alerts are configurable.
- Notifications are edge-triggered to avoid repeated spam.
- Uncollected Forge output is never counted as physical inventory.

### HUD and Material Display
- Change Material Display directly from the interactive HUD: Compact, Enchanted or Enchanted Block where supported.
- Change Gemstone Display directly from the HUD: Fine, Flawless or Perfect.
- Existing progress is preserved when switching display tiers.
- Incomplete percentages are not rounded up to 100%; positive progress below 1% displays `<1%`.

### Armor Set Tracker
- Combine recognized four-piece armor sets into one saved goal and shared HUD.
- Keep Helmet, Chestplate, Leggings and Boots individually accessible in the recipe tree.
- Aggregate material requirements across all four pieces and allocate shared stock only once.
- Split a set to restore the individual HUDs.
- Final Destination catalogue totals for the four-piece set: **1,536 Null Spheres, 384 Enchanted Obsidian and 768 Enchanted Redstone Dust**.

### Performance
- Reuse a shared read-only inventory icon index rather than rescanning inventory for every rendered icon.
- Reuse inventory observations across saved goals.
- Cache recipe and planner calculations when relevant stock, Forge reservations and goal settings have not changed.
- Preserve live inventory observation every five client ticks and slower Forge / WorldContext / optional SkyHanni polling every twenty ticks.
- Added `/rh perf` CPU timings and cache diagnostics.

## Data Freshness and Known Limitations
- Inventory observation normally refreshes every five client ticks.
- Sack tracking depends on itemized Hypixel Sack Notifications or an exact supported SkyHanni active-profile balance. Hypixel may batch notifications; updates occur when the server message arrives.
- Initial Sack contents are unknown until a baseline is observed. The mod does not query authenticated Hypixel profile APIs.
- Ender Chest, island chest and Forge information refresh when their menus or supported visible widgets are observed. Unopened remote storage cannot be read by the client.
- Forge output remains reserved/pending until collection is observed. Unknown slots are not guessed.
- `/rh perf` measures CPU-side tracker/HUD timings, not GPU frame time or actual FPS. Actual FPS gains require a controlled in-game A/B comparison.
- Automated and native UI tests do not replace live Hypixel gameplay acceptance testing. Server-specific message formats and optional SkyHanni compatibility may vary.

## Installation
1. Use Minecraft **26.1.2** with Fabric Loader and Fabric API.
2. Close Minecraft.
3. Back up `config/recipehunter/`.
4. Replace the older Recipe Hunter JAR with **`recipe-hunter-0.4.4+26.1.2.jar`**.
5. Keep only one Recipe Hunter JAR in the `mods` folder.
6. Launch Minecraft and use `/rh`.

## Verification
The release workflow rebuilds the exact release commit with Java 25 and Gradle 9.7.0, runs the catalogue scope check and `gradle clean build plannerTest`, and requires the native Minecraft UI smoke markers before publishing the JAR and SHA-256 checksum.

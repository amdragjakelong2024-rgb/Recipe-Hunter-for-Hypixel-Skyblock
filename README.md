# Recipe Hunter

A client-side recipe book and live material tracker for **Hypixel SkyBlock**.

**Minecraft 26.1.2 · Fabric · Java 25 · Version 0.4.3**

## Test Build 2

See [Test Build 2 notes](RELEASE_NOTES.md). Use the top-right gear or `/rh settings` for persistent settings and Light/Dark themes. Material and gemstone controls are also available on HUDs while the cursor is open. Armor recipes offer **Track full armor set**; **Split set** restores the original individual HUDs. `/rh forge hide` and `/rh forge show` change visibility without stopping tracking. `/rh forge slot1 info` through `slot7 info` describe observed slots.

## Install

Install Fabric and Fabric API for Minecraft 26.1.2. Put the release JAR in your instance's `mods` folder, replacing the previous Recipe Hunter JAR. Keep only one copy. Back up `config/recipehunter/` before upgrading.

## Recipe book

- `/rh` opens **All Recipes**. Search, choose a recipe, set its quantity and select **Save as Goal**.
- `/rh book` shows saved goals with Hide/Show, Remove and ordering controls. Internal navigation is immediate; ESC closes the book.
- Material Display offers Compact, Enchanted and Enchanted Block where supported. Gemstone Display offers the recorded gemstone tiers. Display changes preserve progress and non-gemstone requirements.
- Use `-`, `+`, an exact quantity or **Reset x1**. Expanding a material shows its ingredient chain.
- Hide completed filters observed finished items and recorded craft history. Owned only uses finished items, never ingredient readiness.
- Saving a recipe starts an additional craft: an older copy of its final item does not fill the new goal. Existing usable ingredients still count.
- Hover icons for recipe previews; click an icon to request the corresponding Hypixel recipe. Server unlocks and recipe availability still apply.

## Live counters

Inventory is sampled and HUD progress recomputed every five client ticks (normally four times per second). The mod reads SkyBlock item metadata, exact catalogue names for server-named items, and matching vanilla material IDs for untagged resources.

**Sack Notifications must be enabled** for collection going straight into Sacks. The mod reads per-item additions/removals from notification hover text. Hypixel batches those messages, so Sack counts update when the message arrives, not on every individual block break. The aggregate headline alone cannot identify materials.

Open a Sack once for its historical total. Before a baseline scan, only observed session changes are known. An ambiguous notification overlapping a menu snapshot makes the absolute balance unknown. After the overlap window, further itemized messages resume a conservative observed count; a new scan restores the absolute total. Ender Chest, island chest and Forge data refresh when their respective menus are observed; the client cannot see unopened remote storage.

Raw and compacted variants contribute equivalent progress using recorded recipes. Moving items between observed sources does not add another copy after both source updates arrive. Server updates can arrive at different times. Each saved goal compares against the same observed stock independently; it does not reserve resources away from other goals. Crafting consumes that stock and all affected counters recalculate.

Owned intermediate items cover their recipe branch; their historical ingredients are removed from remaining acquisition rows, never presented as loose stock. For example, an observed X455 removes its embedded 60 Jewels from an X555 goal: with no loose Jewels, the remaining upgrade shows 0/50, not 60/110. This rule applies to every recipe. `Known` means observed allocated items; `Eq.` includes recipe-equivalent material progress.

The mod does not query authenticated Hypixel profile APIs. Inventory is live; unopened remote storage is unknown, and observed closed storage remains a snapshot. `/rh resync` clears session evidence when a rescan is needed. Exact server messages saying you have no item in Sacks replace that Sack balance with zero. Approximate menu amounts are not treated as exact quantities.

Checklist marks and old manual quantity overrides do not create inventory. Forge jobs and explicit diary reservations remain separate from owned materials. A full material-equivalence bar is not proof that intermediate crafts, Forge waiting or unlock requirements are complete.

Press **E** or **T** to use the HUD's `<` and `>` arrows. Each page displays seven materials. Pages never advance automatically. In inventory, drag a panel's title to move it and scroll over it to resize.

## Craft Diary

`/rh diary` provides multiline notes, Edit, done checkboxes, Include/Exclude and Delete. Draft text and the edit target survive navigation and restart. Empty entries are not saved. Paging stops at either end.

An explicit note such as `forge 2 Refined Mithril 4h` records a local reservation and countdown. Ambiguous time ranges remain ordinary notes. Editing the same duration does not restart the countdown. Marking a note Done removes its reservation; it does not invent a collected item. Duplicate diary/Forge observations use the larger pending total, not their sum.

The mod has no AI assistant, provider chat or API-key configuration. Obsolete provider credential files owned by this mod are removed locally on startup without reading or sending their contents.

## Commands

| Command | Action |
| --- | --- |
| `/rh` | All Recipes |
| `/rh book` | Saved recipes |
| `/rh <name>` | Search recipes |
| `/rh current` | Active goal and a clickable recipe link |
| `/rh diary` | Craft Diary |
| `/rh calc` | Arithmetic and optional mining estimate |
| `/rh gui` | HUD position and size |
| `/rh settings` | Appearance and observation settings |
| `/rh save`, `/rh hide`, `/rh show` | Last-selected goal controls |
| `/rh bigger`, `/rh smaller` | Book size |
| `/rh resync` | Clear observations and scan live inventory again; reopen storage pages for their balances |
| `/rh sackstatus` | Sack notification diagnostics |
| `/rh status <ITEM_ID>` | Counts and freshness by observed source |
| `/rh help` | Command list |

The optional calculator's mining estimate uses supplied speed, fortune, block strength, base drops and uptime. It excludes Pristine, RNG and unobserved conditions. It is not an exact completion-time prediction.

## Data and verification

The bundled catalogue is a SkyBlock-only NEU snapshot. Custom heads need texture downloads; server-specific item models require the relevant resource pack. No third-party pack is bundled. Storage observations reset on reconnect or detected profile change. Saved goals and diary entries are scoped to the Minecraft account; this is not a complete per-SkyBlock-profile archive.

Build with **JDK 25 / Gradle 9.7.0**:

```sh
gradle build plannerTest
```

CI checks the catalogue, recipe calculations, inventory/Sack event sequences, compaction, repeat goals, diary persistence, and native Minecraft screen input. Synthetic events and offline client tests do not constitute a Hypixel server session. `/rh sackstatus` and `/rh status` help identify server-format or synchronisation problems.

Code: MIT (`LICENSE`). Catalogue: NEU (`NEU-DATA-LICENSE.txt`). Existing source history is retained. Routine CI creates test artifacts and does not publish releases automatically.

### Restarting a recipe
Use **Finish** to mark the saved goal done, then **Reset recipe** to start another craft with the same quantity and display settings. The initial owned output is excluded; newly collected or compacted output counts. Existing ingredient stock is kept. The search field uses larger text and the book has an animated RECIPE HUNTER title.

### Forge progress hotfix (0.4.2, 2026-10-28)
Observed running/ready Forge jobs now cover their recipe inputs in the acquisition list. Five Refined Mithril jobs reduce the X555 Enchanted Mithril demand from 8,000 to 7,200, without calling those inputs loose stock. Claiming the output preserves remaining needs. Each HUD row shows `Need` and an approximate number of 64-item stacks; the footer shows allocated Forge output count.

Open The Forge to synchronise its slots. A visible Forge tab-list widget can also update exact slot observations without reopening the menu. Unreadable/absent slots preserve their last observation; only an explicit empty/locked slot clears a job. Ready jobs remain pending until collection is observed. Closed menus and absent tab widgets cannot provide live server evidence.

## Performance diagnostics (0.4.3)

Use `/rh perf` for CPU-only timings from the material tracker and HUD rendering submissions. This is not an FPS benchmark and does not measure GPU work or other mods. Compare FPS in the same SkyBlock location with the same graphics settings and a single Recipe Hunter JAR enabled/disabled.

HUD item icons now reuse a read-only inventory lookup per client tick instead of scanning the inventory for every icon on every rendered frame. The inventory is still checked every five client ticks; expensive recipe/planner graph recalculations are skipped while stock, Forge reservations and saved goal settings remain unchanged. Forge readiness alerts continue on a periodic heartbeat.

## 0.4.3 Forge dashboard

Use `/rh forge` for observed Forge slots and countdowns; drag the preview and scroll to resize the persistent HUD. `HUD: On/Off` and `Alerts: On/Off` are saved. Unknown slots are never treated as free or locked. All-job wait time is the longest remaining observed timer.

Hover recipe materials for three seconds for next-step ingredients and missing quantities. Readiness notifications apply to steps in saved active recipe goals and use physical ingredients, not uncollected Forge jobs. Unlock requirements still apply.

Sack updates are read from server packets independently of chat visibility. If compatible SkyHanni is installed, its exact active-profile Sack balances are reused without adding duplicate counts. Otherwise itemized Sack notifications update observed balances; unseen initial contents remain unknown. `/rh sackstatus` reports the active source. No API credentials, session tokens or automatic inventory clicks are used.

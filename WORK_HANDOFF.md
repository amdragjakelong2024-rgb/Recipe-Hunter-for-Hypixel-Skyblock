# Work handoff: Recipe Hunter 0.4.1 — fix GUI and progress before public release

Repository: https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock

## Publishing and coordination

- User requires **NO public release** until all changes below have been implemented and tested. The workflow `.github/workflows/build.yml` was frozen: it runs builds and uploads CI artifacts but does not update public GitHub Releases.
- **Keep version 0.4.1** for the eventual unified public release. The existing Latest 0.4.1 must not be overwritten until signoff. Do not create 0.4.4 or advertise untested builds.
- Fetch the current `main` commit before editing. Other work may be in progress. Merge or rebase any unfinished Work changes carefully; never force-push or silently overwrite the latest bug fixes.
- The owner also wants confusing public releases 0.4.2/0.4.3 cleaned up, with 0.4.1 the single current release. First inventory release/tag/assets, back up exact source references, and remove only superseded artifacts using an authorized release-management method; preserve Git commit history.
- A compilation passing does not constitute in-game verification. Report what was actually tested and do not change an unverified “0%” into an invented percentage.

## Current problem (screenshot 25 Oct 2026)

Detail screen shows black horizontal rows and `Collapse`/`Full` at bottom; they are confusing and not useful. Current planner in `Planner.java` deliberately stops recipe expansion at certain compact tiers, and UI mixes the selected display unit with the actual material demand. The displayed recipe has no clear “this material is needed for this component” structure.

### Replace the controls

At the bottom of the recipe/detail view, replace `Collapse` and `Full` with two clear controls:

1. **Material display**: `Compact` | `Enchanted` | `Enchanted Block` (only show valid representations for the material). Use accessible grouped buttons or selector with clearly highlighted active selection.
2. **Gemstone display**: `Fine` | `Flawless` | `Perfect` (only valid choices, based on the actual gemstone and recipe). The player can swap at any moment.

Display choice must be **presentation only**, not alter the actual requirements, recipe route, saved goal, or collected stock. Persist the display preference per goal (and optionally per material) safely in `Book`. Both selectors must respond immediately and correctly. Do not offer made-up tiers, e.g. no "Perfect" representation for unrelated non-gemstone materials.

### Display totals as useful units

- If the target requires **35 Enchanted Diamond Blocks**, and their verified route requires **160 Enchanted Diamonds per block**, show `35 blocks = 5,600 Enchanted Diamonds = 87 stacks + 32`. Allow swapping back to `35 Enchanted Diamond Blocks` without changing the target.
- If grinding **1 Flawless Ruby**, let the player see progress and the target in Fine Ruby *stacks*, and back in Flawless Ruby. For Perfect Jade, allow `Fine` ↔ `Flawless` ↔ `Perfect` with other necessary ingredients (e.g. crystals) remaining separate.
- Use `Catalog.Route` input/output counts, not hardcoded guesses. If multiple recipes/routes are possible, respect selected route. Preserve non-convertible ingredients, multipliers, batches, crafting yield, and leftovers.
- Display `owned / required`, percentage, stacked count `X stacks + Y`, and optionally equivalent crafted units. Use thousands separators. A tiny positive amount should not display as fake zero; never round unfinished progress to 100%.

### Explain the recipe dependency graph

For each item, show where it is used: e.g. `Enchanted Mithril → Refined Mithril → Mithril Plate → Titanium Drill` (the real selected recipe path). Make intermediate steps expandable on click with icons and their individual counts. Show the active path with `Needed for: [intermediate]` or equivalent, not an unreadable global All Steps dump.

Display actual icons, item labels and progress directly on parchment, without black or blue full-width row backgrounds; use readable typography and align within the open-book pages. Keep the book centered smaller than the full screen and show the real game behind it, not a generated background.

## Live progress is a separate MUST-FIX

User mined raw Mithril while tracking Enchanted Mithril; HUD stayed `0/960 (0%)`. The newer raw-to-enchanted calculation in `MaterialProgress.java` is not sufficient if raw Mithril never reaches the inventory/sacks stock model.

- Trace live data from inventory, Sacks (including raw `MITHRIL_ORE` if that is the catalog ID), Ender Chest and forged intermediates. `Ownership.java` + `MenuTracker.java` + `SackChatTracker.java` + `SackChanges.java` need real-world verification against Hypixel's current messages/menus.
- Avoid counting an item twice when moving from inventory to Sacks, raw to enchanted, enchanted to block, or combining saved chest snapshots with delta notifications.
- Where Hypixel doesn't expose a live Sacks balance, distinguish **known stock**, **session changes**, and **unknown/unscanned stock**, and explain how to sync (opening relevant Sacks). No false real-time guarantees.
- Refresh HUD promptly. Progress state must be independent of display tier; changing the tier cannot reset progress.
- Verify ingredient ID mapping, actual chat hover formats, tick frequency, and multiple goals/progress allocation.
- Add automated tests for full and partial materials, raw + enchanted + block stock, multi-step gemstone conversions, switch display preferences, checked items, Forge reservations, two goals sharing stock, and sack snapshot/delta overlap.
- **In-game test** raw Mithril entering inventory and Sacks, ordinary ore turning into Enchanted Mithril, and selected unit switching while the counter continues to rise.

## Saved goals and HUD

- Multiple saved recipes/goals must be allowed simultaneously.
- `HIDE` must hide that particular goal's HUD panel, not remove/delete the saved goal or make an inert button. `SHOW` must restore it; save across restarts. Each visible goal gets its own independently positioned HUD panel, avoiding overlapping the others.
- Saved goals can be reordered and have per-goal count, progress, and active chosen display unit.

## Acceptance before release

1. A recipe requiring Enchanted/Enchanted Block/Fine/Flawless/Perfect shows correct equivalent amounts and **stack + remainder** units after switching.
2. Raw drops count progressively toward enchanted requirements; crafting/redepositing doesn't lose or duplicate progress.
3. Dependencies show clearly which intermediate uses each input.
4. Book UI doesn't overflow text/buttons; no ugly black ingredient-row backgrounds; clean Minecraft recipe-book look.
5. Multiple HUD goals and Hide/Show work independently.
6. Automated Gradle build and `plannerTest` pass; manual in-game tests are documented.
7. Public 0.4.1 is updated only after explicit user approval. Remove confusing old releases separately through authorized tools without deleting commits.

## Required final report

Give the owner a concise summary of changed files, exact behavior verified, test results, screenshots if in-game verification exists, unresolved limitations, CI artifact link, and explicitly ask for authorization **before** publishing any public release.


## 2026-10-25 live sack diagnostic patch (unreleased)

A separate `update/live-sacks-20261025` branch improves plus/minus-prefixed `[Sacks]` hover parsing, checks ID mapping for Mithril and records incoming-notification counts. Run `/rh sackstatus` in-game while mining to distinguish notifications disabled, hover data unreadable, or successful parsing. This is **not** a fully instant packet-level sack feed: Hypixel batches its chat notifications; the user must enable Hypixel Chat Feedback → Sack Notifications, but should not need to open the sack for each refresh. Verify the messages truly reach `ClientReceiveMessageEvents.GAME` and check real in-game behavior before merging to `main` or publishing.

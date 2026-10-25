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


## Global live tracking requirement (25 October 2026)

**BLOCKER / mandatory acceptance requirement:** The user wants a **universal, item-driven live material tracker covering all recipes, not just Mithril**. It must handle every supported catalogue ingredient that can be legitimately observed, across mining, farming, fishing, foraging, combat, slayer, events, dungeons, gemstone gathering, forge, mob drops and other acquisition paths. Do not ship or describe this as working for *all* items merely because a Mithril regression passes.

### Architecture
- Build one generic inventory/acquisition material ledger indexed by stable Hypixel SkyBlock item ID, not by hand-coded mining or Mithril special cases. Cross-reference the full `Catalog`, normalize known naming variants and aliases, and use exact metadata IDs whenever available.
- Collect signals from itemized Sacks notifications (any sack category), live player inventory slots/stack NBT, and other client-visible evidence of item gain. Catch partial deposits, sacks-full inventory overflow, auto-compaction, enchanted upgrades, stack transfers, picked-up drops, chest withdrawals, forge output, and legitimate rare-drop chat where reliable IDs and quantities are available. Never rely on only one source.
- **A message or scoreboard that says only "+N items" is not enough to infer which materials were gained.** Do not guess. Do not count mining powder, coins, collection XP or SkyBlock experience as physical craft items.
- Avoid double counting when the same gain is reflected by both an inventory delta and a Sack/chat delta, or when crafting converts raw resources to enchanted form, stacking, withdrawing, depositing or reconnecting. Use explicitly modeled transitions and source-aware reconciliation.
- Separate (a) absolute **currently owned** stock (known/unknown) from (b) **session-acquired** resource totals and from (c) **recipe progress equivalence**. A temporary spent resource may still represent historical collection, but it cannot simultaneously be counted as still owned. Clearly label each metric and never fake ownership.
- The progress engine must traverse actual selected catalogue recipes: tier conversions (raw, enchanted, enchanted block), gemstones (rough through perfect), crafts with **multiple inputs**, forge ingredients, route alternatives, per-craft output multipliers, rounding, remaining ingredients, and checked steps. Selected display tier changes the representation only, never the underlying progress.
- For source data not exposed to clients in real time, visibly mark it `unknown / untracked` or `last synced`, rather than displaying misleading 0%. The requested ideal is no manual Sack reopening. If Hypixel sends batched signals, update after each real notification and disclose the delay.
- Event/rare items should use their actual unique item IDs and known quantities; no invented random drop counts or misleading assumptions of drop ownership.

### Mandatory in-game and automated acceptance matrix
- Mining: Mithril, Titanium, Coal, Diamond, Redstone, Gold, gemstone Rough/Fine, enchanted items and blocks.
- Farming: wheat, carrot, potato, sugar cane, nether wart and common auto-compactor upgrades.
- Fishing: raw fish/cod, salmon, ink sac, lily pads and fishing sack acquisitions.
- Foraging: regular and enchanted logs.
- Combat/mob drops: bones, rotten flesh, ender pearls, spider drops and loot entering inventory or corresponding sacks.
- Forge, crystals, multi-input rare/event materials, recipe alternatives and stack-size conversions.
- Enchanted diamond block target converted to enchanted diamond stacks; flawless ruby shown as fine stacks; perfect jade switch between fine/flawless/perfect while retaining crystal costs.
- Inventory-only, Sacks-only, combined inventory+Sacks, bags full/overflowing, deposit/withdrawal, crafting, profile switching, disconnect/reconnect, and multiple goals sharing stock.
- Maintain a fixture-driven matrix over **all catalogue item IDs and crafting graph routes**, reporting unmapped/ambiguous/unsupported cases. Test representative in-game captures from each resource category; synthetic unit tests alone do not prove universality.
- Include diagnostics showing source / last successful update / item ID / quantity for debug. Report which source types were tested live and which remain unverified.

**Publish gate:** Until this generic tracker and UI have passed the acceptance matrix, leave the public release frozen at 0.4.1. Existing branch `update/live-sacks-20261025` holds only a partial Sack parser/diagnostic change; it does NOT meet the universal acceptance requirement.

## Catalogue scope

Recipe lookup and planning must use the bundled Hypixel SkyBlock (NEU) catalogue only. Do not import the Minecraft recipe registry. Vanilla render icons do not establish recipe availability. Keep shared SkyBlock materials such as Cobblestone and Diamond; exclude Netherite and Ancient Debris. CI runs `scripts/check_catalog_scope.py` to catch accidental vanilla imports.
## 2026-10-25 live sack diagnostic patch (unreleased)

A separate `update/live-sacks-20261025` branch improves plus/minus-prefixed `[Sacks]` hover parsing, checks ID mapping for Mithril and records incoming-notification counts. Run `/rh sackstatus` in-game while mining to distinguish notifications disabled, hover data unreadable, or successful parsing. This is **not** a fully instant packet-level sack feed: Hypixel batches its chat notifications; the user must enable Hypixel Chat Feedback → Sack Notifications, but should not need to open the sack for each refresh. Verify the messages truly reach `ClientReceiveMessageEvents.GAME` and check real in-game behavior before merging to `main` or publishing.

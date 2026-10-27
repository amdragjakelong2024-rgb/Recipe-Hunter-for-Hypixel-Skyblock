# Recipe Hunter 0.4.1 development build

This branch is a test build, not the public release. Automatic publishing remains disabled.

## Implemented in this branch

- Integrates `update/live-sacks-20261025`, including signed itemized Sack tooltips and `/rh sackstatus`.
- Resolves Sack labels only when a catalogue name is unambiguous; exact SkyBlock metadata IDs remain authoritative.
- Tracks inventory, Sack observations, Ender Chest snapshots and island storage separately. `/rh status ITEM_ID` prints source quantities, quality and update times.
- A Sack snapshot overlapping a batched message marks only that item uncertain. It cannot suppress unrelated resource categories or credit the overlapping batch twice.
- Catalogue-driven selected recipe trees allocate stock once across queued goals. Display choices do not mutate stock or target quantities.
- Raw/enchanted/block and single-input gemstone compression contribute fractional progress. Multicomponent crafts require every input; they cannot complete from one ingredient alone.
- Forge and diary reservations reduce remaining acquisition separately from current ownership. Pending Forge output never becomes inventory merely because its timer expires.
- Fixes legacy decimal quantity suffixes in Forge ingredient IDs and in the catalogue importer.
- Separate saved-recipe and all-recipe views, category filters, exact quantity/reset, material selectors, ingredient paths, branch expansion, embedded crafting grid and Save as Goal.
- Centered parchment book, dark binding, no full-screen replacement background; real world remains behind all book/tool screens. Native book opening/closing and page transitions respect animation/sound settings.
- Multiple independently positioned HUDs. Pages change only on right click in inventory/HUD editor, never on a timer. Visibility, placement, page and display preference persist.
- Preserves diary editing, calculator, provider settings and AI chat.

## Important limits

- No authenticated Minecraft/Hypixel client is available in this development environment. Visual layout, animation, menu recognition and real message formats remain unverified in game.
- All-catalogue graph/ID tests are synthetic coverage, not proof that every acquisition source sends observable client data.
- Unopened storage has no known absolute balance. Itemized Sack messages provide observed changes, not the historical Sack total. Batched messages update only when Hypixel delivers them.
- Overlapping Sack snapshot/batch windows cannot be split exactly without server timestamps. The absolute total remains Unknown; after the overlap window, later itemized deltas resume a conservative observed lower bound. Reopen the Sack to restore the exact total.
- Session-acquired totals are reported Unknown: inventory gains can be withdrawals, gifts or crafting outputs. The mod does not invent a grind total from them.
- Ender Chest/island storage and Forge are last-observed snapshots, not remote server polling. Reopen them after unseen external changes. Forge inventory claims require in-game validation.
- The animation uses Minecraft's book model plus a page transition. Skin-specific first-person hands are not implemented.
- Automatic source aliases are intentionally not guessed for ambiguous catalogue names. Exact metadata IDs still work.

## In-game acceptance

Use a separate instance/profile configuration and replace the prior mod JAR; never load both JARs together.

1. Open `/rh`: world remains visible around the book. Check both screenshots' two-page arrangement at your normal GUI scale and at 1280x720, 1920x1080 and ultrawide resolutions. Open/close/page sounds and animations should be smooth.
2. Save a 960 Enchanted Mithril goal. Mine raw Mithril into inventory, then Sacks with Sack Notifications enabled. Run `/rh sackstatus` and `/rh status MITHRIL_ORE`. Record the real tooltip if parsing fails.
3. Compare raw, enchanted and compressed stock before/after Auto-Compactor, depositing, withdrawing and inventory overflow. Progress should follow the equivalent material, not reset at each tier.
4. Repeat with wheat/carrot/potato, fish/salmon, logs, bones/rotten flesh/pearls, Slayer and dungeon loot. Record exact item IDs and source counts; don't use aggregate chat as an item identity.
5. A 35 Enchanted Diamond Block goal must show 5,600 Enchanted Diamonds (87 stacks + 32). Switch units repeatedly; target quantity and progress stay unchanged.
6. Flawless Ruby must show its real Fine recipe quantity. Perfect Jade must keep its crystal cost in Fine/Flawless/Perfect views. Try a recipe with an alternative route and a multi-output recipe.
7. Put five Refined Mithril in Forge. Scan Forge and confirm separate reservations. Claim the outputs and check inventory/pending quantities do not overlap. Test a matching diary reservation too.
8. Save two goals sharing a resource. Each goal independently compares current stock; the counters are not reservations for simultaneous crafts. Hide/show, reorder, drag and resize each HUD independently. Use the panel arrows with the inventory/chat cursor to change its page. Leave it open while mining: no timed page switching.
9. Restart and reconnect; saved goals/layout/unit selections remain, session storage evidence resets. Switch SkyBlock profile and confirm previous profile stock is not credited.
10. Type and edit a diary entry, toggle done, save it, restart. Confirm that no AI/provider settings remain.

11. Press Finish on a saved goal, then Reset recipe. Quantity and layout remain, completion clears, old output is excluded. Mine another 160 Mithril and allow compaction; the new enchanted piece must preserve progress. Restart the game and confirm reset state persists.
12. Check the larger Search text, click at both ends, type a long query, select and replace it. RECIPE HUNTER should shimmer above the book without delaying navigation.

Record expected/observed counts and screenshots for failures. Public release stays blocked until live acceptance and explicit approval.

## Files changed

Core: `Catalog.java`, `RecipeProgress.java`, `MaterialProgress.java`, `MaterialLedger.java`, `RecipeHunter.java`, `Book.java`, `scripts/make_catalog.py`.

Source adapters: `SackChanges.java`, `SackChatTracker.java`, `MenuTracker.java`.

Interface: `RecipeBookScreen.java`, `HunterScreen.java`, `ToolsScreen.java`, `HudPanels.java`, `HudLayout.java` (existing layout model retained), `HudEditor.java`, `InventoryOverlay.java`, `BookOpening.java`, `GlowButton.java`, `Theme.java`.

Verification: `PlannerTest.java`, `ProgressTest.java`, `.github/workflows/build.yml`, `WORK_HANDOFF.md`, this checklist. The CI artifact also contains `catalogue-progress.txt`.

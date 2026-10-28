# Recipe Hunter 0.4.2 — correct observed ownership

Minecraft 26.1.2 / Fabric / Java 25. Replace the previous mod JAR; keep one copy.

- Fix all recipe trees: materials embedded in owned intermediate items are no longer shown as loose inventory. X455 → X555 with no loose Jewels now needs 0/50 Jewels; old drill-only coal/redstone/treasurite costs disappear.
- Separate observed physical quantities from equivalent crafting progress; preserve shared stock allocation across repeated recipe branches.
- Recognize exact empty-Sack server messages, stabilize Ender Chest page keys, exclude its navigation row, reject approximate Sack quantities, and add `/rh resync`.
- Keep live inventory refresh, itemized Sack notifications, reset after Finish, larger search text and animated RECIPE HUNTER heading.
- Forge tracking hotfix: show active Forge production separately from owned inventory, credit committed ingredients without creating phantom physical stock, and distinguish unfinished work from items still to grind.
- Debounce transient Forge menu updates so HUD values do not flicker. A claimed job cannot be re-created from an unchanged slot snapshot.

Open each Sack and Ender Chest page once for the starting balance; enable Sack Notifications. Inventory/HUD recomputes every five ticks. Sacks update when Hypixel sends batched per-item messages. Closed storage and Forge are last-observed snapshots; unopened storage is unknown. Open The Forge to refresh detected jobs. In-progress work is not owned stock until the actual output is observed in inventory. No authenticated Hypixel API integration is included.

Validation: whole-catalogue regression tests, all-route physical-stock invariants, mining/transfer/compaction fixtures, and an offline native Minecraft UI test including a real tagged X455 ItemStack. This is not a live Hypixel server acceptance test. Forge regression scenarios cover the full Forge catalogue.

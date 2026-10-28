# Recipe Hunter 0.4.2 — correct observed ownership

Minecraft 26.1.2 / Fabric / Java 25. Replace the previous mod JAR; keep one copy.

- Fix all recipe trees: materials embedded in owned intermediate items are no longer shown as loose inventory. X455 → X555 with no loose Jewels now needs 0/50 Jewels; old drill-only coal/redstone/treasurite costs disappear.
- Separate observed physical quantities from equivalent crafting progress; preserve shared stock allocation across repeated recipe branches.
- Recognize exact empty-Sack server messages, stabilize Ender Chest page keys, exclude its navigation row, reject approximate Sack quantities, and add `/rh resync`.
- Keep live inventory refresh, itemized Sack notifications, reset after Finish, larger search text and animated RECIPE HUNTER heading.

Open each Sack and Ender Chest page once for the starting balance; enable Sack Notifications. Inventory/HUD recomputes every five ticks. Sacks update when Hypixel sends batched per-item messages. Closed storage is a snapshot; unopened storage is unknown. No authenticated Hypixel API integration is included.

Validation: whole-catalogue regression tests, all-route physical-stock invariants, mining/transfer/compaction fixtures, and an offline native Minecraft UI test including a real tagged X455 ItemStack. This is not a live Hypixel server acceptance test.

## 2026-10-28 replacement build — Forge progress hotfix

Re-download the JAR and replace your previous 0.4.2 file. The version number is unchanged.

- Running and ready Forge outputs cover their recipe ingredients across all routes, without becoming physical inventory. Five Refined Mithril jobs reduce X555 Enchanted Mithril demand from 8,000 to 7,200.
- Amber bars distinguish Forge work from owned items. Slot-by-slot Forge tracking waits for stable observations during menu loading; explicit empty or locked slots clear them. The Forge tab widget also updates visible jobs.
- An unchanged menu snapshot cannot recreate a claimed job. Claiming a job preserves remaining material needs instead of counting the output and old job twice after slot/inventory updates arrive.
- HUD and recipe details show missing quantities and approximate 64-item stacks.

Open The Forge once after installing to synchronise jobs. When neither the menu nor the Forge tab widget is available, the mod only has its last observation. Tested with 923 Forge lifecycle assertions across all 120 catalogue Forge routes, the full regression suite and an offline Minecraft UI/HUD run. Live Hypixel acceptance remains unverified.

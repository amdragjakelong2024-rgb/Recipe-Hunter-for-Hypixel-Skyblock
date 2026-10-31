# Recipe Hunter 0.4.3 – TEST BUILD 2

Minecraft 26.1.2 / Fabric / Java 25. Internal version remains 0.4.3. Test artifact only; no public release.

- Dedicated SETTINGS page, accessible through the book gear or `/rh settings`, with persistent General, Forge, Notifications and Display controls and Back navigation.
- Persistent Light/Dark parchment theme throughout the book, goals, settings, diary, calculator, Forge, HUDs and recipe tooltips.
- `/rh forge hide`, `/rh forge show` and `/rh forge slot1 info` through `slot7 info`. Hiding the HUD keeps tracking active. Concurrent countdowns use the longest remaining observed time. Timer expiry is distinguished from server-confirmed Ready to Collect.
- Three-second next-craft hover shows the selected recipe's complete ingredients, known ownership, craftable equivalents and missing inputs. Uncollected Forge output never becomes physical ownership.
- Newly craftable unfinished saved goals notify `You can craft now <ITEM_NAME>`. Craft Ready / Goal Reached / Both, notification sound and Forge alerts are independently configurable. Goal Reached requires the final item or recorded completion, rather than ingredient equivalents alone.
- Material and gemstone display controls directly on recipe HUDs while the cursor is available. Ordinary hidden-cursor gameplay clicks remain untouched.
- Integer progress percentages truncate incomplete progress; positive progress below 1% displays `<1%`.
- Recognized four-piece armor sets combine into one goal/HUD, retaining individual recipe access and original saved goals. Split set restores individual HUD visibility. Shared stock is allocated once. Final Destination catalogue totals: 1,536 Null Spheres, 384 Enchanted Obsidian and 768 Enchanted Redstone.
- Shared stock/icon caches and existing polling intervals preserved. Next-craft readiness and book display lists are cached. `/rh perf` remains available.

## Data freshness and test limitations

Inventory observation runs every five client ticks. Forge, WorldContext and optional read-only SkyHanni polling remain once per 20 ticks; pet menus remain every five ticks. Sack changes depend on itemized server notifications or exact SkyHanni balances. An unseen initial Sack/storage balance remains unknown; old storage observations are not guaranteed live. No API key or fabricated ownership is used.

Automated coverage includes catalogue consistency, planner/accounting/notification regressions, armor aggregation and persistence round trips, plus a native Minecraft startup/UI smoke suite. Native UI scenarios include both themes, settings navigation and 1/4/8 HUD diagnostics. Offline diagnostics are not gameplay CPU/FPS benchmarks. Live Hypixel behavior and actual FPS require player acceptance testing before any public release.

Install only `recipe-hunter-0.4.3-TEST-2.jar`, alongside the required Fabric API, replacing your prior Recipe Hunter JAR. Keep one copy and back up `config/recipehunter/` first.

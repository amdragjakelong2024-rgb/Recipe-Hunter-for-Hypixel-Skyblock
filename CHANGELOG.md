# Recipe Hunter 0.4.4

Minecraft 26.1.2 / Fabric / Java 25. Public release.

Version 0.4.4 is the public release of the feature set previously distributed as 0.4.3 Test Build 2. The build and Fabric metadata are now consistently versioned 0.4.4; the tested feature set is otherwise unchanged.

- Dedicated SETTINGS page, accessible through the book gear or `/rh settings`, with persistent General, Forge, Notifications and Display controls and Back navigation.
- Persistent Light/Dark parchment theme throughout the book, goals, settings, diary, calculator, Forge, HUDs and recipe tooltips.
- `/rh forge hide`, `/rh forge show` and `/rh forge slot1 info` through `slot7 info`. Hiding the HUD keeps tracking active. Concurrent countdowns use the longest remaining observed time. Timer expiry is distinguished from server-confirmed Ready to Collect.
- Three-second next-craft hover shows the selected recipe's complete ingredients, known ownership, craftable equivalents and missing inputs. Uncollected Forge output never becomes physical ownership.
- Newly craftable unfinished saved goals notify `You can craft now <ITEM_NAME>`. Craft Ready / Goal Reached / Both, notification sound and Forge alerts are independently configurable. Goal Reached requires the final item or recorded completion, rather than ingredient equivalents alone.
- Material and gemstone display controls directly on recipe HUDs while the cursor is available. Ordinary hidden-cursor gameplay clicks remain untouched.
- Integer progress percentages truncate incomplete progress; positive progress below 1% displays `<1%`.
- Recognized four-piece armor sets combine into one goal/HUD, retaining individual recipe access and original saved goals. Split set restores individual HUD visibility. Shared stock is allocated once. Final Destination catalogue totals: 1,536 Null Spheres, 384 Enchanted Obsidian and 768 Enchanted Redstone.
- Shared stock/icon caches and existing polling intervals preserved. Next-craft readiness and book display lists are cached. `/rh perf` remains available.

# Changelog

## 0.4.4 — Public release

- Promoted the successfully tested 0.4.3 Test Build 2 feature set to a public release.
- Updated Gradle and Fabric mod metadata to report version 0.4.4.
- Rebuilt and reran the regression and native Minecraft UI smoke suites for the release commit.

## 0.4.3 — Forge dashboard and crafting milestones (development)

- Added `/rh forge` to preview seven observed Forge slots, local countdowns, and parallel-job wait time.
- Added an independently movable, resizable and persistent Forge HUD with visibility and alert toggles.
- Added three-second hover previews for the next catalogue-defined crafting/Forge step, with missing ingredients.
- Added debounced material-ready and Forge-ready notifications; only unfinished active saved goals trigger material milestones.
- Observes itemized Sack updates from incoming server packets, including messages hidden by another chat mod.
- Added an optional read-only SkyHanni active-profile Sack balance bridge; exact balances replace, rather than duplicate, server deltas.
- Keeps unknown Forge slots unknown; never treats queued Forge output as inventory.
- Shows the installed mod version in small type; interface and alerts are in English.
- Extended offline regression, full-catalogue and native Minecraft GUI smoke tests.
- **Limitations:** Live Hypixel formats and optional SkyHanni compatibility still need in-game testing. A successful synthetic test does not guarantee a live server observation.

## 0.4.3 – Performance maintenance

- Avoided per-frame inventory rescans and repeated item-stack copies for every rendered HUD icon.
- Shared one inventory observation across all saved goals in each material update.
- Added change detection to reuse recipe and planner calculations when material stock, Forge reservations and goal settings have not changed.
- Kept live inventory observation at four updates per second and retained Forge notification checks during idle periods.
- Added `/rh perf` CPU-only update and HUD submission timings for in-game diagnosis.
- Added cache invalidation tests for stock changes, display preferences and Forge jobs.
- Real FPS impact still needs a controlled in-game A/B comparison.

## 0.4.1

See [release notes](RELEASE_NOTES.md) for the original 0.4.1 release history.

- Centered recipe book, accurate search/input bounds and direct navigation.
- Per-goal HUD controls, seven material rows and manual page selection.
- Observed live material identity, Sack notifications, raw/compacted equivalence and independent goal counters.
- Additional-craft goals exclude previous finished copies; checklist marks cannot invent ownership.
- Craft Diary editing, drafts, done states, reservations and stable countdowns.
- Removed AI and provider integrations.

## 0.3.1

- Persistent ingredient checklists.

## 0.3.0

- Recipe book, material planning, movable HUD and ownership observations.

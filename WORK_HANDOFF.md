# RECIPE HUNTER 0.4.3 — TEST BUILD 2 WORK HANDOFF

**Owner-approved development target:** the **final candidate for 0.4.3**, distributed as **TEST BUILD 2**, not a public release. Do not bump to 0.4.4. Do not publish GitHub Releases or change the current public release until the owner installs the test JAR, verifies that all requested features work, and **explicitly authorizes publishing**.

## Repository and precise starting point

- Repository: https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock
- **CONTINUE FROM:** branch `update/performance-043-20261030` (this handoff and the performance fixes).
- Prior 0.4.3 Forge branch: `update/forge-dashboard-043`. It does **not** include the new performance optimizations until merged intentionally.
- `main` / public release remain older; do **not** use `main` or public v0.4.2 as the new starting point.
- Read `README.md`, `RELEASE_NOTES.md`, `CHANGELOG.md`, `WORK_HANDOFF.md`, the newest GitHub Actions runs and diff against `update/forge-dashboard-043` before editing.
- Preserve all existing source history and settings. Never force-push or overwrite somebody else's newer commits.
- Source and UI language, commands, notifications, documentation, and patch notes: **ENGLISH ONLY**, including any future contributions.

## Status: FPS / performance fixes ALREADY IMPLEMENTED on this branch

These are **code optimizations, not verified FPS gains**. User reports that Recipe Hunter may be lowering FPS, especially when multiple goal HUDs are active.

1. `Icons.java`: moved from scanning every inventory stack **for every rendered item icon** to a shared read-only inventory icon index rebuilt once per client tick (per active player); avoids repeated item ID resolution and `ItemStack.copy()` per HUD icon per frame. Resource-manager changes clear built icons. Check for safe invalidation on inventory updates, player replacement, texture/resource reload and cache mutation.
2. `Ownership.java` + `RecipeHunter.java`: reuse one inventory snapshot for ledger and per-goal stock, rather than scanning items repeatedly during each progress refresh. Existing live scans remain every **5 client ticks** (usually 4 Hz).
3. `ProgressRefreshPolicy.java` + `RecipeHunter.java`: compare observed item stock, Forge/diary reservations, and saved goal settings before re-expanding large recipe trees / planner graphs; reuse cached progress when unchanged. Forge countdown/readiness alerts continue on periodic heartbeat. Explicit persistence and join/disconnect invalidate caches. Test for any missed invalidation on profile switch, `/rh resync`, incoming Sack changes, automatic compaction, changes in recipe options, and Forge job transitions.
4. `RecipeHunter.java`: `/rh perf` displays last/peak **CPU timing** for material refresh and HUD submission plus cache rebuild/reuse counts and visible HUD count. **It does not measure actual GPU frame time or actual FPS**. Keep this diagnostic.
5. `src/test/java/dev/recipehunter/ProgressTest.java`: regression checks that new inventory material counts, changed display modes, new/claimed Forge jobs and cache reset force reevaluation, while unchanged inputs are reused.
6. `.github/workflows/build.yml`: continuous integration stays **test-artifact-only**. A concurrency group was added to cancel superseded runs for the same branch. Never re-enable automatic publishing.
7. README and CHANGELOG document these optimization changes, limitations and `/rh perf`.

**Important:** As of writing this handoff, CI runs for these FPS changes were queued and a complete successful post-optimization build has **NOT YET BEEN CONFIRMED**. The earlier, pre-optimization 0.4.3 Forge build passed. Check the latest commit's actual CI outcome, fix any compile/test failures, run a native Minecraft UI smoke test, and do not claim the optimized Test 2 JAR is verified until it succeeds.

## Mandatory owner-requested 0.4.3 Test Build 2 finishing work

The owner also gave a separate detailed **Settings / HUD redesign request**. These requirements are **not all implemented yet**; do not present them as finished. They must be implemented/verified before describing Test Build 2 as a final candidate.

### Clean SETTINGS page
- Replace the cluttered screenshot's settings view with a dedicated, large `SETTINGS` heading, clean sections and short labels. Preserve working scan/backend features, but remove unrelated chest/pet explanations, search field and old disorganized layout **from the settings screen**.
- Make SETTINGS accessible by `/rh settings` AND a top-right **gear icon** from `/rh`, with a proper Back action.
- General: Sound Effects, UI Animations, Celebrations/Music.
- Forge: Forge HUD Show/Hide, Forge Notifications.
- Notifications: Crafting Notifications On/Off, trigger Craft Ready / Goal Reached / Both, notification sound.
- Display: Material and Gemstone tiers, HUD scale, theme Light/Dark. Save every change across restarts.

### Forge commands, visibility and timing
- `/rh forge hide` / `/rh forge show`: hide/show persistent Forge HUD **without stopping tracking**.
- `/rh forge slot1 info` through `slot7 info`: readable item name, busy/ready/empty/locked/unknown status, and exact **observed** time until completion; unseen slots remain Unknown (no made-up data).
- Replace `All observed jobs` with `Finished in:`; for concurrent jobs use **max remaining time**, not the sum. Show `Ready to Collect` when appropriate.

### Three-second next-craft hover and notifications
- Simplify hover text after **3 continuous seconds** over tracked ingredients. Show `Next Craft: [ITEM]`, `Requires`, `You Have`, `Missing`, `Ready to Craft` / `Not Ready`. No `branch`, `graph`, `route` jargon. Match the **actual selected catalogue recipe**, including mixed-input recipes and entire ingredient requirements. Fit within screen bounds.
- On a *rising edge* of material readiness for active, unfinished saved goals, chat must say exactly `You can craft now <ITEM_NAME>`. Do not spam or claim queued Forge output is already physical inventory. Allow user to disable / choose notification triggers.

### Quick HUD display selection and readable progress
- Let user change per-goal `Material Display` (Compact / Enchanted / Enchanted Block) and `Gemstone Display` (Fine / Flawless / Perfect) **directly from the interactive HUD/inventory overlay** without reopening `/rh`. No gameplay click interception with cursor hidden.
- Show integer progress percentages with truncation: `2.87%` → `2%`, `99.99%` → `99%`, 100 only for truly complete, and `<1%` for positive sub-1% progress. **Keep internal progress precise** and clarify raw-vs-equivalent item quantities.
- Add a real persistent **Dark Mode** for the parchment Recipe Book, Saved Goals, SETTINGS, Diary, Calculator, Forge screen, recipe/Forge HUDs and tooltips. Keep consistent readable contrast, do not darken the actual Minecraft world, and do not replace the book with generic black panels.
- Keep existing saved goals, resource conversions, live inventory/Sacks, optional read-only SkyHanni bridge, diary, Forge accounting, the duplicate-material HUD fix, and independent HUD panels intact.

## Performance test / acceptance procedure

- First run `gradle build plannerTest`, catalogue checks and native client UI smoke test. Inspect logs and screenshots. Correct compilation and runtime regressions.
- Prefer an A/B in-game comparison in **the same SkyBlock location, render distance, Minecraft settings, resource packs, mods and active saved goals**: Recipe Hunter disabled vs public 0.4.2 vs pre-optimization 0.4.3 vs **0.4.3 Test 2**. Allow several seconds for warm-up; record median/1% low FPS and visible goal count if possible. This needs owner in-game testing; CI cannot prove the real FPS impact.
- Have the user run `/rh perf` while stationary with 1, 4, and 8 goal HUDs, then moving/mining with material updates, Sacks receiving batched notifications, Forge running and Forge HUD shown/hidden. Document tracker/HUD last/peak CPU timings and whether counts update without manual Sack reopening.
- Verify icons preserve game-supplied NBT/custom textures, Inventory stays live and refreshes on same-tick changes, reloading resource packs invalidates caches, switching profiles/reconnecting clears known stock, Forge changes and save/edit operations invalidate planner caches, and notifications do not disappear or spam.
- Check multi-goal cost scaling, no duplicate item ownership, correct multi-tier material equivalence, no fake zero for unknown remote stock, and all menus stable at different GUI scales.
- Work within Minecraft 26.1.2 / Fabric / Java 25.
- No invented benchmark metrics. A successful synthetic test is not an in-game Hypixel acceptance test.

## Deliverables and owner-controlled release gate

1. Keep version string and artifact metadata at **0.4.3**. Mark the installer externally as `recipe-hunter-0.4.3-TEST-2.jar` (the version itself is still `0.4.3`).
2. Provide **one verified installable JAR**, plus an optional ZIP and a GitHub Actions artifact link. Confirm exact commit SHA, integrity, and what CI actually tested.
3. Provide an English Test Build 2 changelog distinguishing implemented vs not implemented and known limits.
4. Commit source changes on `update/performance-043-20261030` (or branch from its latest commit, and provide its precise branch name). Do not lose the FPS fixes.
5. Do **NOT** create a GitHub Release, edit public release assets or move public tags. Do **NOT** merge to `main` solely to publish a test.
6. **Only after the owner personally tests 0.4.3 Test Build 2, confirms everything works, and explicitly requests the release**, may Work publish 0.4.3. This permission is NOT granted by this handoff or by asking for a test JAR.

**Priority:** complete and verify the performance fixes, finish the previously requested Settings/HUD work, deliver the **0.4.3 TEST BUILD 2**, then stop before any public release and wait for the owner's explicit go-ahead.

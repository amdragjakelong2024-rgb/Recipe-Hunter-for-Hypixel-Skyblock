# RECIPE HUNTER 0.4.4 — RELEASE HANDOFF

**Superseding owner authorization (2026-10-31):** The owner explicitly requested that the successfully tested 0.4.3 Test Build 2 source be versioned as **0.4.4** and published as a public GitHub Release. This supersedes the original 0.4.3 test-only release freeze below. Preserve the tested feature set; update the actual Gradle and Fabric metadata to 0.4.4, rebuild the exact release commit, and publish only after the fresh release workflow passes.

## Repository and precise starting point

- Repository: https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock
- **FPS fix tested source commit:** `2215832dc8e60074e4641671b23556c4d2dbabc8`; successful CI [41102450015](https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock/actions/runs/41102450015). Handoff-only commits after that may advance the branch head; always fetch current HEAD before editing.
- **Release source:** branch `update/test2-settings-043`, tested commit `349bba21c1e82e1152aeb66bc813f59059728d53`. The release branch `release/0.4.4` is created from that exact commit.
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
8. **Final FPS fix on `2215832...`**: `RecipeHunter.java` now scans WorldContext/scoreboard, Forge Tab widget and optional SkyHanni Sack bridge **once every 20 ticks (roughly once per second)**, rather than every 5–10 ticks; Pet menus are scanned every 5 ticks rather than every tick. **Inventory and recipe-progress stock observation remains every five ticks (roughly 4 Hz)** and incoming Sack packets are still event-driven. Do not undo the throttling or claim that the SkyHanni/Forge polls are real-time sub-second.
9. At the time of the original handoff, public `v0.4.2` was current and `0.4.3` was TEST BUILD 2. On 2026-10-31 the owner explicitly authorized promoting that tested feature set to public `v0.4.4`.

**Verified FPS fix build (30 October 2026):** GitHub Actions run [41102450015](https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock/actions/runs/41102450015) completed successfully for source commit `2215832dc8e60074e4641671b23556c4d2dbabc8`. It built an installable `0.4.3` JAR, ran automated tests and native Minecraft GUI smoke tests, and uploaded artifact `recipe-hunter-0.4.3-mc26.1.2` (artifact ID `13000750577`). The owner was given an FPS-focused test JAR named `Recipe-Hunter-0.4.3-FPS-FIX-TEST-2.jar`. The successful build is for **the FPS fixes only**, NOT confirmation that the SETTINGS/Dark Mode/Forge command redesign below has been implemented, nor an actual Hypixel FPS benchmark. Changes to this handoff after commit `2215832...` are documentation-only. If Work modifies code, it must rebuild and rerun all tests on the new exact commit.

## 0.4.3 Test Build 2 implementation included in the 0.4.4 release

The owner gave a detailed **Settings / HUD redesign request**. These items were implemented on `update/test2-settings-043` and covered by successful automated and native Minecraft UI checks in run [41177823055](https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock/actions/runs/41177823055). Live Hypixel behavior and real FPS gains still require player-side testing.

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
- Prefer an A/B in-game comparison in **the same SkyBlock location, render distance, Minecraft settings, resource packs, mods and active saved goals**: Recipe Hunter disabled vs public 0.4.2 vs pre-optimization 0.4.3 vs **0.4.4**. Allow several seconds for warm-up; record median/1% low FPS and visible goal count if possible. This needs owner in-game testing; CI cannot prove the real FPS impact.
- Have the user run `/rh perf` while stationary with 1, 4, and 8 goal HUDs, then moving/mining with material updates, Sacks receiving batched notifications, Forge running and Forge HUD shown/hidden. Document tracker/HUD last/peak CPU timings and whether counts update without manual Sack reopening.
- Verify icons preserve game-supplied NBT/custom textures, Inventory stays live and refreshes on same-tick changes, reloading resource packs invalidates caches, switching profiles/reconnecting clears known stock, Forge changes and save/edit operations invalidate planner caches, and notifications do not disappear or spam.
- Check multi-goal cost scaling, no duplicate item ownership, correct multi-tier material equivalence, no fake zero for unknown remote stock, and all menus stable at different GUI scales.
- Work within Minecraft 26.1.2 / Fabric / Java 25.
- No invented benchmark metrics. A successful synthetic test is not an in-game Hypixel acceptance test.

## 0.4.4 release deliverables and status

1. Internal Gradle and Fabric mod version: **0.4.4**. Public JAR filename: `recipe-hunter-0.4.4+26.1.2.jar`.
2. Build the release commit with JDK 25 / Gradle 9.7.0, run catalogue validation, `gradle clean build plannerTest`, and the native Minecraft UI smoke test. Do not publish if any required check fails.
3. Publish the JAR and its SHA-256 checksum as assets on GitHub Release **v0.4.4**. Keep all public text and patch notes in English.
4. Release branch: `release/0.4.4`, based on tested commit `349bba21c1e82e1152aeb66bc813f59059728d53`. Preserve the entire source history and FPS fixes; never force-push.
5. The owner explicitly authorized this public GitHub release on 2026-10-31. This authorization applies to GitHub v0.4.4 only; do not publish to Modrinth or other platforms without separate approval.
6. Regular development CI remains test-artifact-only. Only the dedicated release workflow on the designated release branch may publish v0.4.4.

**Priority:** preserve the tested Test Build 2 feature set, make the actual project and Fabric metadata consistently report 0.4.4, rebuild and retest the exact release commit, then publish the JAR and checksum on GitHub.

## Test Build 2 implementation continuation

Release candidate `0.4.4` descends from tested commit `349bba21c1e82e1152aeb66bc813f59059728d53` on `update/test2-settings-043`. The release branch updates only version metadata and documentation, then rebuilds the exact source. The release workflow requires catalogue validation, regression tests and the extended native UI smoke markers before creating GitHub Release `v0.4.4`. Live Hypixel acceptance and FPS benchmarking remain user testing tasks.

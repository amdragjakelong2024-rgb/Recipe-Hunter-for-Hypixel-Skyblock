# Recipe Hunter 0.4.1 development state

Repository: https://github.com/amdragjakelong2024-rgb/Recipe-Hunter-for-Hypixel-Skyblock
Development branch: `update/complete-041`.

The owner authorised replacing the public 0.4.1 after the current fixes and checks. Keep the version 0.4.1, consolidate its old patch notes, preserve source commits and never force-push. Routine CI must remain build/test-only.

Current work removes the assistant/provider integration, repairs native diary input and draft persistence, and checks the live ownership-to-HUD path. Saved goals are independent comparisons against observed stock, not reservations against each other. Additional craft goals must not reuse a previous finished copy. Checklists and manual stock overrides are not observed ownership.

Before publication: pass `gradle build plannerTest`, native Minecraft UI smoke tests (including diary interactions and real ItemStack identity), inspect the resulting screenshots and JAR, and record the tested source SHA. Do not describe synthetic event tests as an actual Hypixel session.

`README.md` documents the current controls and storage limitations. `RELEASE_NOTES.md` is the consolidated public release description. `/rh sackstatus` and `/rh status <ITEM_ID>` remain available for server-side diagnosis.

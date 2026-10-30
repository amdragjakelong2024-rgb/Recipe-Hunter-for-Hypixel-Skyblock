# Recipe Hunter 0.4.3 — Forge dashboard and recipe milestones

Minecraft 26.1.2 / Fabric / Java 25. Keep only one Recipe Hunter JAR installed.

- `/rh forge` previews observed Forge slots, live countdowns and the time until all observed jobs finish. Concurrent jobs use the longest remaining timer, not the sum.
- A separate Forge HUD stays visible while playing. Drag and resize it in `/rh forge`; position, size, visibility and alert preferences are saved.
- Notifications for materials sufficient for the first craft of a tracked recipe step, Forge jobs ready to collect, and a full Forge. Missing unlocks are not assumed; queued output is not spendable stock.
- Hover a material row or item icon for two seconds to see the actual next recipe, its required ingredients, owned amounts and missing quantities. Mixed recipes check all inputs. Recipe relationships come from the catalogue, not guessed item-name tiers.
- Recipe HUD clicks cover the header and displayed material rows without intercepting clicks in the unused background. Use `/rh gui` to move recipe panels.
- Small version text in the book and Forge HUD. All interface text is English.
- Sack messages are read from incoming server packets even if another mod hides their chat display. An optional read-only SkyHanni bridge imports only exact balances from its active profile, replacing rather than duplicating the same source.

## Data freshness

Forge is observed from its menu and the Forge Tab widget; countdowns then run locally. Unknown slots remain unknown. A full warning requires all seven slot states to be observed, including locked slots. Enable the Forge widget for remote server updates.

Sacks use itemized server notifications and, when available, SkyHanni's exact profile balances. Enable Sack Notifications in Hypixel settings. Without an existing exact balance, Recipe Hunter can only track observed changes; it cannot recover an unseen initial total. Hidden or incomplete server data is not invented. There is no API-key requirement and no claim of zero-latency polling of Hypixel's web API.

Validation includes whole-catalogue regression tests, every crafting/Forge next-step route, parallel timer calculations, readiness notification edges, bridge deduplication, profile-independent resets and native Minecraft UI smoke tests. Live Hypixel acceptance is not performed by CI.

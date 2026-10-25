# Recipe Hunter

## 0.4.3 controls

- `/rh`: opens the native animated Minecraft book, then **Locate any recipe**, search, hide/show, remove and reorder your queue using the arrow buttons.
- `/rh book`: queued recipes and completed recipe collection. Only the first visible queue entry appears on the HUD.
- Compact materials retain Mithril Plate and other Forge intermediates. Inspect one and use **Expand inputs / Collapse** to choose the detail level. **Full** expands the compact Forge tiers. Pending Forge outputs reduce required acquisition after you open The Forge.
- Enter a target quantity directly or press **Reset x1**. Plus/minus still adjust by one.
- Press **E**: use **- HUD / + HUD / Reset HUD**, scroll anywhere over the panel to resize, or drag its title to move it. Right-click cycles materials. Click the panel body to open its target recipe on the server.
- Hover the active panel to see the complete target recipe. Click an item icon or the **In-game** button for `/viewrecipe`; server unlocks and recipe availability still apply.
- AI: select a provider, enter a supported chat model ID and key, then **Save settings**. **Test connection** checks authentication without generating paid text. **Send** and **Analyze goals** make paid requests and include the current goal context. Conversation lasts for this game session.
- Keys and provider settings survive restart in `config/recipehunter-private/`, encrypted with a local device key. Do not share this folder. Encryption does not protect against software that can read both files on your account. Keys are masked, never logged or bundled with the mod. Clear key removes the saved key for the selected provider.
- Custom heads require Minecraft texture downloads; custom server models require the corresponding resource pack. Missing packs fall back to the recorded vanilla item. No third-party texture pack is bundled.


Client-side Ironman recipe planner for **Minecraft 26.1.2 / Fabric / Java 25**.
The bundled NEU recipe catalogue is an offline snapshot. This update does not change Minecraft versions.

Opening, page navigation, quantity confirmation and verified crafts use Minecraft sounds. UI sound effects can be disabled in `/rh settings`; click sounds follow Minecraft audio settings.

## What's new

- **Compact materials:** keep Refined Mithril and enchanted blocks as ingredients instead of automatically expanding them into thousands of lower-tier items. Inspect an ingredient and select **Expand inputs**; **Full** restores the detailed collection-unit view.
- **Recipe hover:** hover a recipe row or its icon to see the recorded crafting grid. Forge and Kat recipes show labelled inputs rather than an invented crafting grid.
- **Quantity:** use `-`, `+`, **Reset x1**, or type a quantity and press **Set**. Counts and materials recalculate together.
- **Inventory access:** open your normal inventory with `E`. Recipe Hunter and HUD size buttons are available there. Drag the **header** of the active goal panel, scroll anywhere over it to resize, and right-click to change its materials page. Normal inventory slots remain usable.
- **Craft Diary:** `/rh diary` stores notes locally. An unambiguous entry such as `2 Refined Mithril forge 4h` becomes a pending reservation with a countdown. Ambiguous time ranges remain notes. Exclude or delete entries when cancelled or incorrect.
- **Storage:** read your opened Ender Chest pages, own sack menus, island chests and Pets pages directly from game items/lore, without screenshots.
- **Forge:** observe jobs in The Forge menu and subtract their outputs from ingredient acquisition needs. Pending outputs remain distinct from actual owned stock. The clock expiring does **not** mean a job was collected.
- **Repeatable goals:** first verified craft records a completion badge. Once the target batch is crafted it leaves the active book; search and save it again to repeat. Previously owned copies of a completed target do not satisfy a new repeat goal. `Finish` manually confirms the goal for recipes not recognised by automatic detection.
- **Grind recommendations:** the Grind tab ranks easiest-to-hardest (a disclosed heuristic based on known rates and requirements) and shortest-to-longest (unknown rates last). Navigation particles have been removed.
- **Calculator:** `/rh calc` supports arithmetic and an optional approximate Mining Speed/Fortune model with explicitly supplied block strength, base drops and uptime. No executable expressions.
- **Experimental external AI:** `/rh ai` supports OpenAI, Gemini and Grok via their official HTTPS endpoints. Enter a model ID and its provider's API key; requests happen only when pressing the labelled Send or Analyze button.

## Quick start

1. Install Fabric for Minecraft **26.1.2**, Java **25**, and Fabric API **0.155.3+26.1.2** or a compatible release.
2. Download `recipe-hunter-0.4.3+26.1.2.jar` from GitHub Releases and place it in `mods`. Do not install the `-sources.jar`.
3. Search using `/rh <item name>`, select a recipe, choose quantity, and Save.
4. Open your inventory with `E` to access the book or move goal panels.
5. Open the storage pages and Forge menu yourself to refresh the observations.
6. Use the Grind tab to record an inventory collection sample, or enter a measured items/hour rate on an ingredient's page. A sample needs at least 30 seconds with no menu open in the same location.

## What the observations mean

This is a passive client mod: it does not open menus, move items, mine or execute routes for you. The server does not continuously send every unopened chest/sack/Forge slot to the client. **Unopened storage is unknown.** Reopen a page after external changes. Menu format changes may require a parser update.

Storage scans reset on reconnect or detected profile switch. Notes remain on disk; pending diary reservations are disabled on a detected profile switch. Books remain scoped to the Minecraft account, as in earlier versions; they are not a full per-SkyBlock-profile archive.

The same item appearing in both the diary and Forge uses the **larger pending total**, rather than adding both sources. This avoids counting a manually described job twice. Independent unobserved jobs of the same item may consequently be undercounted until Forge is reopened. Confirmed collection reduces both pending sources. Jobs not automatically recognised can be excluded manually after collection.

A completed badge is separate from your checklist and from a saved recipe. Existing saved recipes are **not** migrated into completed achievements. Ingredient checkmarks are preserved on reconnect.

Each goal is an independent calculation; it does not reserve inventory against other queued goals.

## Time estimates and AI

Measured or entered collection rates take priority over the optional mining model. Sampling observes inventory increases while menus are closed: dropped-item pickups can inflate it, and sack collection/personal compaction can hide gains. Use a measured manual rate for those cases. A location/menu transition resets the active sample window.

The mod can read Mining Speed, Mining Fortune, Farming Fortune, Foraging Fortune, Pristine and Breaking Power from recognised own-stat menu lore. These are last-observed values, not a guarantee that every conditional buff is visible. Open the stats menu with the relevant equipment. The optional mining model uses Mining Speed and Fortune and user-supplied block assumptions. It is approximate, assumes a four-tick minimum and excludes instamining, abilities, Pristine, RNG, ping and travel beyond the supplied uptime. It should not be used as an exact gemstone prediction.

Forge time is shown separately as **serial base wait**, not active grind time; parallel slots and Quick Forge can shorten actual waiting. Unknown rates are shown as unknown. External AI is advisory and cannot edit counts, checklist state or the journal.

### API privacy and billing

- **ChatGPT Plus is not API credit.** OpenAI API calls have separate billing; other providers have their own quotas/billing.
- API key input is masked. Keys are saved in encrypted private local settings and are never written to the book, source repository or logs. The local device key must be kept private too.
- Fixed provider endpoints only; no arbitrary URLs, HTTP redirects or automatic retry/background request loop.
- A request sends goal summaries, observed stats and diary notes to the chosen provider. Do not put secrets in journal notes you intend to send.
- Provider/model selection is explicit. No provider key is included with the mod. A compatible model ID and available API quota are required.
- Requests have timeouts, a response-length check and a 1,200-token generation cap. The provider decides actual charges; the mod does not claim a price quote.
- Masking and locally saved storage do not protect against malicious software or another malicious mod running in the same process.

Provider references: [OpenAI Chat API](https://developers.openai.com/api/reference/resources/chat), [Gemini compatibility](https://ai.google.dev/gemini-api/docs/openai), [Grok Chat Completions](https://docs.x.ai/developers/model-capabilities/legacy/chat-completions).

## Commands

| Command | Action |
| --- | --- |
| `/rh` | Recipe launcher and active queue controls |
| `/rh book` | Queue and completed recipes |
| `/rh <name>` | Search catalogue |
| `/rh gui` | Dedicated HUD editor |
| `/rh diary` | Craft Diary |
| `/rh calc` | Calculator and mining estimate |
| `/rh ai` | Experimental provider settings / analysis |
| `/rh settings` | Appearance, celebrations, clear observations |
| `/rh assistant` | Toggle local recommendations |
| `/rh save`, `/rh hide`, `/rh show` | Current goal controls |

## Build

Use **JDK 25 and Gradle 9.7.0**. Loom 1.18.2 requires the Gradle 9.7 plugin API.

```sh
gradle build plannerTest
```

GitHub Actions builds the same targets and uploads the JAR and sources. The assertion suite covers recipe graph rounding, stock subtraction, compact ingredients, repeated goals' persistence, diary parsing, reservation deduplication, sack count parsing and arithmetic. Actual Hypixel menu compatibility and paid-provider authentication require in-game/account testing.

## Data and licensing

Code: MIT (`LICENSE`). Bundled recipe data: NEU (`NEU-DATA-LICENSE.txt`). Recipe previews preserve catalogue slot positions. Local book data is under `config/recipehunter/<Minecraft UUID>.json`; back it up before installing an update. Configuration, run files, builds and secrets are excluded from git.

Icon metadata is generated with `scripts/build_icons.py` from NEU commit `bc21f3b10afe7fb11b15569128cea08be72a8241`, with removed catalogue entries recovered from the original recipe snapshot. The bundled compressed metadata contains 8842 item definitions; synthetic planning-only entries use fallback icons.

## Book interface

The interface uses vanilla parchment, ink text, leather binding and XP green/gold accents. Esc closes it with the native book animation. `/rh diary` supports multiline notes, Edit, Delete and done checkboxes. A done diary entry no longer reserves Forge output; it does not add unverified items to your inventory.

The active HUD shows item icons and owned/required counters (for example `11/32`) with progress bars. Forge and manually planned quantities are shown separately. Inventory counters refresh every five client ticks. Storage is refreshed only while the relevant server menu is open. **Open in recipe** opens the server recipe.

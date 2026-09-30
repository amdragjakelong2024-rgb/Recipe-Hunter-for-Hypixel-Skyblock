# Recipe-Hunter-for-Hypixel-Skyblock
A Fabric mod for Hypixel SkyBlock that helps track crafting recipes and required materials.
# Recipe Hunter 0.3.0

An independent client-side Ironman recipe planner for Hypixel SkyBlock, on **Fabric / Minecraft 26.1.2**.

## Install

Use Java 25, Fabric Loader 0.19.3 or compatible newer, and Fabric API 0.155.3+26.1.2. Put the Recipe Hunter JAR in `mods`. Remove the previous Recipe Hunter JAR first; do not install two versions together. Existing books keep the same config location.

This is an experimental build. It compiles against the real Minecraft and Fabric dependencies and passes 88 automated checks. It has **not** been launched or verified against a live Hypixel session in the build environment.

## Commands

- `/rh` or `/rh book`: search, saved recipe book, Assistant toggle.
- `/rh <item name>`: fuzzy item search; pet rarity names are supported.
- `/rh save`: save the last selected goal.
- `/rh hide` / `/rh show`: hide/show the most recently saved goal.
- `/rh gui`: drag individual HUD panels; use the mouse wheel over a panel to resize it. Right-click a panel to browse its material pages. Escape saves and closes.
- `/rh settings`: interface size, default HUD size, celebrations, music, scan controls.
- `/rh bigger`, `/rh smaller`, `/zvetsit`, `/zmensit`: interface size. Czech accented aliases `/zvětšit` and `/zmenšit` also work.
- `/rh assistant`: toggle assistance. Use **Guide** on a specific item to select a destination.
- Escape closes the screen.

## Recipe planning

The offline catalogue contains 8,842 items and 2,892 crafting, Forge and Kat routes. Source snapshot: NEU repository, 2026-09-29, commit `f296ff3a6bf3131e8fc412c13df5c0adc3d85043`.

Materials stops at first-tier enchanted materials rather than expanding them into huge raw counts. A direct raw ingredient remains raw. The exact original recipes remain inspectable.

**Gems: Required** preserves the gem quality requested by each ingredient. For example, Boots of Divan retain the required Flawless Ruby; Gemstone Mixture ingredients retain Fine gems. **Gems: Fine** expands higher qualities down to Fine while keeping other ingredients, including crystals. Three Perfect Jade need 1,200 Fine Jade and three Jade Crystals before subtracting owned materials.

Shared requirements are added before craft-output rounding. Owned intermediate items remove their unneeded dependencies. Alternative recipes can be selected individually. Conversion cycles and absent data are reported rather than guessed.

## Reversible checkmarks

Click `[ ]` next to a material or recipe input to mark that ingredient complete for this goal. Its dependency branch disappears from the remaining list. A completed row remains in Materials so `[x]` can be unchecked at any time. These are manual checklist decisions, not claims that the server verified ownership. Checkmarks and gem preferences survive restarts. They are per goal, not a shared allocation across every goal.

## Inventory, pets and island chests

- Live inventory, armor and offhand use SkyBlock item metadata.
- Pet scanning is manual to open: type **`/pet`**, visit all pages, then return to Recipe Hunter. The mod does not send a pet command. Only pages actually loaded by the client can be recorded.
- On a recognized private island, **open each physical chest or barrel normally**. After the menu loads, its contents are recorded by the clicked block position. Reopening replaces that chest's snapshot instead of adding another copy. Both halves of a double chest use one identity.
- Inventory plus chest snapshots are used by recipe calculations. Reopen changed chests to update them; a co-op member's changes while the chest is closed cannot be observed.
- The mod cannot read unopened chests, closed sacks, offline profiles or an entire island remotely. It never opens containers or moves items for you.
- Scan caches are session-only and reset on reconnect or a recognized profile-switch message. Use **Forget chest scans** before changing to another private island if needed. Books are per Minecraft UUID; full automatic SkyBlock-profile isolation is not implemented.
- `Have` is an optional total owned amount for an item, not an amount to add on top of inventory. Clear removes this manual number. Lower-tier raw stock is not automatically converted into finished enchanted/Fine units.

## Assistant mode

Assistant is optional, initially off. It provides Ironman acquisition instructions and warp suggestions; it never recommends Auction House or Bazaar purchasing.

Curated guidance covers the main Crystal Hollows gemstone regions, Sludge Juice, the Necron's Handle / Hyperion route, and Young Dragon / Summoning Eye progression. Other items fall back to available source notes. Missing routes are explicitly reported.

A green particle trail can guide the player toward a nearby **visible** gemstone block in loaded Crystal Hollows terrain, or a visible Zealot. The local path search checks support, headroom and common hazards. It does not move, mine, attack, warp or solve obstacles. It does not scan through walls or locate unloaded structures. Search is bounded; it may fail where jumping, mining, flight, teleports or a more distant route would be necessary. It is not a claim of the fastest global route through a randomly generated lobby.

Eight eyes summon a random dragon type; neither Young Dragon nor leggings are guaranteed. Young Dragon Leggings also have a 70-fragment recipe. Hyperion requires additional materials beyond the Handle.

## Craft celebration

A short full-screen dimmed banner displays the acquired item's icon and “Congratulations! You crafted …”, with Minecraft's challenge-completion fanfare. It fades after about 5.5 seconds. It is a cosmetic client overlay, not a server achievement, and it does not take control of the mouse.

Current and previously saved goals remain eligible. Detection uses recognized server craft messages, or an inventory increase with ingredient consumption while crafting. Claimable Forge outputs are also observed. Merely taking an item out of a normal chest or clicking a manual checkmark is not treated as crafting. Servers or menus with unrecognized messages / metadata may not trigger the celebration. Both the banner and its sound can be disabled in `/rh settings`.

## Privacy and provenance

No telemetry, paid features, external AI service or API key. The mod reads client-visible game state and stores settings locally in `config/recipehunter/<Minecraft UUID>.json`.

Original code is predominantly AI-generated. Recipe data is from the MIT-licensed NotEnoughUpdates repository. The background is a supplied gameplay screenshot, not generated artwork. Minecraft assets and third-party data retain their respective rights. The included source and documentation are suitable for review; live-server validation remains outstanding.

## Build

Java 25 and Gradle 9.5: `gradle build plannerTest` with access to Fabric and Maven repositories. The included JAR is directly compiled against original unobfuscated Minecraft 26.1.2 classes. No Minecraft/Fabric dependency binaries are bundled.

Sources:
- https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO
- https://hypixelskyblock.minecraft.wiki/w/Mithril_Deposits
- https://hypixelskyblock.minecraft.wiki/w/Gemstone
- https://hypixelskyblock.minecraft.wiki/w/Young_Dragon_Armor
- https://hypixelskyblock.minecraft.wiki/w/Zealot
- https://hypixelskyblock.minecraft.wiki/w/Necron%27s_Handle

## HUD editor and recipe previews

Each goal has its own saved position and scale. Positions adapt to window size and stay on-screen. Normal play cycles three ingredient rows every six seconds. In the editor, pages stay still; right-click advances them. Hover a material to see a small recipe card with the recorded crafting slots and quantities. Forge and upgrade recipes show an ingredient arrangement labelled by process, not a crafting pattern. Items without recipes show source information. Icons use matching inventory stacks when available, otherwise representative Minecraft icons. Detailed names and all inputs remain available in the recipe book.

Mouse hovering requires a free cursor: open `/rh gui` or use the recipe book. During normal gameplay the mouse continues to control the camera.

## Contributing

MIT-licensed source is intended for public use. Please report reproducible bugs with Minecraft/Fabric versions and anonymized steps. Do not attach account configurations, server lists, tokens or full game logs. No contributor's private account data is part of the release. CI builds and runs the offline calculation tests; it does not verify a live SkyBlock session.

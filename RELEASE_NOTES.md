- Reworked the interface as a parchment recipe book with layered binding and experience-green accents.
- Added smooth native book opening/closing and Minecraft sound effects.
- Added multiline diary entries with editing and done checkboxes.
- Added live owned/required counters, item icons, progress bars and separate Forge quantities.
- Replaced long screen explanations with concise storage counters.
- Renamed the recipe link to Open in recipe.

**Minecraft 26.1.2 · Fabric · Java 25**

Replace the old mod with `recipe-hunter-0.4.1+26.1.2.jar`.
Storage and Forge counts refresh when their game menus are observed. Inventory progress refreshes four times per second. Visual/audio behavior still needs in-game verification.

**HUD progress fix:** Normal (unenchanted) ingredients now contribute fractional progress to enchanted and other single-input compression tiers. Counts show the actual crafted quantity plus observed lower-tier stock. No item is credited twice across the visible material rows. Sack quantities remain last-observed snapshots until their in-game menus are opened again.

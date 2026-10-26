## Recipe Hunter 0.4.1

**Minecraft 26.1.2 · Fabric · Java 25**

- Centered parchment recipe book with All Recipes, saved goals, search and immediate internal navigation.
- Live material counters with raw/enchanted equivalence, itemized Sack updates and seven-row HUD pages with manual arrows.
- Fixed additional crafts: old finished copies and checklist marks no longer fill a new goal. Other saved goals no longer reserve stock away from its counter.
- Improved recognition of untagged materials and nested Sack notifications.
- Repaired Craft Diary input, editing, checkboxes, draft persistence and countdown editing.
- Removed the AI assistant, chat and provider configuration.

Replace the previous JAR; keep only one Recipe Hunter version in `mods`.

**Live tracking:** inventory refreshes every five client ticks. Enable Hypixel Sack Notifications for direct-to-Sack collection; updates arrive in server batches. Open storage/Forge menus for their baseline observations. Unopened storage and unreadable or ambiguous messages cannot provide exact totals. Progress bars represent material equivalence, not completed Forge jobs or server unlocks.

Automated regression and offline Minecraft UI tests accompany this build. Live Hypixel session behaviour is not independently verified here. Use `/rh sackstatus` and `/rh status <ITEM_ID>` to inspect observations.

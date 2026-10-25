"""Reject accidental vanilla recipe imports in the bundled SkyBlock catalogue."""
import json
import pathlib

path = pathlib.Path(__file__).resolve().parents[1] / 'src/main/resources/assets/recipehunter/catalog.json'
data = json.loads(path.read_text())
assert data['repository'] == 'https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO', 'Expected SkyBlock catalogue source'
items = data['items']
ids = {item['id'] for item in items}
assert len(ids) == len(items), 'Duplicate SkyBlock IDs'
# These are not SkyBlock items. Minecraft IDs are allowed as render icons only.
def check_id(item_id):
    normalized = item_id.lower().replace(' ', '_')
    assert not normalized.startswith('minecraft:'), f'Vanilla recipe ID: {item_id}'
    assert 'netherite' not in normalized and 'ancient_debris' not in normalized, f'Non-SkyBlock item: {item_id}'

for item in items:
    check_id(item['id'])
    check_id(item['name'])
    for route in item['routes']:
        for ingredient in route['inputs'] + [slot for slot in route.get('slots', []) if slot]:
            check_id(ingredient['id'])
# Shared vanilla materials remain valid when they are part of SkyBlock.
for item_id in ('COBBLESTONE', 'DIAMOND', 'ENCHANTED_DIAMOND_BLOCK', 'REFINED_MITHRIL', 'MITHRIL_PLATE'):
    assert item_id in ids, f'Missing SkyBlock material: {item_id}'
print(f'PASS: {len(items)} SkyBlock catalogue entries; no vanilla recipe IDs or Netherite')

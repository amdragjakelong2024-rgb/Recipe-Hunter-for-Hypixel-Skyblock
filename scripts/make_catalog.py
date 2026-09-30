import json, pathlib, re, collections, sys
repo=pathlib.Path(sys.argv[1]); out=pathlib.Path(sys.argv[2]); data={}
def clean(s): return re.sub(r'§.', '',s)
def ing(s):
 if not s:return None
 a=s.rsplit(':',1)
 return {'id':a[0] if len(a)==2 and a[1].isdigit() else s,'count':int(a[1]) if len(a)==2 and a[1].isdigit() else 1}
for f in sorted((repo/'items').glob('*.json')):
 d=json.loads(f.read_text()); id=d.get('internalname',f.stem); name=clean(d.get('displayname',id)); rarity=''
 if '{LVL}' in name and ';' in id:
  ix=id.rsplit(';',1)[1]
  if ix.isdigit() and int(ix)<6: rarity=['Common','Uncommon','Rare','Epic','Legendary','Mythic'][int(ix)]
  name=rarity+' '+re.sub(r'\[Lvl .*?\]\s*','',name)+' Pet'
 color='ffffff'
 codes=re.findall(r'§([0-9a-f])',d.get('displayname',''))
 if codes:color=['000000','0000aa','00aa00','00aaaa','aa0000','aa00aa','ffaa00','aaaaaa','555555','5555ff','55ff55','55ffff','ff5555','ff55ff','ffff55','ffffff'][int(codes[-1],16)]
 data[id]={'id':id,'name':name,'color':int(color,16),'icon':d.get('itemid','minecraft:paper'),'requirement':clean(d.get('crafttext','')),'wiki':next((x for x in d.get('info',[]) if x.startswith('https://')),''),'routes':[],'sources':[]}
for f in sorted((repo/'items').glob('*.json')):
 d=json.loads(f.read_text()); id=d.get('internalname',f.stem); recipes=list(d.get('recipes',[]))
 if d.get('recipe'): recipes.insert(0,dict(d['recipe'],type='crafting'))
 for r in recipes:
  t=r.get('type'); target=id; inputs=[]; slots=[]; n=int(r.get('count',d.get('count',1)))
  if t=='crafting':
   slots=[ing(r.get(row+str(col),'')) for row in 'ABC' for col in range(1,4)];inputs=[x for x in slots if x];target=r.get('overrideOutputId',id)
  elif t=='forge': inputs=[ing(x) for x in r.get('inputs',[])];target=r.get('overrideOutputId',id)
  elif t=='katgrade':
   target=r.get('output',id);inputs=[ing(r['input'])]+[ing(x) for x in r.get('items',[])]
   if r.get('coins',0): inputs.append({'id':'SKYBLOCK_COIN','count':int(r['coins'])})
  elif t in ['npc_shop','trade']:
   result=ing(r.get('result',''))
   if result and result['id'] in data:
    cost=r.get('cost',[]);cost=cost if isinstance(cost,list) else [cost]
    data[result['id']]['sources'].append('NPC '+data[id]['name']+': '+', '.join(str(ing(x)['count'])+'x '+data.get(ing(x)['id'],{'name':ing(x)['id']})['name'] for x in cost)+' -> '+str(result['count'])+'x. Check shop requirements / limits in game.')
   continue
  elif t=='drops':
   for drop in r.get('drops',[]):
    target=ing(drop['id'])['id']
    if target in data:
     data[target]['sources'].append('Drop: '+clean(r.get('name',data[id]['name']))+'; chance (NEU): '+str(drop.get('chance','unknown'))+'. '+clean(' '.join(drop.get('extra',[]))))
   continue
  else:continue
  if target not in data or not inputs or n<1:continue
  total=collections.Counter()
  for x in inputs:
   if x:total[x['id']]+=x['count']
  data[target]['routes'].append({'type':t,'count':n,'inputs':[{'id':k,'count':v} for k,v in total.items()],'slots':slots,'seconds':int(r.get('duration',r.get('time',0)))})
# Verified acquisition notes; never introduce invented crafting recipes.
notes={
'NECRON_HANDLE': ['No crafting recipe. Repeatable Ironman route: F7 / M7 Bedrock Chest. Opening costs 100,000,000 coins.', 'RNG drop: an estimated average is not a guarantee. Raffle of the Century has also offered it as an event reward.'],
'GRIFFIN;0':['Buy Common Griffin from Diana during Mythological Ritual: 25,000 coins.'],
'MYTHOS_FRAGMENT':['Griffin Burrows during Mythological Ritual, using Archaic or Deific Spade. Ancestral Spade is insufficient.'],
'SOUL_STRING':['Arachne in Spider\'s Den. Damage placement, boss tier and summoning contribution affect the amount.'],
'GRIFFIN_FEATHER':['Griffin Burrows during Diana\'s Mythological Ritual. Plan around the event availability.'],
'GOLD_INGOT':['Mine gold; Gold Mine / Deep Caverns are early sources. Farm gold and compress it into enchanted forms.'],
'COBBLESTONE':['Mine stone / cobblestone or use a Cobblestone Minion.'],
'ANCIENT_CLAW':['Mythological creatures during Diana\'s Mythological Ritual. Choose a Griffin rarity whose mobs you can defeat reliably.']}
for id,ns in notes.items():data[id]['sources']=ns+data[id]['sources']
# Treat gathered raw resources as terminal leaves, not endless block compression cycles.
raw={'GOLD_INGOT','IRON_INGOT','DIAMOND','EMERALD','COAL','REDSTONE','INK_SACK-4','QUARTZ','WHEAT','SLIME_BALL','BONE','MELON','CLAY_BALL','SNOW_BALL','STRING'}
for id in raw:
 if id in data:data[id]['raw']=True
for d in data.values():d['sources']=list(dict.fromkeys(d['sources']))
meta={'date':'2026-09-29','repository':'https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO','commit':'f296ff3a6bf3131e8fc412c13df5c0adc3d85043','items':list(data.values())}
out.write_text(json.dumps(meta,ensure_ascii=False,separators=(',',':')))
print(len(data),'items;',sum(len(x['routes']) for x in data.values()),'craft / forge / Kat routes;',out.stat().st_size,'bytes')

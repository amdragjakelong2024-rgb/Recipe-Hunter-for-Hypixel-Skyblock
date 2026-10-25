"""Build icon metadata from NEU without changing recipe calculations."""
import json,pathlib,re,sys
repo=pathlib.Path(sys.argv[1]);output=pathlib.Path(sys.argv[2]);catalog=json.loads((output.parent/'catalog.json').read_text());icons={}
colors='white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
woods='oak spruce birch jungle acacia dark_oak'.split()
alias={'boat':'oak_boat','brick_block':'bricks','noteblock':'note_block','reeds':'sugar_cane','speckled_melon':'glistering_melon_slice','spawn_egg':'pig_spawn_egg','skull':'player_head','deadbush':'dead_bush','waterlily':'lily_pad','slime':'slime_block','melon':'melon_slice','melon_block':'melon','web':'cobweb','netherbrick':'nether_brick','nether_brick':'nether_bricks','mob_spawner':'spawner','hardened_clay':'terracotta','trapdoor':'oak_trapdoor','fence':'oak_fence','fence_gate':'oak_fence_gate','wooden_door':'oak_door','wooden_button':'oak_button','wooden_pressure_plate':'oak_pressure_plate','sign':'oak_sign','grass':'grass_block','golden_rail':'powered_rail','lit_pumpkin':'jack_o_lantern','quartz_ore':'nether_quartz_ore','snow_layer':'snow','fireworks':'firework_rocket','firework_charge':'firework_star','stone_stairs':'cobblestone_stairs','stone_slab2':'red_sandstone_slab','bed':'red_bed','monster_egg':'infested_stone'}
variants={'fish':['cod','salmon','tropical_fish','pufferfish'],'cooked_fish':['cooked_cod','cooked_salmon'],'dye':['ink_sac','red_dye','green_dye','cocoa_beans','lapis_lazuli','purple_dye','cyan_dye','light_gray_dye','gray_dye','pink_dye','lime_dye','yellow_dye','light_blue_dye','magenta_dye','orange_dye','bone_meal'],'stone':['stone','granite','polished_granite','diorite','polished_diorite','andesite','polished_andesite'],'stonebrick':['stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks'],'dirt':['dirt','coarse_dirt','podzol'],'sand':['sand','red_sand'],'sponge':['sponge','wet_sponge'],'coal':['coal','charcoal'],'red_flower':['poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy'],'yellow_flower':['dandelion'],'double_plant':['sunflower','lilac','tall_grass','large_fern','rose_bush','peony'],'tallgrass':['dead_bush','short_grass','fern'],'stone_slab':['smooth_stone_slab','sandstone_slab','petrified_oak_slab','cobblestone_slab','brick_slab','stone_brick_slab','nether_brick_slab','quartz_slab'],'quartz_block':['quartz_block','chiseled_quartz_block','quartz_pillar'],'prismarine':['prismarine','prismarine_bricks','dark_prismarine']}
for item in catalog['items']:
 p=repo/'items'/(item['id']+'.json')
 if not p.exists():
  import subprocess
  old=subprocess.run(['git','-C',str(repo),'show','f296ff3a6bf3131e8fc412c13df5c0adc3d85043:items/'+item['id']+'.json'],capture_output=True,text=True)
  if old.returncode:continue
  d=json.loads(old.stdout)
 else:d=json.loads(p.read_text())
 nbt=d.get('nbttag','');base=d.get('itemid','minecraft:paper').split(':')[-1];damage=int(d.get('damage',0));base0=base
 if base in variants:base=variants[base][min(damage,len(variants[base])-1)]
 elif base in ('wool','carpet','stained_glass','stained_glass_pane','stained_hardened_clay','banner'):base=colors[(15-damage if base=='banner' else damage)%16]+'_'+('terracotta' if base=='stained_hardened_clay' else base.replace('stained_','stained_'))
 elif base in ('log','log2','planks','sapling','leaves','leaves2','wooden_slab'):base=woods[(damage%4+4 if base.endswith('2') else damage%6) %6]+'_'+{'log2':'log','leaves2':'leaves','wooden_slab':'slab'}.get(base,base)
 elif base=='spawn_egg':base={50:'creeper',51:'skeleton',52:'spider',54:'zombie',55:'slime',56:'ghast',57:'zombified_piglin',58:'enderman',59:'cave_spider',60:'silverfish',61:'blaze',62:'magma_cube',65:'bat',66:'witch',90:'pig',91:'sheep',92:'cow',93:'chicken',94:'squid',95:'wolf',96:'mooshroom',98:'ocelot',100:'horse',101:'rabbit',120:'villager'}.get(damage,'pig')+'_spawn_egg'
 elif base=='skull':base=['skeleton_skull','wither_skeleton_skull','zombie_head','player_head','creeper_head','dragon_head'][min(damage,5)]
 elif base.startswith('record_'):base=base.replace('record_','music_disc_')
 else:base=alias.get(base,base)
 entry={'base':'minecraft:'+base}
 model=re.search(r'ItemModel:"([a-z0-9_:/.\-]+)"',nbt)
 if model:entry['model']=model[1]
 texture=re.search(r'Value:"([A-Za-z0-9+/=]+)"',nbt)
 if texture:entry['texture']=texture[1]
 signature=re.search(r'Signature:"([A-Za-z0-9+/=]+)"',nbt)
 if signature:entry['signature']=signature[1]
 color=re.search(r'(?:^|[,{])color:(\d+)',nbt)
 if color:entry['dye']=int(color[1])
 if 'ench:[' in nbt or item['id'].startswith('ENCHANTED_'):entry['glint']=True
 icons[item['id']]=entry
import gzip
output.write_bytes(gzip.compress(json.dumps(icons,separators=(',',':')).encode(),mtime=0))
print(len(icons),'icon definitions;',output.stat().st_size,'bytes')

package net.chriskatze.katzencraftmetals.checks;

import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Effective selected recipes and loot, with actual native ingredient/tag matching. */
public final class FoundryCraftingChecks {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static ResourceLocation id(String name){return ResourceLocation.parse("katzencraftmetals:"+name);}
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var repository=ServerPacksSource.createVanillaTrustedRepository();repository.reload();repository.setSelected(List.of("vanilla","mod_data"));
        try(var resources=new MultiPackResourceManager(PackType.SERVER_DATA,repository.openAllSelected())) {
            var all=new LinkedHashMap<ResourceLocation,JsonElement>();SimpleJsonResourceReloadListener.scanDirectory(resources,"recipe",new Gson(),all);
            var names=List.of("foundry_controller","foundry_tank","foundry_table","foundry_basin");
            var selected=new LinkedHashMap<ResourceLocation,JsonElement>();for(var name:names)selected.put(id(name),all.get(id(name)));
            Map<TagKey<Item>,List<Holder<Item>>> tags=new HashMap<>();BuiltInRegistries.ITEM.getTags().forEach(pair->tags.put(pair.getFirst(),pair.getSecond().stream().map(h->(Holder<Item>)h).toList()));
            List<Item> deepslate=new ArrayList<>();
            try(var reader=resources.getResource(id("tags/item/foundry_deepslate.json")).orElseThrow().openAsReader()) {
                for(var value:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values"))deepslate.add(BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(value.getAsString())).orElseThrow());
            }
            check(deepslate.size()==8&&!deepslate.contains(Items.DEEPSLATE_IRON_ORE),"Exactly eight full Deepslate building cubes");
            var planks=List.of(Items.OAK_PLANKS,Items.BIRCH_PLANKS,Items.SPRUCE_PLANKS,Items.JUNGLE_PLANKS,Items.ACACIA_PLANKS,Items.DARK_OAK_PLANKS,Items.MANGROVE_PLANKS,Items.CHERRY_PLANKS,Items.BAMBOO_PLANKS,Items.CRIMSON_PLANKS,Items.WARPED_PLANKS);
            tags.put(TagKey.create(Registries.ITEM,id("foundry_deepslate")),deepslate.stream().map(i->(Holder<Item>)i.builtInRegistryHolder()).toList());
            tags.put(ItemTags.PLANKS,planks.stream().map(i->(Holder<Item>)i.builtInRegistryHolder()).toList());BuiltInRegistries.ITEM.bindTags(tags);
            var manager=new DiamondEquipmentChecks.Manager();manager.load(selected,resources);
            Map<String,String> patterns=Map.of("foundry_controller","DDDDFDDDD","foundry_tank"," D DGD D ","foundry_table","S SDDDDPD","foundry_basin","D DD DDDD");
            for(var name:names)for(int variant=0;variant<deepslate.size();variant++)for(var plank:planks) {
                var slots=new ArrayList<ItemStack>();int offset=variant;
                for(char key:patterns.get(name).toCharArray())slots.add(switch(key){
                    case 'D'->new ItemStack(deepslate.get(offset++%deepslate.size()));case 'F'->new ItemStack(Items.FURNACE);
                    case 'G'->new ItemStack(Items.GLASS);case 'S'->new ItemStack(Items.SMOOTH_STONE_SLAB);case 'P'->new ItemStack(plank);default->ItemStack.EMPTY;
                });
                var input=CraftingInput.of(3,3,slots);var match=manager.getRecipeFor(RecipeType.CRAFTING,input,null).orElseThrow();
                check(match.id().equals(id(name))&&BuiltInRegistries.ITEM.getKey(match.value().assemble(input,lookup).getItem()).equals(id(name)),"Runtime mixed Deepslate/plank recipe "+name);
                var wrong=new ArrayList<>(slots);int at=0;while(wrong.get(at).isEmpty())at++;wrong.set(at,new ItemStack(Items.COBBLESTONE));
                check(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,wrong),null).isEmpty(),"Wrong Deepslate rejected");
                if(name.equals("foundry_tank")){var pane=new ArrayList<>(slots);pane.set(4,new ItemStack(Items.GLASS_PANE));check(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,pane),null).isEmpty(),"Glass panes rejected");}
                check(resources.getResource(id("advancement/recipes/decorations/"+name+".json")).isPresent(),"Recipe unlock exists");
            }
            try(var nativeLookup=new NetheriteProgressionChecks.LookupScope(lookup)) {
                var constructor=LootContext.class.getDeclaredConstructor(LootParams.class,RandomSource.class,HolderGetter.Provider.class);constructor.setAccessible(true);
                var json=NetheriteProgressionChecks.json(resources,id("loot_table/blocks/foundry_tank.json"));
                var table=LootTable.DIRECT_CODEC.parse(nativeLookup.access.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),json).getOrThrow();
                var tank=net.chriskatze.katzencraftmetals.block.ModBlocks.FOUNDRY_TANK.get();
                var params=new LootParams(nativeLookup.level,Map.of(LootContextParams.ORIGIN,Vec3.ZERO,LootContextParams.BLOCK_STATE,tank.defaultBlockState(),LootContextParams.TOOL,ItemStack.EMPTY),Map.of(),0);
                var context=constructor.newInstance(params,RandomSource.create(7),null);var drops=new ArrayList<ItemStack>();table.getRandomItemsRaw(context,drops::add);
                check(drops.size()==1&&drops.getFirst().is(tank.asItem())&&drops.getFirst().getCount()==1,"Native tank dropSelf yields exactly one item with bare hands");
            }
        }
        System.out.println("PASS: four effective Foundry recipes with mixed eight Deepslate variants and eleven plank types, wrong ingredients/glass panes rejected, unlock advancements and native one-item tank loot.");
    }
}

package net.chriskatze.katzencraftmetals.checks;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.*;
import net.chriskatze.katzencraftmetals.config.*;
import net.chriskatze.katzencraftmetals.event.CatoDisableItemsEvents;
import net.chriskatze.katzencraftmetals.metal.*;
import net.chriskatze.katzencraftmetals.recipe.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.stream.Collectors;

/** Actual selected resources, recipe manager, casting lifecycle and seeded native loot functions. */
public final class NetheriteProgressionChecks {
    static final List<String> PARTS=List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots");
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    static ResourceLocation id(String p){return ResourceLocation.withDefaultNamespace(p);}
    static JsonElement json(ResourceManager rm,ResourceLocation file) throws Exception {
        try(var reader=rm.getResource(file).orElseThrow().openAsReader()){return JsonParser.parseReader(reader);}
    }
    public static void run(HolderLookup.Provider registries) throws Exception {
        var metal=MetalCatalog.get(id("netherite")).orElseThrow();
        check(MetalCatalog.values().size()==7&&MetalCatalog.values().get(6)==metal,"Seventh metal append");
        check(ModMoltenMetals.getSyncId(id("netherite"))==6,"Netherite synchronization ID");
        check(metal.equipment()==MetalEquipmentProfiles.NETHERITE&&metal.equipment().miningLevel()==7,"Equipment identity/capability");
        check(metal.density()==20000&&metal.meltingTime()==100&&metal.ingotCoolingTime()==100&&metal.blockCoolingTime()==100,"Provisional density/default times");
        check(BlockRequirements.getRequirement(Blocks.ANCIENT_DEBRIS)==6,"Ore gate");
        var repository=ServerPacksSource.createVanillaTrustedRepository();repository.reload();repository.setSelected(List.of("vanilla","mod_data"));
        try(var resources=new MultiPackResourceManager(PackType.SERVER_DATA,repository.openAllSelected())) {
            var recipes=new LinkedHashMap<ResourceLocation,JsonElement>();
            SimpleJsonResourceReloadListener.scanDirectory(resources,"recipe",new Gson(),recipes);
            var selected=new LinkedHashMap<ResourceLocation,JsonElement>();
            List<String> disabled=new ArrayList<>(List.of("netherite_scrap","netherite_scrap_from_blasting","netherite_ingot","netherite_upgrade_smithing_template"));
            PARTS.forEach(p->disabled.add("netherite_"+p+"_smithing"));
            disabled.forEach(p->selected.put(id(p),recipes.get(id(p))));
            PARTS.forEach(p->selected.put(id("netherite_"+p),recipes.get(id("netherite_"+p))));
            for(String name:List.of("netherite_ingot","netherite_block","raw_netherite","netherite_ore","netherite_nugget")) {
                var key=ResourceLocation.parse("katzencraftmetals:foundry_melting/"+name);selected.put(key,recipes.get(key));
            }
            var preserved=List.of("netherite_block","netherite_ingot_from_netherite_block","lodestone",
                    "coast_armor_trim_smithing_template","coast_armor_trim_smithing_template_smithing_trim");
            for(var name:preserved)selected.put(id(name),recipes.get(id(name)));
            for(var name:List.of("netherite_ingot_from_nuggets","netherite_nugget_from_ingot")) {
                var key=ResourceLocation.parse("katzencraftmetals:"+name);selected.put(key,recipes.get(key));
            }
            var manager=new DiamondEquipmentChecks.Manager();manager.load(selected,resources);
            for(var name:preserved)check(manager.byKey(id(name)).isPresent(),"Unrelated recipe lost "+name);
            var oreInput=new SingleRecipeInput(new ItemStack(Items.ANCIENT_DEBRIS));
            check(manager.getRecipeFor(RecipeType.SMELTING,oreInput,null).isEmpty()&&manager.getRecipeFor(RecipeType.BLASTING,oreInput,null).isEmpty(),"Vanilla refining output remains");
            for(String key:disabled)check(manager.byKey(id(key)).isEmpty(),"Obsolete recipe remains "+key);
            for(String part:PARTS) {
                var key=id("netherite_"+part);var input=DiamondEquipmentChecks.grid(recipes.get(key).getAsJsonObject());
                var match=manager.getRecipeFor(RecipeType.CRAFTING,input,null).orElseThrow();
                check(BuiltInRegistries.ITEM.getKey(match.value().assemble(input,registries).getItem()).equals(key),"Direct craft result "+part);
                int count=input.items().stream().filter(s->s.is(Items.NETHERITE_INGOT)).mapToInt(ItemStack::getCount).sum();
                check(count==Map.of("sword",2,"pickaxe",3,"axe",3,"shovel",1,"hoe",2,"helmet",5,"chestplate",8,"leggings",7,"boots",4).get(part),"Ingot cost "+part);
                check(input.items().stream().allMatch(s->s.isEmpty()||s.is(Items.NETHERITE_INGOT)||s.is(Items.STICK)),"Unexpected template/Diamond ingredient");
            }
            for(var entry:Map.of(Items.NETHERITE_SCRAP,6,Items.ANCIENT_DEBRIS,6,Items.NETHERITE_INGOT,6,Items.NETHERITE_BLOCK,54).entrySet()) {
                var input=new SingleRecipeInput(new ItemStack(entry.getKey()));
                var recipe=manager.getRecipeFor(ModRecipes.FOUNDRY_MELTING_TYPE.get(),input,null).orElseThrow().value();
                var resolved=FoundryMeltingRecipes.resolve(List.of(recipe),input.item()).orElseThrow();
                check(resolved.moltenMetal().equals(id("netherite"))&&resolved.moltenAmount()==entry.getValue(),"Foundry yield "+entry.getKey());
                check(resolved.processingTime()==(entry.getValue()==54?900:100),"Foundry proportional time");
            }
            var nugget=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("katzencraftmetals:netherite_nugget")).orElseThrow();
            var nuggetGrid=DiamondEquipmentChecks.grid(recipes.get(ResourceLocation.parse("katzencraftmetals:netherite_ingot_from_nuggets")).getAsJsonObject());
            var ingotResult=manager.getRecipeFor(RecipeType.CRAFTING,nuggetGrid,null).orElseThrow().value().assemble(nuggetGrid,registries);
            check(ingotResult.is(Items.NETHERITE_INGOT)&&ingotResult.getCount()==1&&nuggetGrid.items().stream().filter(s->s.is(nugget)).count()==6,"Six nuggets craft one ingot");
            var ingotGrid=CraftingInput.of(1,1,List.of(new ItemStack(Items.NETHERITE_INGOT)));
            var nuggets=manager.getRecipeFor(RecipeType.CRAFTING,ingotGrid,null).orElseThrow().value().assemble(ingotGrid,registries);
            check(nuggets.is(nugget)&&nuggets.getCount()==6,"One ingot crafts six nuggets");
            var nuggetInput=new SingleRecipeInput(new ItemStack(nugget));
            var nuggetRecipe=manager.getRecipeFor(ModRecipes.FOUNDRY_MELTING_TYPE.get(),nuggetInput,null).orElseThrow().value();
            var nuggetMelt=FoundryMeltingRecipes.resolve(List.of(nuggetRecipe),nuggetInput.item()).orElseThrow();
            check(nuggetMelt.moltenMetal().equals(id("netherite"))&&nuggetMelt.moltenAmount()==1&&nuggetMelt.processingTime()==17,"Nugget melts into one Netherite unit in 17 ticks");
            check(json(resources,ResourceLocation.parse("c:tags/item/nuggets/netherite.json")).toString().contains("katzencraftmetals:netherite_nugget"),"Netherite common nugget tag");
            System.out.println("PASS: registered Netherite nugget, real six-to-one crafting round trip, one-unit/17-tick melting and generated common tag.");
            // Audit actual selected loot, including Bastion replacements: no refined ingot/template bypasses.
            for(var entry:resources.listResources("loot_table",p->p.getPath().endsWith(".json")).entrySet()) {
                try(var reader=entry.getValue().openAsReader()) {
                    var text=JsonParser.parseReader(reader).toString();
                    check(!text.contains("\"minecraft:netherite_upgrade_smithing_template\"")&&!text.contains("\"minecraft:netherite_ingot\""),"Loot progression bypass "+entry.getKey());
                }
            }
            oreLoot(registries,resources);
            cast();
        }
        try(var clientResources=new MultiPackResourceManager(PackType.CLIENT_RESOURCES,repository.openAllSelected())) {
            Map<String,String> names=new HashMap<>();
            for(var namespace:clientResources.getNamespaces())for(var resource:clientResources.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace,"lang/en_us.json"))) {
                try(var reader=resource.openAsReader()) {
                    JsonParser.parseReader(reader).getAsJsonObject().entrySet().stream().filter(e->e.getValue().isJsonPrimitive())
                            .forEach(e->names.put(e.getKey(),e.getValue().getAsString()));
                }
            }
            check("Netherite Ore".equals(names.get("block.minecraft.ancient_debris"))&&"Raw Netherite".equals(names.get("item.minecraft.netherite_scrap")),"Effective language overrides");
        }
        check(CatoDisableItemsEvents.isDisabled(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),"Template Creative predicate");
        check(!CatoDisableItemsEvents.isDisabled(new ItemStack(Items.NETHERITE_PICKAXE)),"Netherite equipment hidden");
        System.out.println("PASS: seven catalog metals, stable appended sync ID, nine real direct crafting grids/costs, thirteen obsolete recipes absent, four Foundry melt inputs/yields/times, loaded loot bypass audit, and Table/Basin casting lifecycle.");
    }
    static void oreLoot(HolderLookup.Provider registries,ResourceManager resources) throws Exception {
        var constructor=LootContext.class.getDeclaredConstructor(LootParams.class,RandomSource.class,HolderGetter.Provider.class);constructor.setAccessible(true);
        int baseTotal=0,fortuneTotal=0;Set<Integer> counts=new HashSet<>();
        // NeoForge's item-enchantment predicate resolves the registry through the current
        // server, even though the native loot table itself needs no running world.
        try(var lookup=new LookupScope(registries)) {
        var table=LootTable.DIRECT_CODEC.parse(lookup.access.createSerializationContext(JsonOps.INSTANCE),json(resources,id("loot_table/blocks/ancient_debris.json"))).getOrThrow();
        for(int mode=0;mode<3;mode++)for(int seed=1;seed<=256;seed++) {
            var tool=new ItemStack(net.chriskatze.katzencraftmetals.item.ModItems.STEEL_PICKAXE.get());
            if(mode>0)tool.enchant(lookup.access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode==1?Enchantments.FORTUNE:Enchantments.SILK_TOUCH),mode==1?3:1);
            var params=new LootParams(lookup.level,Map.of(LootContextParams.TOOL,tool,LootContextParams.BLOCK_STATE,Blocks.ANCIENT_DEBRIS.defaultBlockState(),LootContextParams.ORIGIN,Vec3.ZERO),Map.of(),0);
            var context=constructor.newInstance(params,RandomSource.create(seed * 0x9E3779B97F4A7C15L),null);
            List<ItemStack> drops=new ArrayList<>();table.getRandomItemsRaw(context,drops::add);
            int total=drops.stream().mapToInt(ItemStack::getCount).sum();
            if(mode==2)check(total==1&&drops.stream().allMatch(s->s.is(Items.ANCIENT_DEBRIS)),"Silk Touch intact ore");
            else {
                check(drops.stream().allMatch(s->s.is(Items.NETHERITE_SCRAP)),"Unexpected ore drop");
                if(mode==0){check(total>=1&&total<=3,"Raw yield 1..3");counts.add(total);baseTotal+=total;}
                else fortuneTotal+=total;
            }
        }
        }
        check(counts.equals(Set.of(1,2,3))&&fortuneTotal>baseTotal,"Random yield/Fortune improvement: counts="+counts+" base="+baseTotal+" fortune="+fortuneTotal);
        System.out.println("PASS: actual selected ore loot table evaluated across 768 seeded native loot contexts: 1..3 raw drops, Fortune III improves average yield, Silk Touch returns one intact ore. Harvest permissions are covered by the full equipment matrix.");
    }
    static void cast() throws Exception {
        var advance=FoundryBasinBlockEntity.class.getDeclaredMethod("advanceCooling");advance.setAccessible(true);
        for(boolean ingot:List.of(true,false)) {
            FoundryBasinBlockEntity target=ingot?new FoundryTableBlockEntity(BlockPos.ZERO,ModBlocks.FOUNDRY_TABLE.get().defaultBlockState()):new FoundryBasinBlockEntity(BlockPos.ZERO,ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
            int amount=ingot?6:54;
            check(target.insert(id("netherite"),amount)==amount,"Casting insertion");
            for(int tick=0;tick<100;tick++)advance.invoke(target);
            check(target.isCooled()&&target.getResultCopy().is(ingot?Items.NETHERITE_INGOT:Items.NETHERITE_BLOCK),"Cast output");
            check(target.takeResult().getCount()==1&&target.isEmpty(),"Extraction lifecycle");
        }
        check(ModMoltenMetals.get(id("netherite")).orElseThrow().cooledTexture().equals(id("textures/block/netherite_block.png")),"Basin vanilla block texture");
    }
    /** Isolated lookup-only server fixture, always restored; no worlds, ticks or networking. */
    static final class LookupScope implements AutoCloseable {
        final java.lang.reflect.Field current;
        final Object previous;
        final net.minecraft.server.level.ServerLevel level;
        final RegistryAccess.Frozen access;
        LookupScope(HolderLookup.Provider lookup) throws Exception {
            var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
            var unsafe=(sun.misc.Unsafe)field.get(null);
            var server=unsafe.allocateInstance(net.minecraft.server.dedicated.DedicatedServer.class);
            var enchantments=new MappedRegistry<net.minecraft.world.item.enchantment.Enchantment>(Registries.ENCHANTMENT,com.mojang.serialization.Lifecycle.stable());
            lookup.lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder->Registry.register(enchantments,holder.key(),holder.value()));
            enchantments.freeze();
            access=new RegistryAccess.Frozen() {
                @SuppressWarnings("unchecked")
                public <T> Optional<Registry<T>> registry(ResourceKey<? extends Registry<? extends T>> key){
                    return key.equals(Registries.ENCHANTMENT)?Optional.of((Registry<T>)enchantments):Optional.empty();
                }
                public java.util.stream.Stream<RegistryAccess.RegistryEntry<?>> registries(){return java.util.stream.Stream.empty();}
                public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key){return key.equals(Registries.ENCHANTMENT)?registry(key).map(Registry::asLookup):lookup.lookup(key);}
            };
            var layers=new LayeredRegistryAccess<Object>(List.of()) {
                @Override public RegistryAccess.Frozen compositeAccess(){return access;}
            };
            level=(net.minecraft.server.level.ServerLevel)unsafe.allocateInstance(net.minecraft.server.level.ServerLevel.class);
            var levelRegistries=net.minecraft.world.level.Level.class.getDeclaredField("registryAccess");levelRegistries.setAccessible(true);levelRegistries.set(level,access);
            var levelServer=net.minecraft.server.level.ServerLevel.class.getDeclaredField("server");levelServer.setAccessible(true);levelServer.set(level,server);
            var registries=net.minecraft.server.MinecraftServer.class.getDeclaredField("registries");registries.setAccessible(true);registries.set(server,layers);
            current=net.neoforged.neoforge.server.ServerLifecycleHooks.class.getDeclaredField("currentServer");current.setAccessible(true);
            previous=current.get(null);current.set(null,server);
        }
        @Override public void close() throws Exception {current.set(null,previous);}
    }
    private NetheriteProgressionChecks() {}
}

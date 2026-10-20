package net.chriskatze.katzencraftmetals.checks;

import com.google.gson.*;
import net.chriskatze.katzencraftmetals.event.CatoDisableItemsEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import java.util.*;
import java.util.stream.Collectors;

/** Real pack precedence, RecipeManager lookup/output and mod-bus Creative filtering. Not shipped. */
public final class DiamondEquipmentChecks {
    private static final List<String> PARTS=List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots");
    private static final String PACK="mod/katzencraftmetals:datapacks/disabled_diamond_equipment";
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ResourceLocation id(String path){return ResourceLocation.withDefaultNamespace(path);}
    static final class Manager extends RecipeManager {
        Manager(){super(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            injectContext(ICondition.IContext.EMPTY,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));}
        void load(Map<ResourceLocation,JsonElement> recipes,ResourceManager resources){apply(recipes,resources,InactiveProfiler.INSTANCE);}
    }
    private static Map<ResourceLocation,JsonElement> recipes(ResourceManager resources) throws Exception {
        Map<ResourceLocation,JsonElement> result=new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resources,"recipe",new Gson(),result);
        return result;
    }
    static CraftingInput grid(JsonObject json){
        var pattern=json.getAsJsonArray("pattern");var keys=json.getAsJsonObject("key");
        List<ItemStack> slots=new ArrayList<>();
        for(var row:pattern)for(char c:row.getAsString().toCharArray()) {
            if(c==' ')slots.add(ItemStack.EMPTY);
            else {
                var ingredient=keys.getAsJsonObject(String.valueOf(c));
                Item item;
                if(ingredient.has("item"))item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingredient.get("item").getAsString()));
                else item=BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,ResourceLocation.parse(ingredient.get("tag").getAsString())))
                        .orElseThrow().stream().findFirst().orElseThrow().value();
                slots.add(new ItemStack(item));
            }
        }
        return CraftingInput.of(pattern.get(0).getAsString().length(),pattern.size(),slots);
    }
    public static void run() throws Exception {
        var repository=ServerPacksSource.createVanillaTrustedRepository();
        repository.reload();repository.setSelected(List.of("vanilla","mod_data"));
        check(repository.getSelectedPacks().stream().anyMatch(p->p.getId().equals(PACK)),"Required suppression pack not auto-selected");
        var oldTags=BuiltInRegistries.ITEM.getTags().collect(Collectors.toMap(p->p.getFirst(),p->p.getSecond().stream().toList()));
        var tags=new HashMap<>(oldTags);
        tags.put(ItemTags.PLANKS,List.of(Items.OAK_PLANKS.builtInRegistryHolder()));
        for(var pair:Map.of("c:rods/wooden",Items.STICK,"c:gems/diamond",Items.DIAMOND,"c:ingots/iron",Items.IRON_INGOT,"c:ingots/copper",Items.COPPER_INGOT).entrySet())
            tags.put(TagKey.create(Registries.ITEM,ResourceLocation.parse(pair.getKey())),List.of(pair.getValue().builtInRegistryHolder()));
        BuiltInRegistries.ITEM.bindTags(tags);
        try {
            // Remove ONLY the new required pack to reproduce the previous shipped configuration.
            var baselinePacks=repository.getSelectedPacks().stream().filter(p->!p.getId().equals(PACK)).map(p->p.open()).toList();
            try(var before=new MultiPackResourceManager(PackType.SERVER_DATA,baselinePacks);
                var after=new MultiPackResourceManager(PackType.SERVER_DATA,repository.openAllSelected())) {
                var original=recipes(before);var effective=recipes(after);
                Map<ResourceLocation,JsonElement> originalSubset=new LinkedHashMap<>(),effectiveSubset=new LinkedHashMap<>();
                for(var part:PARTS) {
                    var key=id("diamond_"+part);originalSubset.put(key,original.get(key));effectiveSubset.put(key,effective.get(key));
                    var file=id("recipe/diamond_"+part+".json");
                    check(before.getResource(file).orElseThrow().sourcePackId().equals("mod/neoforge"),"Expected precedence reproduction: "+part);
                    check(after.getResource(file).orElseThrow().sourcePackId().equals(PACK),"Suppression pack lost precedence: "+part);
                }
                List<ResourceLocation> controls=new ArrayList<>();
                for(var material:List.of("wooden","iron","copper","steel","mythril"))for(var part:PARTS) {
                    if(material.equals("wooden")&&PARTS.indexOf(part)>4)continue;
                    var key=ResourceLocation.fromNamespaceAndPath(List.of("wooden","iron").contains(material)?"minecraft":"katzencraftmetals",material+"_"+part);
                    controls.add(key);originalSubset.put(key,original.get(key));effectiveSubset.put(key,effective.get(key));
                    check(original.get(key).equals(effective.get(key)),"Unrelated recipe changed: "+key);
                }
                var baseline=new Manager();baseline.load(originalSubset,before);
                var fixed=new Manager();fixed.load(effectiveSubset,after);
                for(var part:PARTS) {
                    var key=id("diamond_"+part);var input=grid(original.get(key).getAsJsonObject());
                    check(baseline.byKey(key).isPresent()&&baseline.getRecipeFor(RecipeType.CRAFTING,input,null).isPresent(),"Did not reproduce previously craftable "+part);
                    check(fixed.byKey(key).isEmpty()&&fixed.getRecipeFor(RecipeType.CRAFTING,input,null).isEmpty(),"Diamond crafting output remains: "+part);
                }
                for(var key:controls) {
                    var input=grid(effective.get(key).getAsJsonObject());
                    var match=fixed.getRecipeFor(RecipeType.CRAFTING,input,null).orElseThrow();
                    check(BuiltInRegistries.ITEM.getKey(match.value().assemble(input,RegistryAccess.EMPTY).getItem()).equals(key),"Control crafting changed: "+key);
                }
                // Audit all loaded fixed-output recipe sources, not only the nine expected IDs.
                var targets=PARTS.stream().map(p->"minecraft:diamond_"+p).collect(Collectors.toSet());
                Set<ResourceLocation> additional=new LinkedHashSet<>();
                for(var entry:effective.entrySet()) {
                    var json=entry.getValue().getAsJsonObject();var result=json.get("result");
                    String output=result!=null&&result.isJsonObject()&&result.getAsJsonObject().has("id")?result.getAsJsonObject().get("id").getAsString():"";
                    if(targets.contains(output)&&!PARTS.stream().anyMatch(part->entry.getKey().equals(id("diamond_"+part))))additional.add(entry.getKey());
                }
                check(additional.isEmpty(),"Additional fixed-output Diamond acquisition recipes: "+additional);
                // An actual recipe-manager apply on reload must not resurrect the recipes.
                fixed.load(effectiveSubset,after);
                for(var part:PARTS)check(fixed.byKey(id("diamond_"+part)).isEmpty(),"Reload restored Diamond recipe");
                System.out.println("PASS: real pack repository reproduces all nine NeoForge overrides winning before the fix; required pack wins after; actual RecipeManager grids return no Diamond output, 41 Wooden/Iron/Copper/Steel/Mythril recipes still craft; reload and all fixed-output sources audited.");
            }
            repository.reload();
            check(repository.getSelectedPacks().stream().anyMatch(p->p.getId().equals(PACK)),"Repository reload lost required pack");
            creative();
        } finally {BuiltInRegistries.ITEM.bindTags(oldTags);}
    }
    private static void creative(){
        var access=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var parameters=new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS,false,access);
        List<Item> controls=List.of(Items.DIAMOND,Items.DIAMOND_BLOCK,Items.NETHERITE_PICKAXE,Items.IRON_PICKAXE,Items.WOODEN_PICKAXE,
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("katzencraftmetals:copper_pickaxe")),BuiltInRegistries.ITEM.get(ResourceLocation.parse("katzencraftmetals:steel_pickaxe")),BuiltInRegistries.ITEM.get(ResourceLocation.parse("katzencraftmetals:mythril_pickaxe")));
        for(int pass=0;pass<3;pass++) {
            var parent=new InsertableLinkedOpenCustomHashSet<ItemStack>();var search=new InsertableLinkedOpenCustomHashSet<ItemStack>();
            for(var item:controls){parent.add(new ItemStack(item));search.add(new ItemStack(item));}
            for(var part:PARTS) {
                var item=BuiltInRegistries.ITEM.get(id("diamond_"+part));
                for(boolean variant:List.of(false,true)) {
                    var stack=new ItemStack(item);
                    if(variant)stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Variant"));
                    parent.add(stack);search.add(stack.copy());
                    check(CatoDisableItemsEvents.isDisabled(stack),"Search/scroll predicate misses Diamond "+part);
                }
            }
            parent.add(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            search.add(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            // Dispatch on the actual mod bus: calling the handler directly would miss the original bus bug.
            var key=List.of(CreativeModeTabs.COMBAT,CreativeModeTabs.TOOLS_AND_UTILITIES,CreativeModeTabs.COMBAT).get(pass);
            var tab=BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
            ModLoader.postEvent(new BuildCreativeModeTabContentsEvent(tab,key,parameters,parent,search));
            // Other installed listeners legitimately add equipment; retain those additions too.
            check(parent.stream().noneMatch(CatoDisableItemsEvents::isDisabled),"Diamond remains in parent tab");
            check(search.stream().noneMatch(CatoDisableItemsEvents::isDisabled),"Diamond remains in search tab");
            check(parent.stream().map(ItemStack::getItem).filter(controls::contains).distinct().toList().equals(controls),"Creative parent control ordering");
            check(search.stream().map(ItemStack::getItem).filter(controls::contains).distinct().toList().equals(controls),"Creative search control ordering");
            for(var item:controls)check(!CatoDisableItemsEvents.isDisabled(new ItemStack(item)),"Unrelated Creative item hidden");
        }
        System.out.println("PASS: actual mod-bus Creative tab events remove nine Diamond items and component variants from parent/search lists across three rebuilds; gemstone, block, Wood, Iron, Copper, Steel, Mythril and Netherite retained in order.");
    }
    private DiamondEquipmentChecks() {}
}

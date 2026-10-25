package net.chriskatze.katzencraftmetals.checks;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.config.*;
import net.chriskatze.katzencraftmetals.event.*;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.chriskatze.katzencraftmetals.metal.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.nio.file.*;
import java.util.*;

/** Uses actual registered equipment and event handlers. Tag fixtures are isolated to this datagen JVM. */
@EventBusSubscriber(modid="katzencraftmetals",bus=EventBusSubscriber.Bus.MOD)
public final class EquipmentChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static JsonObject attributes(ItemStack stack){
        var result=new JsonObject();
        for(var e:stack.getAttributeModifiers().modifiers())result.addProperty(
                BuiltInRegistries.ATTRIBUTE.getKey(e.attribute().value())+"|"+e.modifier().id()+"|"+e.slot()+"|"+e.modifier().operation(),e.modifier().amount());
        return result;
    }
    private static float total(ItemStack stack,boolean damage){
        var attribute=damage?Attributes.ATTACK_DAMAGE:Attributes.ATTACK_SPEED;float total=damage?1:4;
        for(var e:stack.getAttributeModifiers().modifiers())if(e.attribute().equals(attribute))total+=e.modifier().amount();
        return total;
    }
    private static void close(float a,float b,String message){check(Math.abs(a-b)<.00001,message+": "+a+" != "+b);}
    @SubscribeEvent public static void gather(GatherDataEvent event){
        try{net.chriskatze.katzencraftmetals.client.renderer.FoundrySharedReturnMaterialChecks.run();net.chriskatze.katzencraftmetals.client.renderer.FoundryFrameExposureChecks.run(true);net.chriskatze.katzencraftmetals.client.renderer.FoundryFrameReturnChecks.run();net.chriskatze.katzencraftmetals.block.entity.FoundryBasinWaterChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.block.entity.FoundryWaterloggingChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.client.renderer.FoundrySideAllocationChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.client.renderer.FoundryRenderCacheChecks.run();net.chriskatze.katzencraftmetals.block.entity.FoundryAutomationCacheChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.block.entity.FoundryLightingChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetHatchChecks.run();net.chriskatze.katzencraftmetals.client.renderer.FoundryBasinGeometryChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.block.entity.FoundryDetachedExpansionChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.client.sound.FoundryTankSoundChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.client.sound.FoundryPouringSoundChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.client.renderer.FoundryHorizontalSeamChecks.run();net.chriskatze.katzencraftmetals.block.entity.FoundryDetachedRenderingChecks.run(event.getLookupProvider().join());FoundryCraftingChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.block.entity.FoundryDismantlingChecks.run(event.getLookupProvider().join());net.chriskatze.katzencraftmetals.block.entity.FoundryFuelChecks.run(event.getLookupProvider().join());OreGenerationChecks.run(event.getLookupProvider().join());baseline();profiles();crafting();repairMenu();mining();resourceMatrix();vanillaRecipes();DiamondEquipmentChecks.run();NetheriteProgressionChecks.run(event.getLookupProvider().join());check(overridesChecked,"Actual startup override hook did not run");System.out.println("PASS: equipment profiles, approved armor corrections/Copper stats, mining matrix, event denials, component preservation and explicit vanilla overrides.");}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static void baseline() throws Exception {
        var path=Path.of(System.getProperty("katzencraft.equipment.root"),"verification/equipment/baseline.json");
        var baseline=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        for(var entry:baseline.entrySet()){
            var id=ResourceLocation.parse(entry.getKey());var item=BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
            var stack=new ItemStack(item);var old=entry.getValue().getAsJsonObject();int durability=old.get("durability").getAsInt();
            if(item instanceof ArmorItem armor&&armor.getType()!=ArmorItem.Type.BODY&&id.getNamespace().equals("katzencraftmetals")){
                if(id.getPath().startsWith("steel_"))durability=MetalEquipmentProfiles.STEEL.armorDurability(armor.getType());
                if(id.getPath().startsWith("mythril_"))durability=MetalEquipmentProfiles.MYTHRIL.armorDurability(armor.getType());
            }
            check(stack.getMaxDamage()==durability,"Durability/registry ID: "+id);
            check(stack.getEnchantmentValue()==old.get("enchantability").getAsInt(),"Enchantability: "+id);
            check(attributes(stack).equals(old.get("attributes")),"Original combat/armor attributes: "+id+" "+attributes(stack)+" vs "+old.get("attributes"));
            check(item.isValidRepairItem(stack,new ItemStack(Items.IRON_INGOT))==old.get("repairIron").getAsBoolean(),"Repair behavior: "+id);
        }
        check(baseline.size()==79,"Baseline count");
        System.out.println("PASS: all 79 original registered equipment IDs/stats/attributes/repair paths preserved except approved armor durability.");
    }
    private static void profiles(){
        for(var p:MetalEquipmentProfiles.activeProfiles())for(var kind:MetalEquipmentProfiles.ToolKind.values()){
            var id=ResourceLocation.fromNamespaceAndPath(p.vanilla()?"minecraft":"katzencraftmetals",p.id()+"_"+kind.name().toLowerCase(Locale.ROOT));
            var stack=new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElseThrow());var stats=p.tool(kind);
            check(stack.getMaxDamage()==stats.durability(),"Profile tool durability: "+id);
            close(total(stack,true),stats.damage(),"Total damage: "+id);close(total(stack,false),stats.attacksPerSecond(),"Actual attacks/sec: "+id);
            check(stack.getEnchantmentValue()==stats.enchantability(),"Profile enchantability: "+id);
            if(stack.getItem() instanceof DiggerItem digger){
                var category=switch(kind){case PICKAXE->BlockTags.MINEABLE_WITH_PICKAXE;case AXE->BlockTags.MINEABLE_WITH_AXE;case SHOVEL->BlockTags.MINEABLE_WITH_SHOVEL;case HOE->BlockTags.MINEABLE_WITH_HOE;default->throw new AssertionError();};
                var actual=stack.get(DataComponents.TOOL);
                var exceptions=actual.rules().stream().filter(r->r.blocks().unwrapKey().isEmpty()&&r.speed().isEmpty()&&r.correctForDrops().equals(Optional.of(true))).toList();
                if(kind==MetalEquipmentProfiles.ToolKind.PICKAXE) {
                    check(exceptions.size()==1,"Exactly one narrow native exception: "+id);
                    var expectedBlocks=BlockRequirements.configured().entrySet().stream().filter(e->e.getKey().getNamespace().equals("minecraft")&&e.getValue()<=p.miningLevel()).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
                    check(exceptions.get(0).blocks().stream().map(h->BuiltInRegistries.BLOCK.getKey(h.value())).collect(java.util.stream.Collectors.toSet()).equals(expectedBlocks),"Native exception targets: "+id);
                } else check(exceptions.isEmpty(),"Drop exception added to wrong tool type: "+id);
                var nativeRules=actual.rules().stream().filter(r->!(r.blocks().unwrapKey().isEmpty()&&r.speed().isEmpty()&&r.correctForDrops().equals(Optional.of(true)))).filter(r->r.blocks().unwrapKey().map(t->!t.location().getNamespace().equals("katzencraftmetals")||!t.location().getPath().startsWith("needs_mining_level_")).orElse(true)).toList();
                check(nativeRules.equals(digger.getTier().createToolProperties(category).rules()),"Native tool rules changed: "+id);
            }

        }
        int[][] expected={{132,192,180,156},{264,384,360,312},{440,640,600,520}};
        var pieces=List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS);
        int n=0;for(var p:List.of(MetalEquipmentProfiles.COPPER,MetalEquipmentProfiles.STEEL,MetalEquipmentProfiles.MYTHRIL)){
            for(int i=0;i<4;i++){
                var item=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("katzencraftmetals:"+p.id()+"_"+pieces.get(i).getName())).orElseThrow();
                check(item.getDefaultInstance().getMaxDamage()==expected[n][i],"Approved per-slot durability");
                check(item.isValidRepairItem(item.getDefaultInstance(),new ItemStack(n==0?Items.COPPER_INGOT:n==1?ModItems.STEEL_INGOT.get():ModItems.MYTHRIL_INGOT.get())),"Custom armor repair");
                if(n==0){var repair=net.chriskatze.katzencraftmetals.repair.CatoRepairConfig.get(item);check(repair.material()==Items.COPPER_INGOT&&repair.repairPercent()==.25f,"Copper custom anvil repair entry");}
            }n++;
        }
        check(MetalCatalog.values().stream().map(m->m.id().getPath()).toList().equals(List.of("iron","copper","steel","mythril","gold","platinum","netherite")),"Catalog order");
        check(MetalEquipmentProfiles.IRON.tool(MetalEquipmentProfiles.ToolKind.PICKAXE).durability()==250,"Native Iron, not unused TOML 256");
        try{MetalEquipmentProfiles.vanillaEquipment("iron").enchantability(99).build();throw new AssertionError("Unsupported vanilla method override accepted");}catch(IllegalArgumentException expectedError){}
        var custom=MetalEquipmentProfiles.equipment("test").miningLevel(5).toolDurability(100).pickaxeDurability(123).miningSpeed(4).enchantability(5)
                .swordDamage(2).pickaxeDamage(2).axeDamage(2).shovelDamage(2).hoeDamage(2)
                .attackSpeed(MetalEquipmentProfiles.ToolKind.SWORD,1).attackSpeed(MetalEquipmentProfiles.ToolKind.PICKAXE,1)
                .attackSpeed(MetalEquipmentProfiles.ToolKind.AXE,1).attackSpeed(MetalEquipmentProfiles.ToolKind.SHOVEL,1).attackSpeed(MetalEquipmentProfiles.ToolKind.HOE,1)
                .incorrectBlocks(BlockTags.INCORRECT_FOR_STONE_TOOL).repair("minecraft:copper_ingot").build();
        check(custom.tier(MetalEquipmentProfiles.ToolKind.PICKAXE).getUses()==123&&custom.tier(MetalEquipmentProfiles.ToolKind.HOE).getUses()==100,"Independent tool durability");
    }
    private static void crafting() throws Exception {
        var root=Path.of(System.getProperty("katzencraft.equipment.root"),"src/generated/resources/data/katzencraftmetals/recipe");
        var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,registries);
        for(String part:List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots")){
            var json=JsonParser.parseString(Files.readString(root.resolve("copper_"+part+".json"))).getAsJsonObject();
            var recipe=(net.minecraft.world.item.crafting.CraftingRecipe)net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops,json).getOrThrow();
            var stacks=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
            var pattern=json.getAsJsonArray("pattern");var keys=json.getAsJsonObject("key");
            for(int y=0;y<pattern.size();y++){
                String row=pattern.get(y).getAsString();
                for(int x=0;x<row.length();x++)if(row.charAt(x)!=' '){
                    var id=ResourceLocation.parse(keys.getAsJsonObject(""+row.charAt(x)).get("item").getAsString());
                    check(id.equals(ResourceLocation.parse("minecraft:copper_ingot"))||id.equals(ResourceLocation.parse("minecraft:stick")),"Copper crafting ingredient");
                    stacks.set(y*3+x,new ItemStack(BuiltInRegistries.ITEM.getOptional(id).orElseThrow()));
                }
            }
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
            check(recipe.matches(input,null),"Actual Copper recipe matching: "+part);
            var result=recipe.assemble(input,registries);
            check(result.getCount()==1&&BuiltInRegistries.ITEM.getKey(result.getItem()).equals(ResourceLocation.parse("katzencraftmetals:copper_"+part)),"Copper crafting output: "+part);
        }
        System.out.println("PASS: all nine generated Copper recipes decoded by native recipe codecs, matched real crafting grids and assembled their registered output items.");
    }
    private static void repairMenu() throws Exception {
        var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var unsafe=(sun.misc.Unsafe)f.get(null);
        var player=(TestPlayer)unsafe.allocateInstance(TestPlayer.class);
        for(String part:List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots")){
            var item=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("katzencraftmetals:copper_"+part)).orElseThrow();
            var damaged=new ItemStack(item);damaged.setDamageValue(100);
            damaged.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Copper fixture"));
            var menu=new net.chriskatze.katzencraftmetals.menu.KatzencraftAnvilMenu(0,new net.minecraft.world.entity.player.Inventory(player));
            check(menu.getSlot(0).mayPlace(damaged),"Copper custom anvil target admission");
            menu.getSlot(0).set(damaged);
            check(menu.getSlot(1).mayPlace(new ItemStack(Items.COPPER_INGOT)),"Copper custom anvil ingredient admission");
            menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT));check(menu.getSlot(2).getItem().isEmpty(),"Wrong repair ingredient accepted");
            menu.getSlot(1).set(new ItemStack(Items.COPPER_INGOT));var output=menu.getSlot(2).getItem();
            check(output.is(item)&&output.getDamageValue()==Math.max(0,100-Math.round(damaged.getMaxDamage()*.25f)),"Actual Copper anvil repair output");
            check(output.get(DataComponents.CUSTOM_NAME).equals(damaged.get(DataComponents.CUSTOM_NAME)),"Repair removed custom components");
            check(damaged.getDamageValue()==100,"Repair preview changed input");
            check(item.isValidRepairItem(damaged,new ItemStack(Items.COPPER_INGOT)),"Native Copper repair ingredient");
        }
        System.out.println("PASS: all nine Copper items admitted to the actual custom anvil menu; correct ingredient/output/rounding, rejected wrong ingredient, preserved custom name and unchanged preview input.");
    }
    static class TestPlayer extends Player {
        ItemStack hand=ItemStack.EMPTY;
        private TestPlayer(){super(null,BlockPos.ZERO,0,new GameProfile(UUID.randomUUID(),"fixture"));}
        @Override public ItemStack getMainHandItem(){return hand;}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
    }
    private static <T> Map<TagKey<T>,List<Holder<T>>> tags(Registry<T> registry){
        Map<TagKey<T>,List<Holder<T>>> result=new HashMap<>();registry.getTags().forEach(pair->result.put(pair.getFirst(),pair.getSecond().stream().toList()));return result;
    }
    private static void blockTag(Map<TagKey<Block>,List<Holder<Block>>> tags,TagKey<Block> key,Block...blocks){tags.put(key,Arrays.stream(blocks).map(b->(Holder<Block>)b.builtInRegistryHolder()).toList());}
    private static void mining() throws Exception {
        var oldBlocks=tags(BuiltInRegistries.BLOCK);var oldItems=tags(BuiltInRegistries.ITEM);
        var blocks=new HashMap<>(oldBlocks);var items=new HashMap<>(oldItems);
        var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var unsafe=(sun.misc.Unsafe)f.get(null);
        var player=(TestPlayer)unsafe.allocateInstance(TestPlayer.class);
        Block fixture=ModBlocks.PLATINUM_ORE.get();var state=fixture.defaultBlockState();
        blockTag(blocks,BlockTags.MINEABLE_WITH_PICKAXE,fixture,Blocks.IRON_ORE,Blocks.OBSIDIAN,ModBlocks.MYTHRIL_ORE.get());
        blockTag(blocks,BlockTags.INCORRECT_FOR_WOODEN_TOOL,Blocks.IRON_ORE,Blocks.OBSIDIAN);
        blockTag(blocks,BlockTags.INCORRECT_FOR_GOLD_TOOL,Blocks.IRON_ORE,Blocks.OBSIDIAN);
        blockTag(blocks,BlockTags.INCORRECT_FOR_STONE_TOOL,Blocks.OBSIDIAN);
        blockTag(blocks,BlockTags.INCORRECT_FOR_IRON_TOOL,Blocks.OBSIDIAN);
        blockTag(blocks,BlockTags.INCORRECT_FOR_DIAMOND_TOOL);blockTag(blocks,BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
        blockTag(blocks,BlockRequirements.LEGACY_DIAMOND,ModBlocks.MYTHRIL_ORE.get());
        try {
            for(var profile:MetalEquipmentProfiles.activeProfiles()) {
                var id=ResourceLocation.fromNamespaceAndPath(profile.vanilla()?"minecraft":"katzencraftmetals",profile.id()+"_pickaxe");
                var pick=BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
                player.hand=new ItemStack(pick);check(ToolLevels.capability(player.hand).orElse(-1)==profile.miningLevel(),"Supported pickaxe capability");
                for(int requirement=1;requirement<=8;requirement++) {
                    for(int i=1;i<=8;i++)blockTag(blocks,BlockRequirements.tag(i),i==requirement?new Block[]{fixture}:new Block[]{});
                    BuiltInRegistries.BLOCK.bindTags(blocks);
                    boolean nativeResult=player.hand.isCorrectToolForDrops(state);
                    var event=new PlayerEvent.HarvestCheck(player,state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,nativeResult);
                    ToolRequirementEvents.onHarvestCheck(event);
                    check(event.canHarvest()==(profile.miningLevel()>=requirement),"Capability/requirement matrix "+profile.id()+" / "+requirement);
                    var denied=new PlayerEvent.HarvestCheck(player,state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,false);
                    ToolRequirementEvents.onHarvestCheck(denied);check(!denied.canHarvest(),"Other mod denial overwritten");
                    var speed=new PlayerEvent.BreakSpeed(player,state,10,BlockPos.ZERO);ToolRequirementEvents.onBreakSpeed(speed);
                    close(speed.getNewSpeed(),profile.miningLevel()>=requirement?10:.1f,"Scoped speed penalty");
                }
            }
            for(var wrong:List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_SWORD),new ItemStack(Items.IRON_AXE),new ItemStack(Items.IRON_SHOVEL),new ItemStack(Items.IRON_HOE))){
                player.hand=wrong;var e=new PlayerEvent.HarvestCheck(player,state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,true);ToolRequirementEvents.onHarvestCheck(e);check(!e.canHarvest(),"Wrong tool or empty hand");
            }
            player.hand=new ItemStack(Items.DIAMOND_PICKAXE);
            check(ToolLevels.capability(player.hand).isEmpty(),"Diamond must not be an active progression tier");
            check(ToolLevels.satisfies(player.hand,ModBlocks.MYTHRIL_ORE.get().defaultBlockState(),7),"Saved Diamond access");
            check(!ToolLevels.satisfies(player.hand,state,8),"Diamond future resource access");
            var legacyState=ModBlocks.MYTHRIL_ORE.get().defaultBlockState();
            var legacyEvent=new PlayerEvent.HarvestCheck(player,legacyState,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,player.hand.isCorrectToolForDrops(legacyState));
            ToolRequirementEvents.onHarvestCheck(legacyEvent);check(legacyEvent.canHarvest(),"Actual saved Diamond harvest event");
            player.hand=new ItemStack(Items.IRON_PICKAXE);
            var nativeFalse=new PlayerEvent.HarvestCheck(player,Blocks.OBSIDIAN.defaultBlockState(),EmptyBlockGetter.INSTANCE,BlockPos.ZERO,false);
            ToolRequirementEvents.onHarvestCheck(nativeFalse);check(!nativeFalse.canHarvest(),"Unrelated vanilla restriction overridden");
            var speed=new PlayerEvent.BreakSpeed(player,Blocks.IRON_ORE.defaultBlockState(),10,BlockPos.ZERO);ToolRequirementEvents.onBreakSpeed(speed);close(speed.getNewSpeed(),10,"Unrelated block speed changed");
            check(new ItemStack(Items.GOLDEN_PICKAXE).isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()),"Gold configured Iron access missing");
            check(!player.hand.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"Native iron/obsidian restriction changed");
            // A third-party tool opts in through tags AND native tool rules, not item identity.
            var compat=new ItemStack(Items.BLAZE_ROD);compat.set(DataComponents.TOOL,new Tool(List.of(Tool.Rule.minesAndDrops(List.of(fixture),5)),1,1));
            items.put(ItemTags.PICKAXES,List.of(Items.BLAZE_ROD.builtInRegistryHolder()));BuiltInRegistries.ITEM.bindTags(items);
            check(ToolLevels.capability(compat).isEmpty(),"Unknown tool became Wood");
            items.put(ToolLevels.tag(8),List.of(Items.BLAZE_ROD.builtInRegistryHolder()));BuiltInRegistries.ITEM.bindTags(items);
            check(ToolLevels.satisfies(compat,state,8)&&compat.isCorrectToolForDrops(state),"Tag-based compatibility opt-in");
            player.hand=compat;var compatibleEvent=new PlayerEvent.HarvestCheck(player,state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,compat.isCorrectToolForDrops(state));
            ToolRequirementEvents.onHarvestCheck(compatibleEvent);check(compatibleEvent.canHarvest(),"Actual compatible tool harvest event");
        } finally {BuiltInRegistries.BLOCK.bindTags(oldBlocks);BuiltInRegistries.ITEM.bindTags(oldItems);}
        System.out.println("PASS: 8 capabilities x all 8 requirements, actual native tool rules and event handlers; incorrect tools, Diamond legacy scope, unknown-tool opt-in, native denials and speed restrictions.");
    }
    private static void resourceMatrix() throws Exception {
        var expectedCaps=Map.of("wooden",2,"stone",3,"copper",4,"golden",4,"iron",4,"steel",6,"netherite",7,"mythril",8);
        for(var profile:MetalEquipmentProfiles.activeProfiles())check(profile.miningLevel()==expectedCaps.get(profile.id()),"Requested capability "+profile.id());
        var expectedResources=Map.ofEntries(Map.entry(Blocks.STONE,2),Map.entry(Blocks.COBBLESTONE,2),
                Map.entry(Blocks.COAL_ORE,2),Map.entry(Blocks.DEEPSLATE_COAL_ORE,2),Map.entry(Blocks.IRON_ORE,3),
                Map.entry(Blocks.DEEPSLATE_IRON_ORE,3),Map.entry(Blocks.COPPER_ORE,3),Map.entry(Blocks.DEEPSLATE_COPPER_ORE,3),
                Map.entry(Blocks.GOLD_ORE,4),Map.entry(Blocks.DEEPSLATE_GOLD_ORE,4),Map.entry(Blocks.GOLD_BLOCK,4),Map.entry(Blocks.NETHER_GOLD_ORE,2),
                Map.entry(ModBlocks.PLATINUM_ORE.get(),6),Map.entry(ModBlocks.MYTHRIL_ORE.get(),7));
        for(var entry:expectedResources.entrySet())check(BlockRequirements.configured().get(BuiltInRegistries.BLOCK.getKey(entry.getKey())).equals(entry.getValue()),"Requested resource gate "+entry.getKey());
        var old=tags(BuiltInRegistries.BLOCK);var bindings=new HashMap<>(old);
        var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        var player=(TestPlayer)((sun.misc.Unsafe)f.get(null)).allocateInstance(TestPlayer.class);
        try {
            var targets=BlockRequirements.configured().keySet().stream().map(BuiltInRegistries.BLOCK::get).toList();
            bindings.put(BlockTags.MINEABLE_WITH_PICKAXE,targets.stream().map(b->(Holder<Block>)b.builtInRegistryHolder()).toList());
            for(int n=1;n<=8;n++) {
                int level=n;
                bindings.put(BlockRequirements.tag(n),BlockRequirements.configured().entrySet().stream()
                        .filter(e->e.getValue()==level).map(e->(Holder<Block>)BuiltInRegistries.BLOCK.get(e.getKey()).builtInRegistryHolder()).toList());
            }
            // Native tier denials deliberately contradict the desired Gold/Copper access.
            blockTag(bindings,BlockTags.INCORRECT_FOR_GOLD_TOOL,Blocks.IRON_ORE,Blocks.DEEPSLATE_IRON_ORE,
                    Blocks.COPPER_ORE,Blocks.DEEPSLATE_COPPER_ORE,Blocks.GOLD_ORE,Blocks.DEEPSLATE_GOLD_ORE,Blocks.GOLD_BLOCK,Blocks.OBSIDIAN);
            blockTag(bindings,BlockTags.INCORRECT_FOR_STONE_TOOL,Blocks.GOLD_ORE,Blocks.DEEPSLATE_GOLD_ORE,Blocks.GOLD_BLOCK,Blocks.OBSIDIAN);
            blockTag(bindings,BlockTags.INCORRECT_FOR_WOODEN_TOOL,Blocks.IRON_ORE,Blocks.COPPER_ORE,Blocks.GOLD_ORE,Blocks.OBSIDIAN);
            BuiltInRegistries.BLOCK.bindTags(bindings);
            for(var profile:MetalEquipmentProfiles.activeProfiles()) {
                var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                        profile.vanilla()?"minecraft":"katzencraftmetals",profile.id()+"_pickaxe")));
                player.hand=stack;
                for(var entry:BlockRequirements.configured().entrySet()) {
                    var state=BuiltInRegistries.BLOCK.get(entry.getKey()).defaultBlockState();
                    boolean expected=profile.miningLevel()>=entry.getValue();
                    var event=new PlayerEvent.HarvestCheck(player,state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,stack.isCorrectToolForDrops(state));
                    ToolRequirementEvents.onHarvestCheck(event);
                    check(event.canHarvest()==expected,"Real resource drop gate: "+profile.id()+" / "+entry.getKey());
                    var veto=new PlayerEvent.HarvestCheck(player,state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO,false);
                    ToolRequirementEvents.onHarvestCheck(veto);check(!veto.canHarvest(),"Foreign veto overwritten");
                }
                check(!stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())||profile.miningLevel()>=6,"Unrelated native restriction broadened");
            }
            for(var wrong:List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_AXE),new ItemStack(Items.BLAZE_ROD))) {
                player.hand=wrong;
                for(var block:targets) {
                    var e=new PlayerEvent.HarvestCheck(player,block.defaultBlockState(),EmptyBlockGetter.INSTANCE,BlockPos.ZERO,true);
                    ToolRequirementEvents.onHarvestCheck(e);check(!e.canHarvest(),"Hand/wrong/unknown tool bypass: "+block);
                }
            }
        } finally {BuiltInRegistries.BLOCK.bindTags(old);}
        System.out.println("PASS: actual native drop permission + denial event for every configured resource and all 8 pickaxes; normal/deepslate variants, stone family, Nether Gold, storage and foreign vetoes.");
    }
    private static void vanillaRecipes() throws Exception {
        var root=Path.of(System.getProperty("katzencraft.equipment.root"));
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var savedTags=tags(BuiltInRegistries.ITEM);var craftingTags=new HashMap<>(savedTags);
        craftingTags.put(ItemTags.PLANKS,List.of(Items.OAK_PLANKS.builtInRegistryHolder()));
        craftingTags.put(TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("c:rods/wooden")),List.of(Items.STICK.builtInRegistryHolder()));
        BuiltInRegistries.ITEM.bindTags(craftingTags);
        try {
        for(String kind:List.of("sword","pickaxe","axe","shovel","hoe")) {
            String file="data/minecraft/recipe/wooden_"+kind+".json";
            try(var stream=EquipmentChecks.class.getClassLoader().getResourceAsStream(file)) {
                check(stream!=null,"Native wooden recipe missing "+kind);
                var json=JsonParser.parseString(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var recipe=net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops,json).getOrThrow();
                var pattern=json.getAsJsonArray("pattern");var keys=json.getAsJsonObject("key");
                int width=pattern.get(0).getAsString().length(),height=pattern.size();List<ItemStack> slots=new ArrayList<>();
                for(var row:pattern)for(char c:row.getAsString().toCharArray()) {
                    if(c==' ')slots.add(ItemStack.EMPTY);
                    else {var ingredient=keys.getAsJsonObject(String.valueOf(c));slots.add(new ItemStack(ingredient.has("tag")?(ingredient.get("tag").getAsString().equals("minecraft:planks")?Items.OAK_PLANKS:Items.STICK):BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingredient.get("item").getAsString()))));}
                }
                var input=net.minecraft.world.item.crafting.CraftingInput.of(width,height,slots);
                var shaped=(net.minecraft.world.item.crafting.CraftingRecipe)recipe;
                check(shaped.matches(input,null),"Native wooden grid mismatch "+kind+" JSON="+json+" slots="+slots+" ingredients="+shaped.getIngredients().stream().map(i->Arrays.toString(i.getItems())).toList());
                check(BuiltInRegistries.ITEM.getKey(shaped.assemble(input,RegistryAccess.EMPTY).getItem()).getPath().equals("wooden_"+kind),"Native wooden result "+kind);
            }
            for(String source:List.of("src/main/resources","src/generated/resources"))check(!Files.exists(root.resolve(source).resolve(file)),"Wood override remains");
        }
        for(String kind:List.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots")) {
            var json=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/datapacks/disabled_diamond_equipment/data/minecraft/recipe/diamond_"+kind+".json"))).getAsJsonObject();
            check(net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodec(net.minecraft.world.item.crafting.Recipe.CODEC)
                    .parse(ops,json).getOrThrow().isEmpty(),"Diamond recipe is not actually disabled: "+kind);
        }
        } finally {BuiltInRegistries.ITEM.bindTags(savedTags);}
        System.out.println("PASS: five native Wooden recipes decoded, crafted and assembled; nine Diamond crafting overrides conditionally disabled.");
    }
    private static boolean overridesChecked;
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void checkStartupOverrides(ModifyDefaultComponentsEvent event){
        overrides(event);overridesChecked=true;
    }
    private static void overrides(ModifyDefaultComponentsEvent event){
        Map<Item,net.minecraft.core.component.DataComponentMap> ironDefaults=new HashMap<>();
        for(var kind:MetalEquipmentProfiles.ToolKind.values()){
            var item=MetalEquipmentProfiles.IRON.vanillaTool(kind);ironDefaults.put(item,item.components());
        }
        var original=new ItemStack(Items.IRON_PICKAXE);
        original.setDamageValue(73);original.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Saved pick"));
        original.set(DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        original.set(DataComponents.REPAIR_COST,7);
        var beforePatch=original.getComponentsPatch();var savedDefaults=Items.IRON_PICKAXE.components();
        var beforeAttrs=attributes(original);var beforeTool=original.get(DataComponents.TOOL);
        var p=MetalEquipmentProfiles.vanillaEquipment("iron").miningLevel(4).pickaxeDurability(300).build();
        MetalEquipmentComponentEvents.apply(event,p);
        var changed=new ItemStack(Items.IRON_PICKAXE);check(changed.getMaxDamage()==300,"Explicit vanilla durability");
        check(original.getDamageValue()==73&&original.getComponentsPatch().equals(beforePatch),"Saved components rewritten");
        check(attributes(changed).equals(beforeAttrs),"Unspecified attributes changed");
        check(changed.getEnchantmentValue()==14&&Items.IRON_PICKAXE.isValidRepairItem(changed,new ItemStack(Items.IRON_INGOT)),"Unspecified enchant/repair changed");
        var attack=MetalEquipmentProfiles.vanillaEquipment("iron").miningLevel(4).pickaxeDamage(6).attackSpeed(MetalEquipmentProfiles.ToolKind.PICKAXE,1.4f).build();
        MetalEquipmentComponentEvents.apply(event,attack);
        close(total(new ItemStack(Items.IRON_PICKAXE),true),6,"Explicit total damage override");close(total(new ItemStack(Items.IRON_PICKAXE),false),1.4f,"Explicit attack speed override");
        Map<Item,net.minecraft.core.component.DataComponentMap> armorDefaults=new HashMap<>();
        for(var type:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS)) {
            var item=MetalEquipmentProfiles.IRON.vanillaArmor(type);armorDefaults.put(item,item.components());
        }
        var armor=MetalEquipmentProfiles.vanillaEquipment("iron").miningLevel(4)
                .helmetDurability(200).bootsDurability(210).defense(ArmorItem.Type.HELMET,4).armorToughness(1).build();
        MetalEquipmentComponentEvents.apply(event,armor);
        check(new ItemStack(Items.IRON_HELMET).getMaxDamage()==200&&new ItemStack(Items.IRON_BOOTS).getMaxDamage()==210,"Independent vanilla armor durability");
        check(new ItemStack(Items.IRON_CHESTPLATE).getMaxDamage()==240,"Unspecified vanilla armor durability");
        double defense=new ItemStack(Items.IRON_HELMET).getAttributeModifiers().modifiers().stream()
                .filter(e->e.attribute().equals(Attributes.ARMOR)).mapToDouble(e->e.modifier().amount()).sum();
        check(defense==4,"Vanilla armor defense component override");
        for(var type:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS)) {
            var item=MetalEquipmentProfiles.IRON.vanillaArmor(type);
            var defaults=armorDefaults.get(item);
            event.modify(item,patch->{
                patch.set(DataComponents.MAX_DAMAGE,defaults.get(DataComponents.MAX_DAMAGE));
                var originalAttributes=defaults.get(DataComponents.ATTRIBUTE_MODIFIERS);
                if(originalAttributes!=null)patch.set(DataComponents.ATTRIBUTE_MODIFIERS,originalAttributes);
                else patch.remove(DataComponents.ATTRIBUTE_MODIFIERS);
            });
        }
        var foreignRules=new ArrayList<>(beforeTool.rules());
        foreignRules.add(new Tool.Rule(HolderSet.direct(Blocks.GOLD_ORE.builtInRegistryHolder()),Optional.of(2f),Optional.of(false)));
        event.modify(Items.IRON_PICKAXE,patch->patch.set(DataComponents.TOOL,new Tool(foreignRules,beforeTool.defaultMiningSpeed(),beforeTool.damagePerBlock())));
        MetalEquipmentComponentEvents.apply(event,MetalEquipmentProfiles.IRON);
        var patched=new ItemStack(Items.IRON_PICKAXE).get(DataComponents.TOOL);
        int firstGrant=-1,firstForeign=-1;
        for(int i=0;i<patched.rules().size();i++) {
            var rule=patched.rules().get(i);
            if(rule.blocks().unwrapKey().isEmpty()&&rule.blocks().contains(Blocks.GOLD_ORE.builtInRegistryHolder())) {
                if(rule.correctForDrops().equals(Optional.of(false))&&firstForeign<0)firstForeign=i;
                if(rule.correctForDrops().equals(Optional.of(true))&&firstGrant<0)firstGrant=i;
            }
        }
        check(firstForeign>=0&&firstForeign<firstGrant,"Foreign component denial lost priority");
        check(patched.rules().stream().filter(r->r.speed().equals(Optional.of(2f))).count()==1,"Foreign speed rule duplicated or altered");
        // Restore the actual defaults while the startup hook is legally open.
        event.modify(Items.IRON_PICKAXE,patch->{
            patch.set(DataComponents.MAX_DAMAGE,savedDefaults.get(DataComponents.MAX_DAMAGE));
            patch.set(DataComponents.TOOL,savedDefaults.get(DataComponents.TOOL));
            patch.set(DataComponents.ATTRIBUTE_MODIFIERS,savedDefaults.get(DataComponents.ATTRIBUTE_MODIFIERS));
        });
        for(var entry:ironDefaults.entrySet())if(entry.getKey()!=Items.IRON_PICKAXE)
            event.modify(entry.getKey(),patch->patch.set(DataComponents.TOOL,entry.getValue().get(DataComponents.TOOL)));

        System.out.println("PASS: supported startup component overrides preserve vanilla IDs, custom stack patches, damage, repair and unspecified attributes.");
    }
}


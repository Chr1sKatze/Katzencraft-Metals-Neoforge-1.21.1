package net.chriskatze.katzencraftmetals.config;

import net.chriskatze.katzencraftmetals.metal.MetalCatalog;
import net.chriskatze.katzencraftmetals.metal.MetalDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Explicit catalog resources and basic stone only; unrelated blocks retain native rules. */
public final class BlockRequirements {
    public static final TagKey<Block> LEGACY_DIAMOND = TagKey.create(Registries.BLOCK,id("legacy_diamond_access"));
    private static final List<TagKey<Block>> TAGS=java.util.stream.IntStream.rangeClosed(1,8)
            .mapToObj(level->TagKey.create(Registries.BLOCK,id("needs_mining_level_"+level))).toList();
    private static final Map<ResourceLocation,Integer> CONFIGURED=create();
    public static TagKey<Block> tag(int level){MiningLevels.validate(level);return TAGS.get(level-1);}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("katzencraftmetals",path);}
    private static Map<ResourceLocation,Integer> create(){
        Map<ResourceLocation,Integer> result=new LinkedHashMap<>();
        for(var metal:MetalCatalog.values())for(var form:metal.forms().entrySet()) {
            Integer level=form.getValue()==MetalDefinition.Form.STORAGE_BLOCK?metal.storageMiningLevel():
                    MetalDefinition.isOre(form.getValue())?metal.oreMiningLevel(form.getValue()):null;
            if(level!=null)result.put(form.getKey(),level);
        }
        for(String block:List.of("cut_steel_block","steel_bars","steel_chain","steel_pressure_plate","steel_lever",
                "steel_door","cut_steel_stairs","cut_steel_slab","steel_ladder"))result.put(id(block),MiningLevels.IRON);
        result.put(id("steel_button"),MiningLevels.STONE);
        result.put(id("steel_trapdoor"),MiningLevels.WOOD);
        result.put(id("foundry_controller"), MiningLevels.STONE);
        // Non-metal starting resources. Metal ore/storage levels belong to MetalCatalog.
        for(String block:List.of("stone","cobblestone","deepslate","cobbled_deepslate",
                "granite","diorite","andesite","tuff","calcite","blackstone","basalt","smooth_basalt",
                "coal_ore","deepslate_coal_ore"))
            result.put(ResourceLocation.withDefaultNamespace(block),MiningLevels.WOOD);
        return Collections.unmodifiableMap(result);
    }
    public static Map<ResourceLocation,Integer> configured(){return CONFIGURED;}
    /** Explicit existing content only, never a substring match against future metal names. */
    public static Set<ResourceLocation> legacyDiamondBlocks(){
        Set<ResourceLocation> result=new LinkedHashSet<>();
        for(String name:List.of("steel","platinum","mythril")) {
            var metal=MetalCatalog.get(id(name)).orElseThrow();
            for(var form:metal.forms().keySet())if(CONFIGURED.containsKey(form))result.add(form);
        }
        for(String block:List.of("cut_steel_block","steel_bars","steel_chain","steel_pressure_plate","steel_lever",
                "steel_door","cut_steel_stairs","cut_steel_slab","steel_ladder","steel_button","steel_trapdoor"))result.add(id(block));
        return Set.copyOf(result);
    }
    public static OptionalInt find(BlockState state){
        // Explicit pack tags may override the declared requirement; highest wins.
        for(int level=8;level>=1;level--)if(state.is(tag(level)))return OptionalInt.of(level);
        Integer level=CONFIGURED.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return level==null?OptionalInt.empty():OptionalInt.of(level);
    }
    public static int getRequirement(Block block){return find(block.defaultBlockState()).orElse(-1);}
    private BlockRequirements() {}
}

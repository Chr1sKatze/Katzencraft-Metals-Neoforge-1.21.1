package net.chriskatze.katzencraftmetals.config;

import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import java.util.*;

/** Unknown is absent, never Wood. Packs can opt other tools into mining_capability_1..8. */
public final class ToolLevels {
    private static final List<TagKey<Item>> TAGS=java.util.stream.IntStream.rangeClosed(1,8)
            .mapToObj(level->TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("katzencraftmetals","mining_capability_"+level))).toList();
    private static final Map<ResourceLocation,Integer> CAPABILITIES=capabilities();
    private static Map<ResourceLocation,Integer> capabilities(){
        Map<ResourceLocation,Integer> result=new HashMap<>();
        for(var p:MetalEquipmentProfiles.activeProfiles())result.put(ResourceLocation.fromNamespaceAndPath(
                p.vanilla()?"minecraft":"katzencraftmetals",p.id()+"_pickaxe"),p.miningLevel());
        return Map.copyOf(result);
    }
    public static TagKey<Item> tag(int level){MiningLevels.validate(level);return TAGS.get(level-1);}
    public static boolean pickaxe(ItemStack stack){return !stack.isEmpty()&&(stack.is(ItemTags.PICKAXES)||stack.canPerformAction(ItemAbilities.PICKAXE_DIG));}
    public static OptionalInt capability(ItemStack stack){
        if(!pickaxe(stack))return OptionalInt.empty();
        for(int level=8;level>=1;level--)if(stack.is(tag(level)))return OptionalInt.of(level);
        Integer capability=CAPABILITIES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return capability==null?OptionalInt.empty():OptionalInt.of(capability);
    }
    public static int getLevel(ItemStack stack){return capability(stack).orElse(-1);}
    /** This is an additional restriction, never permission to override a native/event rejection. */
    public static boolean satisfies(ItemStack stack,BlockState state,int requirement){
        MiningLevels.validate(requirement);
        if(!state.is(BlockTags.MINEABLE_WITH_PICKAXE)||!pickaxe(stack))return false;
        if(stack.is(Items.DIAMOND_PICKAXE)&&state.is(BlockRequirements.LEGACY_DIAMOND))return requirement<=7;
        return capability(stack).orElse(-1)>=requirement;
    }
    private ToolLevels() {}
}

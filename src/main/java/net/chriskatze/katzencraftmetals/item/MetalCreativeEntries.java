package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.metal.StandardMetalForms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Compatibility anchors preserve the old layout; new standard forms append in catalog order. */
public final class MetalCreativeEntries {
    private static final List<String> LEGACY_ORDER = List.of("steel_nugget", "steel_ingot", "platinum_nugget",
            "platinum_ingot", "mythril_nugget", "mythril_ingot", "platinum_block", "mythril_block", "steel_block");
    private static final Map<String, String> BEFORE = Map.of(
            "steel_nugget", "raw_platinum", "steel_ingot", "raw_platinum",
            "platinum_nugget", "raw_platinum", "platinum_ingot", "raw_mythril",
            "mythril_nugget", "raw_mythril", "mythril_ingot", "sapphire_gem",
            "platinum_block", "cut_steel_block", "mythril_block", "cut_steel_block", "steel_block", "cut_steel_block");
    private MetalCreativeEntries() {}
    public static void add(BuildCreativeModeTabContentsEvent event) {
        boolean ingredients = event.getTabKey() == CreativeModeTabs.INGREDIENTS;
        boolean blocks = event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS;
        if (!ingredients && !blocks) return;
        var entries = StandardMetalForms.entries().stream().filter(form -> form.form().block == blocks)
                .sorted(Comparator.comparingInt(form -> {
                    int index = LEGACY_ORDER.indexOf(form.id().getPath());
                    return index < 0 ? Integer.MAX_VALUE : index;
                })).toList();
        for (var form : entries) {
            ItemStack stack = new ItemStack(form.item());
            String anchor = BEFORE.get(form.id().getPath());
            if (anchor == null) event.accept(stack);
            else event.insertBefore(new ItemStack(BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.fromNamespaceAndPath("katzencraftmetals", anchor)).orElseThrow()), stack,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}

package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.Set;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class CatoDisableItemsEvents {

    public static final Set<ResourceLocation> DISABLED_ITEMS = Set.of(
            id("minecraft", "enchanting_table"),

            id("minecraft", "wooden_sword"),
            id("minecraft", "wooden_pickaxe"),
            id("minecraft", "wooden_axe"),
            id("minecraft", "wooden_shovel"),
            id("minecraft", "wooden_hoe")
    );

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        for (ResourceLocation id : DISABLED_ITEMS) {
            Item item = BuiltInRegistries.ITEM.get(id);

            if (item == net.minecraft.world.item.Items.AIR) {
                continue;
            }

            event.remove(
                    item.getDefaultInstance(),
                    net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }

    public static boolean isDisabled(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        return DISABLED_ITEMS.contains(id);
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
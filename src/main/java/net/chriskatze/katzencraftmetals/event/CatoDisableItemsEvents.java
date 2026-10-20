package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.Set;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CatoDisableItemsEvents {

    public static final Set<ResourceLocation> DISABLED_ITEMS = Set.of(
            id("minecraft", "enchanting_table"),
            id("minecraft", "diamond_sword"),
            id("minecraft", "diamond_pickaxe"),
            id("minecraft", "diamond_axe"),
            id("minecraft", "diamond_shovel"),
            id("minecraft", "diamond_hoe"),
            id("minecraft", "diamond_helmet"),
            id("minecraft", "diamond_chestplate"),
            id("minecraft", "diamond_leggings"),
            id("minecraft", "diamond_boots"),
            id("minecraft", "netherite_upgrade_smithing_template")
    );

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // Filter by identity, including enchanted/custom-component variants, in both lists.
        var hidden = java.util.stream.Stream.concat(event.getParentEntries().stream(), event.getSearchEntries().stream())
                .filter(CatoDisableItemsEvents::isDisabled).toList();
        for (ItemStack stack : hidden) {
            event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    @SubscribeEvent
    public static void addEquipmentRecipePack(AddPackFindersEvent event) {
        event.addPackFinders(id(KatzencraftMetalsMod.MODID, "datapacks/netherite_progression"),
                PackType.SERVER_DATA, Component.literal("Katzencraft Netherite progression"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
        // NeoForge replaces vanilla equipment recipes too; ordinary mod-pack order is not sufficient.
        event.addPackFinders(id(KatzencraftMetalsMod.MODID, "datapacks/disabled_diamond_equipment"),
                PackType.SERVER_DATA, Component.literal("Katzencraft disabled Diamond equipment recipes"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
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
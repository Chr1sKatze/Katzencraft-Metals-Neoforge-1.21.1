package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.chriskatze.katzencraftmetals.util.ModTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class ToolRequirementEvents {

    private static final int STONE_LEVEL = 1;
    private static final int IRON_LEVEL = 2;
    private static final int STEEL_LEVEL = 3;
    private static final int DIAMOND_LEVEL = 4;
    private static final int MYTHRIL_LEVEL = 5;

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {

        BlockState state = event.getTargetBlock();

        int toolLevel = getToolLevel(
                event.getEntity().getMainHandItem()
        );

        if (state.is(ModTags.Blocks.NEEDS_STEEL_TOOL)) {
            event.setCanHarvest(toolLevel >= 3);
            return;
        }

        if (state.is(ModTags.Blocks.NEEDS_DIAMOND_TOOL)) {
            event.setCanHarvest(toolLevel >= 4);
            return;
        }

        if (state.is(ModTags.Blocks.NEEDS_MYTHRIL_TOOL)) {
            event.setCanHarvest(toolLevel >= 5);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {

        int toolLevel = getToolLevel(
                event.getEntity().getMainHandItem()
        );

        BlockState state = event.getState();

        int requiredLevel = 0;

        if (state.is(ModTags.Blocks.NEEDS_STEEL_TOOL)) {
            requiredLevel = 3;
        } else if (state.is(ModTags.Blocks.NEEDS_DIAMOND_TOOL)) {
            requiredLevel = 4;
        } else if (state.is(ModTags.Blocks.NEEDS_MYTHRIL_TOOL)) {
            requiredLevel = 5;
        }

        if (requiredLevel > 0 && toolLevel < requiredLevel) {

            // make mining painfully slow
            event.setNewSpeed(event.getNewSpeed() * 0.01F);
        }
    }

    private static int getToolLevel(ItemStack stack) {

        Item item = stack.getItem();

        // Stone
        if (item == Items.STONE_PICKAXE) {
            return STONE_LEVEL;
        }

        // Iron
        if (item == Items.IRON_PICKAXE) {
            return IRON_LEVEL;
        }

        // Steel
        if (item == ModItems.STEEL_PICKAXE.get()) {
            return STEEL_LEVEL;
        }

        // Diamond
        if (item == Items.DIAMOND_PICKAXE) {
            return DIAMOND_LEVEL;
        }

        // Mythril
        if (item == ModItems.MYTHRIL_PICKAXE.get()) {
            return MYTHRIL_LEVEL;
        }

        return 0;
    }
}
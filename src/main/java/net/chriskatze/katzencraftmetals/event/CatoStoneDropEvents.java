package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class CatoStoneDropEvents {

    private static final Map<Block, Item> HAND_MINABLE_STONES = Map.ofEntries(
            Map.entry(Blocks.STONE, Items.COBBLESTONE),
            Map.entry(Blocks.DEEPSLATE, Items.COBBLED_DEEPSLATE),

            Map.entry(Blocks.GRANITE, Items.GRANITE),
            Map.entry(Blocks.DIORITE, Items.DIORITE),
            Map.entry(Blocks.ANDESITE, Items.ANDESITE),

            Map.entry(Blocks.TUFF, Items.TUFF),
            Map.entry(Blocks.CALCITE, Items.CALCITE),

            Map.entry(Blocks.BLACKSTONE, Items.BLACKSTONE),

            Map.entry(Blocks.BASALT, Items.BASALT),
            Map.entry(Blocks.SMOOTH_BASALT, Items.BASALT)
    );

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Player player = event.getPlayer();

        // Vanilla pickaxes already get proper drops.
        if (player.getMainHandItem().getItem() instanceof PickaxeItem) {
            return;
        }

        Item dropItem = HAND_MINABLE_STONES.get(event.getState().getBlock());

        if (dropItem == null) {
            return;
        }

        BlockPos pos = event.getPos();

        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                new ItemStack(dropItem)
        ));
    }
}
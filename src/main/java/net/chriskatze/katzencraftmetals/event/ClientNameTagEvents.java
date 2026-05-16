package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.screen.NameTagRenameScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT)
public class ClientNameTagEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.NAME_TAG)) {
            return;
        }

        String currentName = "";

        if (stack.has(DataComponents.CUSTOM_NAME)) {
            currentName = stack.getHoverName().getString();
        }

        Minecraft.getInstance().setScreen(
                new NameTagRenameScreen(event.getHand(), currentName)
        );

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
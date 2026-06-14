package net.chriskatze.katzencraftmetals.mixin;

import net.chriskatze.katzencraftmetals.event.CatoDisableItemsEvents;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public class CreativeModeInventoryScreenMixin {

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Inject(
            method = "scrollTo",
            at = @At("HEAD")
    )
    private void katzencraftmetals$removeDisabledItems(
            float pos,
            CallbackInfo ci
    ) {
        items.removeIf(CatoDisableItemsEvents::isDisabled);
    }
}
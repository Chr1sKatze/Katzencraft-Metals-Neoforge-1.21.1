package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KatzencraftMetalsMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOUNDRY =
            TABS.register("foundry", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.katzencraftmetals.foundry"))
                    .icon(() -> ModBlocks.FOUNDRY_TABLE.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.FOUNDRY_CONTROLLER.get());
                        output.accept(ModBlocks.FOUNDRY_TANK.get());
                        output.accept(ModBlocks.FOUNDRY_BASIN.get());
                        output.accept(ModBlocks.FOUNDRY_TABLE.get());
                    })
                    .build());

    private ModCreativeModeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}

package net.chriskatze.katzencraftmetals.client;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.menu.ModMenuTypes;
import net.chriskatze.katzencraftmetals.screen.CrusherScreen;
import net.chriskatze.katzencraftmetals.screen.KatzencraftAnvilScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static net.chriskatze.katzencraftmetals.KatzencraftMetalsMod.LOGGER;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ModClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

        LOGGER.info("Katzencraft Metals client setup");
        LOGGER.info("Minecraft user: {}", Minecraft.getInstance().getUser().getName());

        event.enqueueWork(() -> {

            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.STEEL_TRAPDOOR.get(),
                    RenderType.cutout()
            );

            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.STEEL_DOOR.get(),
                    RenderType.cutout()
            );

            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.STEEL_BARS.get(),
                    RenderType.cutout()
            );
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.STEEL_DOOR.get(),
                    RenderType.cutout()
            );
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {

        event.register(
                ModMenuTypes.CRUSHER_MENU.get(),
                CrusherScreen::new
        );

        event.register(
                ModMenuTypes.KATZENCRAFT_ANVIL_MENU.get(),
                KatzencraftAnvilScreen::new
        );
    }
}
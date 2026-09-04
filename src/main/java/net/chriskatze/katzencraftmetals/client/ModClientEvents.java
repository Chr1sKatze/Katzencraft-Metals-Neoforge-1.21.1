package net.chriskatze.katzencraftmetals.client;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.chriskatze.katzencraftmetals.client.renderer.CastingCauldronBlockEntityRenderer;
import net.chriskatze.katzencraftmetals.client.renderer.FoundryControllerBlockEntityRenderer;
import net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetBlockEntityRenderer;
import net.chriskatze.katzencraftmetals.client.renderer.FoundryTankConnectedFrameModel;
import net.chriskatze.katzencraftmetals.menu.ModMenuTypes;
import net.chriskatze.katzencraftmetals.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import static net.chriskatze.katzencraftmetals.KatzencraftMetalsMod.LOGGER;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        value = Dist.CLIENT
)
public class ModClientEvents {

    @SubscribeEvent
    public static void onClientSetup(
            FMLClientSetupEvent event
    ) {
        LOGGER.info("Katzencraft Metals client setup");
        LOGGER.info(
                "Minecraft user: {}",
                Minecraft.getInstance().getUser().getName()
        );

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
                    ModBlocks.STEEL_CHAIN.get(),
                    RenderType.cutout()
            );
        });
    }

    @SubscribeEvent
    public static void registerAdditionalModels(
            ModelEvent.RegisterAdditional event
    ) {
        FoundryTankConnectedFrameModel.registerAdditionalModels(event);
    }

    @SubscribeEvent
    public static void modifyBakingResult(
            ModelEvent.ModifyBakingResult event
    ) {
        FoundryTankConnectedFrameModel.modifyBakingResult(event);
    }

    @SubscribeEvent
    public static void registerScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenuTypes.CRUSHER_MENU.get(),
                CrusherScreen::new
        );

        event.register(
                ModMenuTypes.FUEL_CHAMBER_MENU.get(),
                FuelChamberScreen::new
        );

        event.register(
                ModMenuTypes.FOUNDRY_CONTROLLER_MENU.get(),
                FoundryControllerScreen::new
        );

        event.register(
                ModMenuTypes.FOUNDRY_FAUCET_OUTPUT_MENU.get(),
                FoundryFaucetOutputScreen::new
        );

        event.register(
                ModMenuTypes.KATZENCRAFT_ANVIL_MENU.get(),
                KatzencraftAnvilScreen::new
        );
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.CASTING_CAULDRON.get(),
                CastingCauldronBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                ModBlockEntities.FOUNDRY_FAUCET.get(),
                FoundryFaucetBlockEntityRenderer::new
        );

        /*
         * One Controller BER owns the complete active Tank vessel. There is no
         * Tank BER registration anymore.
         */
        event.registerBlockEntityRenderer(
                ModBlockEntities.FOUNDRY_CONTROLLER.get(),
                FoundryControllerBlockEntityRenderer::new
        );
    }
}

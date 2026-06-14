package net.chriskatze.katzencraftmetals;

import com.mojang.logging.LogUtils;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.chriskatze.katzencraftmetals.config.CatoToolTierConfig;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.chriskatze.katzencraftmetals.menu.ModMenuTypes;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.chriskatze.katzencraftmetals.screen.CrusherScreen;
import net.chriskatze.katzencraftmetals.screen.KatzencraftAnvilScreen;
import net.chriskatze.katzencraftmetals.world.KatzencraftWorldRulesData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(KatzencraftMetalsMod.MODID)
public class KatzencraftMetalsMod {

    public static final String MODID = "katzencraftmetals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KatzencraftMetalsMod(IEventBus modEventBus, ModContainer modContainer) {

        // Register your content
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        // Register creative tab injection
        modEventBus.addListener(this::addCreative);

        // Register for general events (optional but fine)
        NeoForge.EVENT_BUS.register(this);

        // Configs
        modContainer.registerConfig(
                ModConfig.Type.COMMON, CatoToolTierConfig.SPEC, "katzencraftmetals-tooltiers.toml");
    }

    // =========================
    // VANILLA CREATIVE TABS
    // =========================

    private void addCreative(BuildCreativeModeTabContentsEvent event) {

        // INGREDIENTS
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {

            event.accept(ModItems.CRUSHED_COAL);

            event.accept(ModItems.IRON_POWDER);

            event.accept(ModItems.STEEL_NUGGET);
            event.accept(ModItems.STEEL_INGOT);
            event.accept(ModItems.STEEL_POWDER);

            event.accept(ModItems.PLATINUM_NUGGET);
            event.accept(ModItems.RAW_PLATINUM);
            event.accept(ModItems.PLATINUM_INGOT);
            event.accept(ModItems.PLATINUM_POWDER);

            event.accept(ModItems.MYTHRIL_NUGGET);
            event.accept(ModItems.RAW_MYTHRIL);
            event.accept(ModItems.MYTHRIL_INGOT);
            event.accept(ModItems.MYTHRIL_POWDER);

            event.accept(ModItems.SAPPHIRE_GEM);
            event.accept(ModItems.AMETHYST_GEM);
        }

        // BUILDING BLOCKS
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {

            event.accept(ModBlocks.PLATINUM_BLOCK);

            event.accept(ModBlocks.MYTHRIL_BLOCK);

            event.accept(ModBlocks.STEEL_BLOCK);
            event.accept(ModBlocks.STEEL_DOOR);
            event.accept(ModBlocks.STEEL_TRAPDOOR);
            event.accept(ModBlocks.STEEL_LEVER);
            event.accept(ModBlocks.STEEL_BARS);
            event.accept(ModBlocks.STEEL_CHAIN);
            event.accept(ModBlocks.STEEL_PRESSURE_PLATE);
            event.accept(ModBlocks.STEEL_BUTTON);
        }

        // NATURAL BLOCKS
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {

            event.accept(ModBlocks.PLATINUM_ORE);
            event.accept(ModBlocks.DEEPSLATE_PLATINUM_ORE);
            event.accept(ModBlocks.NETHER_PLATINUM_ORE);
            event.accept(ModBlocks.END_PLATINUM_ORE);

            event.accept(ModBlocks.MYTHRIL_ORE);
            event.accept(ModBlocks.DEEPSLATE_MYTHRIL_ORE);
            event.accept(ModBlocks.NETHER_MYTHRIL_ORE);
            event.accept(ModBlocks.END_MYTHRIL_ORE);
        }

        // FUNCTIONAL BLOCKS
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.CRUSHER);
        }

        // COMBAT
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {

            event.accept(ModItems.STEEL_SWORD);
            event.accept(ModItems.STEEL_HELMET);
            event.accept(ModItems.STEEL_CHESTPLATE);
            event.accept(ModItems.STEEL_LEGGINGS);
            event.accept(ModItems.STEEL_BOOTS);

            event.accept(ModItems.MYTHRIL_SWORD);
            event.accept(ModItems.MYTHRIL_HELMET);
            event.accept(ModItems.MYTHRIL_CHESTPLATE);
            event.accept(ModItems.MYTHRIL_LEGGINGS);
            event.accept(ModItems.MYTHRIL_BOOTS);
        }

        // TOOLS & UTILITIES
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {

            event.accept(ModItems.STEEL_PICKAXE);
            event.accept(ModItems.STEEL_AXE);
            event.accept(ModItems.STEEL_SHOVEL);
            event.accept(ModItems.STEEL_HOE);

            event.accept(ModItems.MYTHRIL_PICKAXE);
            event.accept(ModItems.MYTHRIL_AXE);
            event.accept(ModItems.MYTHRIL_SHOVEL);
            event.accept(ModItems.MYTHRIL_HOE);
        }
    }

    // =========================
    // OPTIONAL EVENTS
    // =========================

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Katzencraft Metals server starting");
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getServer() == null) return;
        if (level.dimension() != Level.OVERWORLD) return;

        KatzencraftWorldRulesData data = KatzencraftWorldRulesData.get(level);

        if (data.isApplied()) return;

        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());

        data.setApplied();

        LOGGER.info("Applied Katzencraft Metals default gamerules.");
    }

    @EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Katzencraft Metals client setup");
            LOGGER.info("Minecraft user: {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.CRUSHER_MENU.get(), CrusherScreen::new);
            event.register(ModMenuTypes.KATZENCRAFT_ANVIL_MENU.get(), KatzencraftAnvilScreen::new);
        }
    }
}
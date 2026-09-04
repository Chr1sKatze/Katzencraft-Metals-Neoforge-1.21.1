package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KatzencraftMetalsMod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.STEEL_BLOCK.get());
        blockWithItem(ModBlocks.CUT_STEEL_BLOCK.get());
        customLever(ModBlocks.STEEL_LEVER.get(), "steel");
        vanillaPressurePlate((PressurePlateBlock) ModBlocks.STEEL_PRESSURE_PLATE.get(), ModBlocks.STEEL_BLOCK.get());
        vanillaDoor((DoorBlock) ModBlocks.STEEL_DOOR.get(), "steel");
        vanillaChain((ChainBlock) ModBlocks.STEEL_CHAIN.get(), "steel");
        vanillaBars((IronBarsBlock) ModBlocks.STEEL_BARS.get(), "steel");
        vanillaStairs(ModBlocks.CUT_STEEL_STAIRS.get(), ModBlocks.CUT_STEEL_BLOCK.get());
        vanillaSlab(ModBlocks.CUT_STEEL_SLAB.get(), ModBlocks.CUT_STEEL_BLOCK.get());
        vanillaButton((ButtonBlock) ModBlocks.STEEL_BUTTON.get(), ModBlocks.STEEL_BLOCK.get());
        vanillaTrapdoor((TrapDoorBlock) ModBlocks.STEEL_TRAPDOOR.get(), "steel");
        vanillaLadder((LadderBlock) ModBlocks.STEEL_LADDER.get(), "steel");

        blockWithItem(ModBlocks.PLATINUM_BLOCK.get());
        blockWithItem(ModBlocks.PLATINUM_ORE.get());
        blockWithItem(ModBlocks.DEEPSLATE_PLATINUM_ORE.get());
        blockWithItem(ModBlocks.NETHER_PLATINUM_ORE.get());
        blockWithItem(ModBlocks.END_PLATINUM_ORE.get());

        blockWithItem(ModBlocks.MYTHRIL_BLOCK.get());
        blockWithItem(ModBlocks.MYTHRIL_ORE.get());
        blockWithItem(ModBlocks.DEEPSLATE_MYTHRIL_ORE.get());
        blockWithItem(ModBlocks.NETHER_MYTHRIL_ORE.get());
        blockWithItem(ModBlocks.END_MYTHRIL_ORE.get());

        blockWithItem(ModBlocks.CRUSHER.get());
        blockWithItem(ModBlocks.FUEL_CHAMBER.get());

        ModelFile controllerModel = cubeAll(ModBlocks.FOUNDRY_CONTROLLER.get());
        horizontalBlock(ModBlocks.FOUNDRY_CONTROLLER.get(), controllerModel);
        simpleBlockItem(ModBlocks.FOUNDRY_CONTROLLER.get(), controllerModel);

        foundryTankBlock();

        ModelFile castingCauldronModel = new ModelFile.UncheckedModelFile(modLoc("block/casting_cauldron"));
        getVariantBuilder(ModBlocks.CASTING_CAULDRON.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(castingCauldronModel)
                        .build());
        simpleBlockItem(ModBlocks.CASTING_CAULDRON.get(), castingCauldronModel);

        ModelFile faucetModel = new ModelFile.UncheckedModelFile(modLoc("block/foundry_faucet"));
        horizontalBlock(ModBlocks.FOUNDRY_FAUCET.get(), faucetModel);
        simpleBlockItem(ModBlocks.FOUNDRY_FAUCET.get(), faucetModel);
    }


    private void foundryTankBlock() {
        Block tank =
                ModBlocks.FOUNDRY_TANK.get();

        ModelFile itemModel =
                models().getExistingFile(
                        modLoc("block/foundry_tank")
                );

        MultiPartBlockStateBuilder builder =
                getMultipartBuilder(
                        tank
                );

        /*
         * Every exposed Tank face gets a guaranteed panel surface.
         *
         * The conditional frame pieces below are only the metal border. If
         * those pieces are the only geometry, highly connected Tanks can lose
         * most or all visible surface geometry. The panel is slightly behind
         * the frame plane to avoid z-fighting.
         */
        tankPart(
                builder,
                tankPanelModel(
                        "foundry_tank_panel_north",
                        Direction.NORTH,
                        0.0f, 0.0f, 0.11f,
                        16.0f, 16.0f, 0.12f,
                        "side",
                        3.0f, 1.0f, 13.0f, 15.0f
                ),
                FoundryTankBlock.NORTH,
                false
        );

        tankPart(
                builder,
                tankPanelModel(
                        "foundry_tank_panel_south",
                        Direction.SOUTH,
                        0.0f, 0.0f, 15.88f,
                        16.0f, 16.0f, 15.89f,
                        "side",
                        3.0f, 1.0f, 13.0f, 15.0f
                ),
                FoundryTankBlock.SOUTH,
                false
        );

        tankPart(
                builder,
                tankPanelModel(
                        "foundry_tank_panel_west",
                        Direction.WEST,
                        0.11f, 0.0f, 0.0f,
                        0.12f, 16.0f, 16.0f,
                        "side",
                        3.0f, 1.0f, 13.0f, 15.0f
                ),
                FoundryTankBlock.WEST,
                false
        );

        tankPart(
                builder,
                tankPanelModel(
                        "foundry_tank_panel_east",
                        Direction.EAST,
                        15.88f, 0.0f, 0.0f,
                        15.89f, 16.0f, 16.0f,
                        "side",
                        3.0f, 1.0f, 13.0f, 15.0f
                ),
                FoundryTankBlock.EAST,
                false
        );

        tankIsolatedHorizontalPart(
                builder,
                tankPanelModel(
                        "foundry_tank_panel_up",
                        Direction.UP,
                        0.0f, 15.88f, 0.0f,
                        16.0f, 15.89f, 16.0f,
                        "top",
                        0.0f, 0.0f, 16.0f, 16.0f
                ),
                FoundryTankBlock.UP
        );

        tankIsolatedHorizontalPart(
                builder,
                tankPanelModel(
                        "foundry_tank_panel_down",
                        Direction.DOWN,
                        0.0f, 0.11f, 0.0f,
                        16.0f, 0.12f, 16.0f,
                        "top",
                        0.0f, 0.0f, 16.0f, 16.0f
                ),
                FoundryTankBlock.DOWN
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_north_top",
                        Direction.NORTH,
                        0.0f,
                        15.0f,
                        0.0f,
                        16.0f,
                        16.0f,
                        0.1f,
                        "side",
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f
                ),
                FoundryTankBlock.NORTH,
                false,
                FoundryTankBlock.UP,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_north_bottom",
                        Direction.NORTH,
                        0.0f,
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f,
                        0.1f,
                        "side",
                        0.0f,
                        15.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.NORTH,
                false,
                FoundryTankBlock.DOWN,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_north_west",
                        Direction.NORTH,
                        0.0f,
                        0.0f,
                        0.0f,
                        3.0f,
                        16.0f,
                        0.1f,
                        "side",
                        0.0f,
                        0.0f,
                        3.0f,
                        16.0f
                ),
                FoundryTankBlock.NORTH,
                false,
                FoundryTankBlock.WEST,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_north_east",
                        Direction.NORTH,
                        13.0f,
                        0.0f,
                        0.0f,
                        16.0f,
                        16.0f,
                        0.1f,
                        "side",
                        13.0f,
                        0.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.NORTH,
                false,
                FoundryTankBlock.EAST,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_south_top",
                        Direction.SOUTH,
                        0.0f,
                        15.0f,
                        15.9f,
                        16.0f,
                        16.0f,
                        16.0f,
                        "side",
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f
                ),
                FoundryTankBlock.SOUTH,
                false,
                FoundryTankBlock.UP,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_south_bottom",
                        Direction.SOUTH,
                        0.0f,
                        0.0f,
                        15.9f,
                        16.0f,
                        1.0f,
                        16.0f,
                        "side",
                        0.0f,
                        15.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.SOUTH,
                false,
                FoundryTankBlock.DOWN,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_south_west",
                        Direction.SOUTH,
                        0.0f,
                        0.0f,
                        15.9f,
                        3.0f,
                        16.0f,
                        16.0f,
                        "side",
                        13.0f,
                        0.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.SOUTH,
                false,
                FoundryTankBlock.WEST,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_south_east",
                        Direction.SOUTH,
                        13.0f,
                        0.0f,
                        15.9f,
                        16.0f,
                        16.0f,
                        16.0f,
                        "side",
                        0.0f,
                        0.0f,
                        3.0f,
                        16.0f
                ),
                FoundryTankBlock.SOUTH,
                false,
                FoundryTankBlock.EAST,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_west_top",
                        Direction.WEST,
                        0.0f,
                        15.0f,
                        0.0f,
                        0.1f,
                        16.0f,
                        16.0f,
                        "side",
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f
                ),
                FoundryTankBlock.WEST,
                false,
                FoundryTankBlock.UP,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_west_bottom",
                        Direction.WEST,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.1f,
                        1.0f,
                        16.0f,
                        "side",
                        0.0f,
                        15.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.WEST,
                false,
                FoundryTankBlock.DOWN,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_west_north",
                        Direction.WEST,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.1f,
                        16.0f,
                        3.0f,
                        "side",
                        13.0f,
                        0.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.WEST,
                false,
                FoundryTankBlock.NORTH,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_west_south",
                        Direction.WEST,
                        0.0f,
                        0.0f,
                        13.0f,
                        0.1f,
                        16.0f,
                        16.0f,
                        "side",
                        0.0f,
                        0.0f,
                        3.0f,
                        16.0f
                ),
                FoundryTankBlock.WEST,
                false,
                FoundryTankBlock.SOUTH,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_east_top",
                        Direction.EAST,
                        15.9f,
                        15.0f,
                        0.0f,
                        16.0f,
                        16.0f,
                        16.0f,
                        "side",
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f
                ),
                FoundryTankBlock.EAST,
                false,
                FoundryTankBlock.UP,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_east_bottom",
                        Direction.EAST,
                        15.9f,
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f,
                        16.0f,
                        "side",
                        0.0f,
                        15.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.EAST,
                false,
                FoundryTankBlock.DOWN,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_east_north",
                        Direction.EAST,
                        15.9f,
                        0.0f,
                        0.0f,
                        16.0f,
                        16.0f,
                        3.0f,
                        "side",
                        0.0f,
                        0.0f,
                        3.0f,
                        16.0f
                ),
                FoundryTankBlock.EAST,
                false,
                FoundryTankBlock.NORTH,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_side_east_south",
                        Direction.EAST,
                        15.9f,
                        0.0f,
                        13.0f,
                        16.0f,
                        16.0f,
                        16.0f,
                        "side",
                        13.0f,
                        0.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.EAST,
                false,
                FoundryTankBlock.SOUTH,
                false
        );

        /*
         * Horizontal perimeter ownership:
         *
         * NORTH/SOUTH strips own the 1x1 corner pixels. WEST/EAST are split
         * into a middle run plus optional one-pixel end extensions. This
         * reproduces the old renderer's sideMinZ/sideMaxZ rule and guarantees
         * that a normal top/bottom corner has exactly one quad instead of two
         * or three coplanar quads.
         */
        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_top_north",
                        Direction.UP,
                        0.0f,
                        15.9f,
                        0.0f,
                        16.0f,
                        16.0f,
                        1.0f,
                        "top",
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f
                ),
                FoundryTankBlock.UP,
                false,
                FoundryTankBlock.NORTH,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_top_south",
                        Direction.UP,
                        0.0f,
                        15.9f,
                        15.0f,
                        16.0f,
                        16.0f,
                        16.0f,
                        "top",
                        0.0f,
                        15.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.UP,
                false,
                FoundryTankBlock.SOUTH,
                false
        );

        tankHorizontalSidePerimeterParts(
                builder,
                "foundry_tank_top_west",
                Direction.UP,
                FoundryTankBlock.UP,
                FoundryTankBlock.WEST,
                0.0f,
                1.0f,
                15.9f,
                16.0f,
                0.0f,
                1.0f
        );

        tankHorizontalSidePerimeterParts(
                builder,
                "foundry_tank_top_east",
                Direction.UP,
                FoundryTankBlock.UP,
                FoundryTankBlock.EAST,
                15.0f,
                16.0f,
                15.9f,
                16.0f,
                15.0f,
                16.0f
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_bottom_north",
                        Direction.DOWN,
                        0.0f,
                        0.0f,
                        0.0f,
                        16.0f,
                        0.1f,
                        1.0f,
                        "top",
                        0.0f,
                        0.0f,
                        16.0f,
                        1.0f
                ),
                FoundryTankBlock.DOWN,
                false,
                FoundryTankBlock.NORTH,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        "foundry_tank_bottom_south",
                        Direction.DOWN,
                        0.0f,
                        0.0f,
                        15.0f,
                        16.0f,
                        0.1f,
                        16.0f,
                        "top",
                        0.0f,
                        15.0f,
                        16.0f,
                        16.0f
                ),
                FoundryTankBlock.DOWN,
                false,
                FoundryTankBlock.SOUTH,
                false
        );

        tankHorizontalSidePerimeterParts(
                builder,
                "foundry_tank_bottom_west",
                Direction.DOWN,
                FoundryTankBlock.DOWN,
                FoundryTankBlock.WEST,
                0.0f,
                1.0f,
                0.0f,
                0.1f,
                0.0f,
                1.0f
        );

        tankHorizontalSidePerimeterParts(
                builder,
                "foundry_tank_bottom_east",
                Direction.DOWN,
                FoundryTankBlock.DOWN,
                FoundryTankBlock.EAST,
                15.0f,
                16.0f,
                0.0f,
                0.1f,
                15.0f,
                16.0f
        );

        /*
         * Do NOT add diagonal-aware corrections directly to the multipart
         * BlockState. Six direct-neighbor booleans cannot encode diagonals.
         *
         * Instead, generate small standalone correction models below. The
         * client-side FoundryTankConnectedFrameModel selects them from
         * world-aware ModelData during chunk rebuilds. This works both with and
         * without a Controller and requires no Tank BlockEntity.
         */
        generateFoundryTankConnectedCorrectionModels();

        simpleBlockItem(
                tank,
                itemModel
        );
    }


    /**
     * Standalone world-aware correction models.
     *
     * These models are NOT referenced by the multipart BlockState directly.
     * FoundryTankConnectedFrameModel registers them as additional baked models
     * and selects them only when diagonal topology requires a frame that the
     * six direct-neighbor BlockState properties cannot describe.
     *
     * Correction planes sit 0.02 model units (~0.00125 block) outward from the
     * baked Tank plane. That prevents z-fighting at a junction where one edge
     * is static and the diagonal-aware edge is dynamic-model geometry.
     */
    private void generateFoundryTankConnectedCorrectionModels() {
        final float O = 0.02f;

        // -------------------------
        // Vertical side-face edges
        // -------------------------
        correctionSideModel("foundry_tank_correction_side_north_top",
                Direction.NORTH, 0.0f, 15.0f, -O, 16.0f, 16.0f, 0.08f,
                0.0f, 0.0f, 16.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_side_north_bottom",
                Direction.NORTH, 0.0f, 0.0f, -O, 16.0f, 1.0f, 0.08f,
                0.0f, 15.0f, 16.0f, 16.0f);
        correctionMarkerRailModel(
                "foundry_tank_correction_side_north_west",
                Direction.NORTH,
                true,
                O
        );
        correctionMarkerRailModel(
                "foundry_tank_correction_side_north_east",
                Direction.NORTH,
                false,
                O
        );

        correctionSideModel("foundry_tank_correction_side_south_top",
                Direction.SOUTH, 0.0f, 15.0f, 15.92f, 16.0f, 16.0f, 16.0f + O,
                0.0f, 0.0f, 16.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_side_south_bottom",
                Direction.SOUTH, 0.0f, 0.0f, 15.92f, 16.0f, 1.0f, 16.0f + O,
                0.0f, 15.0f, 16.0f, 16.0f);
        /*
         * SOUTH local-left is EAST and local-right is WEST.
         */
        correctionMarkerRailModel(
                "foundry_tank_correction_side_south_east",
                Direction.SOUTH,
                true,
                O
        );
        correctionMarkerRailModel(
                "foundry_tank_correction_side_south_west",
                Direction.SOUTH,
                false,
                O
        );

        correctionSideModel("foundry_tank_correction_side_west_top",
                Direction.WEST, -O, 15.0f, 0.0f, 0.08f, 16.0f, 16.0f,
                0.0f, 0.0f, 16.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_side_west_bottom",
                Direction.WEST, -O, 0.0f, 0.0f, 0.08f, 1.0f, 16.0f,
                0.0f, 15.0f, 16.0f, 16.0f);
        /*
         * WEST local-left is SOUTH and local-right is NORTH.
         */
        correctionMarkerRailModel(
                "foundry_tank_correction_side_west_south",
                Direction.WEST,
                true,
                O
        );
        correctionMarkerRailModel(
                "foundry_tank_correction_side_west_north",
                Direction.WEST,
                false,
                O
        );

        correctionSideModel("foundry_tank_correction_side_east_top",
                Direction.EAST, 15.92f, 15.0f, 0.0f, 16.0f + O, 16.0f, 16.0f,
                0.0f, 0.0f, 16.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_side_east_bottom",
                Direction.EAST, 15.92f, 0.0f, 0.0f, 16.0f + O, 1.0f, 16.0f,
                0.0f, 15.0f, 16.0f, 16.0f);
        /*
         * EAST local-left is NORTH and local-right is SOUTH.
         */
        correctionMarkerRailModel(
                "foundry_tank_correction_side_east_north",
                Direction.EAST,
                true,
                O
        );
        correctionMarkerRailModel(
                "foundry_tank_correction_side_east_south",
                Direction.EAST,
                false,
                O
        );

        // -------------------------
        // UP / DOWN perimeter edges
        // -------------------------
        correctionTopModel("foundry_tank_correction_up_north",
                Direction.UP, 0.0f, 15.92f, 0.0f, 16.0f, 16.0f + O, 1.0f,
                0.0f, 0.0f, 16.0f, 1.0f);
        correctionTopModel("foundry_tank_correction_up_south",
                Direction.UP, 0.0f, 15.92f, 15.0f, 16.0f, 16.0f + O, 16.0f,
                0.0f, 15.0f, 16.0f, 16.0f);
        correctionTopModel("foundry_tank_correction_up_west",
                Direction.UP, 0.0f, 15.92f, 0.0f, 1.0f, 16.0f + O, 16.0f,
                0.0f, 0.0f, 1.0f, 16.0f);
        correctionTopModel("foundry_tank_correction_up_east",
                Direction.UP, 15.0f, 15.92f, 0.0f, 16.0f, 16.0f + O, 16.0f,
                15.0f, 0.0f, 16.0f, 16.0f);

        correctionTopModel("foundry_tank_correction_down_north",
                Direction.DOWN, 0.0f, -O, 0.0f, 16.0f, 0.08f, 1.0f,
                0.0f, 0.0f, 16.0f, 1.0f);
        correctionTopModel("foundry_tank_correction_down_south",
                Direction.DOWN, 0.0f, -O, 15.0f, 16.0f, 0.08f, 16.0f,
                0.0f, 15.0f, 16.0f, 16.0f);
        correctionTopModel("foundry_tank_correction_down_west",
                Direction.DOWN, 0.0f, -O, 0.0f, 1.0f, 0.08f, 16.0f,
                0.0f, 0.0f, 1.0f, 16.0f);
        correctionTopModel("foundry_tank_correction_down_east",
                Direction.DOWN, 15.0f, -O, 0.0f, 16.0f, 0.08f, 16.0f,
                15.0f, 0.0f, 16.0f, 16.0f);

        // ------------------------------------------
        // Historical vertical side-frame seam joins
        // ------------------------------------------
        /*
         * These reproduce FoundryTankSideFrameRenderer.renderMarkerJoin()
         * from the original Tank renderer exactly:
         *
         * - a LOWER Tank whose exposed face continues above contributes a
         *   1x1 extension at its TOP edge;
         * - the Tank ABOVE contributes a 2x1 extension at its BOTTOM edge.
         *
         * v12 invented generic 1x1 side-corner caps instead. Those caps could
         * overlap an existing rail (the yellow z-fighting pixel in the
         * screenshot) while the real historical join pixel remained absent
         * one cell away (the pink position).
         *
         * The models below are face-local equivalents of the old helper, but
         * use the current lightened #frame texture and the same tiny outward
         * correction offset as the other connected-frame models.
         */

        // NORTH: local left = WEST (low X), local right = EAST (high X).
        correctionSideModel("foundry_tank_correction_join_north_left_above",
                Direction.NORTH, 1.0f, 15.0f, -O, 2.0f, 16.0f, 0.08f,
                1.0f, 0.0f, 2.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_north_right_above",
                Direction.NORTH, 14.0f, 15.0f, -O, 15.0f, 16.0f, 0.08f,
                14.0f, 0.0f, 15.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_north_left_below",
                Direction.NORTH, 1.0f, 0.0f, -O, 3.0f, 1.0f, 0.08f,
                1.0f, 15.0f, 3.0f, 16.0f);
        correctionSideModel("foundry_tank_correction_join_north_right_below",
                Direction.NORTH, 13.0f, 0.0f, -O, 15.0f, 1.0f, 0.08f,
                13.0f, 15.0f, 15.0f, 16.0f);

        // SOUTH: local left = EAST (high X), local right = WEST (low X).
        correctionSideModel("foundry_tank_correction_join_south_left_above",
                Direction.SOUTH, 14.0f, 15.0f, 15.92f, 15.0f, 16.0f, 16.0f + O,
                1.0f, 0.0f, 2.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_south_right_above",
                Direction.SOUTH, 1.0f, 15.0f, 15.92f, 2.0f, 16.0f, 16.0f + O,
                14.0f, 0.0f, 15.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_south_left_below",
                Direction.SOUTH, 13.0f, 0.0f, 15.92f, 15.0f, 1.0f, 16.0f + O,
                1.0f, 15.0f, 3.0f, 16.0f);
        correctionSideModel("foundry_tank_correction_join_south_right_below",
                Direction.SOUTH, 1.0f, 0.0f, 15.92f, 3.0f, 1.0f, 16.0f + O,
                13.0f, 15.0f, 15.0f, 16.0f);

        // WEST: local left = SOUTH (high Z), local right = NORTH (low Z).
        correctionSideModel("foundry_tank_correction_join_west_left_above",
                Direction.WEST, -O, 15.0f, 14.0f, 0.08f, 16.0f, 15.0f,
                1.0f, 0.0f, 2.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_west_right_above",
                Direction.WEST, -O, 15.0f, 1.0f, 0.08f, 16.0f, 2.0f,
                14.0f, 0.0f, 15.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_west_left_below",
                Direction.WEST, -O, 0.0f, 13.0f, 0.08f, 1.0f, 15.0f,
                1.0f, 15.0f, 3.0f, 16.0f);
        correctionSideModel("foundry_tank_correction_join_west_right_below",
                Direction.WEST, -O, 0.0f, 1.0f, 0.08f, 1.0f, 3.0f,
                13.0f, 15.0f, 15.0f, 16.0f);

        // EAST: local left = NORTH (low Z), local right = SOUTH (high Z).
        correctionSideModel("foundry_tank_correction_join_east_left_above",
                Direction.EAST, 15.92f, 15.0f, 1.0f, 16.0f + O, 16.0f, 2.0f,
                1.0f, 0.0f, 2.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_east_right_above",
                Direction.EAST, 15.92f, 15.0f, 14.0f, 16.0f + O, 16.0f, 15.0f,
                14.0f, 0.0f, 15.0f, 1.0f);
        correctionSideModel("foundry_tank_correction_join_east_left_below",
                Direction.EAST, 15.92f, 0.0f, 1.0f, 16.0f + O, 1.0f, 3.0f,
                1.0f, 15.0f, 3.0f, 16.0f);
        correctionSideModel("foundry_tank_correction_join_east_right_below",
                Direction.EAST, 15.92f, 0.0f, 13.0f, 16.0f + O, 1.0f, 15.0f,
                13.0f, 15.0f, 15.0f, 16.0f);

        // -----------------------------------
        // 1x1 SIDE concave-corner cap models
        // -----------------------------------
        /*
         * IMPORTANT:
         * v16 placed these at local 1..2 / 14..15.
         * v16.1 moved them the wrong way to 2..3 / 13..14.
         * The verified direction is the opposite:
         *     left edge  -> 0..1
         *     right edge -> 15..16
         */
        /*
         * These fill the single same-face re-entrant corner square in a 2x2
         * side-face cavity:
         *
         *   XX
         *   X.
         *
         * The cap belongs to the block diagonally opposite the missing cell.
         */

        // NORTH: local left = WEST, local right = EAST.
        correctionSolidCapModel("foundry_tank_correction_cap_north_top_west",
                Direction.NORTH,
                0.0f, 15.0f, -O,
                1.0f, 16.0f, 0.08f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_north_top_east",
                Direction.NORTH,
                15.0f, 15.0f, -O,
                16.0f, 16.0f, 0.08f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_north_bottom_west",
                Direction.NORTH,
                0.0f, 0.0f, -O,
                1.0f, 1.0f, 0.08f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_north_bottom_east",
                Direction.NORTH,
                15.0f, 0.0f, -O,
                16.0f, 1.0f, 0.08f
        );

        // SOUTH: local left = EAST, local right = WEST.
        correctionSolidCapModel("foundry_tank_correction_cap_south_top_east",
                Direction.SOUTH,
                15.0f, 15.0f, 15.92f,
                16.0f, 16.0f, 16.0f + O
        );
        correctionSolidCapModel("foundry_tank_correction_cap_south_top_west",
                Direction.SOUTH,
                0.0f, 15.0f, 15.92f,
                1.0f, 16.0f, 16.0f + O
        );
        correctionSolidCapModel("foundry_tank_correction_cap_south_bottom_east",
                Direction.SOUTH,
                15.0f, 0.0f, 15.92f,
                16.0f, 1.0f, 16.0f + O
        );
        correctionSolidCapModel("foundry_tank_correction_cap_south_bottom_west",
                Direction.SOUTH,
                0.0f, 0.0f, 15.92f,
                1.0f, 1.0f, 16.0f + O
        );

        // WEST: local left = SOUTH, local right = NORTH.
        correctionSolidCapModel("foundry_tank_correction_cap_west_top_south",
                Direction.WEST,
                -O, 15.0f, 15.0f,
                0.08f, 16.0f, 16.0f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_west_top_north",
                Direction.WEST,
                -O, 15.0f, 0.0f,
                0.08f, 16.0f, 1.0f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_west_bottom_south",
                Direction.WEST,
                -O, 0.0f, 15.0f,
                0.08f, 1.0f, 16.0f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_west_bottom_north",
                Direction.WEST,
                -O, 0.0f, 0.0f,
                0.08f, 1.0f, 1.0f
        );

        // EAST: local left = NORTH, local right = SOUTH.
        correctionSolidCapModel("foundry_tank_correction_cap_east_top_north",
                Direction.EAST,
                15.92f, 15.0f, 0.0f,
                16.0f + O, 16.0f, 1.0f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_east_top_south",
                Direction.EAST,
                15.92f, 15.0f, 15.0f,
                16.0f + O, 16.0f, 16.0f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_east_bottom_north",
                Direction.EAST,
                15.92f, 0.0f, 0.0f,
                16.0f + O, 1.0f, 1.0f
        );
        correctionSolidCapModel("foundry_tank_correction_cap_east_bottom_south",
                Direction.EAST,
                15.92f, 0.0f, 15.0f,
                16.0f + O, 1.0f, 16.0f
        );

        // -------------------------
        // 1x1 UP / DOWN concave caps
        // -------------------------
        correctionCap("foundry_tank_correction_cap_up_north_west",
                Direction.UP, 0.0f, 15.92f, 0.0f, 1.0f, 16.0f + O, 1.0f);
        correctionCap("foundry_tank_correction_cap_up_north_east",
                Direction.UP, 15.0f, 15.92f, 0.0f, 16.0f, 16.0f + O, 1.0f);
        correctionCap("foundry_tank_correction_cap_up_south_west",
                Direction.UP, 0.0f, 15.92f, 15.0f, 1.0f, 16.0f + O, 16.0f);
        correctionCap("foundry_tank_correction_cap_up_south_east",
                Direction.UP, 15.0f, 15.92f, 15.0f, 16.0f, 16.0f + O, 16.0f);

        correctionCap("foundry_tank_correction_cap_down_north_west",
                Direction.DOWN, 0.0f, -O, 0.0f, 1.0f, 0.08f, 1.0f);
        correctionCap("foundry_tank_correction_cap_down_north_east",
                Direction.DOWN, 15.0f, -O, 0.0f, 16.0f, 0.08f, 1.0f);
        correctionCap("foundry_tank_correction_cap_down_south_west",
                Direction.DOWN, 0.0f, -O, 15.0f, 1.0f, 0.08f, 16.0f);
        correctionCap("foundry_tank_correction_cap_down_south_east",
                Direction.DOWN, 15.0f, -O, 15.0f, 16.0f, 0.08f, 16.0f);
    }

    /**
     * Rebuilds one diagonal-aware vertical side rail exactly like the original
     * FoundryTankSideFrameRenderer instead of relying on a 3-pixel-wide
     * transparent texture slice.
     *
     * Geometry:
     *   - permanent 1px rail;
     *   - at marker rows 3, 7, 11: a 2x1 arm;
     *   - directly below every arm: one 1x1 pixel.
     *
     * This explicit ownership is important at re-entrant corners. A generic
     * 1x1 corner cap can overlap the marker arm (z-fighting), while a cropped
     * 3px strip can make the lower one-pixel part ambiguous at a block seam.
     */
    private BlockModelBuilder correctionMarkerRailModel(
            String name,
            Direction face,
            boolean leftSide,
            float outwardOffset
    ) {
        BlockModelBuilder model =
                models().withExistingParent(
                                name,
                                mcLoc("block/block")
                        )
                        .renderType("cutout")
                        .texture(
                                "particle",
                                modLoc("block/foundry_tank_frame")
                        )
                        .texture(
                                "frame",
                                modLoc("block/foundry_tank_frame")
                        )
                        .ao(false);

        /*
         * Permanent one-pixel rail.
         */
        if (leftSide) {
            addCorrectionMarkerRect(
                    model,
                    face,
                    outwardOffset,
                    0.0f,
                    1.0f,
                    0.0f,
                    16.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    16.0f
            );
        } else {
            addCorrectionMarkerRect(
                    model,
                    face,
                    outwardOffset,
                    15.0f,
                    16.0f,
                    0.0f,
                    16.0f,
                    15.0f,
                    0.0f,
                    16.0f,
                    16.0f
            );
        }

        int[] markerRows = {
                3,
                7,
                11
        };

        for (int row : markerRows) {
            float armMinY =
                    16.0f - (row + 1.0f);

            float armMaxY =
                    16.0f - row;

            float lowerMinY =
                    16.0f - (row + 2.0f);

            float lowerMaxY =
                    16.0f - (row + 1.0f);

            if (leftSide) {
                /*
                 * ##
                 */
                addCorrectionMarkerRect(
                        model,
                        face,
                        outwardOffset,
                        1.0f,
                        3.0f,
                        armMinY,
                        armMaxY,
                        1.0f,
                        row,
                        3.0f,
                        row + 1.0f
                );

                /*
                 * #
                 * The exact one-pixel piece that was still missing in the
                 * reported inner-corner view.
                 */
                addCorrectionMarkerRect(
                        model,
                        face,
                        outwardOffset,
                        1.0f,
                        2.0f,
                        lowerMinY,
                        lowerMaxY,
                        1.0f,
                        row + 1.0f,
                        2.0f,
                        row + 2.0f
                );
            } else {
                addCorrectionMarkerRect(
                        model,
                        face,
                        outwardOffset,
                        13.0f,
                        15.0f,
                        armMinY,
                        armMaxY,
                        13.0f,
                        row,
                        15.0f,
                        row + 1.0f
                );

                addCorrectionMarkerRect(
                        model,
                        face,
                        outwardOffset,
                        14.0f,
                        15.0f,
                        lowerMinY,
                        lowerMaxY,
                        14.0f,
                        row + 1.0f,
                        15.0f,
                        row + 2.0f
                );
            }
        }

        return model;
    }

    /**
     * Adds one face-local side rectangle to a correction model.
     *
     * The conversion is copied from the old FoundryTankCasingQuads coordinate
     * system:
     *   NORTH: horizontal grows west -> east
     *   SOUTH: horizontal grows east -> west
     *   WEST:  horizontal grows south -> north
     *   EAST:  horizontal grows north -> south
     *
     * The outward face keeps the same known-good reversed-U convention used by
     * the current static Tank frames; the inward/back face keeps normal U.
     */
    private void addCorrectionMarkerRect(
            BlockModelBuilder model,
            Direction face,
            float outwardOffset,
            float minHorizontal,
            float maxHorizontal,
            float minY,
            float maxY,
            float minU,
            float minV,
            float maxU,
            float maxV
    ) {
        float fromX;
        float fromZ;
        float toX;
        float toZ;

        switch (face) {
            case NORTH -> {
                fromX = minHorizontal;
                toX = maxHorizontal;
                fromZ = -outwardOffset;
                toZ = 0.08f;
            }
            case SOUTH -> {
                fromX = 16.0f - maxHorizontal;
                toX = 16.0f - minHorizontal;
                fromZ = 15.92f;
                toZ = 16.0f + outwardOffset;
            }
            case WEST -> {
                fromX = -outwardOffset;
                toX = 0.08f;
                fromZ = 16.0f - maxHorizontal;
                toZ = 16.0f - minHorizontal;
            }
            case EAST -> {
                fromX = 15.92f;
                toX = 16.0f + outwardOffset;
                fromZ = minHorizontal;
                toZ = maxHorizontal;
            }
            default -> throw new IllegalArgumentException(
                    "Foundry Tank marker rail must use a horizontal face."
            );
        }

        model.element()
                .from(
                        fromX,
                        minY,
                        fromZ
                )
                .to(
                        toX,
                        maxY,
                        toZ
                )
                .face(face)
                .texture("#frame")
                .uvs(
                        maxU,
                        minV,
                        minU,
                        maxV
                )
                .end()
                .face(face.getOpposite())
                .texture("#frame")
                .uvs(
                        minU,
                        minV,
                        maxU,
                        maxV
                )
                .end()
                .end();
    }

    /**
     * Closed 1x1 concave side-corner cap.
     *
     * The flat v16 cap fixed a pure standing L:
     *
     *   XX
     *   X.
     *
     * but it only emitted the two faces parallel to the Tank face. If another
     * Tank exists one block in front of the missing quadrant, that corner is a
     * true 3-D turn and the tiny orthogonal/end face of the cap becomes visible.
     *
     * Build the exact same confirmed-good 1x1 cap volume as a CLOSED miniature
     * box. The front/back pixel therefore remains in the same position, while
     * the four edge faces close the 3-D corner. No second cap is added, so this
     * does not reintroduce the TURN_* z-fighting problem.
     */
    /**
     * 1x1 concave side-corner cap.
     *
     * IMPORTANT:
     * This pixel belongs ONLY to one vertical Tank side plane.
     *
     * The previous helper rendered all six faces of the tiny cap volume
     * (N/S/E/W/UP/DOWN). That made the otherwise-correct corner pixel visible
     * on top/bottom and perpendicular faces where no frame pixel should exist.
     *
     * Render exactly the same confirmed-good cap volume and position, but only
     * on:
     *   - the intended vertical side face; and
     *   - its opposite face, so the pixel is also visible from inside the Tank.
     *
     * No orthogonal edge faces are generated.
     */
    private BlockModelBuilder correctionSolidCapModel(
            String name,
            Direction face,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ
    ) {
        BlockModelBuilder.ElementBuilder element =
                models().withExistingParent(
                                name,
                                mcLoc("block/block")
                        )
                        .renderType("cutout")
                        .texture(
                                "particle",
                                modLoc("block/foundry_tank_frame")
                        )
                        .texture(
                                "frame",
                                modLoc("block/foundry_tank_frame")
                        )
                        .ao(false)
                        .element()
                        .from(
                                fromX,
                                fromY,
                                fromZ
                        )
                        .to(
                                toX,
                                toY,
                                toZ
                        );

        element.face(face)
                .texture("#frame")
                .uvs(
                        0.0f,
                        0.0f,
                        1.0f,
                        1.0f
                )
                .end();

        element.face(face.getOpposite())
                .texture("#frame")
                .uvs(
                        0.0f,
                        0.0f,
                        1.0f,
                        1.0f
                )
                .end();

        return element.end();
    }

    private void correctionSideModel(
            String name,
            Direction face,
            float fromX, float fromY, float fromZ,
            float toX, float toY, float toZ,
            float minU, float minV, float maxU, float maxV
    ) {
        tankSideModel(
                name,
                face,
                fromX, fromY, fromZ,
                toX, toY, toZ,
                "side",
                minU, minV, maxU, maxV
        );
    }

    private void correctionTopModel(
            String name,
            Direction face,
            float fromX, float fromY, float fromZ,
            float toX, float toY, float toZ,
            float minU, float minV, float maxU, float maxV
    ) {
        tankSideModel(
                name,
                face,
                fromX, fromY, fromZ,
                toX, toY, toZ,
                "top",
                minU, minV, maxU, maxV
        );
    }

    private void correctionCap(
            String name,
            Direction face,
            float fromX, float fromY, float fromZ,
            float toX, float toY, float toZ
    ) {
        tankSideModel(
                name,
                face,
                fromX, fromY, fromZ,
                toX, toY, toZ,
                "frame",
                0.0f, 0.0f, 1.0f, 1.0f
        );
    }

    /**
     * Generates one WEST/EAST top/bottom perimeter run without corner overlap.
     *
     * The middle 14 pixels are always present while this edge is exposed.
     * The NORTH end pixel is present only when there is a Tank to the north,
     * because otherwise the NORTH perimeter strip owns that corner pixel.
     * SOUTH follows the same rule.
     */
    private void tankHorizontalSidePerimeterParts(
            MultiPartBlockStateBuilder builder,
            String namePrefix,
            Direction face,
            BooleanProperty exposedFaceProperty,
            BooleanProperty sideBoundaryProperty,
            float minX,
            float maxX,
            float minY,
            float maxY,
            float minU,
            float maxU
    ) {
        tankPart(
                builder,
                tankSideModel(
                        namePrefix + "_middle",
                        face,
                        minX,
                        minY,
                        1.0f,
                        maxX,
                        maxY,
                        15.0f,
                        "top",
                        minU,
                        1.0f,
                        maxU,
                        15.0f
                ),
                exposedFaceProperty,
                false,
                sideBoundaryProperty,
                false
        );

        tankPart(
                builder,
                tankSideModel(
                        namePrefix + "_north_extension",
                        face,
                        minX,
                        minY,
                        0.0f,
                        maxX,
                        maxY,
                        1.0f,
                        "top",
                        minU,
                        0.0f,
                        maxU,
                        1.0f
                ),
                exposedFaceProperty,
                false,
                sideBoundaryProperty,
                false,
                FoundryTankBlock.NORTH,
                true
        );

        tankPart(
                builder,
                tankSideModel(
                        namePrefix + "_south_extension",
                        face,
                        minX,
                        minY,
                        15.0f,
                        maxX,
                        maxY,
                        16.0f,
                        "top",
                        minU,
                        15.0f,
                        maxU,
                        16.0f
                ),
                exposedFaceProperty,
                false,
                sideBoundaryProperty,
                false,
                FoundryTankBlock.SOUTH,
                true
        );
    }

    private BlockModelBuilder tankPanelModel(
            String name,
            Direction face,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            String texture,
            float minU,
            float minV,
            float maxU,
            float maxV
    ) {
        boolean flipSideTextureU =
                shouldFlipTankSideTextureU(
                        face,
                        texture
                );

        float adjustedMinU =
                flipSideTextureU
                        ? maxU
                        : minU;

        float adjustedMaxU =
                flipSideTextureU
                        ? minU
                        : maxU;

        return models().withExistingParent(
                        name,
                        mcLoc("block/block")
                )
                .renderType("cutout")
                .texture(
                        "particle",
                        modLoc("block/foundry_tank_side")
                )
                .texture(
                        "side",
                        modLoc("block/foundry_tank_side")
                )
                .texture(
                        "top",
                        modLoc("block/foundry_tank_top")
                )
                .element()
                .from(
                        fromX,
                        fromY,
                        fromZ
                )
                .to(
                        toX,
                        toY,
                        toZ
                )
                .face(face)
                .texture("#" + texture)
                .uvs(
                        adjustedMinU,
                        minV,
                        adjustedMaxU,
                        maxV
                )
                .end()
                .end();
    }

    private BlockModelBuilder tankSideModel(
            String name,
            Direction face,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ,
            String texture,
            float minU,
            float minV,
            float maxU,
            float maxV
    ) {
        /*
         * Side-frame pieces are thin physical planes that are visible from
         * both outside and inside the Tank. Their UV handedness must be based
         * on the physical plane being generated, not independently on the two
         * face directions.
         *
         * NORTH/EAST were already visually correct in the stable static model:
         * their outward face used the reversed U range and their inward/back
         * face used the normal U range. SOUTH/WEST used the opposite pair,
         * which mirrored the measurement arms on both sides of those planes.
         *
         * Use the known-good pair for every horizontal side plane:
         *   outward face -> reversed U
         *   inward face  -> normal U
         *
         * Top/bottom texture parts do not use this directional side-frame
         * mapping and therefore keep their supplied UV range unchanged.
         */
        boolean sideFrameTexture =
                "side".equals(texture)
                        && face.getAxis().isHorizontal();

        float adjustedMinU =
                sideFrameTexture
                        ? maxU
                        : minU;

        float adjustedMaxU =
                sideFrameTexture
                        ? minU
                        : maxU;

        float oppositeMinU = minU;
        float oppositeMaxU = maxU;

        return models().withExistingParent(
                        name,
                        mcLoc("block/block")
                )
                .renderType(
                        "cutout"
                )
                .texture(
                        "particle",
                        modLoc("block/foundry_tank_side")
                )
                .texture(
                        "side",
                        modLoc("block/foundry_tank_side")
                )
                .texture(
                        "frame",
                        modLoc("block/foundry_tank_frame")
                )
                .texture(
                        "top",
                        modLoc("block/foundry_tank_top")
                )
                .ao(false)
                .element()
                .from(
                        fromX,
                        fromY,
                        fromZ
                )
                .to(
                        toX,
                        toY,
                        toZ
                )
                .face(
                        face
                )
                .texture(
                        sideFrameTexture
                                ? "#frame"
                                : "#" + texture
                )
                .uvs(
                        adjustedMinU,
                        minV,
                        adjustedMaxU,
                        maxV
                )
                .end()
                .face(
                        face.getOpposite()
                )
                .texture(
                        sideFrameTexture
                                ? "#frame"
                                : "#" + texture
                )
                .uvs(
                        oppositeMinU,
                        minV,
                        oppositeMaxU,
                        maxV
                )
                .end()
                .end();
    }

    private boolean shouldFlipTankSideTextureU(
            Direction face,
            String texture
    ) {
        /*
         * Minecraft's baked block-model side faces do not all share the same
         * horizontal UV direction. The old BER submitted its own vertices, so
         * north/east visually matched south/west there.
         *
         * When baking these model parts through JSON/datagen, the exterior
         * north and east side textures need their U range flipped so the
         * measurement/marker line stays on the same physical Tank corner as it
         * did in the dynamic renderer.
         */
        return "side".equals(
                texture
        )
                && (
                face == Direction.NORTH
                        || face == Direction.EAST
        );
    }

    /**
     * The complete top/bottom texture belongs only to a horizontally isolated
     * Tank. Connected horizontal runs expose only their perimeter frames.
     */
    private void tankIsolatedHorizontalPart(
            MultiPartBlockStateBuilder builder,
            ModelFile model,
            BooleanProperty exposedFaceConnection
    ) {
        builder.part()
                .modelFile(model)
                .addModel()
                .condition(exposedFaceConnection, false)
                .condition(FoundryTankBlock.NORTH, false)
                .condition(FoundryTankBlock.EAST, false)
                .condition(FoundryTankBlock.SOUTH, false)
                .condition(FoundryTankBlock.WEST, false)
                .end();
    }

    private void tankPart(
            MultiPartBlockStateBuilder builder,
            ModelFile model,
            BooleanProperty property,
            boolean value
    ) {
        builder.part()
                .modelFile(model)
                .addModel()
                .condition(
                        property,
                        value
                )
                .end();
    }

    private void tankPart(
            MultiPartBlockStateBuilder builder,
            ModelFile model,
            BooleanProperty firstProperty,
            boolean firstValue,
            BooleanProperty secondProperty,
            boolean secondValue
    ) {
        builder.part()
                .modelFile(
                        model
                )
                .addModel()
                .condition(
                        firstProperty,
                        firstValue
                )
                .condition(
                        secondProperty,
                        secondValue
                )
                .end();
    }

    private void tankPart(
            MultiPartBlockStateBuilder builder,
            ModelFile model,
            BooleanProperty firstProperty,
            boolean firstValue,
            BooleanProperty secondProperty,
            boolean secondValue,
            BooleanProperty thirdProperty,
            boolean thirdValue
    ) {
        builder.part()
                .modelFile(
                        model
                )
                .addModel()
                .condition(
                        firstProperty,
                        firstValue
                )
                .condition(
                        secondProperty,
                        secondValue
                )
                .condition(
                        thirdProperty,
                        thirdValue
                )
                .end();
    }


    private void blockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    // =========================
    // LEVER
    // =========================
    private void customLever(Block block, String textureName) {

        ModelFile lever =
                models().withExistingParent(
                                textureName + "_lever",
                                modLoc("block/custom_lever")
                        )
                        .texture("lever",
                                modLoc("block/" + textureName + "_lever"));

        ModelFile leverOn =
                models().withExistingParent(
                                textureName + "_lever_on",
                                modLoc("block/custom_lever_on")
                        )
                        .texture("lever",
                                modLoc("block/" + textureName + "_lever"));

        leverBlockCustom(
                block,
                lever,
                leverOn
        );
        itemModels().withExistingParent(
                textureName + "_lever",
                modLoc("block/custom_lever")
        ).texture(
                "lever",
                modLoc("block/" + textureName + "_lever")
        );
    }

    // fix for rotating UV
    private void leverBlockCustom(Block block,
                            ModelFile lever,
                            ModelFile leverOn) {

        getVariantBuilder(block).forAllStates(state -> {

            Direction facing = state.getValue(LeverBlock.FACING);
            AttachFace face = state.getValue(LeverBlock.FACE);
            boolean powered = state.getValue(LeverBlock.POWERED);

            return ConfiguredModel.builder()
                    .modelFile(powered ? lever : leverOn)
                    .rotationX(
                            face == AttachFace.FLOOR ? 0 :
                                    face == AttachFace.WALL ? 90 :
                                            180
                    )
                    .rotationY(
                            (int)(
                                    face == AttachFace.CEILING
                                            ? facing.toYRot()
                                            : facing.getOpposite().toYRot()
                            )
                    )
                    .uvLock(false)
                    .build();
        });
    }

    // =========================
    // VANILLA-STYLE PRESSURE PLATE
    // =========================
    private void vanillaPressurePlate(
            PressurePlateBlock pressurePlate,
            Block baseBlock
    ) {
        pressurePlateBlock(
                pressurePlate,
                blockTexture(baseBlock)
        );

        itemModels().withExistingParent(
                blockName(pressurePlate),
                modLoc("block/" + blockName(pressurePlate))
        );
    }

    // =========================
    // VANILLA-STYLE TRAP DOOR
    // =========================
    private void vanillaTrapdoor(TrapDoorBlock block, String textureName) {

        ModelFile bottom =
                models().withExistingParent(
                                textureName + "_trapdoor_bottom",
                                mcLoc("block/template_trapdoor_bottom")
                        )
                        .texture(
                                "texture",
                                modLoc("block/" + textureName + "_trapdoor")
                        );

        ModelFile top =
                models().withExistingParent(
                                textureName + "_trapdoor_top",
                                mcLoc("block/template_trapdoor_top")
                        )
                        .texture(
                                "texture",
                                modLoc("block/" + textureName + "_trapdoor")
                        );

        ModelFile open =
                models().withExistingParent(
                                textureName + "_trapdoor_open",
                                mcLoc("block/template_trapdoor_open")
                        )
                        .texture(
                                "texture",
                                modLoc("block/" + textureName + "_trapdoor")
                        );

        trapdoorBlock(
                block,
                bottom,
                top,
                open,
                true
        );

        itemModels().withExistingParent(
                textureName + "_trapdoor",
                modLoc("block/" + textureName + "_trapdoor_bottom")
        );
    }

    // =========================
    // VANILLA-STYLE DOOR
    // =========================
    private void vanillaDoor(DoorBlock block, String textureName) {

        doorBlockWithRenderType(
                block,
                textureName,
                modLoc("block/" + textureName + "_door_bottom"),
                modLoc("block/" + textureName + "_door_top"),
                "cutout"
        );
        simpleItemTexture(textureName + "_door");
    }

    // =========================
    // VANILLA-STYLE CHAINS
    // =========================
    private void vanillaChain(ChainBlock block, String textureName) {

        ModelFile chain =
                models().withExistingParent(
                                textureName + "_chain",
                                mcLoc("block/chain")
                        )
                        .texture(
                                "all",
                                modLoc("block/" + textureName + "_chain")
                        );

        axisBlock(
                block,
                chain,
                chain
        );

        simpleBlockItem(
                block,
                chain
        );
    }

    // =========================
    // VANILLA-STYLE BARS
    // =========================
    private void vanillaBars(IronBarsBlock block, String textureName) {

        paneBlock(
                block,
                modLoc("block/" + textureName + "_bars"),
                modLoc("block/" + textureName + "_bars")
        );

        itemModels().withExistingParent(
                textureName + "_bars",
                mcLoc("item/generated")
        ).texture(
                "layer0",
                modLoc("block/" + textureName + "_bars")
        );
    }

    // =========================
    // VANILLA-STYLE STAIRS
    // =========================
    private void vanillaStairs(StairBlock stairs, Block baseBlock) {

        String stairsName = blockName(stairs);

        stairsBlock(
                stairs,
                blockTexture(baseBlock)
        );

        itemModels().withExistingParent(
                stairsName,
                modLoc("block/" + stairsName)
        );
    }

    // =========================
    // VANILLA-STYLE SLABS
    // =========================
    private void vanillaSlab(SlabBlock slab, Block baseBlock) {

        String slabName = blockName(slab);
        String baseBlockName = blockName(baseBlock);

        slabBlock(
                slab,
                modLoc("block/" + baseBlockName),
                blockTexture(baseBlock)
        );

        itemModels().withExistingParent(
                slabName,
                modLoc("block/" + slabName)
        );
    }

    // =========================
    // VANILLA-STYLE BUTTON
    // =========================
    private void vanillaButton(ButtonBlock button, Block baseBlock) {

        buttonBlock(
                button,
                blockTexture(baseBlock)
        );

        itemModels().buttonInventory(
                blockName(button),
                blockTexture(baseBlock)
        );
    }

    // =========================
    // VANILLA-STYLE LADDER
    // =========================
    private void vanillaLadder(
            LadderBlock ladder,
            String textureName
    ) {
        String ladderName = blockName(ladder);

        ModelFile ladderModel =
                models().withExistingParent(
                                ladderName,
                                mcLoc("block/ladder")
                        )
                        .texture(
                                "texture",
                                modLoc("block/" + textureName + "_ladder")
                        )
                        .renderType("cutout");

        horizontalBlock(
                ladder,
                ladderModel
        );

        itemModels().singleTexture(
                ladderName,
                mcLoc("item/generated"),
                "layer0",
                modLoc("block/" + textureName + "_ladder")
        );
    }

    // =========================
    // BLOCK NAME HELPER
    // =========================
    private String blockName(Block block) {

        return BuiltInRegistries.BLOCK
                .getKey(block)
                .getPath();
    }

    // =========================
    // HELPER TO GENERATE ITEM MODELS
    // =========================

    private void simpleItemTexture(String itemName) {

        itemModels().singleTexture(
                itemName,
                mcLoc("item/generated"),
                "layer0",
                modLoc("item/" + itemName)
        );
    }
}

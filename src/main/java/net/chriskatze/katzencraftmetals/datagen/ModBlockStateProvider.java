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
         * Do NOT add static horizontal corner models here.
         *
         * Convex/static corners are already owned by NORTH/SOUTH. Concave
         * diagonal corners cannot be represented by the six adjacency
         * properties and are supplied by
         * FoundryControllerTankFrameCorrectionRenderer instead.
         */

        simpleBlockItem(
                tank,
                itemModel
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
                .face(
                        face
                )
                .texture(
                        "#" + texture
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
                        "#" + texture
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

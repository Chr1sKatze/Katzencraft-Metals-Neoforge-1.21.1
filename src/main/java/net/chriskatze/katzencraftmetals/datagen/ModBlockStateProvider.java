package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.chriskatze.katzencraftmetals.block.custom.CastingCauldronBlock;

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
        blockWithItem(ModBlocks.FOUNDRY_TANK.get());
        ModelFile castingCauldronModel =
                new ModelFile.UncheckedModelFile(
                        modLoc("block/casting_cauldron")
                );

        getVariantBuilder(
                ModBlocks.CASTING_CAULDRON.get()
        ).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(castingCauldronModel)
                        .build()
        );

        simpleBlockItem(
                ModBlocks.CASTING_CAULDRON.get(),
                castingCauldronModel
        );
        ModelFile faucetModel =
                new ModelFile.UncheckedModelFile(
                        modLoc("block/foundry_faucet")
                );

        horizontalBlock(
                ModBlocks.FOUNDRY_FAUCET.get(),
                faucetModel
        );

        simpleBlockItem(
                ModBlocks.FOUNDRY_FAUCET.get(),
                faucetModel
        );
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
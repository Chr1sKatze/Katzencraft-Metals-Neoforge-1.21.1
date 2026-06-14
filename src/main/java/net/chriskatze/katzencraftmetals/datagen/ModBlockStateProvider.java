package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KatzencraftMetalsMod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.STEEL_BLOCK.get());
        customLever(ModBlocks.STEEL_LEVER.get(), "steel");
        customButton(ModBlocks.STEEL_BUTTON.get(), "steel");

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
    }


    private void blockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }

    // =========================
    // LEVER
    // =========================

    private void leverBlock(Block block,
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

    private void customLever(Block block, String textureName) {

        ModelFile lever =
                models().withExistingParent(
                                textureName + "_lever",
                                modLoc("block/custom_lever")
                        )
                        .texture("base",
                                modLoc("block/" + textureName + "_lever_base"))
                        .texture("handle",
                                modLoc("block/" + textureName + "_lever_handle"));


        ModelFile leverOn =
                models().withExistingParent(
                                textureName + "_lever_on",
                                modLoc("block/custom_lever_on")
                        )
                        .texture("base",
                                modLoc("block/" + textureName + "_lever_base"))
                        .texture("handle",
                                modLoc("block/" + textureName + "_lever_handle"));

        leverBlock(
                block,
                lever,
                leverOn
        );

        simpleBlockItem(
                block,
                lever
        );
    }

    // =========================
    // BUTTON
    // =========================

    private void buttonBlockCustom(ButtonBlock block,
                                   ModelFile button,
                                   ModelFile buttonPressed) {

        getVariantBuilder(block).forAllStates(state -> {

            Direction facing = state.getValue(ButtonBlock.FACING);
            AttachFace face = state.getValue(ButtonBlock.FACE);
            boolean powered = state.getValue(ButtonBlock.POWERED);

            return ConfiguredModel.builder()
                    .modelFile(powered ? buttonPressed : button)
                    .rotationX(
                            face == AttachFace.FLOOR ? 0 :
                                    face == AttachFace.WALL ? 90 :
                                            180
                    )
                    .rotationY(
                            (int)(
                                    face == AttachFace.CEILING
                                            ? facing
                                            : facing.getOpposite()
                            ).toYRot()
                    )
                    .uvLock(false)
                    .build();
        });
    }

    private void customButton(Block block, String textureName) {

        ModelFile button =
                models().withExistingParent(
                                textureName + "_button",
                                modLoc("block/custom_button")
                        )
                        .texture("button",
                                modLoc("block/" + textureName + "_button"));

        ModelFile buttonPressed =
                models().withExistingParent(
                                textureName + "_button_pressed",
                                modLoc("block/custom_button_pressed")
                        )
                        .texture("button",
                                modLoc("block/" + textureName + "_button"));

        buttonBlockCustom(
                (ButtonBlock) block,
                button,
                buttonPressed
        );

        simpleBlockItem(
                block,
                button
        );
    }
}
package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.*;
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
        customPressurePlate(ModBlocks.STEEL_PRESSURE_PLATE.get(), "steel");
        customTrapdoor((TrapDoorBlock) ModBlocks.STEEL_TRAPDOOR.get(), "steel");
        customDoor((DoorBlock) ModBlocks.STEEL_DOOR.get(), "steel");

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
    // BUTTON
    // =========================
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
        itemModels().withExistingParent(
                textureName + "_button",
                modLoc("block/custom_button")
        ).texture(
                "button",
                modLoc("block/" + textureName + "_button")
        );
    }

    // fix for rotating UV
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

    // =========================
    // PRESSURE PLATE
    // =========================
    private void customPressurePlate(Block block, String textureName) {

        ModelFile pressurePlateUp =
                models().withExistingParent(
                                textureName + "_pressure_plate",
                                modLoc("block/custom_pressure_plate_up")
                        )
                        .texture("plate",
                                modLoc("block/" + textureName + "_pressure_plate"));

        ModelFile pressurePlateDown =
                models().withExistingParent(
                                textureName + "_pressure_plate_down",
                                modLoc("block/custom_pressure_plate_down")
                        )
                        .texture("plate",
                                modLoc("block/" + textureName + "_pressure_plate"));

        pressurePlateBlock(
                (PressurePlateBlock) block,
                pressurePlateUp,
                pressurePlateDown
        );
        itemModels().withExistingParent(
                textureName + "_pressure_plate",
                modLoc("block/custom_pressure_plate_up")
        ).texture(
                "plate",
                modLoc("block/" + textureName + "_pressure_plate")
        );
    }

    // =========================
    // TRAP DOOR
    // =========================
    private void customTrapdoor(TrapDoorBlock block, String textureName) {

        ModelFile bottom =
                models().withExistingParent(
                                textureName + "_trapdoor_bottom",
                                mcLoc("block/template_trapdoor_bottom")
                        )
                        .texture("texture",
                                modLoc("block/" + textureName + "_trapdoor"));

        ModelFile top =
                models().withExistingParent(
                                textureName + "_trapdoor_top",
                                mcLoc("block/template_trapdoor_top")
                        )
                        .texture("texture",
                                modLoc("block/" + textureName + "_trapdoor"));

        ModelFile open =
                models().withExistingParent(
                                textureName + "_trapdoor_open",
                                mcLoc("block/template_trapdoor_open")
                        )
                        .texture("texture",
                                modLoc("block/" + textureName + "_trapdoor"));

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
    // DOOR
    // =========================
    private void customDoor(DoorBlock block, String textureName) {

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
package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KatzencraftMetalsMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher", () ->
                    BlockEntityType.Builder.of(
                            CrusherBlockEntity::new,
                            ModBlocks.CRUSHER.get()
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<FuelChamberBlockEntity>> FUEL_CHAMBER =
            BLOCK_ENTITIES.register(
                    "fuel_chamber",
                    () -> BlockEntityType.Builder.of(
                                    FuelChamberBlockEntity::new,
                                    ModBlocks.FUEL_CHAMBER.get()
                            )
                            .build(null)
            );

    public static final Supplier<BlockEntityType<FoundryControllerBlockEntity>>
            FOUNDRY_CONTROLLER =
            BLOCK_ENTITIES.register(
                    "foundry_controller",
                    () -> BlockEntityType.Builder.of(
                                    FoundryControllerBlockEntity::new,
                                    ModBlocks.FOUNDRY_CONTROLLER.get()
                            )
                            .build(null)
            );

    public static final Supplier<BlockEntityType<CastingCauldronBlockEntity>>
            CASTING_CAULDRON =
            BLOCK_ENTITIES.register(
                    "casting_cauldron",
                    () -> BlockEntityType.Builder.of(
                                    CastingCauldronBlockEntity::new,
                                    ModBlocks.CASTING_CAULDRON.get()
                            )
                            .build(null)
            );

    public static final Supplier<BlockEntityType<FoundryFaucetBlockEntity>>
            FOUNDRY_FAUCET =
            BLOCK_ENTITIES.register(
                    "foundry_faucet",
                    () -> BlockEntityType.Builder.of(
                                    FoundryFaucetBlockEntity::new,
                                    ModBlocks.FOUNDRY_FAUCET.get()
                            )
                            .build(null)
            );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}

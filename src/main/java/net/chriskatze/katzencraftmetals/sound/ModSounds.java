package net.chriskatze.katzencraftmetals.sound;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(
                    BuiltInRegistries.SOUND_EVENT,
                    KatzencraftMetalsMod.MODID
            );

    public static final DeferredHolder<SoundEvent, SoundEvent>
            FOUNDRY_SMELTING_START =
            registerFixedRange(
                    "foundry_smelting_start",
                    6.0F
            );

    public static final DeferredHolder<SoundEvent, SoundEvent>
            FOUNDRY_SMELTING_LOOP =
            registerFixedRange(
                    "foundry_smelting_loop",
                    6.0F
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> FOUNDRY_POURING_LOOP =
            registerVariableRange("foundry_pouring_loop");

    public static final DeferredHolder<SoundEvent, SoundEvent> FOUNDRY_TANK_LOOP =
            registerVariableRange("foundry_tank_loop");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent>
    registerVariableRange(
            String name
    ) {
        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(
                        KatzencraftMetalsMod.MODID,
                        name
                );

        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(
                        id
                )
        );
    }

    private static DeferredHolder<SoundEvent, SoundEvent>
    registerFixedRange(
            String name,
            float range
    ) {
        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(
                        KatzencraftMetalsMod.MODID,
                        name
                );

        return SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createFixedRangeEvent(
                        id,
                        range
                )
        );
    }

    public static void register(
            IEventBus modEventBus
    ) {
        SOUND_EVENTS.register(
                modEventBus
        );
    }
}
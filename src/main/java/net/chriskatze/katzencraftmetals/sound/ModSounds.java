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
            register(
                    "foundry_smelting_start"
            );

    public static final DeferredHolder<SoundEvent, SoundEvent>
            FOUNDRY_SMELTING_LOOP =
            register(
                    "foundry_smelting_loop"
            );

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(
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

    public static void register(
            IEventBus modEventBus
    ) {
        SOUND_EVENTS.register(
                modEventBus
        );
    }
}

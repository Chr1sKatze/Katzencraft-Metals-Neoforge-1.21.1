package net.chriskatze.katzencraftmetals.hunger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class CatoHunger {

    private static final String TAG = "KatzencraftHunger";
    public static final int MAX_HUNGER = 100;

    public static int get(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG)) {
            data.putInt(TAG, MAX_HUNGER);
        }

        return data.getInt(TAG);
    }

    public static void set(Player player, int value) {
        int clamped = Math.max(0, Math.min(MAX_HUNGER, value));
        player.getPersistentData().putInt(TAG, clamped);

        if (!player.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new net.chriskatze.katzencraftmetals.network.SyncHungerPayload(clamped)
            );
        }
    }

    public static void add(Player player, int amount) {
        set(player, get(player) + amount);
    }
}
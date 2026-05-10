package net.chriskatze.katzencraftmetals.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class KatzencraftWorldRulesData extends SavedData {

    private static final String NAME = "katzencraftmetals_world_rules";
    private boolean applied;

    public KatzencraftWorldRulesData() {
        this.applied = false;
    }

    public static KatzencraftWorldRulesData load(CompoundTag tag, HolderLookup.Provider registries) {
        KatzencraftWorldRulesData data = new KatzencraftWorldRulesData();
        data.applied = tag.getBoolean("Applied");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Applied", applied);
        return tag;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied() {
        this.applied = true;
        setDirty();
    }

    public static KatzencraftWorldRulesData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(
                        KatzencraftWorldRulesData::new,
                        KatzencraftWorldRulesData::load
                ),
                NAME
        );
    }
}
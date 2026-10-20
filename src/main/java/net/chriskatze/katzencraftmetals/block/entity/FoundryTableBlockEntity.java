package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** One fixed ingot mold, reusing the Basin's persisted casting and output-only container lifecycle. */
public class FoundryTableBlockEntity extends FoundryBasinBlockEntity {
    public static final int MOLTEN_PER_INGOT = 6;
    static final int EVENT_POUR_SPARK = 1;
    public static final float MOLD_MIN_X = 6.0f / 16.0f;
    public static final float MOLD_MAX_X = 10.0f / 16.0f;
    public static final float MOLD_MIN_Z = 4.0f / 16.0f;
    public static final float MOLD_MAX_Z = 12.0f / 16.0f;
    public static final float MOLD_OPENING = 13.0f / 16.0f;
    public static final float MOLD_BOTTOM = 10.5f / 16.0f;
    public static final float MOLD_TOP = 12.5f / 16.0f;

    public FoundryTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_TABLE.get(), pos, state);
    }

    @Override protected void onMoltenInserted() {
        if (level != null && !level.isClientSide()) {
            // Vanilla block events are transient: one packet per accepted pour,
            // never replayed by renderer frames, NBT loads or cooling updates.
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_POUR_SPARK, 0);
        }
    }

    @Override public boolean triggerEvent(int id, int data) {
        if (id != EVENT_POUR_SPARK) return super.triggerEvent(id,data);
        if (level != null && level.isClientSide()) {
            FoundryPouringSparks.spawnImpact(level,worldPosition.getX()+.5,
                    worldPosition.getY()+MOLD_OPENING+.01,worldPosition.getZ()+.5,
                    (level.random.nextDouble()*2-1)*.011,(level.random.nextDouble()*2-1)*.011,true);
        }
        return true;
    }

    @Override protected int configuredCoolingTime(ResourceLocation metal) {
        return net.chriskatze.katzencraftmetals.metal.MetalCatalog.get(metal)
                .map(definition -> definition.ingotCoolingTime()).orElse(MAX_COOLING_PROGRESS);
    }

    @Override public int getRequiredMoltenAmount() { return MOLTEN_PER_INGOT; }

    @Override public boolean canAccept(ResourceLocation metal, int amount) {
        return ModMoltenMetals.get(metal).map(definition -> !definition.createIngotResult().isEmpty()).orElse(false)
                && super.canAccept(metal, amount);
    }

    @Override protected ItemStack createResult(ResourceLocation metal) {
        return ModMoltenMetals.get(metal).map(definition -> definition.createIngotResult()).orElse(ItemStack.EMPTY);
    }
}

package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles;
import net.minecraft.world.item.Tier;

/** Compatibility aliases; statistics and native tool rules live in equipment profiles. */
public final class ModToolTiers {
    public static final Tier STEEL = MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").tier(MetalEquipmentProfiles.ToolKind.PICKAXE);
    public static final Tier MYTHRIL = MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").tier(MetalEquipmentProfiles.ToolKind.PICKAXE);
    private ModToolTiers() {}
}

package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;

/** Registration adapter; all balance, repair, sound and layer properties come from profiles. */
public final class ModArmorMaterials {
    public static final Holder<ArmorMaterial> STEEL = register("steel", MetalEquipmentProfiles.forMetal("katzencraftmetals:steel"));
    public static final Holder<ArmorMaterial> MYTHRIL = register("mythril", MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril"));
    public static final Holder<ArmorMaterial> COPPER = register("copper", MetalEquipmentProfiles.forMetal("minecraft:copper"));
    private static Holder<ArmorMaterial> register(String id, MetalEquipmentProfiles.Profile profile) {
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
                ResourceLocation.fromNamespaceAndPath("katzencraftmetals",id),profile.armorMaterial());
    }
    private ModArmorMaterials() {}
}

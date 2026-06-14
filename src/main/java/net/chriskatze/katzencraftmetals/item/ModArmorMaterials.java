package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {

    public static final Holder<ArmorMaterial> STEEL =
            Registry.registerForHolder(
                    BuiltInRegistries.ARMOR_MATERIAL,
                    ResourceLocation.fromNamespaceAndPath(
                            KatzencraftMetalsMod.MODID,
                            "steel"
                    ),
                    new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 2);
                                map.put(ArmorItem.Type.LEGGINGS, 5);
                                map.put(ArmorItem.Type.CHESTPLATE, 6);
                                map.put(ArmorItem.Type.HELMET, 2);
                                map.put(ArmorItem.Type.BODY, 5);
                            }),
                            18,
                            SoundEvents.ARMOR_EQUIP_IRON,
                            () -> Ingredient.of(ModItems.STEEL_INGOT.get()),
                            List.of(new ArmorMaterial.Layer(
                                    ResourceLocation.fromNamespaceAndPath(
                                            KatzencraftMetalsMod.MODID,
                                            "steel"
                                    )
                            )),
                            0.0F,
                            0.0F
                    )
            );

    public static final Holder<ArmorMaterial> MYTHRIL =
            Registry.registerForHolder(
                    BuiltInRegistries.ARMOR_MATERIAL,
                    ResourceLocation.fromNamespaceAndPath(
                            KatzencraftMetalsMod.MODID,
                            "mythril"
                    ),
                    new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 3);
                                map.put(ArmorItem.Type.LEGGINGS, 6);
                                map.put(ArmorItem.Type.CHESTPLATE, 8);
                                map.put(ArmorItem.Type.HELMET, 3);
                                map.put(ArmorItem.Type.BODY, 8);
                            }),
                            25,
                            SoundEvents.ARMOR_EQUIP_DIAMOND,
                            () -> Ingredient.of(ModItems.MYTHRIL_INGOT.get()),
                            List.of(new ArmorMaterial.Layer(
                                    ResourceLocation.fromNamespaceAndPath(
                                            KatzencraftMetalsMod.MODID,
                                            "mythril"
                                    )
                            )),
                            2.0F,
                            0.0F
                    )
            );

    private ModArmorMaterials() {
    }
}
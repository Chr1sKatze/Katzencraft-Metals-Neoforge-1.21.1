package net.chriskatze.katzencraftmetals.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CatoToolTierConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Stone
    public static final ModConfigSpec.IntValue STONE_DURABILITY;
    public static final ModConfigSpec.DoubleValue STONE_SPEED;
    public static final ModConfigSpec.DoubleValue STONE_ATTACK_BONUS;
    public static final ModConfigSpec.IntValue STONE_ENCHANTABILITY;
    public static final ModConfigSpec.IntValue STONE_HARVEST_LEVEL;

    // Iron
    public static final ModConfigSpec.IntValue IRON_DURABILITY;
    public static final ModConfigSpec.DoubleValue IRON_SPEED;
    public static final ModConfigSpec.DoubleValue IRON_ATTACK_BONUS;
    public static final ModConfigSpec.IntValue IRON_ENCHANTABILITY;
    public static final ModConfigSpec.IntValue IRON_HARVEST_LEVEL;

    // Steel
    public static final ModConfigSpec.IntValue STEEL_DURABILITY;
    public static final ModConfigSpec.DoubleValue STEEL_SPEED;
    public static final ModConfigSpec.DoubleValue STEEL_ATTACK_BONUS;
    public static final ModConfigSpec.IntValue STEEL_ENCHANTABILITY;
    public static final ModConfigSpec.IntValue STEEL_HARVEST_LEVEL;

    // Diamond
    public static final ModConfigSpec.IntValue DIAMOND_DURABILITY;
    public static final ModConfigSpec.DoubleValue DIAMOND_SPEED;
    public static final ModConfigSpec.DoubleValue DIAMOND_ATTACK_BONUS;
    public static final ModConfigSpec.IntValue DIAMOND_ENCHANTABILITY;
    public static final ModConfigSpec.IntValue DIAMOND_HARVEST_LEVEL;

    // Mythril
    public static final ModConfigSpec.IntValue MYTHRIL_DURABILITY;
    public static final ModConfigSpec.DoubleValue MYTHRIL_SPEED;
    public static final ModConfigSpec.DoubleValue MYTHRIL_ATTACK_BONUS;
    public static final ModConfigSpec.IntValue MYTHRIL_ENCHANTABILITY;
    public static final ModConfigSpec.IntValue MYTHRIL_HARVEST_LEVEL;

    static {

        BUILDER.push("stone");
        STONE_DURABILITY = BUILDER.defineInRange("durability", 128, 1, 100000);
        STONE_SPEED = BUILDER.defineInRange("speed", 4.0D, 0.1D, 100.0D);
        STONE_ATTACK_BONUS = BUILDER.defineInRange("attack_bonus", 1.0D, 0.0D, 100.0D);
        STONE_ENCHANTABILITY = BUILDER.defineInRange("enchantability", 5, 0, 100);
        STONE_HARVEST_LEVEL = BUILDER.defineInRange("harvest_level", 1, 0, 100);
        BUILDER.pop();

        BUILDER.push("iron");
        IRON_DURABILITY = BUILDER.defineInRange("durability", 256, 1, 100000);
        IRON_SPEED = BUILDER.defineInRange("speed", 6.0D, 0.1D, 100.0D);
        IRON_ATTACK_BONUS = BUILDER.defineInRange("attack_bonus", 2.0D, 0.0D, 100.0D);
        IRON_ENCHANTABILITY = BUILDER.defineInRange("enchantability", 14, 0, 100);
        IRON_HARVEST_LEVEL = BUILDER.defineInRange("harvest_level", 2, 0, 100);
        BUILDER.pop();

        BUILDER.push("steel");
        STEEL_DURABILITY = BUILDER.defineInRange("durability", 512, 1, 100000);
        STEEL_SPEED = BUILDER.defineInRange("speed", 7.0D, 0.1D, 100.0D);
        STEEL_ATTACK_BONUS = BUILDER.defineInRange("attack_bonus", 3.0D, 0.0D, 100.0D);
        STEEL_ENCHANTABILITY = BUILDER.defineInRange("enchantability", 18, 0, 100);
        STEEL_HARVEST_LEVEL = BUILDER.defineInRange("harvest_level", 3, 0, 100);
        BUILDER.pop();

        BUILDER.push("diamond");
        DIAMOND_DURABILITY = BUILDER.defineInRange("durability", 1024, 1, 100000);
        DIAMOND_SPEED = BUILDER.defineInRange("speed", 8.0D, 0.1D, 100.0D);
        DIAMOND_ATTACK_BONUS = BUILDER.defineInRange("attack_bonus", 4.0D, 0.0D, 100.0D);
        DIAMOND_ENCHANTABILITY = BUILDER.defineInRange("enchantability", 10, 0, 100);
        DIAMOND_HARVEST_LEVEL = BUILDER.defineInRange("harvest_level", 4, 0, 100);
        BUILDER.pop();

        BUILDER.push("mythril");
        MYTHRIL_DURABILITY = BUILDER.defineInRange("durability", 1536, 1, 100000);
        MYTHRIL_SPEED = BUILDER.defineInRange("speed", 10.0D, 0.1D, 100.0D);
        MYTHRIL_ATTACK_BONUS = BUILDER.defineInRange("attack_bonus", 5.0D, 0.0D, 100.0D);
        MYTHRIL_ENCHANTABILITY = BUILDER.defineInRange("enchantability", 25, 0, 100);
        MYTHRIL_HARVEST_LEVEL = BUILDER.defineInRange("harvest_level", 5, 0, 100);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
package net.chriskatze.katzencraftmetals.hunger;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class CatoFoodValues {

    private static final Map<Item, Integer> FOOD_VALUES = new HashMap<>();

    static {
        // Basic foods
        FOOD_VALUES.put(Items.APPLE, 6);
        FOOD_VALUES.put(Items.BREAD, 10);
        FOOD_VALUES.put(Items.COOKED_BEEF, 25);
        FOOD_VALUES.put(Items.COOKED_PORKCHOP, 25);
        FOOD_VALUES.put(Items.COOKED_CHICKEN, 18);
        FOOD_VALUES.put(Items.COOKED_MUTTON, 20);
        FOOD_VALUES.put(Items.COOKED_COD, 14);
        FOOD_VALUES.put(Items.COOKED_SALMON, 18);
        FOOD_VALUES.put(Items.BAKED_POTATO, 12);
        FOOD_VALUES.put(Items.CARROT, 8);
        FOOD_VALUES.put(Items.GOLDEN_CARROT, 25);

        // Bad / small foods
        FOOD_VALUES.put(Items.ROTTEN_FLESH, 8);
        FOOD_VALUES.put(Items.SPIDER_EYE, 2);
        FOOD_VALUES.put(Items.POISONOUS_POTATO, 8);
    }

    public static int getValue(Item item) {
        return FOOD_VALUES.getOrDefault(item, 0);
    }

    public static boolean hasCustomFoodValue(Item item) {
        return FOOD_VALUES.containsKey(item);
    }
}
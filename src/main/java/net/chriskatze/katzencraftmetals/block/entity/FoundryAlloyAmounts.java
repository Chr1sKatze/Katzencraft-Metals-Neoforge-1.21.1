package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyIngredient;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small helper for alloy amount math and map serialization.
 *
 * This keeps pure amount/map logic out of FoundryControllerAlloying,
 * so that class can focus on the active alloy job state machine.
 */
final class FoundryAlloyAmounts {

    private FoundryAlloyAmounts() {
    }

    static Map<ResourceLocation, Integer> calculateRequirements(
            FoundryAlloyRecipe recipe,
            int batches
    ) {
        Map<ResourceLocation, Integer> result =
                new LinkedHashMap<>();

        for (
                FoundryAlloyIngredient ingredient :
                recipe.ingredients()
        ) {
            int total =
                    safeMultiply(
                            ingredient.amount(),
                            batches
                    );

            if (total <= 0) {
                return Map.of();
            }

            result.merge(
                    ingredient.metal(),
                    total,
                    Integer::sum
            );
        }

        return result;
    }

    static boolean hasIngredients(
            FoundryTankNetwork network,
            Map<ResourceLocation, Integer> required
    ) {
        for (
                Map.Entry<ResourceLocation, Integer> entry :
                required.entrySet()
        ) {
            if (
                    network.getMoltenAmount(
                            entry.getKey()
                    ) < entry.getValue()
            ) {
                return false;
            }
        }

        return true;
    }

    static boolean hasCompletionSpace(
            FoundryTankNetwork network,
            int totalInput,
            int totalOutput
    ) {
        long finalTotal =
                (long) network.getTotalMoltenAmount()
                        - totalInput
                        + totalOutput;

        return finalTotal >= 0L
                && finalTotal <= network.getCapacity();
    }

    static int totalAmount(
            Map<ResourceLocation, Integer> contents
    ) {
        long total = 0L;

        for (Integer amount : contents.values()) {
            if (amount != null && amount > 0) {
                total += amount;
            }
        }

        return total > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) total;
    }

    static void refund(
            FoundryTankNetwork network,
            Map<ResourceLocation, Integer> contents
    ) {
        for (
                Map.Entry<ResourceLocation, Integer> entry :
                contents.entrySet()
        ) {
            network.insert(
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    static int safeMultiply(
            int first,
            int second
    ) {
        long result =
                (long) first
                        * second;

        return result > Integer.MAX_VALUE
                ? -1
                : (int) result;
    }

    static CompoundTag writeAmounts(
            Map<ResourceLocation, Integer> contents
    ) {
        CompoundTag result =
                new CompoundTag();

        for (
                Map.Entry<ResourceLocation, Integer> entry :
                contents.entrySet()
        ) {
            if (entry.getValue() > 0) {
                result.putInt(
                        entry.getKey().toString(),
                        entry.getValue()
                );
            }
        }

        return result;
    }

    static void readAmounts(
            CompoundTag source,
            Map<ResourceLocation, Integer> destination
    ) {
        destination.clear();

        for (String key : source.getAllKeys()) {
            ResourceLocation metal =
                    ResourceLocation.tryParse(key);

            int amount =
                    source.getInt(key);

            if (
                    metal != null
                            && amount > 0
                            && ModMoltenMetals.contains(metal)
            ) {
                destination.put(
                        metal,
                        amount
                );
            }
        }
    }
}

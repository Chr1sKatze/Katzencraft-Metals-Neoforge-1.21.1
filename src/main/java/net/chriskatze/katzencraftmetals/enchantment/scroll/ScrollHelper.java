package net.chriskatze.katzencraftmetals.enchantment.scroll;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ScrollHelper {

    public record ScrollInfo(ScrollCategory category, ScrollTier tier) {}

    public static Optional<ScrollInfo> getScrollInfo(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Item item = stack.getItem();

        // WEAPON
        if (item == ModItems.COMMON_WEAPON_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.WEAPON, ScrollTier.COMMON));
        }

        if (item == ModItems.ADVANCED_WEAPON_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.WEAPON, ScrollTier.ADVANCED));
        }

        if (item == ModItems.MASTER_WEAPON_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.WEAPON, ScrollTier.MASTER));
        }

        // ARMOR
        if (item == ModItems.COMMON_ARMOR_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.ARMOR, ScrollTier.COMMON));
        }

        if (item == ModItems.ADVANCED_ARMOR_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.ARMOR, ScrollTier.ADVANCED));
        }

        if (item == ModItems.MASTER_ARMOR_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.ARMOR, ScrollTier.MASTER));
        }

        // GATHERING
        if (item == ModItems.COMMON_GATHERING_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.GATHERING, ScrollTier.COMMON));
        }

        if (item == ModItems.ADVANCED_GATHERING_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.GATHERING, ScrollTier.ADVANCED));
        }

        if (item == ModItems.MASTER_GATHERING_SCROLL.get()) {
            return Optional.of(new ScrollInfo(ScrollCategory.GATHERING, ScrollTier.MASTER));
        }

        return Optional.empty();
    }

    public static boolean isScroll(ItemStack stack) {
        return getScrollInfo(stack).isPresent();
    }

    private ScrollHelper() {
    }
}
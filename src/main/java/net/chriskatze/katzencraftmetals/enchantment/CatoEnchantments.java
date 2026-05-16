package net.chriskatze.katzencraftmetals.enchantment;

import net.chriskatze.katzencraftmetals.enchantment.scroll.ScrollCategory;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CatoEnchantments {

    public static final int MAX_ENCHANTMENTS_PER_ITEM = 3;
    public static final int GLOBAL_MAX_ENCHANT_LEVEL = 3;

    public record CatoEnchantmentDefinition(
            String id,
            ScrollCategory category,
            int maxLevel,
            Predicate<ItemStack> validTarget,
            EnchantmentKey enchantmentKey
    ) {}

    public record EnchantmentKey(net.minecraft.resources.ResourceKey<Enchantment> key) {}

    public static List<CatoEnchantmentDefinition> getDefinitions() {
        List<CatoEnchantmentDefinition> list = new ArrayList<>();

        // ============================================================
        // WEAPON
        // ============================================================

        list.add(new CatoEnchantmentDefinition(
                "critical_chance",
                ScrollCategory.WEAPON,
                3,
                stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem,
                new EnchantmentKey(enchantKey("katzencraftmetals", "critical_chance"))
        ));

        list.add(new CatoEnchantmentDefinition(
                "critical_damage",
                ScrollCategory.WEAPON,
                3,
                stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem,
                new EnchantmentKey(enchantKey("katzencraftmetals", "critical_damage"))
        ));

        list.add(new CatoEnchantmentDefinition(
                "attack_speed",
                ScrollCategory.WEAPON,
                3,
                stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem,
                new EnchantmentKey(enchantKey("katzencraftmetals", "attack_speed"))
        ));

        list.add(new CatoEnchantmentDefinition(
                "attack_damage",
                ScrollCategory.WEAPON,
                3,
                stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem,
                new EnchantmentKey(enchantKey("katzencraftmetals", "attack_damage"))
        ));

        // ============================================================
        // ARMOR
        // ============================================================

        list.add(new CatoEnchantmentDefinition(
                "protection",
                ScrollCategory.ARMOR,
                3,
                stack -> stack.getItem() instanceof ArmorItem,
                new EnchantmentKey(Enchantments.PROTECTION)
        ));

        list.add(new CatoEnchantmentDefinition(
                "feather_falling",
                ScrollCategory.ARMOR,
                3,
                stack -> stack.getItem() instanceof ArmorItem armorItem
                        && armorItem.getEquipmentSlot() == EquipmentSlot.FEET,
                new EnchantmentKey(Enchantments.FEATHER_FALLING)
        ));

        // ============================================================
        // GATHERING
        // ============================================================

        list.add(new CatoEnchantmentDefinition(
                "efficiency",
                ScrollCategory.GATHERING,
                3,
                stack -> stack.getItem() instanceof DiggerItem,
                new EnchantmentKey(Enchantments.EFFICIENCY)
        ));

        list.add(new CatoEnchantmentDefinition(
                "fortune",
                ScrollCategory.GATHERING,
                3,
                stack -> stack.getItem() instanceof DiggerItem,
                new EnchantmentKey(Enchantments.FORTUNE)
        ));

        return list;
    }

    public static List<CatoEnchantmentDefinition> getAvailableDefinitions(
            ScrollCategory category,
            ItemStack targetStack
    ) {
        return getDefinitions().stream()
                .filter(definition -> definition.category() == category)
                .filter(definition -> definition.validTarget().test(targetStack))
                .toList();
    }

    public static Optional<Holder.Reference<Enchantment>> resolve(
            HolderLookup.Provider lookupProvider,
            CatoEnchantmentDefinition definition
    ) {
        return lookupProvider
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(definition.enchantmentKey().key());
    }

    private static net.minecraft.resources.ResourceKey<Enchantment> enchantKey(String namespace, String path) {
        return net.minecraft.resources.ResourceKey.create(
                Registries.ENCHANTMENT,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path)
        );
    }

    private CatoEnchantments() {
    }
}
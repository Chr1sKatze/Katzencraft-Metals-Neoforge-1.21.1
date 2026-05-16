package net.chriskatze.katzencraftmetals.event.client;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.config.CatoEnchantmentConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        value = Dist.CLIENT
)
public class CatoTooltipEvents {

    private static final double BASE_CRIT_CHANCE = 5.0D;
    private static final double BASE_CRIT_DAMAGE = 50.0D;

    private static final ResourceKey<Enchantment> CRITICAL_CHANCE_ENCHANTMENT =
            enchantmentKey("critical_chance");

    private static final ResourceKey<Enchantment> CRITICAL_DAMAGE_ENCHANTMENT =
            enchantmentKey("critical_damage");

    private static final ResourceKey<Enchantment> ATTACK_SPEED_ENCHANTMENT =
            enchantmentKey("attack_speed");

    private static final ResourceKey<Enchantment> ATTACK_DAMAGE_ENCHANTMENT =
            enchantmentKey("attack_damage");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        List<Component> originalTooltip = new ArrayList<>(event.getToolTip());

        if (!(event.getItemStack().getItem() instanceof SwordItem)) {
            return;
        }

        Component originalName = event.getToolTip().getFirst();

        event.getToolTip().set(
                0,
                Component.literal(originalName.getString())
                        .withStyle(ChatFormatting.WHITE)
        );

        double baseAttackDamage = getBaseAttackDamage(event);
        double baseAttackSpeed = getBaseAttackSpeed(event);
        Double attackRange = findTooltipNumber(originalTooltip, "Attack Range");

        boolean looksLikeWeapon = baseAttackDamage > 1.0D
                || baseAttackSpeed != 4.0D
                || attackRange != null;

        if (!looksLikeWeapon) {
            return;
        }

        int chanceLevel = getEnchantmentLevel(event, CRITICAL_CHANCE_ENCHANTMENT);
        int critDamageLevel = getEnchantmentLevel(event, CRITICAL_DAMAGE_ENCHANTMENT);
        int attackSpeedLevel = getEnchantmentLevel(event, ATTACK_SPEED_ENCHANTMENT);
        int attackDamageLevel = getEnchantmentLevel(event, ATTACK_DAMAGE_ENCHANTMENT);

        double attackDamageBonus = CatoEnchantmentConfig.ATTACK_DAMAGE_PER_LEVEL * attackDamageLevel;
        double attackSpeedBonus = CatoEnchantmentConfig.ATTACK_SPEED_PER_LEVEL * attackSpeedLevel;
        double critChanceBonus = CatoEnchantmentConfig.CRITICAL_CHANCE_PER_LEVEL * chanceLevel;
        double critDamageBonus = CatoEnchantmentConfig.CRITICAL_DAMAGE_PER_LEVEL * critDamageLevel;

        double finalAttackDamage = baseAttackDamage * (1.0D + attackDamageBonus / 100.0D);
        double finalAttackSpeed = baseAttackSpeed * (1.0D + attackSpeedBonus / 100.0D);
        double finalCritChance = BASE_CRIT_CHANCE + critChanceBonus;
        double finalCritDamage = BASE_CRIT_DAMAGE + critDamageBonus;

        event.getToolTip().removeIf(component -> {
            String lower = component.getString().toLowerCase();

            return lower.contains("when in main hand")
                    || lower.contains("attack damage")
                    || lower.contains("attack speed")
                    || lower.contains("attack range")
                    || lower.contains("critical hit chance")
                    || lower.contains("critical hit damage")
                    || lower.contains("critical chance")
                    || lower.contains("critical damage")
                    || lower.contains("crit chance")
                    || lower.contains("crit damage");
        });

        int insertIndex = Math.min(1, event.getToolTip().size());

        if (attackSpeedLevel > 0) {
            event.getToolTip().add(insertIndex++, Component.literal(
                    "Attack Speed " + roman(attackSpeedLevel) + " +" + format(attackSpeedBonus) + "%"
            ).withStyle(levelColor(attackSpeedLevel)));
        }

        if (attackDamageLevel > 0) {
            event.getToolTip().add(insertIndex++, Component.literal(
                    "Attack Damage " + roman(attackDamageLevel) + " +" + format(attackDamageBonus) + "%"
            ).withStyle(levelColor(attackDamageLevel)));
        }

        if (chanceLevel > 0) {
            event.getToolTip().add(insertIndex++, Component.literal(
                    "Critical Chance " + roman(chanceLevel) + " +" + format(critChanceBonus) + "%"
            ).withStyle(levelColor(chanceLevel)));
        }

        if (critDamageLevel > 0) {
            event.getToolTip().add(insertIndex++, Component.literal(
                    "Critical Damage " + roman(critDamageLevel) + " +" + format(critDamageBonus) + "%"
            ).withStyle(levelColor(critDamageLevel)));
        }

        event.getToolTip().add(insertIndex++, Component.literal("When in Main Hand")
                .withStyle(ChatFormatting.GRAY));

        event.getToolTip().add(insertIndex++, Component.literal(
                "attack damage " + format(baseAttackDamage) + " → " + format(finalAttackDamage)
        ).withStyle(ChatFormatting.WHITE));

        if (attackRange != null) {
            event.getToolTip().add(insertIndex++, Component.literal(
                    "attack range " + format(attackRange)
            ).withStyle(ChatFormatting.WHITE));
        }

        event.getToolTip().add(insertIndex++, Component.literal(
                "attack speed " + format(baseAttackSpeed) + " → " + format(finalAttackSpeed)
        ).withStyle(ChatFormatting.WHITE));

        event.getToolTip().add(insertIndex++, Component.literal(
                "crit chance " + format(BASE_CRIT_CHANCE) + "% → " + format(finalCritChance) + "%"
        ).withStyle(ChatFormatting.WHITE));

        event.getToolTip().add(insertIndex, Component.literal(
                "crit damage " + format(BASE_CRIT_DAMAGE) + "% → " + format(finalCritDamage) + "%"
        ).withStyle(ChatFormatting.WHITE));
    }

    private static double getBaseAttackSpeed(ItemTooltipEvent event) {
        double value = 4.0D;

        for (var entry : event.getItemStack().getAttributeModifiers().modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_SPEED)) {
                value += entry.modifier().amount();
            }
        }

        return value;
    }

    private static double getBaseAttackDamage(ItemTooltipEvent event) {
        double value = 1.0D;

        for (var entry : event.getItemStack().getAttributeModifiers().modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                value += entry.modifier().amount();
            }
        }

        return value;
    }

    private static Double findTooltipNumber(List<Component> tooltip, String contains) {
        for (Component component : tooltip) {
            String text = component.getString();

            if (!text.contains(contains)) {
                continue;
            }

            Double value = findLastNumber(text);

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private static Double findLastNumber(String text) {
        Double lastNumber = null;
        StringBuilder currentNumber = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isDigit(c) || c == '.' || c == ',') {
                currentNumber.append(c);
            } else if (!currentNumber.isEmpty()) {
                lastNumber = parseNumber(currentNumber.toString());
                currentNumber.setLength(0);
            }
        }

        if (!currentNumber.isEmpty()) {
            lastNumber = parseNumber(currentNumber.toString());
        }

        return lastNumber;
    }

    private static Double parseNumber(String text) {
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int getEnchantmentLevel(
            ItemTooltipEvent event,
            ResourceKey<Enchantment> enchantmentKey
    ) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(event.getItemStack());

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantmentHolder = entry.getKey();

            if (enchantmentHolder.unwrapKey().isPresent()
                    && enchantmentHolder.unwrapKey().get().equals(enchantmentKey)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }

    private static ChatFormatting levelColor(int level) {
        return switch (level) {
            case 1 -> ChatFormatting.GREEN;
            case 2 -> ChatFormatting.BLUE;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(level);
        };
    }

    private static String format(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }

        return String.format("%.1f", value);
    }

    private static ResourceKey<Enchantment> enchantmentKey(String path) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, path)
        );
    }
}
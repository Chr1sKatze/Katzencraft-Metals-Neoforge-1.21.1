package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.config.CatoEnchantmentConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class CatoEnchantmentAttributeEvents {

    private static final ResourceKey<Enchantment> CRITICAL_CHANCE_ENCHANTMENT =
            enchantmentKey("critical_chance");

    private static final ResourceKey<Enchantment> CRITICAL_DAMAGE_ENCHANTMENT =
            enchantmentKey("critical_damage");

    private static final ResourceKey<Enchantment> ATTACK_SPEED_ENCHANTMENT =
            enchantmentKey("attack_speed");

    private static final ResourceKey<Enchantment> ATTACK_DAMAGE_ENCHANTMENT =
            enchantmentKey("attack_damage");

    private static final ResourceKey<Attribute> CRIT_CHANCE_ATTRIBUTE =
            ResourceKey.create(
                    Registries.ATTRIBUTE,
                    ResourceLocation.fromNamespaceAndPath("critical_strike", "chance")
            );

    private static final ResourceKey<Attribute> CRIT_DAMAGE_ATTRIBUTE =
            ResourceKey.create(
                    Registries.ATTRIBUTE,
                    ResourceLocation.fromNamespaceAndPath("critical_strike", "damage")
            );

    private static final ResourceLocation CRITICAL_CHANCE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "critical_chance_bonus");

    private static final ResourceLocation CRITICAL_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "critical_damage_bonus");

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        addCriticalChance(event);
        addCriticalDamage(event);
        addAttackSpeed(event);
        addAttackDamage(event);
    }

    private static void addCriticalChance(ItemAttributeModifierEvent event) {
        int level = getEnchantmentLevel(event, CRITICAL_CHANCE_ENCHANTMENT);

        if (level <= 0) {
            return;
        }

        var attributeOptional = BuiltInRegistries.ATTRIBUTE.getHolder(CRIT_CHANCE_ATTRIBUTE);

        if (attributeOptional.isEmpty()) {
            return;
        }

        Holder<Attribute> attribute = attributeOptional.get();

        double bonus = CatoEnchantmentConfig.CRITICAL_CHANCE_PER_LEVEL * level;

        event.replaceModifier(
                attribute,
                new AttributeModifier(
                        CRITICAL_CHANCE_MODIFIER_ID,
                        bonus,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static void addCriticalDamage(ItemAttributeModifierEvent event) {
        int level = getEnchantmentLevel(event, CRITICAL_DAMAGE_ENCHANTMENT);

        if (level <= 0) {
            return;
        }

        var attributeOptional = BuiltInRegistries.ATTRIBUTE.getHolder(CRIT_DAMAGE_ATTRIBUTE);

        if (attributeOptional.isEmpty()) {
            return;
        }

        Holder<Attribute> attribute = attributeOptional.get();

        double bonus = CatoEnchantmentConfig.CRITICAL_DAMAGE_PER_LEVEL * level;

        event.replaceModifier(
                attribute,
                new AttributeModifier(
                        CRITICAL_DAMAGE_MODIFIER_ID,
                        bonus,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "attack_speed_bonus");

    private static void addAttackSpeed(ItemAttributeModifierEvent event) {
        int level = getEnchantmentLevel(event, ATTACK_SPEED_ENCHANTMENT);

        if (level <= 0) {
            return;
        }

        double bonus = (CatoEnchantmentConfig.ATTACK_SPEED_PER_LEVEL * level) / 100.0D;

        System.out.println("ATTACK SPEED ENCHANT LEVEL = " + level);
        System.out.println("ADDING ATTACK SPEED BONUS = " + bonus);
        event.replaceModifier(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_MODIFIER_ID,
                        bonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "attack_damage_bonus");

    private static void addAttackDamage(ItemAttributeModifierEvent event) {
        int level = getEnchantmentLevel(event, ATTACK_DAMAGE_ENCHANTMENT);

        if (level <= 0) {
            return;
        }

        double bonus = (CatoEnchantmentConfig.ATTACK_DAMAGE_PER_LEVEL * level) / 100.0D;

        event.replaceModifier(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_MODIFIER_ID,
                        bonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static int getEnchantmentLevel(
            ItemAttributeModifierEvent event,
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

    private static ResourceKey<Enchantment> enchantmentKey(String path) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, path)
        );
    }
}
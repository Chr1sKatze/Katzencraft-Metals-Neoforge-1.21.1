package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class MobEquipmentEvents {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        clearEquipment(mob);

        if (mob instanceof Zombie zombie) {
            applyZombieEquipment(zombie);
        }

        if (mob instanceof AbstractSkeleton skeleton) {
            applySkeletonEquipment(skeleton);
        }
    }

    private static void clearEquipment(Mob mob) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);

        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
        mob.setDropChance(EquipmentSlot.LEGS, 0.0F);
        mob.setDropChance(EquipmentSlot.FEET, 0.0F);
    }

    private static void applyZombieEquipment(Zombie zombie) {
        // Main hand weapons
        if (roll(zombie, 0.15F)) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.03F);
        }

        if (roll(zombie, 0.10F)) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            zombie.setDropChance(EquipmentSlot.MAINHAND, 0.025F);
        }

        // Off hand
        if (roll(zombie, 0.20F)) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
            zombie.setDropChance(EquipmentSlot.OFFHAND, 0.00F);
        }

        // Armor
        if (roll(zombie, 0.08F)) {
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            zombie.setDropChance(EquipmentSlot.HEAD, 0.03F);
        }

        if (roll(zombie, 0.06F)) {
            zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            zombie.setDropChance(EquipmentSlot.CHEST, 0.03F);
        }

        if (roll(zombie, 0.06F)) {
            zombie.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            zombie.setDropChance(EquipmentSlot.LEGS, 0.03F);
        }

        if (roll(zombie, 0.08F)) {
            zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            zombie.setDropChance(EquipmentSlot.FEET, 0.03F);
        }
    }

    private static void applySkeletonEquipment(AbstractSkeleton skeleton) {
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.02F);
    }

    private static boolean roll(Mob mob, float chance) {
        return mob.getRandom().nextFloat() < chance;
    }
}
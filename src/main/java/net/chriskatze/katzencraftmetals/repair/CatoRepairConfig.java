package net.chriskatze.katzencraftmetals.repair;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class CatoRepairConfig {

    public record RepairEntry(Item material, float repairPercent) {}

    private static final Map<Item, RepairEntry> REPAIR_ENTRIES = new HashMap<>();

    static {
        // =========================
        // IRON TOOLS / WEAPONS
        // =========================

        REPAIR_ENTRIES.put(Items.IRON_PICKAXE, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_AXE, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_SHOVEL, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_HOE, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_SWORD, new RepairEntry(Items.IRON_INGOT, 0.50F));

        // =========================
        // IRON ARMOR
        // =========================

        REPAIR_ENTRIES.put(Items.IRON_HELMET, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_CHESTPLATE, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_LEGGINGS, new RepairEntry(Items.IRON_INGOT, 0.50F));
        REPAIR_ENTRIES.put(Items.IRON_BOOTS, new RepairEntry(Items.IRON_INGOT, 0.50F));

        // =========================
        // DIAMOND EXAMPLES
        // =========================

        REPAIR_ENTRIES.put(Items.DIAMOND_PICKAXE, new RepairEntry(Items.DIAMOND, 0.35F));
        REPAIR_ENTRIES.put(Items.DIAMOND_AXE, new RepairEntry(Items.DIAMOND, 0.35F));
        REPAIR_ENTRIES.put(Items.DIAMOND_SWORD, new RepairEntry(Items.DIAMOND, 0.35F));
    }

    public static RepairEntry get(Item item) {
        return REPAIR_ENTRIES.get(item);
    }

    public static boolean canRepair(Item item) {
        return REPAIR_ENTRIES.containsKey(item);
    }

    public static Iterable<RepairEntry> getAllEntries() {
        return REPAIR_ENTRIES.values();
    }

    private CatoRepairConfig() {
    }
}
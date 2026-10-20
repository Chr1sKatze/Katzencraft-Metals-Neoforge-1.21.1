package net.chriskatze.katzencraftmetals.config;

/** Mod block-requirement gates, NOT Minecraft native tiers. All 1..8 are valid.
 * Named aliases describe block gates only; actual tool capabilities come exclusively
 * from MetalEquipmentProfiles and may share a level or change independently.
 */
public final class MiningLevels {
    public static final int MINIMUM=1, WOOD=2, STONE=3, COPPER=3, GOLD=3, IRON=4;
    public static final int RESOURCE_5=5, STEEL=6, NETHERITE=7, MYTHRIL=8;
    public static void validate(int level){if(level<1||level>8)throw new IllegalArgumentException("Mining requirement must be 1..8");}
    private MiningLevels() {}
}

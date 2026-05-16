package net.chriskatze.katzencraftmetals.enchantment.scroll;

public enum ScrollTier {
    COMMON(1),
    ADVANCED(2),
    MASTER(3);

    private final int targetLevel;

    ScrollTier(int targetLevel) {
        this.targetLevel = targetLevel;
    }

    public int targetLevel() {
        return targetLevel;
    }
}
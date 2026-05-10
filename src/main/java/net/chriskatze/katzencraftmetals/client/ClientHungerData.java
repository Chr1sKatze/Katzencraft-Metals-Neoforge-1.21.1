package net.chriskatze.katzencraftmetals.client;

public class ClientHungerData {

    private static int hunger = 100;
    private static float displayedHunger = 100.0F;

    public static int getHunger() {
        return hunger;
    }

    public static float getDisplayedHunger() {
        return displayedHunger;
    }

    public static void setHunger(int value) {
        hunger = Math.max(0, Math.min(100, value));
        displayedHunger = hunger;
    }

    public static void tickDisplay() {
        displayedHunger += (hunger - displayedHunger) * 0.15F;

        if (Math.abs(displayedHunger - hunger) < 0.05F) {
            displayedHunger = hunger;
        }
    }
}
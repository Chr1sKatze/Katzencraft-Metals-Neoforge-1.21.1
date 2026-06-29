package net.chriskatze.katzencraftmetals.event;

/**
 * Intentionally inert. Tank removal is now handled once by FoundryTankBlock
 * through FoundryTankNetwork.prepareUpwardRemoval().
 *
 * This class can be deleted after the Step 8B checkpoint has been tested and
 * pushed.
 */
@Deprecated(forRemoval = true)
public final class FoundryMultiMetalEvents {

    private FoundryMultiMetalEvents() {
    }
}

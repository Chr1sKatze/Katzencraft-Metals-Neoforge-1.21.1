package net.chriskatze.katzencraftmetals.client.renderer;

record FoundryTankLiquidGeometry(
        float minX,
        float maxX,
        float minY,
        float surfaceY,
        float minZ,
        float maxZ,
        boolean renderTop,
        boolean renderBottom
) {
}

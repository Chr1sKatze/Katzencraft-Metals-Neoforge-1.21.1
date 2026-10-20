package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** Keeps only the table's lower stream inside its rotated mold; adds no faces or draw calls. */
final class FoundryFaucetTableStreamClip implements VertexConsumer {
    private final VertexConsumer out;
    private final BlockPos faucet;
    private final Direction facing;
    private final AABB mold;
    private final Direction downRotation;
    FoundryFaucetTableStreamClip(VertexConsumer out, BlockPos faucet, Direction facing, AABB mold) {
        this(out, faucet, facing, mold, Direction.NORTH);
    }
    FoundryFaucetTableStreamClip(VertexConsumer out, BlockPos faucet, Direction facing, AABB mold, Direction downRotation) {
        this.out=out; this.faucet=faucet; this.facing=facing; this.mold=mold; this.downRotation=downRotation;
    }
    @Override public VertexConsumer addVertex(Matrix4f pose,float x,float y,float z) {
        if (faucet.getY()+y <= mold.maxY) {
            double wx, wz;
            Direction rotation = facing == Direction.DOWN ? downRotation : facing;
            float shiftedZ = facing == Direction.DOWN ? z+FoundryFaucetRenderConstants.DOWN_STREAM_Z_OFFSET : z;
            switch (rotation) {
                case EAST -> { wx=1-shiftedZ; wz=x; }
                case SOUTH -> { wx=1-x; wz=1-shiftedZ; }
                case WEST -> { wx=shiftedZ; wz=1-x; }
                case DOWN -> { wx=x; wz=z+FoundryFaucetRenderConstants.DOWN_STREAM_Z_OFFSET; }
                default -> { wx=x; wz=shiftedZ; }
            }
            wx+=faucet.getX(); wz+=faucet.getZ();
            double gap=FoundryTableBlockEntityRenderer.WALL_CLEARANCE;
            float dx=(float)(Mth.clamp(wx,mold.minX+gap,mold.maxX-gap)-wx);
            float dz=(float)(Mth.clamp(wz,mold.minZ+gap,mold.maxZ-gap)-wz);
            switch (rotation) {
                case EAST -> { x+=dz; z-=dx; }
                case SOUTH -> { x-=dx; z-=dz; }
                case WEST -> { x-=dz; z+=dx; }
                default -> { x+=dx; z+=dz; }
            }
        }
        out.addVertex(pose,x,y,z); return this;
    }
    @Override public VertexConsumer addVertex(float x,float y,float z) { out.addVertex(x,y,z);return this; }
    @Override public VertexConsumer setColor(int r,int g,int b,int a) { out.setColor(r,g,b,a);return this; }
    @Override public VertexConsumer setUv(float u,float v) { out.setUv(u,v);return this; }
    @Override public VertexConsumer setUv1(int u,int v) { out.setUv1(u,v);return this; }
    @Override public VertexConsumer setUv2(int u,int v) { out.setUv2(u,v);return this; }
    @Override public VertexConsumer setNormal(float x,float y,float z) { out.setNormal(x,y,z);return this; }
}

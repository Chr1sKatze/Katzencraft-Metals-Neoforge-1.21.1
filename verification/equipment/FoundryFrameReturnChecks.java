package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Captures real underside quads, replacement material/UVs and side-wall preservation. */
public final class FoundryFrameReturnChecks {
    private record Point(float x,float y,float z) {}
    private static final class Capture implements VertexConsumer {
        final List<Point> points=new ArrayList<>();final List<float[]> uvs=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){points.add(new Point(x,y,z));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){uvs.add(new float[]{u,v});return this;}
        public VertexConsumer setUv1(int a,int b){return this;}
        public VertexConsumer setUv2(int a,int b){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run() throws Exception {
        var bottom=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerBottom",BlockPos.class,
                FoundryTankRenderedMetalLayer.class,FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,
                PoseStack.Pose.class,int.class,float.class,float.class);bottom.setAccessible(true);
        var side=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerSide",BlockPos.class,Direction.class,
                FoundryTankRenderedMetalLayer.class,FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,
                PoseStack.Pose.class,int.class,float.class,float.class);side.setAccessible(true);
        var texture=javax.imageio.ImageIO.read(java.nio.file.Path.of(System.getProperty("katzencraft.equipment.root"),
                "src/main/resources/assets/katzencraftmetals/textures/block/foundry_tank_top.png").toFile());
        int checked=0;float inset=FoundryTankRenderConstants.LIQUID_SIDE_INSET;
        for(var axis:List.of(Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH)) {
            // Isolated; straight/stacked; narrow T; one-sided L; wide support; wide T; concave corner.
            for(int layout=0;layout<7;layout++) {
                var support=new BlockPos(9,5,-7);var bridge=support.above();var cells=new HashSet<BlockPos>(Set.of(support));
                if(layout>0)cells.add(bridge);
                if(layout==2||layout==5){cells.add(bridge.relative(axis));cells.add(bridge.relative(axis.getOpposite()));}
                if(layout==3)cells.add(bridge.relative(axis));
                if(layout==4||layout==5){
                    cells.add(support.relative(axis));cells.add(bridge.relative(axis));
                    cells.add(bridge.relative(axis,2));
                    if(layout==5){cells.add(support.relative(axis.getOpposite()));cells.add(bridge.relative(axis.getOpposite(),2));}
                }
                if(layout==6){cells.add(bridge.relative(axis));cells.add(bridge.relative(axis.getClockWise()));}
                for(var metal:List.of("iron","steel","gold","copper"))for(float fill:new float[]{.01f,.5f,1f}) {
                    Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers=new HashMap<>();
                    for(var p:cells)layers.put(p,List.of(new FoundryTankRenderedMetalLayer(ResourceLocation.parse("minecraft:"+metal),0,p.getY()==support.getY()?1:fill,true,false)));
                    for(var pos:cells) {
                        var layer=layers.get(pos).getFirst();
                        var geometry=new FoundryTankLiquidGeometry(cells.contains(pos.west())?0:inset,cells.contains(pos.east())?1:1-inset,
                                0,layer.maxY(),cells.contains(pos.north())?0:inset,cells.contains(pos.south())?1:1-inset,true,false);
                        var original=new Capture();var liquid=new Capture();var frames=new ArrayList<FoundryTankJunctionMaterial.Face>();
                        bottom.invoke(null,pos,layer,geometry,layers,original,new PoseStack().last(),0,0f,1f);
                        var wrapped=FoundryTankJunctionMaterial.wrap(liquid,frames,()->0,pos,layer,layers);
                        bottom.invoke(null,pos,layer,geometry,layers,wrapped,new PoseStack().last(),0,0f,1f);
                        boolean joins=pos.getY()>support.getY()&&cells.contains(pos.below());
                        if(joins) {
                            check(liquid.points.isEmpty(),"Every generated support-footprint underside return uses frame material");
                            check(frames.size()*4==original.points.size(),"No missing or duplicated underside return quads");
                        } else check(frames.isEmpty()&&liquid.points.equals(original.points),"Legitimate unsupported/isolated liquid bottom unchanged");
                        var routed=new Capture();for(var f:frames)FoundryTankJunctionMaterial.emitFrame(routed,f);
                        check(joins?routed.points.equals(original.points):routed.points.isEmpty(),"Replacement retains exact emitted positions, winding and depth");
                        for(int f=0;f<frames.size();f++) {
                            var face=frames.get(f);float minX=1,maxX=0,minZ=1,maxZ=0;
                            for(var v:face.vertices()){
                                check(v.y==0&&v.ny==-1,"Only downward boundary returns changed");
                                minX=Math.min(minX,v.x);maxX=Math.max(maxX,v.x);minZ=Math.min(minZ,v.z);maxZ=Math.max(maxZ,v.z);
                            }
                            check(maxX-minX<=inset+.000001f||maxZ-minZ<=inset+.000001f,"Return remains at most .12 model pixels wide");
                            // Test the sampled texture over the entire emitted quad, not only its existence.
                            var uv0=routed.uvs.get(f*4);var uv1=routed.uvs.get(f*4+1);var uv3=routed.uvs.get(f*4+3);
                            for(int u=0;u<=8;u++)for(int v=0;v<=8;v++){
                                float tx=uv0[0]+(uv1[0]-uv0[0])*u/8+(uv3[0]-uv0[0])*v/8;
                                float ty=uv0[1]+(uv1[1]-uv0[1])*u/8+(uv3[1]-uv0[1])*v/8;
                                int color=texture.getRGB(Math.clamp((int)(tx*texture.getWidth()),0,texture.getWidth()-1),Math.clamp((int)(ty*texture.getHeight()),0,texture.getHeight()-1));
                                check((color>>>24)==255,"Frame return samples opaque authored frame texture across quad");
                            }
                        }
                        // The newly handled one-sided/wide returns must not paint an aligned vertical wall.
                        if(layout==3||layout==4||layout==6)for(var direction:Direction.Plane.HORIZONTAL){
                            var normal=new Capture();var remaining=new Capture();var sideFrames=new ArrayList<FoundryTankJunctionMaterial.Face>();
                            side.invoke(null,pos,direction,layer,geometry,layers,normal,new PoseStack().last(),0,0f,1f);
                            side.invoke(null,pos,direction,layer,geometry,layers,FoundryTankJunctionMaterial.wrap(remaining,sideFrames,()->0,pos,layer,layers),new PoseStack().last(),0,0f,1f);
                            if(!cells.contains(pos.relative(direction)))check(sideFrames.isEmpty()&&remaining.points.equals(normal.points),"No new dark horizontal seam or hidden legitimate exterior surface");
                        }
                        checked++;
                    }
                }
            }
        }
        System.out.println("PASS: "+checked+" production mesh cases: isolated/stacked/T/one-sided/wide/concave returns, four rotations, partial/full four-metal layers; exact position/winding/depth replacement, opaque texture coverage, <=.12-pixel widths, untouched legitimate bottoms and continuous side walls. GPU/in-game screenshot confirmation remains required.");
    }
}

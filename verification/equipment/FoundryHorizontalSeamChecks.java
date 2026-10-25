package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Captures production side/bottom quad emission and material routing; no GPU claims. */
public final class FoundryHorizontalSeamChecks {
    private record Point(float x,float y,float z) {}
    private static final class Capture implements VertexConsumer {
        final List<Point> points=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){points.add(new Point(x,y,z));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static double area(List<Point> points){
        double result=0;
        for(int i=0;i<points.size();i+=4){
            Point a=points.get(i),b=points.get(i+1),d=points.get(i+3);
            double ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=d.x-a.x,vy=d.y-a.y,vz=d.z-a.z;
            result+=Math.sqrt(Math.pow(uy*vz-uz*vy,2)+Math.pow(uz*vx-ux*vz,2)+Math.pow(ux*vy-uy*vx,2));
        }
        return result;
    }
    public static void run() throws Exception {
        var side=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerSide",BlockPos.class,Direction.class,
                FoundryTankRenderedMetalLayer.class,FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,
                PoseStack.Pose.class,int.class,float.class,float.class);side.setAccessible(true);
        var bottom=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerBottom",BlockPos.class,
                FoundryTankRenderedMetalLayer.class,FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,
                PoseStack.Pose.class,int.class,float.class,float.class);bottom.setAccessible(true);
        var create=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("createRenderedLayers",BlockPos.class,Set.class,Map.class);create.setAccessible(true);
        BlockPos support=BlockPos.ZERO,bridge=support.above();
        int checks=0;
        for(Direction axis:List.of(Direction.EAST,Direction.SOUTH))for(int layout=0;layout<5;layout++){
            Set<BlockPos> cells=new HashSet<>(Set.of(support));
            if(layout>0)cells.add(bridge);
            if(layout>=2){cells.add(bridge.relative(axis));cells.add(bridge.relative(axis.getOpposite()));}
            if(layout==3){cells.add(support.relative(axis));cells.add(support.relative(axis.getOpposite()));}
            if(layout==4)cells.remove(bridge.relative(axis.getOpposite())); // L/step, not a crossbar
            for(float upper: new float[]{0, .01f, 1, 40, 108})for(boolean mixed:new boolean[]{false,true}){
                var iron=ResourceLocation.withDefaultNamespace("iron");var gold=ResourceLocation.withDefaultNamespace("gold");
                Map<Integer,Map<ResourceLocation,Float>> levels=new HashMap<>();
                levels.put(0,mixed?Map.of(gold,54f,iron,54f):Map.of(iron,108f));
                levels.put(1,upper==0?Map.of():Map.of(iron,upper));
                Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers=new HashMap<>();
                for(BlockPos p:cells){
                    @SuppressWarnings("unchecked") var local=(List<FoundryTankRenderedMetalLayer>)create.invoke(null,p,cells,levels);
                    layers.put(p,local);
                }
                for(BlockPos p:cells)for(var layer:layers.get(p)){
                    float inset=FoundryTankRenderConstants.LIQUID_SIDE_INSET;
                    var geometry=new FoundryTankLiquidGeometry(cells.contains(p.west())?0:inset,cells.contains(p.east())?1:1-inset,
                            layer.minY(),layer.maxY(),cells.contains(p.north())?0:inset,cells.contains(p.south())?1:1-inset,
                            layer.renderTop(),layer.renderBottom());
                    for(Direction direction:Direction.Plane.HORIZONTAL){
                        var original=new Capture();var liquid=new Capture();var frames=new ArrayList<FoundryTankJunctionMaterial.Face>();
                        var routed=FoundryTankJunctionMaterial.wrap(liquid,frames,()->0,p,layer,layers);
                        var pose=new PoseStack().last();
                        side.invoke(null,p,direction,layer,geometry,layers,original,pose,0,0f,1f);
                        side.invoke(null,p,direction,layer,geometry,layers,routed,pose,0,0f,1f);
                        var frame=new Capture();for(var face:frames)FoundryTankJunctionMaterial.emitFrame(frame,face);
                        check(Math.abs(area(original.points)-area(liquid.points)-area(frame.points))<.000001,"Exact side coverage retained");
                        // Front/back wall of the T: aligned upper/lower wall must stay liquid through its central run.
                        if(layout==2&&upper>0&&direction.getAxis()!=axis.getAxis()&&!frames.isEmpty()){
                            for(var face:frames){
                                float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
                                for(var v:face.vertices()){float c=axis==Direction.EAST?v.x:v.z;min=Math.min(min,c);max=Math.max(max,c);}
                                check(max<=inset+.00001f||min>=1-inset-.00001f,"No dark band across continuous stacked wall");
                            }
                        }
                        if(layout==2&&upper>0&&p.equals(bridge)&&direction.getAxis()!=axis.getAxis())
                            check(!frames.isEmpty(),"Genuine crossbar's exposed end returns remain frame material");
                        if((layout==1||layout==3||layout==4)&&!cells.contains(p.relative(direction)))check(frames.isEmpty(),"Ordinary stack, straight wall and L/step have no junction bands");
                        checks++;
                    }
                    var original=new Capture();var liquid=new Capture();var frames=new ArrayList<FoundryTankJunctionMaterial.Face>();
                    bottom.invoke(null,p,layer,geometry,layers,original,new PoseStack().last(),0,0f,1f);
                    bottom.invoke(null,p,layer,geometry,layers,FoundryTankJunctionMaterial.wrap(liquid,frames,()->0,p,layer,layers),new PoseStack().last(),0,0f,1f);
                    var frame=new Capture();for(var face:frames)FoundryTankJunctionMaterial.emitFrame(frame,face);
                    check(Math.abs(area(original.points)-area(liquid.points)-area(frame.points))<.000001,"Bridge underside repairs retained exactly");
                }
            }
        }
        System.out.println("PASS: "+checks+" production side emissions: both crossbar orientations, single/stacked/rectangular/L/step layouts, below/at/above boundary and multiple metals; liquid wall continuity, frame returns, exact side/underside coverage. Shared attached/detached emitter; GPU inspection still required.");
    }
}

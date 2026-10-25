package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Production shared-boundary returns seen edge-on/inside the tank, not generic underside quads. */
public final class FoundrySharedReturnMaterialChecks {
    private record V(float x,float y,float z) {}
    private static final class Capture implements VertexConsumer {
        final List<V> points=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){points.add(new V(x,y,z));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int a,int b){return this;}
        public VertexConsumer setUv2(int a,int b){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run() throws Exception {
        var emit=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerSide",BlockPos.class,Direction.class,
                FoundryTankRenderedMetalLayer.class,FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,
                PoseStack.Pose.class,int.class,float.class,float.class);emit.setAccessible(true);
        int count=0;
        for(var direction:Direction.Plane.HORIZONTAL)for(int kind=0;kind<3;kind++) {
            var pos=BlockPos.ZERO;var neighbor=pos.relative(direction);var edge=direction.getClockWise();
            boolean step=kind!=0;
            Set<BlockPos> cells=kind==1?Set.of(pos,neighbor,pos.above()):kind==2?Set.of(pos,neighbor,pos.below()):Set.of(pos,neighbor,pos.relative(edge));
            for(var metal:List.of("iron","gold","copper","steel")) {
                Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers=new HashMap<>();
                for(var p:cells)layers.put(p,List.of(new FoundryTankRenderedMetalLayer(ResourceLocation.parse((metal.equals("steel")?"katzencraftmetals:":"minecraft:")+metal),
                        (kind==1&&p.equals(pos.above())||kind==2&&p.equals(pos))?0:.2f/16,
                        (kind==1&&p.equals(pos)||kind==2&&p.equals(pos.below()))?1:15.8f/16,true,true)));
                float inset=FoundryTankRenderConstants.LIQUID_SIDE_INSET;
                var layer=layers.get(pos).getFirst();
                var geometry=new FoundryTankLiquidGeometry(cells.contains(pos.west())?0:inset,cells.contains(pos.east())?1:1-inset,
                        layer.minY(),layer.maxY(),cells.contains(pos.north())?0:inset,cells.contains(pos.south())?1:1-inset,true,true);
                var original=new Capture();var liquid=new Capture();var frames=new ArrayList<FoundryTankJunctionMaterial.Face>();
                emit.invoke(null,pos,direction,layer,geometry,layers,original,new PoseStack().last(),0,0f,1f);
                emit.invoke(null,pos,direction,layer,geometry,layers,FoundryTankJunctionMaterial.wrap(liquid,frames,()->0,pos,layer,layers),new PoseStack().last(),0,0f,1f);
                System.out.println("SHARED RETURN "+direction+" "+(step?"height-step":"vertical corner")+": original="+original.points+", liquid="+liquid.points+", frameQuads="+frames.size());
                check(original.points.size()==4,"Exactly one existing return quad in targeted fixture");
                check(liquid.points.isEmpty()&&frames.size()==1,"Actual shared-boundary return must use frame material, not the animated molten texture");
                check(!frames.getFirst().bottom()&&frames.getFirst().light()==0,"Existing stone-frame pass and ambient frame lighting, not molten fullbright material");
                var frame=new Capture();FoundryTankJunctionMaterial.emitFrame(frame,frames.getFirst());
                check(frame.points.equals(original.points),"Same vertices/winding/depth; material replacement, no extra geometry");
                // A normal exterior wall is never a frame return.
                var outside=direction.getOpposite();var normal=new Capture();var preserved=new Capture();var negative=new ArrayList<FoundryTankJunctionMaterial.Face>();
                emit.invoke(null,pos,outside,layer,geometry,layers,normal,new PoseStack().last(),0,0f,1f);
                emit.invoke(null,pos,outside,layer,geometry,layers,FoundryTankJunctionMaterial.wrap(preserved,negative,()->0,pos,layer,layers),new PoseStack().last(),0,0f,1f);
                check(negative.isEmpty()&&normal.points.equals(preserved.points),"Legitimate exterior wall unchanged");
                // A general partial-height liquid boundary between connected cells is not a frame band.
                Map<BlockPos,List<FoundryTankRenderedMetalLayer>> partial=new HashMap<>();
                var a=new FoundryTankRenderedMetalLayer(layer.metal(),.2f/16,.8f,true,true);
                var b=new FoundryTankRenderedMetalLayer(ResourceLocation.withDefaultNamespace("gold"),.2f/16,.6f,true,true);
                partial.put(pos,List.of(a));partial.put(neighbor,List.of(b));
                var full=new FoundryTankLiquidGeometry(inset,1-inset,a.minY(),a.maxY(),inset,1-inset,true,true);
                var expected=new Capture();var actual=new Capture();var invalid=new ArrayList<FoundryTankJunctionMaterial.Face>();
                emit.invoke(null,pos,direction,a,full,partial,expected,new PoseStack().last(),0,0f,1f);
                emit.invoke(null,pos,direction,a,full,partial,FoundryTankJunctionMaterial.wrap(actual,invalid,()->0,pos,a,partial),new PoseStack().last(),0,0f,1f);
                check(invalid.isEmpty()&&actual.points.equals(expected.points),"Normal partial-height/mixed-metal liquid boundary remains liquid");count++;
            }
        }
        System.out.println("PASS: "+count+" real shared return quads: four orientations, corner/upper/lower-step provenance, four metals, exact position/material replacement and untouched exterior walls.");
    }
}

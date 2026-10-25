package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Equivalent production calls: plain maps retain the original calculation path as an oracle. */
public final class FoundrySideAllocationChecks {
    private static MethodHandle side, trim;
    private static volatile Object blackhole;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static class Sink implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z){return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private static final class AllocationSink extends Sink {
        // Exclude identical downstream vertex transformations from the metadata allocation comparison.
        @Override public VertexConsumer addVertex(Matrix4f m,float x,float y,float z){return this;}
        @Override public VertexConsumer setNormal(PoseStack.Pose p,float x,float y,float z){return this;}
    }
    private static final class Capture extends Sink {
        final int[] data=new int[8192];int size;
        void put(int n){data[size++]=n;}
        void f(float n){put(Float.floatToRawIntBits(n));}
        void reset(){Arrays.fill(data,0);size=0;}
        @Override public VertexConsumer addVertex(float x,float y,float z){put(1);f(x);f(y);f(z);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){put(2);put(r);put(g);put(b);put(a);return this;}
        @Override public VertexConsumer setUv(float u,float v){put(3);f(u);f(v);return this;}
        @Override public VertexConsumer setUv1(int u,int v){put(4);put(u);put(v);return this;}
        @Override public VertexConsumer setUv2(int u,int v){put(5);put(u);put(v);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){put(6);f(x);f(y);f(z);return this;}
    }
    private static void emit(BlockPos pos,Direction direction,FoundryTankRenderedMetalLayer layer,
                             FoundryTankLiquidGeometry geometry,Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers,
                             VertexConsumer out,PoseStack.Pose pose){
        try{side.invokeExact(pos,direction,layer,geometry,(Map)layers,out,pose,17,.1875f,.25f);}
        catch(Throwable error){throw new AssertionError(error);}
    }
    private static Object trim(BlockPos pos,FoundryTankRenderedMetalLayer layer,Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers){
        try{return (Object)trim.invokeExact(pos,layer,(Map)layers);}
        catch(Throwable error){throw new AssertionError(error);}
    }
    private static FoundryTankLiquidGeometry geometry(BlockPos p,FoundryTankRenderedMetalLayer layer,Set<BlockPos> cells){
        float inset=FoundryTankRenderConstants.LIQUID_SIDE_INSET;
        return new FoundryTankLiquidGeometry(cells.contains(p.west())?0:inset,cells.contains(p.east())?1:1-inset,
                layer.minY(),layer.maxY(),cells.contains(p.north())?0:inset,cells.contains(p.south())?1:1-inset,
                layer.renderTop(),layer.renderBottom());
    }
    private static long allocated(com.sun.management.ThreadMXBean bean,Runnable action){
        long id=Thread.currentThread().threadId(),best=Long.MAX_VALUE;
        for(int round=0;round<3;round++){
            long start=bean.getThreadAllocatedBytes(id);action.run();
            best=Math.min(best,bean.getThreadAllocatedBytes(id)-start);
        }return best;
    }
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var type=FoundryControllerTankLiquidRenderer.class;
        var method=type.getDeclaredMethod("renderLayerSide",BlockPos.class,Direction.class,FoundryTankRenderedMetalLayer.class,
                FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,PoseStack.Pose.class,int.class,float.class,float.class);
        method.setAccessible(true);side=MethodHandles.lookup().unreflect(method);
        method=type.getDeclaredMethod("getShaderInterfaceTrim",BlockPos.class,FoundryTankRenderedMetalLayer.class,Map.class);
        method.setAccessible(true);trim=MethodHandles.lookup().unreflect(method).asType(MethodType.methodType(Object.class,BlockPos.class,FoundryTankRenderedMetalLayer.class,Map.class));
        var iron=ResourceLocation.withDefaultNamespace("iron");var gold=ResourceLocation.withDefaultNamespace("gold");
        var p=BlockPos.ZERO;
        List<Set<BlockPos>> layouts=List.of(Set.of(p),Set.of(p,p.above()),Set.of(p,p.east(),p.south()),
                Set.of(p,p.above(),p.above().east(),p.above().west()),Set.of(p,p.east(),p.south(),p.south().east(),p.above()));
        var poseStack=new PoseStack();poseStack.translate(.125,.25,-.75);poseStack.scale(.8f,1.2f,1.1f);var pose=poseStack.last();
        var before=new Capture();var after=new Capture();int comparisons=0;
        for(var cells:layouts)for(float fill:new float[]{.001f,53.999f,54f,107.999f,108f,108.001f,215.999f,320f}){
            var cache=new FoundryControllerTankLiquidRenderer.LayerCache();
            cache.update(cells,Map.of(iron,fill,gold,54f));
            Map<BlockPos,List<FoundryTankRenderedMetalLayer>> plain=Collections.unmodifiableMap(new LinkedHashMap<>(cache.layers));
            for(var pos:cells)for(var layer:cache.layers.get(pos)){
                check(Objects.equals(trim(pos,layer,plain),trim(pos,layer,cache.layers)),"Cached trim differs from direct original arithmetic");
                for(var direction:Direction.Plane.HORIZONTAL){
                    before.reset();after.reset();var g=geometry(pos,layer,cells);
                    emit(pos,direction,layer,g,plain,before,pose);emit(pos,direction,layer,g,cache.layers,after,pose);
                    check(before.size==after.size&&Arrays.equals(before.data,after.data),"Exact side vertex/UV/light/normal mismatch at "+pos+" "+direction+" fill="+fill);
                    comparisons++;
                }
            }
        }
        var cells=new HashSet<BlockPos>();for(int z=0;z<4;z++)for(int x=0;x<4;x++)cells.add(new BlockPos(x,0,z));
        var cache=new FoundryControllerTankLiquidRenderer.LayerCache();cache.update(cells,Map.of(iron,864f,gold,864f));
        Map<BlockPos,List<FoundryTankRenderedMetalLayer>> plain=Collections.unmodifiableMap(new LinkedHashMap<>(cache.layers));
        var center=new BlockPos(1,0,1);var layer=cache.layers.get(center).getFirst();var g=geometry(center,layer,cells);
        VertexConsumer sink=new AllocationSink();var identityPose=new PoseStack().last();
        Runnable oldTrim=()->{for(int i=0;i<50000;i++)blackhole=trim(center,layer,plain);};
        Runnable newTrim=()->{for(int i=0;i<50000;i++)blackhole=trim(center,layer,cache.layers);};
        Runnable oldSide=()->{for(int i=0;i<50000;i++)emit(center,Direction.NORTH,layer,g,plain,sink,identityPose);};
        Runnable newSide=()->{for(int i=0;i<50000;i++)emit(center,Direction.NORTH,layer,g,cache.layers,sink,identityPose);};
        for(int i=0;i<3;i++){oldTrim.run();newTrim.run();oldSide.run();newSide.run();}
        if(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean&&bean.isThreadAllocatedMemorySupported()){
            if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
            long a=allocated(bean,oldTrim),b=allocated(bean,newTrim),c=allocated(bean,oldSide),d=allocated(bean,newSide);
            check(b<a&&d<c,"Warm metadata paths must allocate less under identical inputs");
            System.out.println("PASS: measured ThreadMXBean allocations / 50000 warmed calls: trim "+a+" -> "+b+" bytes; interior side "+c+" -> "+d+" bytes. Excludes cache construction and common downstream vertex transformations; not JFR estimates or FPS.");
        }else System.out.println("SKIP: JDK thread allocation accounting unavailable; geometry checks still run.");
        physical(lookup);
        System.out.println("PASS: "+comparisons+" exact old/new side emission comparisons across corners, bridge/T, partial/stacked/mixed layouts; direct/cached trims. Existing cache invalidation and live-wave tests also run.");
    }
    private static void physical(HolderLookup.Provider lookup) throws Exception {
        var helpers=Class.forName("net.chriskatze.katzencraftmetals.block.entity.FoundryDismantlingChecks");
        var make=helpers.getDeclaredMethod("world",HolderLookup.Provider.class);make.setAccessible(true);Object world=make.invoke(null,lookup);
        var cells=Set.of(BlockPos.ZERO,BlockPos.ZERO.east());
        var tanks=helpers.getDeclaredMethod("tanks",world.getClass(),Set.class);tanks.setAccessible(true);tanks.invoke(null,world,cells);
        var controllerMethod=helpers.getDeclaredMethod("controller",world.getClass());controllerMethod.setAccessible(true);
        var controller=(net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity)controllerMethod.invoke(null,world);
        var network=controller.getOwnedTankNetwork();
        var filter=FoundryControllerBlockEntityRenderer.class.getDeclaredMethod("getPhysicalClientStructure",controller.getClass().getSuperclass(),network.getClass());filter.setAccessible(true);
        check(filter.invoke(null,controller,network)==network.getTankPositions(),"Unchanged physical layout reuses immutable network positions");
        var blocks=world.getClass().getDeclaredField("blocks");blocks.setAccessible(true);
        ((Map<?,?>)blocks.get(world)).remove(BlockPos.ZERO);
        check(filter.invoke(null,controller,network).equals(Set.of(BlockPos.ZERO.east())),"Live stale-block removal still filtered immediately");
    }
}

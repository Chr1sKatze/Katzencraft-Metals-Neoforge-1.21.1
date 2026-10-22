package net.chriskatze.katzencraftmetals.block.entity;

import io.netty.buffer.Unpooled;
import net.chriskatze.katzencraftmetals.client.FoundryDetachedTankCache;
import net.chriskatze.katzencraftmetals.network.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Production replica/codec lifecycle and actual shared renderer layer calculations, without a GPU. */
public final class FoundryDetachedRenderingChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static ResourceLocation id(String name){return ResourceLocation.withDefaultNamespace(name);}
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var origin=new BlockPos(14,0,0);var left=new BlockPos(15,0,0);var right=new BlockPos(16,0,0);
        var cells=Set.of(left,right,left.above(),right.above());var amounts=Map.of(id("gold"),160,id("iron"),137);
        var snapshot=new FoundryVesselSnapshot(left,cells,amounts,origin);var vessel=UUID.randomUUID();
        var data=new FoundryDetachedVessels();data.put(vessel,snapshot);
        var saved=data.save(new CompoundTag(),lookup);var reloaded=FoundryDetachedVessels.load(saved,lookup);
        check(reloaded.snapshots().get(vessel).equals(snapshot),"SavedData preserves visual origin, complete structure and exact multi-metal amounts");
        long a=ChunkPos.asLong(0,0),b=ChunkPos.asLong(1,0);
        var viewA=FoundryDetachedSync.inChunk(reloaded,a);var viewB=FoundryDetachedSync.inChunk(reloaded,b);
        check(viewA.equals(Map.of(vessel,snapshot))&&viewB.equals(viewA),"Watching either intersecting chunk supplies FULL vessel snapshot");
        check(FoundryDetachedSync.inChunk(reloaded,ChunkPos.asLong(5,5)).isEmpty(),"Unrelated chunk receives no vessel");
        var payload=new FoundryDetachedPayload(id("overworld"),a,3,false,viewA);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        FoundryDetachedPayload roundTrip;
        try{FoundryDetachedPayload.STREAM_CODEC.encode(buffer,payload);roundTrip=FoundryDetachedPayload.STREAM_CODEC.decode(buffer);}finally{buffer.release();}
        check(roundTrip.equals(payload),"Real wire codec preserves every cell, metal, amount, origin and revision");
        var one=new FoundryDetachedTankCache();var two=new FoundryDetachedTankCache();one.apply(roundTrip);two.apply(roundTrip);
        one.apply(new FoundryDetachedPayload(id("overworld"),b,3,false,viewB));
        check(one.views(id("overworld")).size()==1,"One draw source despite duplicate chunk subscriptions");
        one.apply(new FoundryDetachedPayload(id("overworld"),a,3,true,Map.of()));
        check(one.views(id("overworld")).equals(viewA),"Unloading anchor chunk retains complete data from other watched chunk");
        one.apply(new FoundryDetachedPayload(id("overworld"),b,2,false,Map.of()));
        check(one.views(id("overworld")).equals(viewA),"Older visual update cannot overwrite current view");
        one.apply(new FoundryDetachedPayload(id("overworld"),b,3,true,Map.of()));
        check(one.views(id("overworld")).isEmpty()&&two.views(id("overworld")).size()==1,"Independent multiplayer replicas and complete unwatch cleanup");
        one.apply(roundTrip);check(one.views(id("the_nether")).isEmpty(),"Dimension isolation");one.clear();check(one.views(id("overworld")).isEmpty(),"Disconnect/world unload cache cleanup");
        one.apply(new FoundryDetachedPayload(id("overworld"),a,4,false,Map.of()));
        one.apply(roundTrip);check(one.views(id("overworld")).isEmpty(),"Old pre-claim packet cannot resurrect a retired view");
        check(reloaded.snapshots().get(vessel).contents().equals(amounts),"Replica edits never modify authoritative SavedData");
        var renderer=Class.forName("net.chriskatze.katzencraftmetals.client.renderer.FoundryControllerTankLiquidRenderer");
        var distribute=renderer.getDeclaredMethod("distributeAcrossHorizontalLevels",Set.class,Map.class);distribute.setAccessible(true);
        var create=renderer.getDeclaredMethod("createRenderedLayers",BlockPos.class,Set.class,Map.class);create.setAccessible(true);
        var floats=new LinkedHashMap<ResourceLocation,Float>();amounts.forEach((metal,n)->floats.put(metal,n.floatValue()));
        var levels=(Map<?,?>)distribute.invoke(null,snapshot.positions(),floats);
        for(var p:cells)check(create.invoke(null,p,cells,levels).equals(create.invoke(null,p,viewB.get(vessel).positions(),levels)),"Identical attached/detached emitted layer geometry");
        var bottom=(Map<?,?>)levels.get(0);var top=(Map<?,?>)levels.get(1);
        check(bottom.get(id("gold")).equals(80f)&&bottom.get(id("iron")).equals(28f)&&top.get(id("iron")).equals(40.5f),"Density ordering and shared 108-unit cell capacity across loaded/unloaded chunks");
        var world=FoundryDismantlingChecks.world(lookup);
        var smootherType=Class.forName("net.chriskatze.katzencraftmetals.client.renderer.FoundryControllerLiquidSmoother");
        var constructor=smootherType.getDeclaredConstructor();constructor.setAccessible(true);var smoother=constructor.newInstance();
        var smooth=smootherType.getDeclaredMethod("getDisplayedAmounts",Object.class,net.minecraft.world.level.Level.class,Map.class,int.class,float.class);smooth.setAccessible(true);
        check(smooth.invoke(smoother,vessel,world,amounts,cells.size()*108,0f).equals(floats),"Detached first frame initializes at exact saved heights, no empty-rise animation");
        check(reloaded.claim(cells).orElseThrow().contents().equals(amounts)&&reloaded.claim(cells).isEmpty(),"Controller recovers identical amounts exactly once");
        // Guard against regression to passing Controller crumbling buffers into remote tank draws.
        var root=java.nio.file.Path.of(System.getProperty("katzencraft.equipment.root"));
        var source=java.nio.file.Files.readString(root.resolve("src/main/java/net/chriskatze/katzencraftmetals/client/renderer/FoundryControllerBlockEntityRenderer.java"));
        check(source.contains("bufferSource = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource()")&&source.contains("OverlayTexture.NO_OVERLAY"),"Remote draws use clean buffers, not Controller destruction wrapper");
        var tankSource=java.nio.file.Files.readString(root.resolve("src/main/java/net/chriskatze/katzencraftmetals/block/custom/FoundryTankBlock.java"));
        check(!tankSource.contains("renderBreakingTexture"),"Normal targeted tank cracks remain native chunk-model rendering");
        System.out.println("PASS: detached SavedData/render-origin reload; actual wire codec; two-client chunk watch/unwatch/reload and revision handling; single vessel draw source; exact shared multi-layer geometry and smoothing; no replica authority; one-time Controller claim; clean destruction-buffer routing.");
    }
}

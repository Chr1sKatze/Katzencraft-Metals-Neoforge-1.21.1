package net.chriskatze.katzencraftmetals.block.entity;

import io.netty.buffer.Unpooled;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.chriskatze.katzencraftmetals.client.FoundryDetachedTankCache;
import net.chriskatze.katzencraftmetals.network.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Native placement/handshake/one-shot refresh, escrow persistence and actual wire/client geometry. */
public final class FoundryDetachedExpansionChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void place(FoundryDismantlingChecks.World w,BlockPos pos,Set<BlockPos> connectTo){
        var state=ModBlocks.FOUNDRY_TANK.get().defaultBlockState();
        for(var direction:Direction.values())state=state.setValue(FoundryTankBlock.connectionProperty(direction),connectTo.contains(pos.relative(direction)));
        w.blocks.put(pos,state);
        // Actual onPlace runs before neighboring updateShape handshakes.
        state.onPlace(w,pos,Blocks.AIR.defaultBlockState(),false);
        check(w.scheduled.containsKey(pos),"Placement queues one post-handshake refresh");
        for(var direction:Direction.values()){
            var neighbor=pos.relative(direction);var old=w.getBlockState(neighbor);
            if(!old.is(ModBlocks.FOUNDRY_TANK.get()))continue;
            var updated=old.updateShape(direction.getOpposite(),state,w,neighbor,pos);
            w.blocks.put(neighbor,updated);
            state=state.updateShape(direction,updated,w,pos,neighbor);w.blocks.put(pos,state);
        }
    }
    private static void flush(FoundryDismantlingChecks.World w){
        var scheduled=Map.copyOf(w.scheduled);w.scheduled.clear();
        scheduled.forEach((pos,block)->{var state=w.getBlockState(pos);if(state.is(block))state.tick(w,pos,w.random);});
    }
    private static FoundryControllerBlockEntity controller(FoundryDismantlingChecks.World w,BlockPos position){
        var controller=new FoundryDismantlingChecks.Controller(position);controller.setLevel(w);
        w.blocks.put(position,controller.getBlockState());w.entities.put(position,controller);
        check(controller.ensureTankNetwork(),"Controller attaches");return controller;
    }
    private static FoundryDetachedPayload wire(FoundryDetachedPayload payload){
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        try{FoundryDetachedPayload.STREAM_CODEC.encode(buffer,payload);return FoundryDetachedPayload.STREAM_CODEC.decode(buffer);}finally{buffer.release();}
    }
    private static float surface(Set<BlockPos> cells,Map<ResourceLocation,Integer> amounts,BlockPos pos) throws Exception {
        var renderer=Class.forName("net.chriskatze.katzencraftmetals.client.renderer.FoundryControllerTankLiquidRenderer");
        var distribute=renderer.getDeclaredMethod("distributeAcrossHorizontalLevels",Set.class,Map.class);distribute.setAccessible(true);
        var create=renderer.getDeclaredMethod("createRenderedLayers",BlockPos.class,Set.class,Map.class);create.setAccessible(true);
        Map<ResourceLocation,Float> floats=new HashMap<>();amounts.forEach((id,n)->floats.put(id,n.floatValue()));
        var levels=distribute.invoke(null,cells,floats);var layers=(List<?>)create.invoke(null,pos,cells,levels);
        check(!layers.isEmpty(),"Newly added cell receives actual rendered liquid layers");
        var maxY=layers.getLast().getClass().getDeclaredMethod("maxY");maxY.setAccessible(true);
        return (float)maxY.invoke(layers.getLast());
    }
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var iron=ResourceLocation.withDefaultNamespace("iron");var gold=ResourceLocation.withDefaultNamespace("gold");
        for(int offset:List.of(0,15))for(boolean mixed:List.of(false,true)){
            var w=FoundryDismantlingChecks.world(lookup);var a=new BlockPos(offset,0,0);var b=a.east();var c=b.east();var d=c.east();
            var originalCells=Set.of(a,b);FoundryDismantlingChecks.tanks(w,originalCells);
            var owner=controller(w,a.west());owner.getOwnedTankNetwork().insert(iron,80);if(mixed)owner.getOwnedTankNetwork().insert(gold,40);
            var expected=owner.getOwnedTankNetwork().getMoltenContents();
            float beforeSurface=surface(originalCells,expected,a);
            w.destroyBlock(owner.getBlockPos(),true,null,512);
            var data=FoundryDetachedVessels.get(w);var id=data.snapshots().keySet().iterator().next();var original=data.snapshots().get(id);
            long revision=data.visualRevision();place(w,c,originalCells);
            check(data.snapshots().get(id).positions().equals(originalCells),"Do not resolve incomplete onPlace handshake");
            flush(w);var expanded=data.snapshots().get(id);
            check(expanded.positions().equals(Set.of(a,b,c))&&expanded.contents().equals(expected),"First addition expands full escrow with exact per-metal quantities");
            check(expanded.anchor().equals(original.anchor())&&expanded.renderOrigin().equals(original.renderOrigin()),"Stable anchor, UUID and visual phase");
            check(data.visualRevision()==revision+1&&data.isDirty(),"One persistent/synchronized update for expansion");
            check(surface(expanded.positions(),expanded.contents(),c)<beforeSurface,"Actual shared liquid renderer immediately redistributes into added capacity");
            place(w,d,expanded.positions());flush(w);expanded=data.snapshots().get(id);
            check(expanded.positions().equals(Set.of(a,b,c,d))&&expanded.contents().equals(expected),"Consecutive additions preserve contents");
            long unchanged=data.visualRevision();FoundryTankDismantling.placed(w,d);
            check(data.visualRevision()==unchanged,"No-op refresh emits no extra snapshot revision");
            // Each chunk receives the FULL updated layout. Two clients deduplicate and retain it independently.
            var dim=ResourceLocation.withDefaultNamespace("overworld");var clientA=new FoundryDetachedTankCache();var clientB=new FoundryDetachedTankCache();
            for(long chunk:expanded.positions().stream().map(p->ChunkPos.asLong(p.getX()>>4,p.getZ()>>4)).distinct().toList()){
                var payload=wire(new FoundryDetachedPayload(dim,chunk,data.visualRevision(),false,FoundryDetachedSync.inChunk(data,chunk)));
                clientA.apply(payload);clientB.apply(payload);
            }
            check(clientA.views(dim).equals(Map.of(id,expanded))&&clientB.views(dim).equals(clientA.views(dim)),"Actual codec and chunk views update both multiplayer replicas without duplicate inventory");
            var restored=FoundryDetachedVessels.load(data.save(new CompoundTag(),lookup),lookup);w.data.set("katzencraftmetals_foundry_vessels",restored);
            check(restored.snapshots().get(id).equals(expanded),"Save/reload preserves expanded layout and every quantity");
            w.destroyBlock(d,true,null,512);data=FoundryDetachedVessels.get(w);
            check(data.snapshots().get(id).positions().equals(Set.of(a,b,c))&&data.snapshots().get(id).contents().equals(expected),"Expansion then removal retains all metal when surviving capacity suffices");
            // A physically touching independent Tank must not inherit the detached pool.
            var independent=a.south();place(w,independent,Set.of());flush(w);
            check(!data.snapshots().get(id).positions().contains(independent)&&data.snapshots().size()==1,"Logical connectivity, not physical adjacency");
            var replacement=controller(w,a.west());
            check(replacement.getOwnedTankNetwork().getTankPositions().equals(Set.of(a,b,c))&&replacement.getOwnedTankNetwork().getMoltenContents().equals(expected),"Controller claims complete expanded vessel once");
            check(data.snapshots().isEmpty()&&data.claim(Set.of(a,b,c)).isEmpty(),"No detached duplicate remains after reconnection");
        }
        // Several placements before their scheduled ticks coalesce into one final-layout update.
        var w=FoundryDismantlingChecks.world(lookup);var a=BlockPos.ZERO;var b=a.east();var c=b.east();
        FoundryDismantlingChecks.tanks(w,Set.of(a));var owner=controller(w,a.west());owner.getOwnedTankNetwork().insert(iron,100);w.destroyBlock(owner.getBlockPos(),true,null,512);
        var data=FoundryDetachedVessels.get(w);long revision=data.visualRevision();place(w,b,Set.of(a));place(w,c,Set.of(a,b));flush(w);
        check(data.visualRevision()==revision+1&&data.snapshots().values().iterator().next().positions().equals(Set.of(a,b,c)),"Back-to-back additions settle once into final connected layout");
        // Removal before the scheduled placement refresh still captures all newly connected cells.
        var d=c.east();place(w,d,Set.of(a,b,c));w.destroyBlock(c,true,null,512);
        var kept=data.snapshots().values().iterator().next();check(kept.positions().equals(Set.of(a,b))&&kept.contents().equals(Map.of(iron,100))&&w.drops.getOrDefault(d,0)==1,"Pre-removal shared refresh includes pending addition and preserves pooled retention");flush(w);
        // Explicit atomic merge support never copies an escrow or drops its individual metals.
        var merging=new FoundryDetachedVessels();var first=UUID.randomUUID();var second=UUID.randomUUID();
        merging.put(first,new FoundryVesselSnapshot(a,Set.of(a),Map.of(iron,40),a.west()));
        merging.put(second,new FoundryVesselSnapshot(c,Set.of(c),Map.of(gold,70),c.east()));
        check(merging.expand(Set.of(a,b,c))&&merging.snapshots().size()==1&&merging.snapshots().get(first).contents().equals(Map.of(iron,40,gold,70)),"One authoritative merged escrow, exact quantities");
        check(!merging.expand(Set.of(a,b))&&merging.snapshots().get(first).positions().equals(Set.of(a,b,c)),"Expansion cannot silently truncate a partial component");
        System.out.println("PASS: native detached Tank placement/handshake and one-shot refresh; consecutive and pending edits; single/mixed metals; real liquid-height redistribution; stable origin/anchor/UUID; old/new chunk wire views and two replicas; save/reload; removal capacity preservation; one-time Controller reconnection; independent vessels and atomic escrow merges.");
    }
}

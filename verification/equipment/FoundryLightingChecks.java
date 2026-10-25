package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Native state emission and actual authoritative storage/casting lifecycles in a map-world fixture. */
public final class FoundryLightingChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static int light(FoundryDismantlingChecks.World world,BlockPos pos){return world.getBlockState(pos).getLightEmission(world,pos);}
    private static void occupancy(FoundryDismantlingChecks.World world,FoundryVesselSnapshot snapshot){
        var expected=FoundryMoltenLighting.occupied(snapshot);
        for(var p:snapshot.positions())check(light(world,p)==(expected.contains(p)?12:0),"Only occupied vertical/horizontal sections emit 12: "+p);
    }
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var iron=FoundryDismantlingChecks.id("iron");var gold=FoundryDismantlingChecks.id("gold");
        var bottom=BlockPos.ZERO;var upper=bottom.above();
        var positions=Set.of(bottom,bottom.east(),upper,upper.east());
        var w=FoundryDismantlingChecks.world(lookup);FoundryDismantlingChecks.tanks(w,positions);var c=FoundryDismantlingChecks.controller(w);var network=c.getOwnedTankNetwork();
        occupancy(w,c.getTankStorage().snapshot());check(w.lightTransitions==0,"Empty initialization needs no light updates");
        network.insert(iron,1);occupancy(w,c.getTankStorage().snapshot());check(w.lightTransitions==2,"First unit lights entire shared bottom horizontal level only");
        var stateTag=net.minecraft.nbt.NbtUtils.writeBlockState(w.getBlockState(bottom));
        var roundTrip=net.minecraft.nbt.NbtUtils.readBlockState(lookup.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),stateTag);
        check(roundTrip.getValue(FoundryTankBlock.MOLTEN_LIT)&&roundTrip.getLightEmission()==12,"Native chunk block-state serialization persists light bit");
        var packet=new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(w,bottom);
        check(packet.getBlockState().getLightEmission()==12,"Ordinary block update packet carries client light state");
        int changed=w.lightTransitions;network.insert(gold,100);network.insert(iron,115);occupancy(w,c.getTankStorage().snapshot());check(w.lightTransitions==changed,"Different metals and same occupied row do not recalculate lighting");
        network.insert(gold,1);occupancy(w,c.getTankStorage().snapshot());check(w.lightTransitions==changed+2,"Crossing 216 units lights upper row");
        network.extract(gold,1);occupancy(w,c.getTankStorage().snapshot());check(w.lightTransitions==changed+4,"Dropping to 216 extinguishes upper row");
        network.insert(iron,216);occupancy(w,c.getTankStorage().snapshot());
        changed=w.lightTransitions;
        // Exact quantities and density order are untouched by light flags and update packets.
        var before=c.getTankStorage().snapshot();w.destroyBlock(c.getBlockPos(),true,null,512);
        var data=FoundryDetachedVessels.get(w);check(data.snapshots().values().iterator().next().contents().equals(before.contents()),"Controller removal retains contents");
        occupancy(w,before);check(w.lightTransitions==changed,"Controller removal never flickers existing lights");
        var restored=FoundryDetachedVessels.load(data.save(new CompoundTag(),lookup),lookup);w.data.set("katzencraftmetals_foundry_vessels",restored);
        c=FoundryDismantlingChecks.controller(w);occupancy(w,c.getTankStorage().snapshot());check(w.lightTransitions==changed,"Saved detached handoff/reconnection does not toggle lighting");
        // Unloaded cells are not loaded or written; reconciliation after load corrects their saved flag.
        w.unloaded=new HashSet<>(Set.of(upper,upper.east()));c.getOwnedTankNetwork().extract(iron,216);
        check(light(w,upper)==12,"Unloaded section not written");w.unloaded.clear();
        FoundryMoltenLighting.update(w,positions,c.getTankStorage().snapshot());occupancy(w,c.getTankStorage().snapshot());
        w.destroyBlock(c.getBlockPos(),true,null,512);
        var extra=bottom.south();var expanded=new HashSet<>(positions);expanded.add(extra);FoundryDismantlingChecks.tanks(w,expanded);
        FoundryTankDismantling.placed(w,extra);var snapshot=FoundryDetachedVessels.get(w).snapshots().values().iterator().next();occupancy(w,snapshot);
        w.destroyBlock(extra,true,null,512);snapshot=FoundryDetachedVessels.get(w).snapshots().values().iterator().next();occupancy(w,snapshot);
        c=FoundryDismantlingChecks.controller(w);network=c.getOwnedTankNetwork();network.extract(iron,Integer.MAX_VALUE);network.extract(gold,Integer.MAX_VALUE);occupancy(w,c.getTankStorage().snapshot());
        changed=w.lightTransitions;network.extract(iron,1);check(w.lightTransitions==changed,"Empty repeated drains do not update lighting");
        check(ModBlocks.FOUNDRY_CONTROLLER.get().defaultBlockState().getLightEmission()==0&&ModBlocks.FOUNDRY_FAUCET.get().defaultBlockState().getLightEmission()==0,"Controller/Faucet have no added light");
        for(boolean table:new boolean[]{false,true}){
            var pos=new BlockPos(8,0,0);var block=table?ModBlocks.FOUNDRY_TABLE.get():ModBlocks.FOUNDRY_BASIN.get();BlockState state=block.defaultBlockState();w.blocks.put(pos,state);
            FoundryBasinBlockEntity basin=table?new FoundryTableBlockEntity(pos,state):new FoundryBasinBlockEntity(pos,state);basin.setLevel(w);w.entities.put(pos,basin);
            changed=w.lightTransitions;check(light(w,pos)==0,"Empty casting target dark");basin.insert(iron,1);check(light(w,pos)==12&&w.lightTransitions==changed+1,"First molten unit illuminates target");
            basin.insert(iron,basin.getRequiredMoltenAmount()-1);check(w.lightTransitions==changed+1,"Further filling doesn't change light");
            // Actual load/update tag retains cooling state; current persisted cast-state retains emission.
            var loaded=table?new FoundryTableBlockEntity(pos,basin.getBlockState()):new FoundryBasinBlockEntity(pos,basin.getBlockState());loaded.handleUpdateTag(basin.getUpdateTag(lookup),lookup);
            check(loaded.getMoltenAmount()==basin.getMoltenAmount()&&!loaded.isCooled(),"Molten state synchronized/reloaded");
            for(int tick=0;tick<basin.getCoolingDuration();tick++)FoundryBasinBlockEntity.serverTick(w,pos,basin.getBlockState(),basin);
            check(basin.isCooled()&&!basin.getResultCopy().isEmpty()&&light(w,pos)==0&&w.lightTransitions==changed+2,"Cooling completion extinguishes once while output remains");
            FoundryBasinBlockEntity.serverTick(w,pos,basin.getBlockState(),basin);basin.takeResult();check(light(w,pos)==0&&w.lightTransitions==changed+2,"Completion and collection don't cause duplicate light updates");
        }
        System.out.println("PASS: native 0/12 state emission; horizontal/vertical occupancy, multi-metal thresholds, unchanged-occupancy update counts, detach/save/reconnect, unloaded-position reconciliation, detached expansion/removal, drain, Basin/Table cooling/sync/completion. Real light propagation/chunk packets require Minecraft verification.");
    }
}

package net.chriskatze.katzencraftmetals.block.entity;

import com.google.gson.*;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.chriskatze.katzencraftmetals.config.BlockRequirements;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.*;
import java.nio.file.*;

/** Actual removal callbacks and storage adapters in an isolated map world, plus exact conservation cases. */
public final class FoundryDismantlingChecks {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static ResourceLocation id(String p){return ResourceLocation.withDefaultNamespace(p);}
    static final class Controller extends FoundryControllerBlockEntity {
        int updates;
        Controller(BlockPos p){super(p,ModBlocks.FOUNDRY_CONTROLLER.get().defaultBlockState());}
        @Override public void setChanged(){}
        @Override void syncToClient(){updates++;}
    }
    static final class World extends ServerLevel {
        Map<BlockPos,BlockState> blocks;Map<BlockPos,BlockEntity> entities;Map<BlockPos,Integer> drops;DimensionDataStorage data;
        private World(){super(null,null,null,null,null,null,null,false,0,List.of(),false,null);}
        @Override public long getGameTime(){return 100;}
        @Override public BlockState getBlockState(BlockPos p){return blocks.getOrDefault(p,Blocks.AIR.defaultBlockState());}
        @Override public BlockEntity getBlockEntity(BlockPos p){return entities.get(p);}
        @Override public void removeBlockEntity(BlockPos p){entities.remove(p);}
        @Override public DimensionDataStorage getDataStorage(){return data;}
        @Override public java.util.List<net.minecraft.server.level.ServerPlayer> players(){return List.of();}
        @Override public boolean hasChunkAt(BlockPos p){return true;}
        @Override public void blockEntityChanged(BlockPos p){}
        @Override public void sendBlockUpdated(BlockPos p,BlockState a,BlockState b,int flags){}
        @Override public void updateNeighborsAt(BlockPos p,Block block){}
        @Override public boolean destroyBlock(BlockPos p,boolean drop,Entity cause,int depth){
            var old=getBlockState(p);if(old.isAir())return false;
            if(drop&&old.is(ModBlocks.FOUNDRY_TANK.get()))drops.merge(p.immutable(),1,Integer::sum);
            blocks.remove(p);old.onRemove(this,p,Blocks.AIR.defaultBlockState(),false);entities.remove(p);return true;
        }
    }
    static World world(HolderLookup.Provider lookup) throws Exception {
        var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);
        var w=(World)u.allocateInstance(World.class);
        var random=net.minecraft.world.level.Level.class.getDeclaredField("random");random.setAccessible(true);random.set(w,net.minecraft.util.RandomSource.create(7));
        w.blocks=new HashMap<>();w.entities=new HashMap<>();w.drops=new HashMap<>();
        var temp=Files.createTempDirectory(Path.of(System.getProperty("katzencraft.equipment.root"),"build"),"foundry-dismantling-");
        w.data=new DimensionDataStorage(temp.toFile(),null,lookup);return w;
    }
    static void tanks(World w,Set<BlockPos> positions){for(var p:positions){var state=ModBlocks.FOUNDRY_TANK.get().defaultBlockState();for(var d:Direction.values())state=state.setValue(FoundryTankBlock.connectionProperty(d),positions.contains(p.relative(d)));w.blocks.put(p,state);}}
    static Controller controller(World w){var c=new Controller(new BlockPos(-1,0,0));c.setLevel(w);w.blocks.put(c.getBlockPos(),c.getBlockState());w.entities.put(c.getBlockPos(),c);check(c.ensureTankNetwork(),"Controller attaches actual structure");return c;}
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var iron=id("iron");var gold=id("gold");
        var p0=BlockPos.ZERO;var p1=p0.east();var p2=p1.east();var line=Set.of(p0,p1,p2);
        for(int fill:List.of(0,1,53,108,109,215,216,323,324)) {
            var w=world(lookup);tanks(w,line);var c=controller(w);var network=c.getOwnedTankNetwork();
            if(fill>0)check(network.insert(iron,fill)==fill,"Insert initial metal");
            var before=c.getTankStorage().snapshot();var expected=before.retain(Set.of(p0));
            w.destroyBlock(p1,true,null,512);
            check(w.getBlockState(p0).is(ModBlocks.FOUNDRY_TANK.get())&&w.getBlockState(p2).isAir(),"Controller component survives; disconnected side removed");
            check(w.drops.equals(Map.of(p1,1,p2,1)),"Exactly one primary and one automatic drop");
            check(c.getOwnedTankNetwork().getCapacity()==108&&c.getTankStorage().snapshot().contents().equals(expected.contents()),"Pre-edit survivor metal retained exactly");
            check(c.updates>0,"Structure/storage synchronization requested");
            var saved=c.getUpdateTag(lookup);var client=new Controller(c.getBlockPos());client.handleUpdateTag(saved,lookup);
            check(client.getTankStorage().snapshot().contents().equals(expected.contents()),"Survivor client update tag matches authoritative amounts");
        }
        // Removing an empty upper cell must not trim metal from the lower tank.
        var lower=new BlockPos(0,0,0);var upper=lower.above();var vertical=Set.of(lower,upper);
        var w=world(lookup);tanks(w,vertical);var c=controller(w);c.getOwnedTankNetwork().insert(iron,108);w.destroyBlock(upper,true,null,512);
        check(c.getOwnedTankNetwork().getMoltenAmount(iron)==108&&w.drops.equals(Map.of(upper,1)),"Empty upper tank removal preserves full lower tank");
        // Controller removal, detached save/reload and one-time replacement claim.
        w=world(lookup);tanks(w,line);c=controller(w);c.getOwnedTankNetwork().insert(iron,200);c.getOwnedTankNetwork().insert(gold,47);
        var original=c.getTankStorage().snapshot();w.destroyBlock(c.getBlockPos(),true,null,512);
        for(var p:line)check(w.getBlockState(p).is(ModBlocks.FOUNDRY_TANK.get()),"Controller removal leaves tanks intact");
        check(c.getTankStorage().snapshot().contents().isEmpty()&&FoundryDetachedVessels.get(w).snapshots().size()==1,"Contents transferred once, old Controller emptied");
        var tag=FoundryDetachedVessels.get(w).save(new CompoundTag(),lookup);
        var restored=FoundryDetachedVessels.load(tag,lookup);w.data.set("katzencraftmetals_foundry_vessels",restored);
        var replacement=controller(w);check(replacement.getTankStorage().snapshot().contents().equals(original.contents()),"Replacement recovers all saved molten amounts");
        check(restored.snapshots().isEmpty()&&restored.claim(line).isEmpty(),"Escrow cannot be claimed twice");
        // No Controller: stable saved anchor wins, including after world-data reload.
        w.destroyBlock(replacement.getBlockPos(),true,null,512);w.destroyBlock(p1,true,null,512);
        var detached=FoundryDetachedVessels.get(w).snapshots().values().stream().findFirst().orElseThrow();
        check(detached.positions().equals(Set.of(p0))&&detached.contents().equals(original.retain(Set.of(p0)).contents()),"Orphan split preserves anchored section's units");
        check(w.drops.equals(Map.of(p1,1,p2,1)),"Orphan recursive removal drops once");
        w.destroyBlock(p0,true,null,512);check(FoundryDetachedVessels.get(w).snapshots().isEmpty(),"Last tank removal discards only its molten units");
        // Failed/partial claims cannot consume the saved snapshot, and overlapping
        // detach records are rejected before any transfer occurs.
        var escrow=new FoundryDetachedVessels();var escrowId=UUID.randomUUID();escrow.put(escrowId,original);
        try{escrow.claim(Set.of(p0));throw new AssertionError("Partial escrow claimed");}catch(IllegalStateException expected){}
        check(escrow.snapshots().size()==1,"Failed partial claim retained all metal");
        try{escrow.put(UUID.randomUUID(),original);throw new AssertionError("Overlapping escrow accepted");}catch(IllegalStateException expected){}
        check(escrow.snapshots().size()==1,"Failed overlapping detach does not duplicate");
        // Rapid successive removals always use the current pre-edit distribution.
        var rapidWorld=world(lookup);var square=Set.of(p0,p1,p0.south(),p1.south());tanks(rapidWorld,square);var rapid=controller(rapidWorld);
        rapid.getOwnedTankNetwork().insert(iron,103);rapid.getOwnedTankNetwork().insert(gold,137);
        var rapidBefore=rapid.getTankStorage().snapshot();rapidWorld.destroyBlock(p1.south(),true,null,512);
        var firstSurvivors=new HashSet<>(square);firstSurvivors.remove(p1.south());
        check(rapid.getTankStorage().snapshot().contents().equals(rapidBefore.retain(firstSurvivors).contents()),"First rapid removal");
        var secondBefore=rapid.getTankStorage().snapshot();rapidWorld.destroyBlock(p1,true,null,512);
        check(rapid.getTankStorage().snapshot().contents().equals(secondBefore.retain(Set.of(p0,p0.south())).contents()),"Second rapid removal uses committed snapshot");
        // Connected but invalid after removing the low bridge: preserve one valid seeded subset.
        var arch=Set.of(new BlockPos(0,0,0),new BlockPos(1,0,0),new BlockPos(2,0,0),new BlockPos(0,1,0),new BlockPos(1,1,0),new BlockPos(2,1,0));
        var available=new HashSet<>(arch);available.remove(new BlockPos(1,0,0));
        var survivor=FoundryVesselSnapshot.survivors(available,List.of(p0),p0,(a,b)->true);
        check(FoundryTankStructureValidation.validate(survivor).valid()&&survivor.contains(p0)&&survivor.size()<available.size(),"Connected invalid simple-fluid shape repaired");
        // All removed/surviving unit assignments exactly partition every metal, including tiny layers.
        var grid=new HashSet<BlockPos>();for(int y=0;y<2;y++)for(int x=0;x<3;x++)grid.add(new BlockPos(x,y,0));
        for(int amount=1;amount<=648;amount++) {
            var snapshot=new FoundryVesselSnapshot(p0,grid,Map.of(iron,amount));
            int kept=snapshot.retain(Set.of(p0,p0.above())).contents().getOrDefault(iron,0);
            var removed=new HashSet<>(grid);removed.removeAll(Set.of(p0,p0.above()));
            check(kept+snapshot.retain(removed).contents().getOrDefault(iron,0)==amount,"Unit conservation at every fill threshold");
        }
        var mix=new FoundryVesselSnapshot(p0,grid,Map.of(iron,53,gold,109));
        check(mix.retain(grid).contents().equals(mix.contents()),"Density-layer material preservation");
        var tank=ModBlocks.FOUNDRY_TANK.get().defaultBlockState();float progress=0;
        for(int tick=1;tick<=160;tick++){progress+=tank.getDestroyProgress(null,net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p0);check((progress>=1)==(tick==160),"Exactly 160 client progress increments");}
        check(!tank.requiresCorrectToolForDrops()&&BlockRequirements.getRequirement(ModBlocks.FOUNDRY_CONTROLLER.get())==3,"Tank hand drops and Controller level 3 gate");
        System.out.println("PASS: native tank/Controller removal callbacks; nine fill levels; exact one-time auto drops; preserved lower/full/partial/empty tanks; detached SavedData reload + replacement claim; orphan splits; invalid shape repair; 648 conservation thresholds; client update tags; 160 mining increments and level-3 Controller.");
    }
}

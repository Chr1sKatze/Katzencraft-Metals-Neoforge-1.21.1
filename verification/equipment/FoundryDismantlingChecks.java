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
        Map<BlockPos,BlockState> blocks;Map<BlockPos,BlockEntity> entities;Map<BlockPos,Integer> drops;Map<BlockPos,Block> scheduled;DimensionDataStorage data;long time;int lightTransitions;Set<BlockPos> unloaded;
        private World(){super(null,null,null,null,null,null,null,false,0,List.of(),false,null);}
        int waterTicks;int lastBlockDelay;
        @Override public int getMinBuildHeight(){return -64;}
        @Override public int getHeight(){return 384;}
        @Override public void levelEvent(net.minecraft.world.entity.player.Player player,int event,BlockPos pos,int data){}
        @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        @Override public void scheduleTick(BlockPos pos,net.minecraft.world.level.material.Fluid fluid,int delay){if(fluid==net.minecraft.world.level.material.Fluids.WATER){check(delay==5,"Native water tick delay");waterTicks++;}}
        @Override public void playSound(net.minecraft.world.entity.player.Player player,BlockPos pos,net.minecraft.sounds.SoundEvent sound,net.minecraft.sounds.SoundSource category,float volume,float pitch){}
        @Override public void gameEvent(net.minecraft.world.entity.Entity entity,net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> event,BlockPos pos){}
        @Override public long getGameTime(){return time;}
        @Override public void scheduleTick(BlockPos pos,Block block,int delay){scheduled.put(pos.immutable(),block);lastBlockDelay=delay;}
        @Override public BlockState getBlockState(BlockPos p){return blocks.getOrDefault(p,Blocks.AIR.defaultBlockState());}
        @Override public BlockEntity getBlockEntity(BlockPos p){return entities.get(p);}
        @Override public void removeBlockEntity(BlockPos p){entities.remove(p);}
        @Override public DimensionDataStorage getDataStorage(){return data;}
        @Override public java.util.List<net.minecraft.server.level.ServerPlayer> players(){return List.of();}
        @Override public boolean hasChunkAt(BlockPos p){return unloaded==null||!unloaded.contains(p);}
        @Override public boolean setBlock(BlockPos p,BlockState state,int flags,int depth){
            var old=getBlockState(p);
            if(old==state)return false;
            if(old.getLightEmission()!=state.getLightEmission())lightTransitions++;
            blocks.put(p.immutable(),state);var entity=entities.get(p);if(entity!=null)entity.setBlockState(state);
            if(old.getBlock()!=state.getBlock())old.onRemove(this,p,state,false);
            return true;
        }
        @Override public void blockEntityChanged(BlockPos p){}
        @Override public void blockEvent(BlockPos p,Block block,int id,int value){}
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
        w.blocks=new HashMap<>();w.entities=new HashMap<>();w.drops=new HashMap<>();w.scheduled=new HashMap<>();w.time=100;
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
            check(c.getOwnedTankNetwork().getTotalMoltenAmount()==Math.min(fill,FoundryTankNetwork.TANK_CAPACITY),"Only final capacity overflow lost, including disconnected automatic removals");
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
        check(detached.positions().equals(Set.of(p0))&&detached.contents().equals(original.retain(Set.of(p0)).contents()),"Orphan split preserves pooled inventory up to final capacity");
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
        // Pool retention depends only on final capacity, never on old per-cell shares.
        var grid=new HashSet<BlockPos>();for(int y=0;y<2;y++)for(int x=0;x<3;x++)grid.add(new BlockPos(x,y,0));
        for(int amount=1;amount<=648;amount++) {
            var snapshot=new FoundryVesselSnapshot(p0,grid,Map.of(iron,amount));
            int kept=snapshot.retain(Set.of(p0,p0.above())).contents().getOrDefault(iron,0);
            check(kept==Math.min(amount,2*FoundryTankNetwork.TANK_CAPACITY),"Retain every unit that fits at every fill threshold");
        }
        var mix=new FoundryVesselSnapshot(p0,grid,Map.of(iron,53,gold,109));
        check(mix.retain(grid).contents().equals(mix.contents()),"Density-layer material preservation");
        capacityRetention(lookup);
        var tank=ModBlocks.FOUNDRY_TANK.get().defaultBlockState();float progress=0;
        for(int tick=1;tick<=160;tick++){progress+=tank.getDestroyProgress(null,net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p0);check((progress>=1)==(tick==160),"Exactly 160 client progress increments");}
        check(!tank.requiresCorrectToolForDrops()&&BlockRequirements.getRequirement(ModBlocks.FOUNDRY_CONTROLLER.get())==3,"Tank hand drops and Controller level 3 gate");
        System.out.println("PASS: native tank/Controller removal callbacks; nine fill levels; exact one-time auto drops; preserved lower/full/partial/empty tanks; detached SavedData reload + replacement claim; orphan splits; invalid shape repair; 648 capacity-retention thresholds; client update tags; 160 mining increments and level-3 Controller.");
    }
    private static void capacityRetention(HolderLookup.Provider lookup) throws Exception {
        var iron=id("iron");var gold=id("gold");var p0=BlockPos.ZERO;var p1=p0.east();var p2=p1.east();
        // Native removal callbacks with hard-coded expected quantities, not expectations
        // obtained by calling the retention method being tested.
        for(boolean detached:List.of(false,true))for(boolean split:List.of(false,true)){
            for(int fill:List.of(0,1,40,100,108,109,200,216,217,324)){
                var w=world(lookup);tanks(w,Set.of(p0,p1,p2));var c=controller(w);c.getOwnedTankNetwork().insert(iron,fill);
                if(detached)w.destroyBlock(c.getBlockPos(),true,null,512);
                w.destroyBlock(split?p1:p2,true,null,512);
                int capacity=(split?1:2)*FoundryTankNetwork.TANK_CAPACITY;
                int expected=Math.min(fill,capacity);
                Map<net.minecraft.resources.ResourceLocation,Integer> amounts;
                if(detached){
                    var data=FoundryDetachedVessels.get(w);
                    var saved=data.save(new net.minecraft.nbt.CompoundTag(),lookup);
                    var reloaded=FoundryDetachedVessels.load(saved,lookup);w.data.set("katzencraftmetals_foundry_vessels",reloaded);
                    amounts=reloaded.snapshots().values().iterator().next().contents();
                    var replacement=controller(w);
                    check(replacement.getOwnedTankNetwork().getTotalMoltenAmount()==expected,"Detached save/reload/claim retains capacity-limited total");
                    check(reloaded.snapshots().isEmpty(),"Controller claim consumes detached record once");
                    c=replacement;
                }else amounts=c.getOwnedTankNetwork().getMoltenContents();
                check(amounts.getOrDefault(iron,0)==expected,"Partial/full/overflow and disconnected final-capacity retention");
                check(w.drops.getOrDefault(split?p2:p1,0)==(split?1:0),"Automatic dismantling rules unchanged");
                // Repeated removal trims against the committed pool, without double charging.
                if(!split){w.destroyBlock(p1,true,null,512);check(c.getOwnedTankNetwork().getTotalMoltenAmount()==Math.min(fill,108),"Repeated removal only loses final overflow");}
            }
        }
        for(boolean detached:List.of(false,true)){
            var w=world(lookup);tanks(w,Set.of(p0,p1));var c=controller(w);c.getOwnedTankNetwork().insert(iron,40);
            if(detached)w.destroyBlock(c.getBlockPos(),true,null,512);
            w.destroyBlock(p1,true,null,512);
            Map<net.minecraft.resources.ResourceLocation,Integer> amounts=detached?FoundryDetachedVessels.get(w).snapshots().values().iterator().next().contents():c.getOwnedTankNetwork().getMoltenContents();
            check(amounts.equals(Map.of(iron,40)),"Two Tanks / 40 units retain all 40 after one removal");
        }
        for(boolean detached:List.of(false,true))for(boolean overflow:List.of(false,true)){
            var w=world(lookup);tanks(w,Set.of(p0,p1));var c=controller(w);
            int ironAmount=overflow?100:40,goldAmount=overflow?50:30;
            c.getOwnedTankNetwork().insert(iron,ironAmount);c.getOwnedTankNetwork().insert(gold,goldAmount);
            if(detached)w.destroyBlock(c.getBlockPos(),true,null,512);
            w.destroyBlock(p1,true,null,512);
            var amounts=detached?FoundryDetachedVessels.get(w).snapshots().values().iterator().next().contents():c.getOwnedTankNetwork().getMoltenContents();
            check(amounts.equals(Map.of(gold,goldAmount,iron,overflow?58:40)),"Mixed metals preserve all quantities when fitting; overflow removes lighter iron before gold");
            if(!detached){
                var replica=new Controller(c.getBlockPos());replica.handleUpdateTag(c.getUpdateTag(lookup),lookup);
                check(replica.getTankStorage().snapshot().contents().equals(amounts),"Existing client update data carries corrected mixed quantities");
            }else{
                var packet=net.chriskatze.katzencraftmetals.network.FoundryDetachedSync.inChunk(FoundryDetachedVessels.get(w),0L);
                check(packet.values().iterator().next().contents().equals(amounts),"Detached visual/audio replica carries corrected mixed quantities");
            }
        }
        // Removing the floor of an arch triggers the existing valid-subset repair.
        var arch=Set.of(p0,p1,p2,p0.above(),p1.above(),p2.above());
        for(boolean detached:List.of(false,true)){
            var w=world(lookup);tanks(w,arch);var c=controller(w);c.getOwnedTankNetwork().insert(iron,100);c.getOwnedTankNetwork().insert(gold,40);
            if(detached)w.destroyBlock(c.getBlockPos(),true,null,512);
            w.destroyBlock(p1,true,null,512);
            var retained=detached?FoundryDetachedVessels.get(w).snapshots().values().iterator().next():c.getTankStorage().snapshot();
            check(FoundryTankStructureValidation.validate(retained.positions()).valid(),"Final repaired arch remains valid");
            check(retained.contents().values().stream().mapToInt(Integer::intValue).sum()==Math.min(140,retained.positions().size()*108),"Only final valid subset capacity determines loss");
        }
        System.out.println("PASS: pooled molten retention across attached/detached removals, exact 40-unit case, 40 native fill/split/overflow cases, mixed-metal density policy, recursive dismantling, repeated removal, saved escrow reload/one-time claim, and existing active/detached client update paths.");
    }

}

package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/** Native-state equivalence/read-count checks; not a server MSPT benchmark. */
public final class FoundryAutomationCacheChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static final class World extends ServerLevel {
        Map<BlockPos,BlockState> blocks; Set<BlockPos> unloaded; long reads;
        private World(){super(null,null,null,null,null,null,null,false,0,List.of(),false,null);}
        @Override public BlockState getBlockState(BlockPos pos){reads++;return blocks.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
        @Override public boolean hasChunkAt(BlockPos pos){return !unloaded.contains(pos);}
    }
    private static World world() throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var w=(World)((sun.misc.Unsafe)field.get(null)).allocateInstance(World.class);
        w.blocks=new HashMap<>();w.unloaded=new HashSet<>();return w;
    }
    private static BlockState lever(AttachFace face,Direction facing,boolean powered){
        return Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,face)
                .setValue(BlockStateProperties.HORIZONTAL_FACING,facing).setValue(BlockStateProperties.POWERED,powered);
    }
    // Previous production scan, retained only as an independent regression oracle.
    private static boolean reference(World w, Set<BlockPos> tanks) {
        var anchors=new HashSet<BlockPos>();var origins=new ArrayList<>(tanks);
        origins.addAll(FoundryTankNetwork.findAttachedFaucets(w,tanks));
        for(var origin:origins){
            anchors.add(origin);
            for(var direction:Direction.values()){
                var support=origin.relative(direction);anchors.add(support);var state=w.getBlockState(support);
                if(!state.isAir()&&state.getFluidState().isEmpty())for(var sturdy:Direction.values())if(state.isFaceSturdy(w,support,sturdy)){
                    for(var extension:Direction.values())anchors.add(support.relative(extension));break;
                }
            }
        }
        for(var anchor:anchors)for(var direction:Direction.values()){
            var pos=anchor.relative(direction);var state=w.getBlockState(pos);
            if(!(state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock)||!state.getValue(BlockStateProperties.POWERED))continue;
            var attachment=switch(state.getValue(BlockStateProperties.ATTACH_FACE)){
                case FLOOR->Direction.DOWN;case CEILING->Direction.UP;
                case WALL->state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();};
            if(pos.relative(attachment).equals(anchor))return true;
        }
        return false;
    }
    public static void run(net.minecraft.core.HolderLookup.Provider lookup) throws Exception {
        var w=world();var tanks=new HashSet<BlockPos>();
        for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)tanks.add(new BlockPos(x,y,z));
        for(var p:tanks)w.blocks.put(p,ModBlocks.FOUNDRY_TANK.get().defaultBlockState());
        // Solid platform is the expensive ordinary control-rim case from the old scan.
        for(int x=-2;x<=5;x++)for(int z=-2;z<=5;z++)w.blocks.put(new BlockPos(x,-1,z),Blocks.STONE.defaultBlockState());
        var search=new FoundryTankLeverAutoPourControl.LeverSearch(tanks);
        w.reads=0;check(!reference(w,tanks),"Unpowered reference");long oldReads=w.reads;
        w.reads=0;check(!search.enabled(w),"Unpowered cached search");long newReads=w.reads;
        check(newReads<oldReads,"Deduplicate live block probes in the no-powered-lever case: "+oldReads+" -> "+newReads);
        int cases=0;
        // Exhaustively relocate a single powered/unpowered lever through and beyond the bounded rim.
        for(int x=-4;x<=7;x++)for(int y=-3;y<=6;y++)for(int z=-4;z<=7;z+=2)
            for(var face:AttachFace.values())for(var facing:Direction.Plane.HORIZONTAL){
                var p=new BlockPos(x,y,z);var previous=w.blocks.put(p,lever(face,facing,true));
                check(reference(w,tanks)==search.enabled(w),"Lever face/anchor mismatch at "+p+" "+face+" "+facing);
                w.blocks.put(p,lever(face,facing,false));check(!search.enabled(w),"Lever off is immediate without cache reset");
                if(previous==null)w.blocks.remove(p);else w.blocks.put(p,previous);cases++;
            }
        // Same cached search sees Faucet rotation/removal and changing solid supports immediately.
        var faucetPos=new BlockPos(4,0,0);var support=faucetPos.east();var farLever=support.east().above();
        w.blocks.put(farLever,lever(AttachFace.FLOOR,Direction.NORTH,true));
        for(boolean attached:new boolean[]{true,false})for(boolean sturdy:new boolean[]{true,false}){
            w.blocks.put(faucetPos,attached?ModBlocks.FOUNDRY_FAUCET.get().defaultBlockState().setValue(FoundryFaucetBlock.FACING,Direction.EAST):Blocks.AIR.defaultBlockState());
            w.blocks.put(support,sturdy?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState());
            check(reference(w,tanks)==search.enabled(w),"Live attachment/support path");
        }
        w.blocks.put(faucetPos,ModBlocks.FOUNDRY_FAUCET.get().defaultBlockState().setValue(FoundryFaucetBlock.FACING,Direction.EAST));
        w.blocks.put(support,Blocks.STONE.defaultBlockState());
        check(search.enabled(w),"Valid loaded remote support lever contributes");
        w.unloaded.add(farLever);check(!search.enabled(w),"No lever in an unloaded position is queried");
        var nativeWorld=FoundryDismantlingChecks.world(lookup);
        var line=Set.of(BlockPos.ZERO,BlockPos.ZERO.east());
        FoundryDismantlingChecks.tanks(nativeWorld,line);
        var controller=FoundryDismantlingChecks.controller(nativeWorld);
        var network=controller.getOwnedTankNetwork();
        nativeWorld.blocks.put(BlockPos.ZERO.above(),lever(AttachFace.FLOOR,Direction.NORTH,true));
        check(FoundryTankLeverAutoPourControl.isAutoPourEnabled(nativeWorld,network),"Production Controller cache sees lever on");
        var field=FoundryTankLeverAutoPourControl.class.getDeclaredField("LEVER_STATE_CACHE");field.setAccessible(true);
        var state=((Map<?,?>)field.get(null)).get(controller);
        nativeWorld.blocks.put(BlockPos.ZERO.above(),lever(AttachFace.FLOOR,Direction.NORTH,false));nativeWorld.time++;
        check(!FoundryTankLeverAutoPourControl.isAutoPourEnabled(nativeWorld,network),"Next server tick sees off without throttling");
        check(((Map<?,?>)field.get(null)).get(controller)==state,"Unchanged structure reuses search across ticks");
        FoundryDismantlingChecks.tanks(nativeWorld,Set.of(BlockPos.ZERO,BlockPos.ZERO.east(),BlockPos.ZERO.east(2)));
        controller.markTankStructureDirty();
        FoundryTankLeverAutoPourControl.isAutoPourEnabled(nativeWorld,controller.getOwnedTankNetwork());
        check(((Map<?,?>)field.get(null)).get(controller)!=state,"Structure revision invalidates even in same tick");
        var otherWorld=FoundryDismantlingChecks.world(lookup);FoundryDismantlingChecks.tanks(otherWorld,line);
        var other=FoundryDismantlingChecks.controller(otherWorld);
        check(!FoundryTankLeverAutoPourControl.isAutoPourEnabled(otherWorld,other.getOwnedTankNetwork()),"Independent/reloaded world cannot inherit lever state");
        System.out.println("PASS: "+cases+" native lever attachment/power oracle cases; live Faucet/support edits and unloaded probes; stable 64-Tank idle scan reads "+oldReads+" -> "+newReads+". Counts, not MSPT/FPS measurements.");
    }
}

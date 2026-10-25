package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.*;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.AudioFormat;

/** Real synchronized state predicates, loop lifecycle and PCM decoding; no audio-device claims. */
public final class FoundryPouringSoundChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var factory=FoundryDismantlingChecks.class.getDeclaredMethod("world",HolderLookup.Provider.class);factory.setAccessible(true);
        Level world=(Level)factory.invoke(null,lookup);
        var blocksField=world.getClass().getDeclaredField("blocks");blocksField.setAccessible(true);
        var entitiesField=world.getClass().getDeclaredField("entities");entitiesField.setAccessible(true);
        var blocks=(Map<BlockPos,BlockState>)blocksField.get(world);var entities=(Map<BlockPos,BlockEntity>)entitiesField.get(world);
        var time=world.getClass().getDeclaredField("time");time.setAccessible(true);
        var animation=Class.forName("net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetStreamAnimation");
        var states=animation.getDeclaredMethod("visualStates");states.setAccessible(true);
        var visualStates=(Map)states.invoke(null);
        var advance=animation.getDeclaredMethod("updateAndGetAnimationProgress",FoundryFaucetBlockEntity.class,float.class,Map.class);advance.setAccessible(true);
        for(Direction facing:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.DOWN))for(boolean table:List.of(false,true)){
            BlockPos pos=new BlockPos(0,5,0),tank=pos.relative(facing.getOpposite()),receiver=pos.below(2);
            blocks.clear();entities.clear();time.setLong(world,100);net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetStreamAnimation.clearSoundStates();
            var state=ModBlocks.FOUNDRY_FAUCET.get().defaultBlockState().setValue(FoundryFaucetBlock.FACING,facing);
            var faucet=new FoundryFaucetBlockEntity(pos,state);faucet.setLevel(world);blocks.put(pos,state);entities.put(pos,faucet);
            blocks.put(tank,ModBlocks.FOUNDRY_TANK.get().defaultBlockState());
            FoundryBasinBlockEntity basin=table?new FoundryTableBlockEntity(receiver,ModBlocks.FOUNDRY_TABLE.get().defaultBlockState()):new FoundryBasinBlockEntity(receiver,ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
            blocks.put(receiver,basin.getBlockState());entities.put(receiver,basin);
            check(!FoundryFaucetSoundManager.flowing(world,faucet),"Idle is silent");
            var tag=new CompoundTag();tag.putBoolean("Pouring",true);tag.putString("PouringMetal","minecraft:iron");tag.putInt("StreamAnimationStep",1);
            faucet.handleUpdateTag(tag,lookup);
            check(FoundryFaucetSoundManager.flowing(world,faucet),"Synced active pour starts both orientations/targets");
            advance.invoke(null,faucet,.5f,visualStates);
            time.setLong(world,116);advance.invoke(null,faucet,.5f,visualStates);
            check(FoundryFaucetSoundManager.flowing(world,faucet),"Full stream remains audible");
            var replica=new FoundryFaucetBlockEntity(pos,state);replica.setLevel(world);replica.handleUpdateTag(faucet.getUpdateTag(lookup),lookup);
            entities.put(pos,replica);check(FoundryFaucetSoundManager.flowing(world,replica)&&!FoundryFaucetSoundManager.flowing(world,faucet),"Independent client replica and stale BE identity");entities.put(pos,faucet);
            tag.putBoolean("Pouring",false);faucet.handleUpdateTag(tag,lookup);
            time.setLong(world,117);advance.invoke(null,faucet,.5f,visualStates);
            check(FoundryFaucetSoundManager.flowing(world,faucet),"Residual stream remains audible after active pour stops");
            Object visual=visualStates.get(faucet);var lastTime=visual.getClass().getDeclaredField("lastRenderTime");lastTime.setAccessible(true);
            double visualTime=lastTime.getDouble(visual);
            for(int tick=118;tick<=131;tick++){time.setLong(world,tick);check(FoundryFaucetSoundManager.flowing(world,faucet),"Offscreen/unsampled residual follows existing animation duration");}
            check(lastTime.getDouble(visual)==visualTime,"Sound sampling must not mutate the visual animation");
            time.setLong(world,132);advance.invoke(null,faucet,.5f,visualStates);
            check(!FoundryFaucetSoundManager.flowing(world,faucet),"Silent when actual shutdown animation reaches terminal progress");
            tag.putBoolean("Pouring",true);faucet.handleUpdateTag(tag,lookup);check(FoundryFaucetSoundManager.flowing(world,faucet),"Restart");
            // Early stops use the same full drain timeline, rather than server-step countdown length.
            for(int startup:new int[]{1,4,8}){
                visualStates.remove(faucet);net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetStreamAnimation.clearSoundStates();
                time.setLong(world,200);tag.putBoolean("Pouring",true);faucet.handleUpdateTag(tag,lookup);advance.invoke(null,faucet,.5f,visualStates);
                time.setLong(world,200+startup);advance.invoke(null,faucet,.5f,visualStates);
                tag.putBoolean("Pouring",false);faucet.handleUpdateTag(tag,lookup);time.setLong(world,201+startup);advance.invoke(null,faucet,.5f,visualStates);
                var residualLoop=new FoundryFaucetLoopSoundInstance(pos,()->FoundryFaucetSoundManager.flowing(world,faucet));
                for(int tick=202+startup;tick<=215+startup;tick++){time.setLong(world,tick);residualLoop.tick();check(!residualLoop.isStopped(),"No fade while early-stop residual remains");}
                time.setLong(world,216+startup);advance.invoke(null,faucet,.5f,visualStates);residualLoop.tick();check(!residualLoop.isStopped(),"Fade starts only after stream disappearance");
                // Reopening within the fade retains this exact loop instance.
                tag.putBoolean("Pouring",true);faucet.handleUpdateTag(tag,lookup);advance.invoke(null,faucet,.75f,visualStates);residualLoop.tick();check(!residualLoop.isStopped(),"Consecutive pour reuses fading loop");residualLoop.cancel();
            }
            blocks.put(pos.below(),Blocks.STONE.defaultBlockState());check(!FoundryFaucetSoundManager.flowing(world,faucet),"Blocked stream stops");blocks.remove(pos.below());
            entities.remove(receiver);check(!FoundryFaucetSoundManager.flowing(world,faucet),"Missing target stops");entities.put(receiver,basin);
            blocks.remove(tank);check(!FoundryFaucetSoundManager.flowing(world,faucet),"Missing tank stops");blocks.put(tank,ModBlocks.FOUNDRY_TANK.get().defaultBlockState());
            basin.insert(net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron"),basin.getRequiredMoltenAmount());
            check(FoundryFaucetSoundManager.flowing(world,faucet),"A full cast must not suppress an existing visible stream");
            var unloaded=world.getClass().getDeclaredField("unloaded");unloaded.setAccessible(true);
            unloaded.set(world,new HashSet<>(Set.of(pos)));check(!FoundryFaucetSoundManager.flowing(world,faucet),"Unloaded Faucet is silent");unloaded.set(world,new HashSet<>());
            entities.remove(pos);check(!FoundryFaucetSoundManager.flowing(world,faucet),"Destroyed Faucet is silent");
        }
        boolean[] active={true};var first=new FoundryFaucetLoopSoundInstance(BlockPos.ZERO,()->active[0]);
        var second=new FoundryFaucetLoopSoundInstance(BlockPos.ZERO.east(),()->true);
        var soundField=net.minecraft.client.resources.sounds.AbstractSoundInstance.class.getDeclaredField("sound");soundField.setAccessible(true);
        var unit=net.minecraft.util.valueproviders.ConstantFloat.of(1);
        var resolved=new net.minecraft.client.resources.sounds.Sound(net.minecraft.resources.ResourceLocation.parse("katzencraftmetals:foundry_pouring_loop"),unit,unit,1,net.minecraft.client.resources.sounds.Sound.Type.FILE,true,false,16);
        soundField.set(first,resolved);soundField.set(second,resolved);
        check(first.isLooping()&&first.getPitch()==1&&first.getDelay()==0&&!first.isRelative(),"Native loop, exact speed, positional playback");
        for(int i=0;i<100;i++)first.tick();check(!first.isStopped()&&first.getVolume()==.85f,"Stable loop does not restart");
        active[0]=false;first.tick();active[0]=true;first.tick();check(!first.isStopped()&&first.getVolume()==.85f,"Rapid reopen reuses loop");
        active[0]=false;for(int i=0;i<2;i++)first.tick();check(first.isStopped()&&!second.isStopped(),"Two-tick fade, independent simultaneous Faucets");second.cancel();check(second.isStopped(),"World/chunk cleanup cancels loop");
        var source=new AudioStream(){boolean closed;public AudioFormat getFormat(){return new AudioFormat(48000,16,2,true,false);}public ByteBuffer read(int n){return ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN).putShort((short)1000).putShort((short)3000).putShort((short)-32768).putShort((short)32767).flip();}public void close(){closed=true;}};
        try(var mono=new FoundryFaucetLoopSoundInstance.MonoStream(source)){
            var pcm=mono.read(4);check(mono.getFormat().getChannels()==1&&mono.getFormat().getSampleRate()==48000&&pcm.getShort()==2000&&pcm.getShort()==0,"Mono downmix averages without overflow or time/pitch change");
        }check(source.closed,"Decoder closes with native stream");
        var root=Path.of(System.getProperty("katzencraft.equipment.root"));
        var decoder=Class.forName("net.minecraft.client.sounds.JOrbisAudioStream").getConstructor(java.io.InputStream.class);
        try(var input=Files.newInputStream(root.resolve("src/main/resources/assets/katzencraftmetals/sounds/foundry_pouring_loop.ogg"));var nativeStream=(AudioStream)decoder.newInstance(input);var mono=new FoundryFaucetLoopSoundInstance.MonoStream(nativeStream)){
            long bytes=0;for(int i=0;i<10000;i++){var pcm=mono.read(16384);if(!pcm.hasRemaining())break;bytes+=pcm.remaining();}
            check(bytes>48000*2,"Supplied Vorbis recording decodes to positional mono PCM");
        }
        System.out.println("PASS: pouring sound's real synced start/stop/restart across five orientations and both casts, target/tank removal/fullness, independent replicas/loops, residual visual timeline, two-tick fade and cancellation, pitch 1, native Vorbis decode and stereo-to-mono PCM. Minecraft audio/multiplayer/chunk verification remains required.");
    }
}

package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.block.entity.*;
import net.chriskatze.katzencraftmetals.client.FoundryDetachedTankCache;
import net.chriskatze.katzencraftmetals.network.FoundryDetachedPayload;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.level.Level;
import java.nio.file.*;
import java.util.*;

/** Real inventory/escrow/cache handoff plus production sound reconciliation and decoder checks. */
public final class FoundryTankSoundChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static FoundryTankSoundTracker.Source source(FoundryTankNetwork network){
        return new FoundryTankSoundTracker.Source(network.getTankPositions(),!network.getMoltenContents().isEmpty());
    }
    private static FoundryTankSoundTracker.Source source(FoundryVesselSnapshot snapshot){
        return new FoundryTankSoundTracker.Source(snapshot.positions(),!snapshot.contents().isEmpty());
    }
    private static final class Playback {
        final FoundryTankSoundTracker tracker=new FoundryTankSoundTracker();
        final List<FoundryTankLoopSoundInstance> starts=new ArrayList<>();
        final Set<FoundryTankLoopSoundInstance> playing=new HashSet<>();
        void update(List<FoundryTankSoundTracker.Source> sources,boolean loaded){
            tracker.update(sources,p->loaded,sound->{starts.add(sound);playing.add(sound);},playing::remove);
        }
        void ticks(int count){for(int i=0;i<count;i++){List.copyOf(playing).forEach(FoundryTankLoopSoundInstance::tick);playing.removeIf(FoundryTankLoopSoundInstance::isStopped);}}
    }
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var fixture=FoundryDismantlingChecks.class;
        var worldFactory=fixture.getDeclaredMethod("world",HolderLookup.Provider.class);worldFactory.setAccessible(true);
        Level world=(Level)worldFactory.invoke(null,lookup);
        var tanks=fixture.getDeclaredMethod("tanks",worldFactory.getReturnType(),Set.class);tanks.setAccessible(true);
        var controllerFactory=fixture.getDeclaredMethod("controller",worldFactory.getReturnType());controllerFactory.setAccessible(true);
        var cells=Set.of(BlockPos.ZERO,BlockPos.ZERO.east());tanks.invoke(null,world,cells);
        var controller=(FoundryControllerBlockEntity)controllerFactory.invoke(null,world);
        var network=controller.getOwnedTankNetwork();var iron=ResourceLocation.withDefaultNamespace("iron");
        var client=new Playback();client.update(List.of(source(network)),true);check(client.starts.isEmpty(),"Empty Foundry is silent");
        network.insert(iron,1);client.update(List.of(source(network)),true);check(client.starts.size()==1,"First molten unit starts one loop for two Tanks");
        var original=client.starts.getFirst();
        network.insert(iron,100);network.extract(iron,50);
        for(int i=0;i<100;i++){client.update(List.of(source(network)),true);client.ticks(1);}
        check(client.starts.size()==1&&client.playing.contains(original),"Partial drain and stable inventory preserve the exact same loop");
        network.extract(iron,51);client.update(List.of(source(network)),true);client.ticks(3);check(!original.isStopped(),"Brief complete-drain fade");
        client.ticks(1);check(original.isStopped()&&client.playing.isEmpty(),"Empty stops after four ticks");
        network.insert(iron,70);client.update(List.of(source(network)),true);var active=client.starts.getLast();check(client.starts.size()==2,"Refill restarts once");
        var attached=source(network);
        world.destroyBlock(controller.getBlockPos(),true);
        var data=FoundryDetachedVessels.get((net.minecraft.server.level.ServerLevel)world);
        var saved=data.save(new net.minecraft.nbt.CompoundTag(),lookup);var restored=FoundryDetachedVessels.load(saved,lookup);
        var snapshot=restored.snapshots().values().iterator().next();check(snapshot.contents().get(iron)==70,"Detached persistence preserves molten amount");
        var cache=new FoundryDetachedTankCache();var dim=ResourceLocation.withDefaultNamespace("overworld");
        cache.apply(new FoundryDetachedPayload(dim,0,1,false,restored.snapshots()));
        var detached=source(cache.views(dim).values().iterator().next());
        client.update(List.of(attached,detached),true);client.ticks(1);
        client.update(List.of(detached),true);client.ticks(10);
        check(client.starts.size()==2&&client.playing.equals(Set.of(active)),"Controller removal and overlapping detached view reuse the existing loop without a fade/restart");
        var replacement=(FoundryControllerBlockEntity)controllerFactory.invoke(null,world);
        check(replacement.getOwnedTankNetwork().getTotalMoltenAmount()==70,"Replacement claims exact same metal");
        attached=source(replacement.getOwnedTankNetwork());
        client.update(List.of(attached,detached),true);client.ticks(1);
        cache.apply(new FoundryDetachedPayload(dim,0,2,false,Map.of()));
        client.update(List.of(attached),true);client.ticks(1);
        check(client.starts.size()==2&&client.playing.equals(Set.of(active)),"Reconnection and replica retirement neither duplicate nor restart");
        client.update(List.of(attached),false);check(client.playing.isEmpty()&&active.isStopped(),"Unloaded/destroyed vessel cancels immediately");
        client.update(List.of(attached),true);check(client.starts.size()==3,"Chunk return creates one fresh loop");
        var other=new FoundryTankSoundTracker.Source(Set.of(new BlockPos(10,0,0)),true);
        client.update(List.of(attached,other,detached),true);check(client.playing.size()==2&&client.starts.size()==4,"Separate Foundries get independent loops; stale duplicate does not add one");
        var secondClient=new Playback();secondClient.update(List.of(attached,other),true);check(secondClient.playing.size()==2,"Independent multiplayer-client ownership");
        replacement.getOwnedTankNetwork().extract(iron,70);
        client.update(List.of(source(replacement.getOwnedTankNetwork()),detached,other),true);client.ticks(4);
        check(client.playing.size()==1&&secondClient.playing.size()==2,"Attached empty state takes precedence over stale detached state, without touching another client");
        client.update(List.of(),false);check(client.playing.isEmpty(),"No loaded vessels leaves no audio");
        secondClient.tracker.clear(secondClient.playing::remove);check(secondClient.playing.isEmpty(),"Logout/dimension/reload clears all loops");
        var field=AbstractSoundInstance.class.getDeclaredField("sound");field.setAccessible(true);var unit=ConstantFloat.of(1);
        var resolved=new Sound(ResourceLocation.parse("katzencraftmetals:tank_loop"),unit,unit,1,Sound.Type.FILE,true,false,16);
        var loop=new FoundryTankLoopSoundInstance(new BlockPos(3,4,5));field.set(loop,resolved);
        check(loop.getPitch()==1&&loop.getVolume()==.30f&&loop.getSource()==net.minecraft.sounds.SoundSource.BLOCKS&&loop.isLooping()&&!loop.isRelative(),"Exact requested sound properties");
        check(loop.getX()==3.5&&loop.getY()==4.5&&loop.getZ()==5.5&&loop.getAttenuation()==SoundInstance.Attenuation.LINEAR,"Native positional attenuation at tank center");
        check(BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("katzencraftmetals:foundry_tank_loop")).isPresent(),"Registered event");
        var root=Path.of(System.getProperty("katzencraft.equipment.root"));
        var json=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("src/main/resources/assets/katzencraftmetals/sounds.json"))).getAsJsonObject();
        var sound=json.getAsJsonObject("foundry_tank_loop").getAsJsonArray("sounds").get(0).getAsJsonObject();
        check(sound.get("name").getAsString().equals("katzencraftmetals:tank_loop")&&sound.get("stream").getAsBoolean(),"Correct streamed asset reference");
        var decoder=Class.forName("net.minecraft.client.sounds.JOrbisAudioStream").getConstructor(java.io.InputStream.class);
        try(var input=Files.newInputStream(root.resolve("src/main/resources/assets/katzencraftmetals/sounds/tank_loop.ogg"));var stream=(AudioStream)decoder.newInstance(input)){
            check(stream.getFormat().getChannels()==1&&stream.getFormat().getSampleRate()==48000,"Unmodified mono 48kHz Vorbis");
            long bytes=0;for(int i=0;i<10000;i++){var pcm=stream.read(16384);if(!pcm.hasRemaining())break;bytes+=pcm.remaining();}
            double duration=bytes/96000.0;check(duration>18&&duration<21,"Approximately 19 seconds decoded without speed change");
            System.out.println("Tank ambient recording decoded: "+duration+" seconds.");
        }
        System.out.println("PASS: tank ambient first fill, partial/complete drain, four-tick fade, exact loop identity through persisted detached state and Controller reconnection, stale-source deduplication, unload/reload, independent Foundries/clients, native sound properties and authored Vorbis decoding. Minecraft audio verification remains required.");
    }
}

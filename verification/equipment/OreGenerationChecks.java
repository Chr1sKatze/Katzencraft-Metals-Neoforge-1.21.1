package net.chriskatze.katzencraftmetals.checks;

import com.mojang.serialization.*;
import net.chriskatze.katzencraftmetals.metal.*;
import net.chriskatze.katzencraftmetals.worldgen.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.neoforged.neoforge.common.world.*;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.*;
import static net.chriskatze.katzencraftmetals.metal.MetalDefinition.Form.*;
import static net.chriskatze.katzencraftmetals.metal.MetalOreGeneration.Distribution.*;
import static net.chriskatze.katzencraftmetals.metal.MetalOreGeneration.Mode.*;

/** Registry-loaded production bootstraps, real rule tests/codecs and biome modifier phases. */
public final class OreGenerationChecks {
    static void check(boolean ok,String message) { if (!ok) throw new AssertionError(message); }
    static ResourceLocation id(String p) { return ResourceLocation.withDefaultNamespace(p); }
    static void invalid(Runnable action) { try { action.run(); } catch (IllegalArgumentException | NullPointerException expected) { return; } throw new AssertionError("Invalid ore configuration accepted"); }
    static MetalDefinition.Builder metal() { return MetalDefinition.builder("minecraft:gold","Gold",19300,6).form(ORE,"minecraft:gold_ore").form(NETHER_ORE,"minecraft:nether_gold_ore"); }
    public static void run(HolderLookup.Provider provider) throws Exception {
        invalid(() -> metal().oreGeneration(ORE,c -> c.dimension("minecraft:overworld").replace("minecraft:stone").height(10,-10)).build());
        invalid(() -> metal().oreGeneration(ORE,c -> c.replace("minecraft:stone")).build());
        invalid(() -> metal().oreGeneration(ORE,c -> c.dimension("minecraft:overworld")).build());
        invalid(() -> metal().oreGeneration(ORE,c -> c.dimension("minecraft:overworld").replace("minecraft:stone").veinSize(65)).build());
        invalid(() -> metal().oreGeneration(ORE,c -> c.dimension("minecraft:overworld").replace("minecraft:stone").attemptsPerChunk(0)).build());
        invalid(() -> metal().oreGeneration(END_ORE,c -> c.dimension("minecraft:the_end").replace("minecraft:end_stone")).build());
        check(metal().oreGenerationMode(REPLACE).build().oreGeneration().isEmpty(),"Forms/mode alone must not generate");
        var disabled=metal().oreGenerationMode(REPLACE).oreGeneration(ORE,c -> c.dimension("minecraft:overworld").replace("minecraft:stone").enabled(false)).build();
        check(ModOreGeneration.entries(List.of(disabled)).isEmpty(),"Disabled entry emits no feature or replacement");
        var example=metal().oreGenerationMode(REPLACE)
                .oreGeneration(ORE,c -> c.dimension("minecraft:overworld").biomes("minecraft:badlands").replace("minecraft:stone").height(-50,30))
                .oreGeneration(NETHER_ORE,c -> c.dimension("example:moon").biomes("#example:craters").replace("#example:moon_stone").mode(ADDITIVE)).build();
        check(example.oreGeneration().get(0).mode()==REPLACE&&example.oreGeneration().get(1).mode()==ADDITIVE,"Per-entry/default mode");
        check(example.oreGeneration().get(1).dimension().equals(ResourceLocation.parse("example:moon")),"Modded dimension");
        for (var distribution:MetalOreGeneration.Distribution.values()) {
            var a=RandomSource.create(76234);var b=RandomSource.create(76234);long sum=0;int[] bins=new int[4];
            for(int i=0;i<100000;i++) { int y=distribution.sample(a,-64,63);check(y==distribution.sample(b,-64,63),"Deterministic seeded samples");check(y>=-64&&y<=63,"Height bounds");sum+=y;bins[(y+64)/32]++; }
            double mean=sum/100000.0;
            if(distribution==UNIFORM) check(Arrays.stream(bins).allMatch(v->v>24000&&v<26000),"Uniform histogram");
            if(distribution==TRIANGLE)check(bins[1]>bins[0]*2&&bins[2]>bins[3]*2,"Triangle middle peak");
            if(distribution==BOTTOM_HEAVY)check(mean< -20&&bins[0]>bins[1]&&bins[1]>bins[2]&&bins[2]>bins[3],"Bottom weighting");
            if(distribution==TOP_HEAVY)check(mean>20&&bins[0]<bins[1]&&bins[1]<bins[2]&&bins[2]<bins[3],"Top weighting");
            check(distribution.sample(a,12,12)==12,"Single-height interval");
        }
        var biomeRegistry=new MappedRegistry<Biome>(Registries.BIOME,Lifecycle.stable());
        var plains=biomeRegistry.register(ResourceKey.create(Registries.BIOME,id("plains")),provider.lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,id("plains"))).value(),RegistrationInfo.BUILT_IN);
        var badlands=biomeRegistry.register(ResourceKey.create(Registries.BIOME,id("badlands")),provider.lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,id("badlands"))).value(),RegistrationInfo.BUILT_IN);
        var tag=TagKey.create(Registries.BIOME,ResourceLocation.parse("example:craters"));biomeRegistry.bindTags(Map.of(tag,List.of(badlands)));
        check(new OreGenerationScope(id("overworld"),List.of("#example:craters","minecraft:plains")).matchesBiome(plains),"OR of ID/tag");
        check(new OreGenerationScope(id("overworld"),List.of("#example:craters")).matchesBiome(badlands),"Biome tag");
        check(!new OreGenerationScope(id("overworld"),List.of("#example:craters")).matchesBiome(plains),"Biome tag exclusion");
        check(new OreGenerationScope(id("overworld"),List.of()).matchesBiome(plains),"Unrestricted biome");
        actualPlacement(provider,plains);
        var selected=new ModOreGeneration.Entry(example,example.oreGeneration().getFirst(),ModOreGeneration.id("test/gold"));
        var scopes=ModOreGeneration.replacementScopes(List.of(selected));
        check(scopes.keySet().equals(Set.of(id("ore_gold"),id("ore_gold_lower"),id("ore_gold_extra"))),"Exact overworld Gold family includes badlands extra, excludes Nether");
        check(ModOreGeneration.vanillaPlacements(id("gold"),NETHER_ORE).equals(List.of(id("ore_gold_nether"),id("ore_gold_deltas"))),"Nether Gold includes delta variant");
        check(ModOreGeneration.vanillaPlacements(id("netherite"),ORE).equals(List.of(id("ore_ancient_debris_large"),id("ore_debris_small"))),"Native debris registry IDs");
        check(ModOreGeneration.replacementScopes().isEmpty()&&NoiseOreOverrides.SCOPES.isEmpty(),"Default vanilla/debris generation untouched");
        var filter=new OreReplacementFilter(scopes.get(id("ore_gold")));
        check(!filter.allows(id("overworld"),badlands)&&filter.allows(id("overworld"),plains)&&filter.allows(ResourceLocation.parse("example:moon"),badlands),"Replacement scope preserves other biomes AND dimensions");
        // Run production configured/placed/biome bootstraps against captured native registries.
        var contexts=new Contexts(provider);
        ModOreGeneration.configured(contexts.context(Registries.CONFIGURED_FEATURE));
        ModOreGeneration.placed(contexts.context(Registries.PLACED_FEATURE));
        ModOreGeneration.biome(contexts.context(NeoForgeRegistries.Keys.BIOME_MODIFIERS));
        check(contexts.registry(Registries.CONFIGURED_FEATURE).size()==8&&contexts.registry(Registries.PLACED_FEATURE).size()==8,"Eight explicit migrated deposits");
        for(var entry:ModOreGeneration.entries()) {
            var configured=contexts.registry(Registries.CONFIGURED_FEATURE).get(entry.id());
            var config=(OreConfiguration)configured.config();
            check(config.size==entry.config().veinSize(),"Vein size independent");
            for(var target:config.targetStates)check(BuiltInRegistries.BLOCK.getKey(target.state.getBlock()).equals(entry.block()),"Correct ore variant");
            if(entry.config().form()==NETHER_ORE) {
                var rule=config.targetStates.getFirst().target;
                check(rule.test(Blocks.NETHERRACK.defaultBlockState(),RandomSource.create(1))&&!rule.test(Blocks.END_STONE.defaultBlockState(),RandomSource.create(1)),"Exact replacement block rule");
            }
            var configuredJson=ConfiguredFeature.DIRECT_CODEC.encodeStart(contexts.ops(),configured).getOrThrow().toString();
            for(var selector:entry.config().replacements())check(configuredJson.contains(selector.replace("#","")),"Exact replacement target codec");
            var placed=contexts.registry(Registries.PLACED_FEATURE).get(entry.id());
            check(placed.placement().size()==4&&placed.placement().get(0) instanceof CountPlacement&&placed.placement().get(1) instanceof InSquarePlacement&&placed.placement().get(2) instanceof OreHeightPlacement&&placed.placement().get(3) instanceof BiomeFilter,"Native count/spread/biome modifiers");
            var encoded=PlacedFeature.DIRECT_CODEC.encodeStart(contexts.ops(),placed).getOrThrow();
            check(PlacedFeature.DIRECT_CODEC.parse(contexts.ops(),encoded).isSuccess(),"Registered placement codec round trip");
        }
        var replacementContexts=new Contexts(provider);
        ModOreGeneration.configured(replacementContexts.context(Registries.CONFIGURED_FEATURE),List.of(selected));
        ModOreGeneration.placed(replacementContexts.context(Registries.PLACED_FEATURE),List.of(selected));
        ModOreGeneration.biome(replacementContexts.context(NeoForgeRegistries.Keys.BIOME_MODIFIERS),List.of(selected));
        var modifier=(CatalogOreBiomeModifier)replacementContexts.registry(NeoForgeRegistries.Keys.BIOME_MODIFIERS).get(ModOreGeneration.id("catalog_ores"));
        check(modifier.replacements().size()==3,"Exactly three vanilla holders guarded once");
        var builder=ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(badlands.value().modifiableBiomeInfo().getOriginalBiomeInfo());
        var ores=builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);
        var before=new ArrayList<>(ores);
        for(var phase:BiomeModifier.Phase.values())modifier.modify(badlands,phase,builder);
        for(var r:modifier.replacements()) {
            int index=before.indexOf(r.original());
            if(index>=0)check(ores.get(index).equals(r.guarded())&&!ores.contains(r.original()),"In-place native replacement");
            var nativePlacement=r.original().value();var guarded=r.guarded().value();
            check(nativePlacement.feature().equals(guarded.feature())&&guarded.placement().subList(0,nativePlacement.placement().size()).equals(nativePlacement.placement()),"Native placement settings retained");
        }
        for(int i=0;i<before.size();i++) {var old=before.get(i);if(modifier.replacements().stream().noneMatch(r->r.original().equals(old)))check(ores.get(i).equals(old),"Unrelated world feature preserved");}
        check(ores.stream().filter(h->h.equals(modifier.additions().getFirst().feature())).count()==1,"One replacement deposit, no duplicate add");
        var patchBuilder=new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE,c->ModOreGeneration.configured(c,List.of(selected)))
                .add(Registries.PLACED_FEATURE,c->ModOreGeneration.placed(c,List.of(selected)))
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS,c->ModOreGeneration.biome(c,List.of(selected)));
        var nativeLookup=net.minecraft.data.registries.RegistryPatchGenerator.createLookup(java.util.concurrent.CompletableFuture.completedFuture(provider),patchBuilder).join().full();
        var nativeModifier=nativeLookup.lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).getOrThrow(ModOreGeneration.key(NeoForgeRegistries.Keys.BIOME_MODIFIERS,ModOreGeneration.id("catalog_ores"))).value();
        var nativeOps=nativeLookup.createSerializationContext(JsonOps.INSTANCE);
        var encoded=BiomeModifier.DIRECT_CODEC.encodeStart(nativeOps,nativeModifier).getOrThrow();
        check(BiomeModifier.DIRECT_CODEC.parse(nativeOps,encoded).isSuccess(),"Native registry-patch REPLACE codec round trip");
        var iron=MetalDefinition.builder("minecraft:iron","Iron",7874,6).form(ORE,"minecraft:iron_ore").oreGenerationMode(REPLACE)
                .oreGeneration(ORE,c->c.dimension("minecraft:overworld").biomes("minecraft:badlands").replace("minecraft:stone")).build();
        var noiseScopes=NoiseOreOverrides.scopes(List.of(new ModOreGeneration.Entry(iron,iron.oreGeneration().getFirst(),ModOreGeneration.id("test/iron"))));
        check(NoiseOreOverrides.filter(Blocks.RAW_IRON_BLOCK.defaultBlockState(),id("overworld"),badlands,noiseScopes)==null,"Large noise ore bypass removed in scope");
        check(NoiseOreOverrides.filter(Blocks.COPPER_ORE.defaultBlockState(),id("overworld"),badlands,noiseScopes).is(Blocks.COPPER_ORE),"Other noise metal preserved");
        check(NoiseOreOverrides.filter(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),id("overworld"),plains,noiseScopes).is(Blocks.DEEPSLATE_IRON_ORE),"Noise ore preserved outside scope");
        System.out.println("PASS: four seeded ore distributions (400,000 samples), invalid config rejection, biome ID/tag OR scopes, correct ore variants/targets, eight native bootstrap deposits, registered codec round trips, selective Gold replacement in native biome phases, unchanged defaults and scoped Iron/Copper noise-vein handling.");
    }
    static void actualPlacement(HolderLookup.Provider provider,Holder<Biome> biome) throws Exception {
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        var level=(net.minecraft.server.level.ServerLevel)unsafe.allocateInstance(net.minecraft.server.level.ServerLevel.class);
        var dimension=net.minecraft.world.level.Level.class.getDeclaredField("dimension");dimension.setAccessible(true);
        dimension.set(level,net.minecraft.world.level.Level.OVERWORLD);
        var settings=provider.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS,id("overworld")));
        var generator=new NoiseBasedChunkGenerator(new FixedBiomeSource(biome),settings);
        int[] worldBounds={-32,64};
        var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(OreGenerationChecks.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(proxy,method,args)->switch(method.getName()) {
            case "getMinBuildHeight" -> worldBounds[0];
            case "getHeight" -> worldBounds[1];
            case "getMaxBuildHeight" -> worldBounds[0]+worldBounds[1];
            case "getLevel" -> level;
            case "getBiome" -> biome;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        var context=new PlacementContext(world,generator,Optional.empty());
        for(var distribution:MetalOreGeneration.Distribution.values()) {
            var placement=new OreHeightPlacement(new OreGenerationScope(id("overworld"),List.of()),-64,64,distribution);
            var a=RandomSource.create(20261020);var b=RandomSource.create(20261020);
            for(int i=0;i<2000;i++) {
                var at=placement.getPositions(context,a,new BlockPos(7,0,9)).findFirst().orElseThrow();
                check(at.equals(placement.getPositions(context,b,new BlockPos(7,0,9)).findFirst().orElseThrow()),"Actual placement deterministic");
                check(at.getX()==7&&at.getZ()==9&&at.getY()>=-32&&at.getY()<32,"Actual placement clips to dimension build limits");
            }
        }
        // A shifted modded dimension can extend higher than the generator settings.
        // WorldGenerationContext's min + depth is not always the actual intersection.
        worldBounds[0]=100;worldBounds[1]=400;
        var shifted=new PlacementContext(world,generator,Optional.empty());
        for(var distribution:MetalOreGeneration.Distribution.values()) {
            var placement=new OreHeightPlacement(new OreGenerationScope(id("overworld"),List.of()),-1000,1000,distribution);
            var random=RandomSource.create(23);
            for(int i=0;i<2000;i++) {int y=placement.getPositions(shifted,random,BlockPos.ZERO).findFirst().orElseThrow().getY();check(y>=100&&y<320,"Shifted dimension respects BOTH world and generator maxima");}
        }
        worldBounds[0]=-32;worldBounds[1]=64;
        check(new OreHeightPlacement(new OreGenerationScope(id("the_nether"),List.of()),-64,64,UNIFORM).getPositions(context,RandomSource.create(1),BlockPos.ZERO).findAny().isEmpty(),"Wrong dimension emits nothing");
        check(new OreHeightPlacement(new OreGenerationScope(id("overworld"),List.of()),100,120,TOP_HEAVY).getPositions(context,RandomSource.create(1),BlockPos.ZERO).findAny().isEmpty(),"Disjoint height emits nothing");
        check(new OreHeightPlacement(new OreGenerationScope(id("overworld"),List.of("minecraft:badlands")),-64,64,UNIFORM).getPositions(context,RandomSource.create(1),BlockPos.ZERO).findAny().isEmpty(),"Actual placement rejects unmatched biome");
    }
    static final class Contexts {
        final HolderLookup.Provider provider;
        final Map<ResourceKey<?>,MappedRegistry<?>> registries=new HashMap<>();
        Contexts(HolderLookup.Provider p){provider=p;}
        @SuppressWarnings("unchecked") <T> MappedRegistry<T> registry(ResourceKey<? extends Registry<T>> key){return (MappedRegistry<T>)registries.computeIfAbsent(key,k->new MappedRegistry<T>(key,Lifecycle.stable()));}
        <T> BootstrapContext<T> context(ResourceKey<? extends Registry<T>> key){var registry=registry(key);return new BootstrapContext<>() {
            public Holder.Reference<T> register(ResourceKey<T> id,T value,Lifecycle lifecycle){return registry.register(id,value,RegistrationInfo.BUILT_IN);}
            @SuppressWarnings("unchecked") public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> lookup){
                var local=registries.get(lookup);
                if(local!=null)return new HolderGetter<S>() {
                    public Optional<Holder.Reference<S>> get(ResourceKey<S> id){
                        var own=((MappedRegistry<S>)local).getHolder(id);
                        return own.isPresent()?own:provider.lookupOrThrow(lookup).get(id);
                    }
                    public Optional<HolderSet.Named<S>> get(net.minecraft.tags.TagKey<S> tag){return provider.lookupOrThrow(lookup).get(tag);}
                };
                return provider.lookupOrThrow(lookup);
            }
        };}
        RegistryOps<com.google.gson.JsonElement> ops(){return RegistryOps.create(JsonOps.INSTANCE,new RegistryOps.RegistryInfoLookup(){
            @SuppressWarnings({"unchecked","rawtypes"}) public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key){
                var r=(MappedRegistry<T>)registries.get(key);
                if(r!=null)return Optional.of(new RegistryOps.RegistryInfo<>(r.asLookup(),r.asLookup(),Lifecycle.stable()));
                var fallback=provider.lookup(key);
                return fallback.map(v->new RegistryOps.RegistryInfo<>(v,v,Lifecycle.stable()));
            }
        });}
    }
}

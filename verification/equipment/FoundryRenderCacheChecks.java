package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Exercises production layout caches, not a replacement geometry implementation. */
public final class FoundryRenderCacheChecks {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run() throws Exception {
        var density = ModMoltenMetals.heaviestFirst();
        check(density == ModMoltenMetals.heaviestFirst(), "Density sorting reuses its immutable result");
        check(ModMoltenMetals.lightestFirst() == ModMoltenMetals.lightestFirst(), "Ascending sorting also cached");
        check(density.equals(ModMoltenMetals.values().stream().sorted(Comparator.comparingInt(MoltenMetalDefinition::density).reversed()).toList()), "Stable density/tie order unchanged");
        try { density.clear(); throw new AssertionError("Density list mutable"); } catch (UnsupportedOperationException expected) {}

        var distribute = FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("distributeAcrossHorizontalLevels", Set.class, Map.class);
        var create = FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("createRenderedLayers", BlockPos.class, Set.class, Map.class);
        distribute.setAccessible(true); create.setAccessible(true);
        var renderer = new FoundryControllerTankLiquidRenderer();
        Object world = new Object(), attached = new Object(), detached = UUID.randomUUID();
        var cells = new LinkedHashSet<BlockPos>();
        for (int y=0;y<4;y++) for(int z=0;z<4;z++) for(int x=0;x<4;x++) cells.add(new BlockPos(x,y,z));
        var iron = ResourceLocation.withDefaultNamespace("iron");
        var gold = ResourceLocation.withDefaultNamespace("gold");
        var amounts = new LinkedHashMap<ResourceLocation,Float>(); amounts.put(iron, 1300f); amounts.put(gold, 200f);
        var cached = renderer.cachedLayers(attached, world, cells, amounts);
        var layers = cached.layers;
        var geometries = new HashMap<>(cached.cells);
        for (int i=0;i<1000;i++) {
            var hit = renderer.cachedLayers(attached, world, cells, amounts);
            check(hit.layers == layers, "Stable frames must not rebuild any layer lists/maps");
            for (var p : cells) check(hit.cells.get(p) == geometries.get(p), "Stable cell geometry/route metadata reused");
        }
        int layouts = 0;
        // Includes insertion/extraction, changing composition, tiny partial-tick changes and crossings.
        for (float value : new float[]{1300.001f, 1299.999f, 1299.5f, 108f, 107.999f, 0f, 54f, 1200f}) {
            amounts.put(iron,value);
            var next = renderer.cachedLayers(attached, world, cells, amounts);
            check(next.layers != layers, "Exact changed displayed amount invalidates layout"); layers=next.layers;
            @SuppressWarnings("unchecked") var byY=(Map<Integer,Map<ResourceLocation,Float>>)distribute.invoke(null,cells,amounts);
            for (var p:cells) check(next.layers.get(p).equals(create.invoke(null,p,cells,byY)), "Cache equals direct production layout at animated height");
            layouts++;
        }
        var detachedCache = renderer.cachedLayers(detached,world,cells,amounts);
        check(detachedCache != cached && detachedCache.layers.equals(cached.layers), "Detached has identical layout with independent identity");
        amounts.put(gold,0f);
        check(renderer.cachedLayers(attached,world,cells,amounts).layers != layers,"Composition changes invalidate even when old snapshot map is mutated");
        cells.remove(new BlockPos(3,3,3));
        check(renderer.cachedLayers(attached,world,cells,amounts).layers.size()==63,"Removal/unloaded physical cell invalidates");
        cells.add(new BlockPos(3,3,3));
        check(renderer.cachedLayers(attached,world,cells,amounts).layers.size()==64,"Expansion/reloaded cell invalidates");
        var replacement=renderer.cachedLayers(new Object(),world,cells,amounts);
        check(replacement!=cached,"Controller reconnection receives its own cache");
        check(renderer.cachedLayers(attached,new Object(),cells,amounts)!=cached,"World reload clears old-world cache");

        // Immutable layouts also reuse exact per-layer junction classifications.
        var support=BlockPos.ZERO;
        var crossbar=Set.of(support,support.above(),support.above().east(),support.above().west());
        var cross=renderer.cachedLayers(new Object(),world,crossbar,Map.of(iron,350f));
        int routes=0;
        for(var pos:crossbar){
            Object cell=cross.cells.get(pos);
            var accessor=cell.getClass().getDeclaredMethod("routes");accessor.setAccessible(true);
            @SuppressWarnings("unchecked") var actual=(List<FoundryTankJunctionMaterial.Route>)accessor.invoke(cell);
            for(int i=0;i<actual.size();i++){
                var expected=FoundryTankJunctionMaterial.route(pos,cross.layers.get(pos).get(i),cross.layers);
                check(Objects.equals(actual.get(i),expected),"Cached route equals live junction classification");
                if(expected!=null)routes++;
            }
        }
        check(routes>0,"T-junction routing remains present");

        // Cache sampling never freezes interface waves; no render-time values are cached.
        var wave=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("sampleInterfaceWave", double.class,double.class,long.class,long.class,float.class,float.class);
        wave.setAccessible(true);
        float a=(float)wave.invoke(null,1.2,2.3,77L,100L,0f,.08f);
        float b=(float)wave.invoke(null,1.2,2.3,77L,100L,.5f,.08f);
        check(a!=b && Math.abs(a-b)<.01f,"Interface wave remains continuous between partial ticks with stable cached layout");
        // Animated vertex emission is also covered by the native seam regression suite.
        System.out.println("PASS: density-list identity/order; 1000 stable 64-cell frames reuse all layer/cell geometry/routes (64,000 cell rebuilds avoided); "
                +layouts+" exact animated-layout comparisons; composition/mutable inputs, removal/reload, detached/independent/reconnected/world identities. No FPS claim.");
    }
}

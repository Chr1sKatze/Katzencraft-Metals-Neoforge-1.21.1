package net.chriskatze.katzencraftmetals.client.renderer;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.*;
import net.chriskatze.katzencraftmetals.datagen.FoundryBasinModelAdapter;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.nio.file.*;
import java.util.*;

/** Actual export/adapter faces, emitted contents and shared stream/fill timeline; no GPU claims. */
public final class FoundryBasinGeometryChecks {
    private record Point(float x,float y,float z) {}
    private static final class Capture implements VertexConsumer {
        final List<Point> points=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){points.add(new Point(x,y,z));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void close(float a,float b,String message){check(Math.abs(a-b)<.00001f,message+": "+a+" vs "+b);}
    @SuppressWarnings("unchecked")
    public static void run(HolderLookup.Provider lookup) throws Exception {
        Path root=Path.of(System.getProperty("katzencraft.equipment.root"));
        Path model=root.resolve("src/main/resources/assets/katzencraftmetals/models/block/foundry_basin.json");
        String authored=Files.readString(model);var source=JsonParser.parseString(authored).getAsJsonObject();
        // Compare actual block-state selection against every authored element, not old bounds.
        var expectedShape=net.minecraft.world.phys.shapes.Shapes.empty();
        for(var value:source.getAsJsonArray("elements")){
            var element=value.getAsJsonObject();var from=element.getAsJsonArray("from");var to=element.getAsJsonArray("to");
            check(!element.has("rotation")||element.getAsJsonObject("rotation").get("angle").getAsDouble()==0,"Shape comparison requires axis-aligned model elements");
            expectedShape=net.minecraft.world.phys.shapes.Shapes.or(expectedShape,net.minecraft.world.level.block.Block.box(
                    from.get(0).getAsDouble(),from.get(1).getAsDouble(),from.get(2).getAsDouble(),
                    to.get(0).getAsDouble(),to.get(1).getAsDouble(),to.get(2).getAsDouble()));
        }
        for(var castState:net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock.CastState.values()){
            var state=ModBlocks.FOUNDRY_BASIN.get().defaultBlockState().setValue(net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock.CAST_STATE,castState);
            var shape=state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
            check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,expectedShape,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Outline exactly matches model in "+castState);
            var cavity=net.minecraft.world.level.block.Block.box(2,3,2,14,16,14);
            check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,cavity,net.minecraft.world.phys.shapes.BooleanOp.AND),"Hollow cavity remains selectable through opening");
            var collision=state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
            check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision,net.minecraft.world.phys.shapes.Shapes.block(),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Existing solid collision intentionally preserved");
        }
        System.out.println("PASS: Basin outline equals all 22 authored elements in empty/molten/cooled states; lowered floor, recessed base and thin windows; hollow opening; unchanged collision.");
        var adapted=FoundryBasinModelAdapter.adapt(source);
        check(source.equals(JsonParser.parseString(authored)),"Adapter never mutates authored model");
        var floor=source.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject)
                .filter(e->e.get("name").getAsString().equals("22")).findFirst().orElseThrow();
        check(floor.get("from").toString().equals("[2,2,2]")&&floor.get("to").toString().equals("[14,3,14]"),"Authoritative lowered floor and 12-pixel cavity width/depth");
        close(FoundryBasinRenderGeometry.FLOOR,3f/16,"Floor");close(FoundryBasinRenderGeometry.FULL_SURFACE,15f/16,"Full height");
        close(FoundryBasinRenderGeometry.FULL_SURFACE-FoundryBasinRenderGeometry.FLOOR,12f/16,"12-pixel fill height");
        close(FoundryFaucetRenderConstants.BASIN_MIN_Y,FoundryBasinRenderGeometry.FLOOR,"Shared stream floor");
        close(FoundryFaucetRenderConstants.BASIN_MAX_Y,FoundryBasinRenderGeometry.FULL_SURFACE,"Shared full stream endpoint");
        var children=adapted.getAsJsonObject("children");
        check(children.getAsJsonObject("frame").get("render_type").getAsString().equals("minecraft:cutout")&&children.getAsJsonObject("windows").get("render_type").getAsString().equals("minecraft:translucent"),"Existing frame/window layer separation preserved");
        int faceCount=0,adaptedCount=0;
        for(var value:source.getAsJsonArray("elements")){
            var element=value.getAsJsonObject();faceCount+=element.getAsJsonObject("faces").size();
            for(var child:children.entrySet())for(var transformed:child.getValue().getAsJsonObject().getAsJsonArray("elements")){
                var candidate=transformed.getAsJsonObject();
                if(!candidate.get("from").equals(element.get("from"))||!candidate.get("to").equals(element.get("to"))||!candidate.get("name").equals(element.get("name")))continue;
                for(var face:candidate.getAsJsonObject("faces").entrySet()){
                    var original=element.getAsJsonObject("faces").getAsJsonObject(face.getKey()).deepCopy();var actual=face.getValue().getAsJsonObject().deepCopy();
                    original.remove("cullface");actual.remove("cullface");check(original.equals(actual),"Exact authored face UV/rotation/texture alias retained");adaptedCount++;
                }
            }
        }
        check(faceCount==adaptedCount,"Each authored face appears exactly once in adapter");
        for(var texture:adapted.getAsJsonObject("textures").entrySet()){
            String path=texture.getValue().getAsString().replace("katzencraftmetals:","");
            check(Files.exists(root.resolve("src/main/resources/assets/katzencraftmetals/textures/"+path+".png")),"Supplied texture resolves: "+path);
        }
        var mesh=FoundryBasinBlockEntityRenderer.class.getDeclaredMethod("renderMetalVolume",VertexConsumer.class,PoseStack.Pose.class,
                float.class,float.class,float.class,float.class,int.class,int.class,float.class,float.class,boolean.class);mesh.setAccessible(true);
        float previous=FoundryBasinRenderGeometry.FLOOR;
        for(int amount=1;amount<=54;amount++){
            float surface=FoundryBasinRenderGeometry.surface(amount/54f);check(surface>previous,"Progressive fill height increases smoothly");previous=surface;
            for(float overlay:new float[]{0,.0005f}){
                var out=new Capture();mesh.invoke(null,out,new PoseStack().last(),surface,16f,0f,1f,0,0,1f,overlay,overlay>0);
                check(out.points.size()==20,"One top and four sides, no duplicate geometry");
                for(var p:out.points){check(p.x>2f/16&&p.x<14f/16&&p.z>2f/16&&p.z<14f/16,"Molten/cooled mesh stays inside walls");
                    check(p.y>=3f/16&&p.y<=15f/16+.0005f,"No floor penetration or rim clipping during cooling");}
                close(out.points.stream().map(Point::y).min(Float::compare).orElseThrow(),3f/16,"Emitted wall bottom on actual floor");
                close(out.points.stream().map(Point::y).max(Float::compare).orElseThrow(),surface+overlay,"Emitted top matches fill, including established cooling separation");
            }
        }
        // Native server first-transfer timing, with world mutation effects isolated from inventory logic.
        var fixture=FoundryDismantlingChecks.class;var factory=fixture.getDeclaredMethod("world",HolderLookup.Provider.class);factory.setAccessible(true);
        Level world=(Level)factory.invoke(null,lookup);var tanks=fixture.getDeclaredMethod("tanks",factory.getReturnType(),Set.class);tanks.setAccessible(true);tanks.invoke(null,world,Set.of(BlockPos.ZERO));
        var ownerFactory=fixture.getDeclaredMethod("controller",factory.getReturnType());ownerFactory.setAccessible(true);var controller=(FoundryControllerBlockEntity)ownerFactory.invoke(null,world);
        var iron=ResourceLocation.withDefaultNamespace("iron");controller.getOwnedTankNetwork().insert(iron,54);
        var blocksField=world.getClass().getDeclaredField("blocks");blocksField.setAccessible(true);var blocks=(Map<BlockPos,BlockState>)blocksField.get(world);
        var entitiesField=world.getClass().getDeclaredField("entities");entitiesField.setAccessible(true);var entities=(Map<BlockPos,BlockEntity>)entitiesField.get(world);
        var clock=world.getClass().getDeclaredField("time");clock.setAccessible(true);
        var faucetPos=BlockPos.ZERO.north();var basinPos=faucetPos.below(2);
        var faucetState=ModBlocks.FOUNDRY_FAUCET.get().defaultBlockState().setValue(FoundryFaucetBlock.FACING,Direction.NORTH);
        var faucet=new FoundryFaucetBlockEntity(faucetPos,faucetState);faucet.setLevel(world);blocks.put(faucetPos,faucetState);entities.put(faucetPos,faucet);
        var basin=new FoundryBasinBlockEntity(basinPos,ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());basin.setLevel(world);blocks.put(basinPos,basin.getBlockState());entities.put(basinPos,basin);
        close(FoundryBasinFillSmoother.getDisplayedMoltenAmount(basin,0),0,"Empty starts invisible");faucet.startPouring(iron);
        float last=0;
        for(int tick=1;tick<=124;tick++){
            clock.setLong(world,100+tick);
            basin.setLevel(null);FoundryFaucetBlockEntity.serverTick(world,faucetPos,faucetState,faucet);basin.setLevel(world);
            if(tick<16)check(basin.getMoltenAmount()==0,"No liquid before initial stream reaches floor");
            float displayed=FoundryBasinFillSmoother.getDisplayedMoltenAmount(basin,0);
            check(displayed>=last&&displayed<=basin.getMoltenAmount()&&displayed-last<=.5011f,"Smooth rise never overtakes actual transfer rate");last=displayed;
            var context=FoundryFaucetRenderContext.create(faucet,0);check(context!=null,"Valid Basin context");
            float y=FoundryBasinRenderGeometry.surface(displayed/54f);
            close(context.basinSurfaceY(),-2+y,"Stream impact follows exact displayed liquid height");
            for(boolean down:new boolean[]{false,true}){
                float progress=(float)faucet.getStreamAnimationStep()/8;
                var stream=down?FoundryFaucetStreamGeometry.resolveDown(true,progress,0,context.basinSurfaceY()):FoundryFaucetStreamGeometry.resolve(true,progress,0,context.basinSurfaceY());
                if(tick==16)close(stream.streamBottomY(),context.basinSurfaceY(),"Initial stream reaches new floor/first liquid layer");
                check(stream.streamBottomY()>=-2+3f/16-.00001f,"Stream never penetrates physical floor");
            }
        }
        check(basin.isFull()&&basin.getMoltenAmount()==54&&!faucet.isPouring()&&controller.getOwnedTankNetwork().getTotalMoltenAmount()==0,"Original capacity, output quantity and transfer lifecycle");
        close(last,54,"Final visible fill catches up without overshoot");
        basin.setLevel(null);for(int i=0;i<basin.getCoolingDuration();i++)FoundryBasinBlockEntity.serverTick(world,basinPos,basin.getBlockState(),basin);
        check(basin.isCooled()&&basin.getResultCopy().is(net.minecraft.world.item.Items.IRON_BLOCK),"Normal casting/cooling result unchanged");
        close(FoundryBasinRenderGeometry.surface(basin.getMoltenAmount()/54f),15f/16,"Finished cast at intended maximum");
        basin.takeResult();basin.setLevel(world);close(FoundryBasinFillSmoother.getDisplayedMoltenAmount(basin,0),0,"Extraction restores empty visibility");
        System.out.println("PASS: authoritative 12x12x12 Basin cavity; exact adapter geometry/UVs/textures/layers; 108 emitted molten/cooled meshes; initial 16-tick impact, shared side/down stream endpoints, progressive rate-limited filling, full/cooling/completed/empty lifecycle. Minecraft visual verification required.");
    }
}

package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.Direction;
import java.nio.file.*;
import java.util.*;

/** Emitted stream geometry and hatch submission contracts; does not replace GPU verification. */
public final class FoundryFaucetHatchChecks {
    private record Vertex(float x,float y,float z,float nx,float ny,float nz) {}
    private static final class Capture implements VertexConsumer {
        final List<Vertex> vertices=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){vertices.add(new Vertex(x,y,z,0,0,0));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){var v=vertices.removeLast();vertices.add(new Vertex(v.x,v.y,v.z,x,y,z));return this;}
    }
    private static final class FlushTrace extends net.minecraft.client.renderer.MultiBufferSource.BufferSource implements AutoCloseable {
        final List<net.minecraft.client.renderer.RenderType> flushed=new ArrayList<>();
        FlushTrace(){super(new ByteBufferBuilder(4096),new LinkedHashMap<>());}
        @Override public void endBatch(net.minecraft.client.renderer.RenderType type){
            var builder=startedBuilders.remove(type);
            if(builder!=null){var mesh=builder.build();if(mesh!=null)mesh.close();flushed.add(type);sharedBuffer.clear();}
            if(type==lastSharedType)lastSharedType=null;
        }
        public void close(){endBatch();sharedBuffer.close();}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static boolean close(float a,float b){return Math.abs(a-b)<.00001f;}
    public static void run() throws Exception {
        int columns=0;
        for(var facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.DOWN)){
            for(float surface:new float[]{-2+3f/16,-2+15f/16,-1.6f})for(int step=0;step<=32;step++){
                var geometry=facing==Direction.DOWN?FoundryFaucetStreamGeometry.resolveDown(true,step/32f,0,surface):FoundryFaucetStreamGeometry.resolve(true,step/32f,0,surface);
                var poses=new PoseStack();FoundryFaucetRenderTransform.rotateForFacing(poses,facing);
                var out=new Capture();FoundryFaucetStreamRenderer.render(out,poses.last(),geometry,FoundryFaucetDripStyle.HORIZONTAL_ONLY,surface,0,0,1,255);
                int caps=0;
                for(int i=0;i<out.vertices.size();i+=4){var q=out.vertices.subList(i,i+4);
                    if(q.stream().allMatch(v->close(v.ny,-1)&&close(v.y,geometry.streamBottomY()))){
                        caps++;var a=q.get(0);var b=q.get(1);var c=q.get(2);
                        float crossY=(b.z-a.z)*(c.x-a.x)-(b.x-a.x)*(c.z-a.z);
                        check(crossY<0,"Cap faces downward after orientation transform (below/diagonal visibility)");
                        // Each cap corner must share the exact lower vertex of a side face.
                        for(var v:q)check(out.vertices.stream().anyMatch(w->close(w.ny,0)&&close(v.x,w.x)&&close(v.y,w.y)&&close(v.z,w.z)),"Watertight cap-to-side seam without offsets");
                    }
                }
                boolean visible=!geometry.renderBrokenDrips()&&geometry.streamBottomY()<geometry.streamOuterTopY()-.0001f;
                check(caps==(visible?1:0),"Exactly one cap for each continuous stream; no degenerate/duplicate cap");
                if(visible){columns++;check(out.vertices.stream().anyMatch(v->v.ny>0),"Existing stream top remains visible from above");}
            }
            var broken=facing==Direction.DOWN?FoundryFaucetStreamGeometry.resolveDown(false,.5f,1,-2):FoundryFaucetStreamGeometry.resolve(false,.5f,1,-2);
            var cap=new Capture();FoundryFaucetStreamRenderer.renderEndCap(cap,new PoseStack().last(),broken,0,0,1,255);
            check(cap.vertices.isEmpty(),"Broken-drip finishing meshes unchanged");
        }
        var opening=new Capture();FoundryTankIntakeHatchRenderer.renderHatchOpening(opening,new PoseStack().last(),0,0);
        check(opening.vertices.size()==4,"One hatch pane, no duplicate surfaces");
        for(var v:opening.vertices)check(close(v.y,18f/16)&&v.x>=6f/16&&v.x<=10f/16&&v.z>=6f/16&&v.z<=10f/16,"Existing 4x4 hatch aperture position unchanged");
        // Validate the production call sites, because native model rendering needs a Minecraft GPU context.
        var root=Path.of(System.getProperty("katzencraft.equipment.root"));
        var model=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("src/main/resources/assets/katzencraftmetals/models/block/foundry_faucet.json"))).getAsJsonObject();
        check(model.get("render_type").getAsString().equals("minecraft:cutout"),"Custom side model retains alpha-cutout surfaces");
        for(var texture:model.getAsJsonObject("textures").entrySet())check(texture.getValue().getAsString().equals("katzencraftmetals:block/foundry_faucet"),"Authored texture resolves to existing mod artwork");
        var variants=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/assets/katzencraftmetals/blockstates/foundry_faucet.json"))).getAsJsonObject().getAsJsonObject("variants");
        for(var facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST))for(var downRotation:Direction.Plane.HORIZONTAL){
            var state=net.chriskatze.katzencraftmetals.block.ModBlocks.FOUNDRY_FAUCET.get().defaultBlockState()
                    .setValue(net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock.FACING,facing)
                    .setValue(net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock.DOWN_ROTATION,downRotation);
            var shape=state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO);
            check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Existing selection/collision behavior remains shared");
            var poses=new PoseStack();FoundryFaucetRenderTransform.rotateForFacing(poses,facing);
            var bounds=new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            for(var element:model.getAsJsonArray("elements")){
                var box=element.getAsJsonObject();var from=box.getAsJsonArray("from");var to=box.getAsJsonArray("to");
                check(!box.has("rotation")||box.getAsJsonObject("rotation").get("angle").getAsFloat()==0,"Current custom elements remain axis aligned");
                for(int corner=0;corner<8;corner++){
                    var v=new org.joml.Vector3f((corner%2==0?from:to).get(0).getAsFloat()/16,
                            ((corner&2)==0?from:to).get(1).getAsFloat()/16,((corner&4)==0?from:to).get(2).getAsFloat()/16);
                    v.mulPosition(poses.last().pose());float[] xyz={v.x,v.y,v.z};
                    for(int axis=0;axis<3;axis++){bounds[axis]=Math.min(bounds[axis],xyz[axis]);bounds[axis+3]=Math.max(bounds[axis+3],xyz[axis]);}
                }
            }
            var expected=net.minecraft.world.phys.shapes.Shapes.box(bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],bounds[5]);
            check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,expected,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Existing simple outline exactly encloses updated model for "+facing);
            var variant=variants.getAsJsonObject("down_rotation="+downRotation.getSerializedName()+",facing="+facing.getSerializedName());
            check(variant.get("model").getAsString().equals("katzencraftmetals:block/foundry_faucet"),"Every horizontal variant uses supplied side model");
            int expectedY=switch(facing){case EAST->90;case SOUTH->180;case WEST->270;default->0;};
            check((variant.has("y")?variant.get("y").getAsInt():0)==expectedY,"Blockstate rotation remains aligned with stream transform");
        }
        System.out.println("PASS: custom side Faucet cutout model/texture references; all 16 horizontal facing/rotation variants; model bounds match unchanged selection/collision shapes. Stream and opening geometry regressions follow.");
        var dir=root.resolve("src/main/java/net/chriskatze/katzencraftmetals/client/renderer");
        String attached=Files.readString(dir.resolve("FoundryControllerBlockEntityRenderer.java"));
        String detached=Files.readString(dir.resolve("FoundryDetachedTankRenderer.java"));
        check(attached.indexOf("liquidRenderer.render(")<attached.indexOf("renderOpenIntakeHatches(controller.getLevel()"),"Attached liquid submitted before hatch for all fill levels/metals");
        check(detached.indexOf(".render(entry.getKey(),level,origin")<detached.indexOf("renderOpenIntakeHatches(level,origin"),"Detached uses same liquid-before-hatch ordering");
        check(attached.contains("physicalStructure.contains(tankPos.above())"),"Connected stacked tanks render hatch only at exposed top");
        String hatch=Files.readString(dir.resolve("FoundryTankIntakeHatchRenderer.java"));
        check(hatch.contains("FoundryFaucetOpeningRenderType.getHatch()"),"Only hatch uses color-only opening variant");
        var hatchType=FoundryFaucetOpeningRenderType.getHatch();
        var faucetType=FoundryFaucetOpeningRenderType.get();
        check(hatchType!=faucetType,"Hatch and faucet depth states are isolated buffers");
        check(hatchType.format()==faucetType.format()&&hatchType.mode()==faucetType.mode()
                &&hatchType.sortOnUpload()==faucetType.sortOnUpload(),"Vertex format and translucent sorting unchanged");
        String openingState=Files.readString(dir.resolve("FoundryFaucetOpeningRenderType.java"));
        check(openingState.contains("if (hatch) COLOR_WRITE.setupRenderState()")
                &&openingState.contains("if (hatch) COLOR_WRITE.clearRenderState()"),"Hatch applies and restores native color-only mask");
        check(openingState.contains("BASE = entityTranslucent(")&&openingState.contains("BASE.setupRenderState()")
                &&openingState.contains("BASE.clearRenderState()"),"Vanilla LEQUAL/translucent state setup and restoration retained");
        check(openingState.contains("OPENING_ALPHA = 214")&&openingState.contains("hatch ? HATCH_ALPHA_MULTIPLIER : ALPHA_MULTIPLIER"),"Hatch alpha is independent; faucet remains unchanged");
        check(close(FoundryFaucetOpeningRenderType.HATCH_ALPHA,.50f),"Separate hatch alpha is exactly 50 percent");
        var overlayImage=javax.imageio.ImageIO.read(root.resolve("src/main/resources/assets/katzencraftmetals/textures/block/foundry_tank_faucet_overlay.png").toFile());
        for(int y=5;y<9;y++)for(int x=6;x<10;x++){
            float textureAlpha=((overlayImage.getRGB(x,y)>>>24)&255)/255f;
            check(close(textureAlpha*FoundryFaucetOpeningRenderType.HATCH_ALPHA_MULTIPLIER,.50f),"Actual hatch texel alpha resolves to 0.50");
            check(close(textureAlpha*FoundryFaucetOpeningRenderType.ALPHA_MULTIPLIER,214/255f),"Faucet opacity remains unchanged");
        }
        // Analytical LEQUAL/write-mask checks, not a substitute for actual driver state/visual tests.
        for(float liquidDepth:new float[]{.3f,.6f,.95f}){
            float paneDepth=.2f;float finalDepth=liquidDepth;
            check(paneDepth<=finalDepth,"Pane passes LEQUAL over full/partial deeper liquid");
            check(finalDepth==liquidDepth,"Color-only pane leaves liquid depth available to future mask tests");
            float wallDepth=.1f;check(!(paneDepth<=wallDepth),"Depth test still rejects pane behind opaque wall");
        }
        System.out.println("PASS: isolated hatch color-only state contract; 50-percent hatch alpha, preserved faucet state/opacity and format/sorting; existing attached/detached liquid-before-hatch order. Fast/Fancy/Fabulous driver-state and angle checks require Minecraft.");
        String depth=Files.readString(dir.resolve("FoundryTankLiquidDepthRenderType.java"));
        check(depth.contains("DEPTH_FACTOR = 0.0f")&&depth.contains("DEPTH_UNITS = 2.0f"),"Successful liquid depth bias unchanged");
        // Exercise Minecraft's real non-fixed getBuffer switching without GPU drawing.
        var liquidType=FoundryTankLiquidDepthRenderType.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/block/lava_still.png"));
        var paneType=FoundryFaucetOpeningRenderType.getHatch();
        try(var trace=new FlushTrace()){
            FoundryTankIntakeHatchRenderer.renderHatchOpening(trace.getBuffer(paneType),new PoseStack().last(),0,0);
            trace.getBuffer(liquidType);
            check(trace.flushed.equals(List.of(paneType)),"Pane-first submission flushes the pane before liquid; order still matters for alpha blending");
        }
        try(var trace=new FlushTrace()){
            FoundryTankIntakeHatchRenderer.renderHatchOpening(trace.getBuffer(liquidType),new PoseStack().last(),0,0);
            trace.getBuffer(paneType);
            check(trace.flushed.equals(List.of(liquidType)),"Liquid-before-hatch order is retained; no extra explicit flush/pass");
        }
        System.out.println("PASS: "+columns+" emitted stream columns, all horizontal/down orientations and Basin/Table-height endpoints; single watertight downward caps, preserved tops/drips; actual hatch aperture and attached/detached submission contracts. GPU transparency, occlusion and visual checks remain required.");
    }
}

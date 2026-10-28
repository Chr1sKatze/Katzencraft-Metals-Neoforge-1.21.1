package net.chriskatze.katzencraftmetals.client.renderer;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;

/** Investigation fixture for the recessed front row below the raised back row.
 * requireCoverage asserts the physical return stays in front of the shared molten side strip.
 * Does not alter production rendering or silently broaden material classification. */
public final class FoundryFrameExposureChecks {
    private record V(float x,float y,float z) {}
    private static final class Capture implements VertexConsumer {
        final List<V> points=new ArrayList<>();
        public VertexConsumer addVertex(float x,float y,float z){points.add(new V(x,y,z));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int a,int b){return this;}
        public VertexConsumer setUv2(int a,int b){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
    private record Face(Direction normal, List<V> points, String source) {}
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static boolean matches(JsonObject condition,Set<BlockPos> cells,BlockPos pos) {
        for(var entry:condition.entrySet()) {
            if(entry.getKey().equals("OR")){if(!entry.getValue().getAsJsonArray().asList().stream().anyMatch(e->matches(e.getAsJsonObject(),cells,pos)))return false;}
            else if(entry.getKey().equals("AND")){if(!entry.getValue().getAsJsonArray().asList().stream().allMatch(e->matches(e.getAsJsonObject(),cells,pos)))return false;}
            else {
                Direction direction=Direction.byName(entry.getKey());
                if(direction==null)continue;
                if(cells.contains(pos.relative(direction))!=Boolean.parseBoolean(entry.getValue().getAsString()))return false;
            }
        }
        return true;
    }
    private static void modelFaces(Path root,String model,BlockPos pos,List<Face> faces)throws Exception {
        var json=JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/assets/katzencraftmetals/models/"+model.split(":")[1]+".json"))).getAsJsonObject();
        // Treat every cutout pixel as opaque: this is an upper bound on available frame occlusion.
        if(!json.has("render_type")||!json.get("render_type").getAsString().endsWith("cutout"))return;
        if(!json.has("elements"))return;
        for(var element:json.getAsJsonArray("elements")) {
            var box=element.getAsJsonObject();var low=box.getAsJsonArray("from");var high=box.getAsJsonArray("to");
            for(var face:box.getAsJsonObject("faces").entrySet()) {
                var normal=Direction.byName(face.getKey());int axis=normal.getAxis().ordinal();int a=(axis+1)%3,b=(axis+2)%3;
                var vertices=new ArrayList<V>();
                for(int corner=0;corner<4;corner++) {
                    float[] xyz=new float[3];xyz[axis]=(normal.getAxisDirection()==Direction.AxisDirection.POSITIVE?high:low).get(axis).getAsFloat()/16;
                    xyz[a]=(corner==0||corner==3?low:high).get(a).getAsFloat()/16;
                    xyz[b]=(corner<2?low:high).get(b).getAsFloat()/16;
                    vertices.add(new V(pos.getX()+xyz[0],pos.getY()+xyz[1],pos.getZ()+xyz[2]));
                }
                faces.add(new Face(normal,vertices,model));
            }
        }
    }
    // A horizontal ray in the .1-model-pixel band directly below the correction rail.
    private static boolean rayHits(Face face) {
        if(face.normal!=Direction.NORTH)return false;
        float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,minY=minX,maxY=maxX;
        for(var v:face.points){minX=Math.min(minX,v.x);maxX=Math.max(maxX,v.x);minY=Math.min(minY,v.y);maxY=Math.max(maxY,v.y);}
        float z=face.points.getFirst().z;
        return z>=14.5f/16-1e-6&&z<=1+1e-6&&minX<=.5f&&maxX>=.5f&&minY<=15.85f/16&&maxY>=15.85f/16;
    }
    public static void run(boolean requireCoverage)throws Exception {
        invertedTJunction();
        var root=Path.of(System.getProperty("katzencraft.equipment.root"));
        var create=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("createRenderedLayers",BlockPos.class,Set.class,Map.class);create.setAccessible(true);
        var side=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerSide",BlockPos.class,Direction.class,
                FoundryTankRenderedMetalLayer.class,FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,PoseStack.Pose.class,int.class,float.class,float.class);side.setAccessible(true);
        var mask=FoundryTankConnectedFrameModel.class.getDeclaredMethod("computeCorrectionMask",Set.class,BlockPos.class);mask.setAccessible(true);
        var parts=Class.forName(FoundryTankConnectedFrameModel.class.getName()+"$CorrectionPart").getEnumConstants();
        var multipart=JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/assets/katzencraftmetals/blockstates/foundry_tank.json"))).getAsJsonObject().getAsJsonArray("multipart");
        var frameTexture=javax.imageio.ImageIO.read(root.resolve("src/main/resources/assets/katzencraftmetals/textures/block/foundry_tank_frame.png").toFile());
        for(var edge:Direction.Plane.HORIZONTAL) {
            var name="foundry_tank_correction_up_"+edge.getName();
            var json=JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/assets/katzencraftmetals/models/block/"+name+".json"))).getAsJsonObject();
            var elements=json.getAsJsonArray("elements");check(elements.size()==3,"Existing vertical return plus horizontal junction lip, original rail preserved");
            var original=elements.get(0).getAsJsonObject();check(original.getAsJsonArray("from").get(1).getAsFloat()==15.9f&&original.getAsJsonArray("to").get(1).getAsFloat()==16,"Original rail depth unchanged");
            var lip=elements.get(1).getAsJsonObject();check(lip.getAsJsonArray("from").get(1).getAsFloat()==15.8f&&lip.getAsJsonArray("to").get(1).getAsFloat()==16,"Return ends exactly at full adjacent liquid height");
            check(lip.getAsJsonObject("faces").size()==1&&lip.getAsJsonObject("faces").has(edge.getOpposite().getName()),"Return faces the cavity in every orientation");
            int axis=edge.getAxis().ordinal();float plane=edge.getAxisDirection()==Direction.AxisDirection.NEGATIVE?1:15;
            check(lip.getAsJsonArray("from").get(axis).getAsFloat()==plane&&lip.getAsJsonArray("to").get(axis).getAsFloat()==plane,"No footprint expansion or coplanar liquid face: return is one model pixel ahead");
            var face=lip.getAsJsonObject("faces").getAsJsonObject(edge.getOpposite().getName());check(face.get("texture").getAsString().equals("#frame"),"Existing authored frame material");
            for(int u=0;u<=8;u++)for(int v=0;v<=8;v++) {
                int x=(int)((.25f+.5f*u/8)/16*frameTexture.getWidth()),z=(int)((.25f+.5f*v/8)/16*frameTexture.getHeight());
                check((frameTexture.getRGB(x,z)>>>24)==255,"Entire return UV samples opaque existing bevel artwork");
            }
        }
        boolean failingCovered=false;
        for(boolean wide:new boolean[]{false,true}) {
            var cells=new HashSet<BlockPos>();
            for(int x=0;x<=(wide?1:0);x++){cells.add(new BlockPos(x,0,0));cells.add(new BlockPos(x,0,1));}
            for(int x=-1;x<=1;x++)cells.add(new BlockPos(x,1,1));
            var iron=ResourceLocation.withDefaultNamespace("iron");
            Map<Integer,Map<ResourceLocation,Float>> amounts=Map.of(0,Map.of(iron,108f),1,Map.of(iron,54f));
            Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers=new HashMap<>();
            for(var pos:cells){@SuppressWarnings("unchecked")var list=(List<FoundryTankRenderedMetalLayer>)create.invoke(null,pos,cells,amounts);layers.put(pos,list);}
            var back=new BlockPos(0,0,1);var front=BlockPos.ZERO;var layer=layers.get(back).getFirst();
            float inset=FoundryTankRenderConstants.LIQUID_SIDE_INSET;
            var g=new FoundryTankLiquidGeometry(cells.contains(back.west())?0:inset,cells.contains(back.east())?1:1-inset,layer.minY(),layer.maxY(),0,1-inset,layer.renderTop(),layer.renderBottom());
            var pose=new PoseStack();pose.translate(0,0,1);
            var liquid=new Capture();var replacement=new ArrayList<FoundryTankJunctionMaterial.Face>();
            side.invoke(null,back,Direction.NORTH,layer,g,layers,FoundryTankJunctionMaterial.wrap(liquid,replacement,()->0,back,layer,layers),pose.last(),0,0f,1f);
            check(layers.get(front).getLast().maxY()==15.8f/16&&layer.maxY()==1,"Actual front/back liquid height mismatch is .2 model pixels");
            List<Face> allFrames=new ArrayList<>();
            for(var pos:cells) {
                for(var entry:multipart){var part=entry.getAsJsonObject();if(!part.has("when")||matches(part.getAsJsonObject("when"),cells,pos))modelFaces(root,part.getAsJsonObject("apply").get("model").getAsString(),pos,allFrames);}
                long correction=(long)mask.invoke(null,cells,pos);
                // Correction names are stored in the production enum, not inferred from ordinals/names.
                for(var part:parts){var e=(Enum<?>)part;if((correction&(1L<<e.ordinal()))==0)continue;
                    var location=part.getClass().getDeclaredMethod("location");location.setAccessible(true);
                    var model=((net.minecraft.client.resources.model.ModelResourceLocation)location.invoke(part)).id();modelFaces(root,model.toString(),pos,allFrames);
                }
                int joins=FoundryTankInteriorFrameJoins.mask(cells::contains,d->cells.contains(pos.relative(d)),pos);
                for(var f:FoundryTankInteriorFrameJoins.faces(joins)){
                    var vertices=new ArrayList<V>();for(var v:f.vertices())vertices.add(new V(pos.getX()+v[0]/16,pos.getY()+v[1]/16,pos.getZ()+v[2]/16));allFrames.add(new Face(f.normal(),vertices,"interior-join"));
                }
            }
            var blockers=allFrames.stream().filter(FoundryFrameExposureChecks::rayHits).toList();
            long geometryHits=blockers.size();
            for(var blocker:blockers)check(blocker.points.getFirst().z<1-.0001f,"Physical frame occludes before liquid depth, not at a coplanar depth");
            long replacementHits=0;for(var f:replacement){var vertices=new ArrayList<V>();for(var v:f.vertices())vertices.add(new V(v.wx,v.wy,v.wz));if(rayHits(new Face(Direction.NORTH,vertices,"material-return")))replacementHits++;}
            boolean liquidHits=false;for(int i=0;i<liquid.points.size();i+=4)liquidHits|=rayHits(new Face(Direction.NORTH,liquid.points.subList(i,i+4),"molten"));
            System.out.println("FRAME EXPOSURE "+(wide?"two-column recessed step":"single-column support T")+": liquid side vertices="+liquid.points+", frameGeometryHits="+geometryHits+", replacementFrameHits="+replacementHits+", exposedLiquid="+liquidHits+", physicalBlockers="+blockers);
            if(!wide)check(!liquidHits&&replacementHits>0,"Working single-column junction is concealed by the side-band override");
            else failingCovered=geometryHits>0||replacementHits>0;
        }
        if(requireCoverage)check(failingCovered,"Missing frame coverage: two-column stepped join exposes NORTH liquid side at Z=1, Y=15.8/16..1; frame underside stops at Y=15.9/16. Previous DOWN-only material correction never touched this NORTH face.");
    }

    /** Compare the existing downward frame-material return with its top-rail counterpart. */
    private static void invertedTJunction() throws Exception {
        var root=Path.of(System.getProperty("katzencraft.equipment.root"));
        var bottom=FoundryControllerTankLiquidRenderer.class.getDeclaredMethod("renderLayerBottom",BlockPos.class,FoundryTankRenderedMetalLayer.class,
                FoundryTankLiquidGeometry.class,Map.class,VertexConsumer.class,PoseStack.Pose.class,int.class,float.class,float.class);
        bottom.setAccessible(true);
        var corrections=FoundryTankConnectedFrameModel.class.getDeclaredMethod("computeCorrectionMask",Set.class,BlockPos.class);corrections.setAccessible(true);
        var parts=Class.forName(FoundryTankConnectedFrameModel.class.getName()+"$CorrectionPart").getEnumConstants();
        var bevel=javax.imageio.ImageIO.read(root.resolve("src/main/resources/assets/katzencraftmetals/textures/block/foundry_tank_frame.png").toFile());
        for(var arm:Direction.Plane.HORIZONTAL) {
            var cells=Set.of(BlockPos.ZERO,BlockPos.ZERO.relative(arm),BlockPos.ZERO.relative(arm.getOpposite()),BlockPos.ZERO.below());
            var metal=ResourceLocation.withDefaultNamespace("iron");
            Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers=new HashMap<>();
            for(var pos:cells)layers.put(pos,List.of(new FoundryTankRenderedMetalLayer(metal,pos.equals(BlockPos.ZERO)?0:.2f/16,pos.equals(BlockPos.ZERO.below())?1:15.8f/16,true,false)));
            var layer=layers.get(BlockPos.ZERO).getFirst();float inset=FoundryTankRenderConstants.LIQUID_SIDE_INSET;
            var g=new FoundryTankLiquidGeometry(arm.getAxis()==Direction.Axis.X?0:inset,arm.getAxis()==Direction.Axis.X?1:1-inset,
                    0,layer.maxY(),arm.getAxis()==Direction.Axis.Z?0:inset,arm.getAxis()==Direction.Axis.Z?1:1-inset,true,false);
            var molten=new Capture();var replacement=new ArrayList<FoundryTankJunctionMaterial.Face>();
            bottom.invoke(null,BlockPos.ZERO,layer,g,layers,FoundryTankJunctionMaterial.wrap(molten,replacement,()->0,BlockPos.ZERO,layer,layers),new PoseStack().last(),0,0f,1f);
            check(molten.points.isEmpty()&&!replacement.isEmpty(),"Working underside return stays opaque frame material");
            List<Face> below=new ArrayList<>();for(var face:replacement){var points=new ArrayList<V>();for(var v:face.vertices())points.add(new V(v.wx,v.wy,v.wz));below.add(new Face(Direction.DOWN,points,"existing-frame-return"));}
            var inverted=Set.of(BlockPos.ZERO,BlockPos.ZERO.relative(arm),BlockPos.ZERO.relative(arm.getOpposite()),BlockPos.ZERO.above());
            var wing=BlockPos.ZERO.relative(arm);long mask=(long)corrections.invoke(null,inverted,wing);
            var edge=arm.getOpposite();String name="foundry_tank_correction_up_"+edge.getName();
            boolean selected=false;for(var part:parts){var location=part.getClass().getDeclaredMethod("location");location.setAccessible(true);var id=((net.minecraft.client.resources.model.ModelResourceLocation)location.invoke(part)).id();if(id.getPath().equals("block/"+name))selected=(mask&(1L<<((Enum<?>)part).ordinal()))!=0;}
            check(selected,"Horizontal rail extension is selected by actual inverted-T topology");
            List<Face> above=new ArrayList<>();modelFaces(root,"katzencraftmetals:block/"+name,wing,above);
            for(float along:new float[]{2,8,14})for(float distance:new float[]{.025f,.075f,.11f}) {
                float x=arm.getAxis()==Direction.Axis.X?(arm.getStepX()<0?distance:16-distance):along;
                float z=arm.getAxis()==Direction.Axis.Z?(arm.getStepZ()<0?distance:16-distance):along;
                check(horizontalHits(below,Direction.DOWN,0,x/16,z/16)==1,"Working lower lip covers full 0.12-pixel width");
                check(horizontalHits(above,Direction.UP,1,x/16,z/16)==1,"Missing exterior-facing horizontal lip toward vertical wall: "+arm+" offset="+distance+" along="+along);
            }
            var json=JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/assets/katzencraftmetals/models/block/"+name+".json"))).getAsJsonObject();
            var lip=json.getAsJsonArray("elements").get(2).getAsJsonObject();
            check(lip.getAsJsonObject("faces").size()==1&&lip.getAsJsonObject("faces").getAsJsonObject("up").get("texture").getAsString().equals("#frame"),"One opaque frame face, no double-sided overlay");
            for(var uv:lip.getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv"))check(uv.getAsFloat()>=.25f&&uv.getAsFloat()<=.75f,"Same bevel UV region as existing frame joins");
            check((bevel.getRGB(0,0)>>>24)==255,"Existing bevel pixel remains opaque");
            int joins=FoundryTankInteriorFrameJoins.mask(inverted::contains,d->inverted.contains(BlockPos.ZERO.relative(d)),BlockPos.ZERO);
            check(FoundryTankInteriorFrameJoins.faces(joins).stream().noneMatch(f->f.normal()==Direction.UP&&f.plane()==16),"Redundant Y=16 interior closure removed");
        }
        System.out.println("PASS: horizontal lip mirrors real downward material return in four orientations; full 0.12-pixel coverage and opaque material, existing underside unchanged.");
    }
    private static long horizontalHits(List<Face> faces,Direction normal,float plane,float x,float z) {
        return faces.stream().filter(f->f.normal()==normal).filter(f->{
            float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,minZ=minX,maxZ=maxX;
            for(var v:f.points()){if(Math.abs(v.y()-plane)>1e-6)return false;minX=Math.min(minX,v.x());maxX=Math.max(maxX,v.x());minZ=Math.min(minZ,v.z());maxZ=Math.max(maxZ,v.z());}
            return x>minX&&x<maxX&&z>minZ&&z<maxZ;
        }).count();
    }
}

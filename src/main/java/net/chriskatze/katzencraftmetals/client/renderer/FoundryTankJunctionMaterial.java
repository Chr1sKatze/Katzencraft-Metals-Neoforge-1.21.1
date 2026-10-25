package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Routes existing junction extension geometry once, using frame or liquid material. */
final class FoundryTankJunctionMaterial implements VertexConsumer {
    private final VertexConsumer liquid;
    private final List<Face> frame;
    private final boolean bottomBand, topBand, undersideReturn;
    private final int cornerReturns, lowerSteps, upperSteps;
    private final Footprint below, above;

    private record Footprint(float minX, float maxX, float minZ, float maxZ) {
        static Footprint at(BlockPos pos, Map<BlockPos, ?> cells) {
            float inset = FoundryTankRenderConstants.LIQUID_SIDE_INSET;
            return new Footprint(cells.containsKey(pos.west()) ? 0 : inset,
                    cells.containsKey(pos.east()) ? 1 : 1 - inset,
                    cells.containsKey(pos.north()) ? 0 : inset,
                    cells.containsKey(pos.south()) ? 1 : 1 - inset);
        }

        boolean samePlane(Vertex v) {
            float plane = v.nx < -.5f ? minX : v.nx > .5f ? maxX : v.nz < 0 ? minZ : maxZ;
            return Math.abs(plane - (Math.abs(v.nx) > .5f ? v.x : v.z))
                    < FoundryTankRenderConstants.LIQUID_EPSILON;
        }

        float min(boolean alongX) { return alongX ? minX : minZ; }
        float max(boolean alongX) { return alongX ? maxX : maxZ; }
    }
    private final int frameLight;
    private final Vertex[] vertices = new Vertex[4];
    private int count;

    record Face(Vertex[] vertices, boolean bottom, int light) {}
    static final class Vertex {
        float x,y,z,wx,wy,wz,u,v,nx,ny,nz;
        int r,g,b,a,o1,o2,l1,l2;
        Vertex copy() {
            Vertex n = new Vertex(); n.x=x;n.y=y;n.z=z;n.wx=wx;n.wy=wy;n.wz=wz;
            n.u=u;n.v=v;n.nx=nx;n.ny=ny;n.nz=nz;n.r=r;n.g=g;n.b=b;n.a=a;
            n.o1=o1;n.o2=o2;n.l1=l1;n.l2=l2;return n;
        }
    }

    static VertexConsumer wrap(VertexConsumer liquid, List<Face> frame, java.util.function.IntSupplier light,
                               BlockPos pos, FoundryTankRenderedMetalLayer layer,
                               Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers) {
        Route route = route(pos, layer, layers);
        return route == null ? liquid : new FoundryTankJunctionMaterial(liquid, frame, light.getAsInt(),
                route.bottom(), route.top(), route.underside(), route.corners(), route.lowerSteps(), route.upperSteps(), route.below(), route.above());
    }

    record Route(boolean bottom, boolean top, boolean underside, int corners, int lowerSteps, int upperSteps, Footprint below, Footprint above) {}

    /** Geometry-only classification; rebuilt with the exact smoothed layer layout. */
    static Route route(BlockPos pos, FoundryTankRenderedMetalLayer layer,
                       Map<BlockPos,List<FoundryTankRenderedMetalLayer>> layers) {
        List<FoundryTankRenderedMetalLayer> below=layers.getOrDefault(pos.below(),List.of());
        List<FoundryTankRenderedMetalLayer> above=layers.getOrDefault(pos.above(),List.of());
        boolean joinedBelow=layer.minY()<=FoundryTankRenderConstants.LIQUID_EPSILON && !below.isEmpty()
                && below.getLast().maxY()>=1-FoundryTankRenderConstants.LIQUID_EPSILON;
        boolean bottom=joinedBelow && isSupportedCrossbar(pos,pos.below(),layers);
        // The gap-repair underside outside the support footprint lies on the frame return.
        // Its ownership does not require a two-ended T: one-sided steps and wider supports
        // have the same narrow return. Side bands keep the stricter crossbar rule below.
        boolean underside=joinedBelow && hasUndersideReturn(pos,layers);
        boolean top=layer.maxY()>=1-FoundryTankRenderConstants.LIQUID_EPSILON && !above.isEmpty()
                && above.getFirst().minY()<=FoundryTankRenderConstants.LIQUID_EPSILON
                && isSupportedCrossbar(pos.above(),pos,layers);
        int corners=0, lowerSteps=0, upperSteps=0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor=pos.relative(side);
            var adjacent=layers.get(neighbor);
            if (adjacent==null || adjacent.isEmpty()) continue;
            Direction low=side.getAxis()==Direction.Axis.Z?Direction.WEST:Direction.NORTH;
            Direction high=low.getOpposite();
            int shift=side.get2DDataValue()*2;
            if (layers.containsKey(pos.relative(low)) && !layers.containsKey(neighbor.relative(low))) corners|=1<<shift;
            if (layers.containsKey(pos.relative(high)) && !layers.containsKey(neighbor.relative(high))) corners|=2<<shift;
            // Only restored floor/ceiling headroom at a real structural step.
            // Ordinary layer boundaries and exterior walls never satisfy these conditions.
            if (layer.maxY()>=1-FoundryTankRenderConstants.LIQUID_EPSILON && !above.isEmpty()
                    && !layers.containsKey(neighbor.above())
                    && Math.abs(adjacent.getLast().maxY()-FoundryTankRenderConstants.LIQUID_MAX_INSET)<FoundryTankRenderConstants.LIQUID_EPSILON)
                upperSteps|=1<<side.get2DDataValue();
            if (layer.minY()<=FoundryTankRenderConstants.LIQUID_EPSILON && !below.isEmpty()
                    && !layers.containsKey(neighbor.below())
                    && Math.abs(adjacent.getFirst().minY()-FoundryTankRenderConstants.LIQUID_INSET)<FoundryTankRenderConstants.LIQUID_EPSILON)
                lowerSteps|=1<<side.get2DDataValue();
        }
        return bottom || top || underside || corners!=0 || lowerSteps!=0 || upperSteps!=0
                ? new Route(bottom, top, underside, corners, lowerSteps, upperSteps, Footprint.at(pos.below(), layers),
                Footprint.at(pos.above(), layers)) : null;
    }

    /** No captured supplier allocation on the common non-junction path. Lighting stays live. */
    static VertexConsumer wrap(VertexConsumer liquid, List<Face> frame, Route route,
                               net.minecraft.world.level.Level level, BlockPos pos) {
        return route == null ? liquid : new FoundryTankJunctionMaterial(liquid, frame,
                net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos),
                route.bottom(), route.top(), route.underside(), route.corners(), route.lowerSteps(), route.upperSteps(), route.below(), route.above());
    }
    private static boolean hasUndersideReturn(BlockPos pos, Map<BlockPos, ?> cells) {
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (cells.containsKey(pos.relative(direction)) && !cells.containsKey(pos.below().relative(direction))) return true;
        }
        return false;
    }

    private static boolean isSupportedCrossbar(BlockPos bridge, BlockPos support, Map<BlockPos,?> cells) {
        // These keys are the Controller's connected structure, not world occupancy.
        // A footprint difference alone also matches ordinary walls with a step
        // behind them. Require a crossbar beyond BOTH ends of its support run.
        for (var positive : List.of(net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH)) {
            var negative = positive.getOpposite();
            boolean sameNegative = cells.containsKey(bridge.relative(negative)) == cells.containsKey(support.relative(negative));
            boolean samePositive = cells.containsKey(bridge.relative(positive)) == cells.containsKey(support.relative(positive));
            if (sameNegative && samePositive) continue;
            int bridgeMin = runLength(bridge, negative, cells), bridgeMax = runLength(bridge, positive, cells);
            int supportMin = runLength(support, negative, cells), supportMax = runLength(support, positive, cells);
            if (bridgeMin > supportMin && bridgeMax > supportMax) return true;
        }
        return false;
    }
    private static int runLength(BlockPos start, net.minecraft.core.Direction direction, Map<BlockPos,?> cells) {
        int length = 0;
        while (cells.containsKey(start.relative(direction, length + 1))) length++;
        return length;
    }
    private FoundryTankJunctionMaterial(VertexConsumer liquid,List<Face> frame,int light,boolean bottom,boolean top,boolean underside,int corners,int lower,int upper,
                                       Footprint below,Footprint above) {
        this.liquid=liquid;this.frame=frame;this.frameLight=light;bottomBand=bottom;topBand=top;undersideReturn=underside;
        this.below=below;this.above=above;cornerReturns=corners;lowerSteps=lower;upperSteps=upper;
    }
    private Vertex current(){return vertices[count-1];}
    @Override public VertexConsumer addVertex(float x,float y,float z){Vertex v=new Vertex();v.x=v.wx=x;v.y=v.wy=y;v.z=v.wz=z;vertices[count++]=v;return this;}
    @Override public VertexConsumer addVertex(Matrix4f pose,float x,float y,float z){
        addVertex(x,y,z);Vector3f world=new Vector3f(x,y,z).mulPosition(pose);current().wx=world.x;current().wy=world.y;current().wz=world.z;return this;
    }
    @Override public VertexConsumer setColor(int r,int g,int b,int a){Vertex v=current();v.r=r;v.g=g;v.b=b;v.a=a;return this;}
    @Override public VertexConsumer setUv(float u,float v){current().u=u;current().v=v;return this;}
    @Override public VertexConsumer setUv1(int u,int v){current().o1=u;current().o2=v;return this;}
    @Override public VertexConsumer setUv2(int u,int v){current().l1=u;current().l2=v;return this;}
    @Override public VertexConsumer setNormal(float x,float y,float z){current().nx=x;current().ny=y;current().nz=z;if(count==4){route();count=0;}return this;}

    private void route(){
        if(vertices[0].ny<-.5f){
            if((bottomBand || undersideReturn) && vertices[0].y<=FoundryTankRenderConstants.LIQUID_EPSILON)frame.add(new Face(copy(),true,frameLight));
            else emit(liquid,vertices);return;
        }
        if(Math.abs(vertices[0].ny)>.5f){emit(liquid,vertices);return;}
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(Vertex v:vertices){min=Math.min(min,v.y);max=Math.max(max,v.y);}
        if (isSharedReturn(min,max)) { frame.add(new Face(copy(),false,frameLight)); return; }
        // Only the extension beyond an ordinary floor/ceiling inset is classified.
        float inset=FoundryTankRenderConstants.LIQUID_INSET;
        TreeSet<Float> cuts=new TreeSet<>();cuts.add(min);cuts.add(max);
        if(bottomBand&&min<inset&&inset<max)cuts.add(inset);
        if(topBand&&min<1-inset&&1-inset<max)cuts.add(1-inset);
        Float previous=null;
        for(float end:cuts){if(previous!=null&&end>previous){
            float middle=(previous+end)*.5f;Vertex[] piece=slice(previous,end,min,max);
            if(bottomBand&&middle<inset) routeSideBand(piece,below);
            else if(topBand&&middle>1-inset) routeSideBand(piece,above);
            else emit(liquid,piece);
        }previous=end;}
    }
    /** Match the actual corner/height-return quads emitted by renderLayerSide, not a generic seam. */
    private boolean isSharedReturn(float minY,float maxY) {
        Vertex first=vertices[0];
        Direction side=first.nx<-.5f?Direction.WEST:first.nx>.5f?Direction.EAST:first.nz<0?Direction.NORTH:Direction.SOUTH;
        float plane=side.getAxis()==Direction.Axis.X?first.x:first.z;
        float expected=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE?0:1;
        float epsilon=FoundryTankRenderConstants.LIQUID_EPSILON;
        if (Math.abs(plane-expected)>epsilon) return false;
        int bit=1<<side.get2DDataValue();
        if ((upperSteps&bit)!=0 && minY>=FoundryTankRenderConstants.LIQUID_MAX_INSET-epsilon) return true;
        if ((lowerSteps&bit)!=0 && maxY<=FoundryTankRenderConstants.LIQUID_INSET+epsilon) return true;
        int corners=(cornerReturns>>(side.get2DDataValue()*2))&3;
        if (corners==0) return false;
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for (Vertex v:vertices) {
            float c=side.getAxis()==Direction.Axis.Z?v.x:v.z;min=Math.min(min,c);max=Math.max(max,c);
        }
        return ((corners&1)!=0 && max<=FoundryTankRenderConstants.LIQUID_SIDE_INSET+epsilon)
                || ((corners&2)!=0 && min>=FoundryTankRenderConstants.LIQUID_SIDE_MAX-epsilon);
    }

    /** Only the return outside the adjoining wall footprint belongs to the frame.
     * The coincident wall interval remains liquid, even at a structural T-junction. */
    private void routeSideBand(Vertex[] piece, Footprint adjoining) {
        Vertex first=piece[0];
        if(!adjoining.samePlane(first)) {
            frame.add(new Face(piece,false,frameLight));
            return;
        }
        boolean alongX=Math.abs(first.nz)>.5f;
        float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(Vertex v:piece){float coordinate=alongX?v.x:v.z;min=Math.min(min,coordinate);max=Math.max(max,coordinate);}
        TreeSet<Float> cuts=new TreeSet<>();cuts.add(min);cuts.add(max);
        float coveredMin=adjoining.min(alongX),coveredMax=adjoining.max(alongX);
        if(min<coveredMin&&coveredMin<max)cuts.add(coveredMin);
        if(min<coveredMax&&coveredMax<max)cuts.add(coveredMax);
        Float previous=null;
        for(float end:cuts){if(previous!=null&&end>previous){
            Vertex[] strip=new Vertex[4];
            for(int i=0;i<4;i++){
                Vertex v=piece[i],other=null;
                float coordinate=alongX?v.x:v.z;
                for(Vertex candidate:piece)if(candidate.y==v.y&&(alongX?candidate.x:candidate.z)!=coordinate){other=candidate;break;}
                if(other==null)throw new IllegalStateException("Expected axis-aligned liquid band");
                float edge=coordinate==min?previous:end;
                float t=(edge-coordinate)/((alongX?other.x:other.z)-coordinate);
                Vertex n=v.copy();strip[i]=n;
                n.x=v.x+(other.x-v.x)*t;n.z=v.z+(other.z-v.z)*t;
                n.wx=v.wx+(other.wx-v.wx)*t;n.wy=v.wy+(other.wy-v.wy)*t;n.wz=v.wz+(other.wz-v.wz)*t;
                n.u=v.u+(other.u-v.u)*t;n.v=v.v+(other.v-v.v)*t;
            }
            float middle=(previous+end)*.5f;
            if(middle<coveredMin||middle>coveredMax)frame.add(new Face(strip,false,frameLight));
            else emit(liquid,strip);
        }previous=end;}
    }
    private Vertex[] copy(){Vertex[] result=new Vertex[4];for(int i=0;i<4;i++)result[i]=vertices[i].copy();return result;}
    private Vertex[] slice(float min,float max,float oldMin,float oldMax){
        Vertex[] result=copy();
        if(min==oldMin&&max==oldMax)return result;
        for(int i=0;i<4;i++){
            Vertex v=vertices[i],other=null;
            for(Vertex candidate:vertices)if(candidate.x==v.x&&candidate.z==v.z&&candidate.y!=v.y){other=candidate;break;}
            if(other==null)throw new IllegalStateException("Expected axis-aligned liquid side");
            float y=v.y==oldMin?min:max,t=(y-v.y)/(other.y-v.y);Vertex n=result[i];n.y=y;
            n.wx=v.wx+(other.wx-v.wx)*t;n.wy=v.wy+(other.wy-v.wy)*t;n.wz=v.wz+(other.wz-v.wz)*t;
            n.u=v.u+(other.u-v.u)*t;n.v=v.v+(other.v-v.v)*t;
        }return result;
    }
    private static void emit(VertexConsumer consumer,Vertex[] vertices){
        for(Vertex v:vertices)consumer.addVertex(v.wx,v.wy,v.wz).setColor(v.r,v.g,v.b,v.a).setUv(v.u,v.v)
                .setUv1(v.o1,v.o2).setUv2(v.l1,v.l2).setNormal(v.nx,v.ny,v.nz);
    }
    static void emitFrame(VertexConsumer consumer,Face face){
        for(Vertex v:face.vertices()){
            float u=Math.clamp(v.u,.5f/16,15.5f/16);
            float textureV=face.bottom()?1-v.z:1-v.y;
            textureV=Math.clamp(textureV,.5f/16,15.5f/16);
            consumer.addVertex(v.wx,v.wy,v.wz).setColor(255,255,255,255).setUv(u,textureV)
                    .setOverlay(v.o1|(v.o2<<16)).setLight(face.light()).setNormal(v.nx,v.ny,v.nz);
        }
    }
}

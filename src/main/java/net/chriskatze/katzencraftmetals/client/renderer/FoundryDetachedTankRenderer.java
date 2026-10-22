package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.client.FoundryDetachedTankCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.*;

/** One existing liquid renderer per detached vessel; no fake Controller or second inventory. */
@EventBusSubscriber(modid="katzencraftmetals",value=Dist.CLIENT)
public final class FoundryDetachedTankRenderer {
    private static final Set<BlockPos> ACTIVE=new HashSet<>();
    private static final Map<UUID,FoundryControllerTankLiquidRenderer> RENDERERS=new HashMap<>();
    public static void markActive(Set<BlockPos> positions){ACTIVE.addAll(positions);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel().isClientSide())clear();}
    private static void clear(){FoundryDetachedTankCache.INSTANCE.clear();RENDERERS.clear();ACTIVE.clear();}
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES){ACTIVE.clear();return;}
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var minecraft=Minecraft.getInstance();var level=minecraft.level;if(level==null)return;
        var views=FoundryDetachedTankCache.INSTANCE.views(level.dimension().location());RENDERERS.keySet().retainAll(views.keySet());
        var camera=event.getCamera().getPosition();
        for(var entry:views.entrySet()) {
            var snapshot=entry.getValue();if(snapshot.contents().isEmpty()||!Collections.disjoint(ACTIVE,snapshot.positions()))continue;
            var visible=new HashSet<BlockPos>();
            for(var p:snapshot.positions())if(level.hasChunkAt(p)&&level.getBlockState(p).is(ModBlocks.FOUNDRY_TANK.get()))visible.add(p);
            if(visible.isEmpty())continue;
            AABB bounds=null;for(var p:visible)bounds=bounds==null?new AABB(p):bounds.minmax(new AABB(p));
            if(!event.getFrustum().isVisible(bounds)||bounds.getCenter().distanceToSqr(camera)>320.0*320.0)continue;
            var origin=snapshot.renderOrigin();var pose=event.getPoseStack();pose.pushPose();pose.translate(origin.getX()-camera.x,origin.getY()-camera.y,origin.getZ()-camera.z);
            RENDERERS.computeIfAbsent(entry.getKey(),id->new FoundryControllerTankLiquidRenderer()).render(entry.getKey(),level,origin,snapshot.contents(),snapshot.positions(),Set.copyOf(visible),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false),pose,minecraft.renderBuffers().bufferSource(),OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}

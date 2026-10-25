package net.chriskatze.katzencraftmetals.block.entity;

import com.mojang.authlib.GameProfile;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock;
import net.chriskatze.katzencraftmetals.event.FoundryExplosionProtection;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;

/** Real bucket exchanges, state transitions and explosion-event filtering in the shared map-world fixture. */
public final class FoundryBasinWaterChecks {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static final class BucketPlayer extends Player {
        ItemStack hand;
        Inventory inventory;
        boolean creative;
        private BucketPlayer() { super(null, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "bucket-test")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return creative; }
        @Override public boolean hasInfiniteMaterials() { return creative; }
        @Override public Inventory getInventory() { return inventory; }
        @Override public ItemStack getItemInHand(InteractionHand h) { return hand; }
        @Override public void setItemInHand(InteractionHand h, ItemStack item) { hand=item; }
    }
    // Keep unrelated accessories-mod inventory hooks out of this headless bucket fixture.
    static final class BucketInventory extends Inventory {
        BucketInventory(Player player) { super(player); }
        @Override public boolean contains(ItemStack stack) { return items.stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, stack)); }
        @Override public boolean add(ItemStack stack) { int slot=getFreeSlot();if(slot<0)return false;items.set(slot,stack.copy());stack.setCount(0);return true; }
    }
    public static void run(HolderLookup.Provider lookup) throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)field.get(null);
        var method=FoundryBasinBlock.class.getDeclaredMethod("useItemOn",ItemStack.class,BlockState.class,Level.class,
                BlockPos.class,Player.class,InteractionHand.class,BlockHitResult.class);method.setAccessible(true);
        var block=ModBlocks.FOUNDRY_BASIN.get();var pos=BlockPos.ZERO;var iron=FoundryDismantlingChecks.id("iron");
        check(!ModBlocks.FOUNDRY_TABLE.get().defaultBlockState().hasProperty(FoundryBasinBlock.CONTAINED_WATER), "Table state definition unchanged");
        for(boolean creative:new boolean[]{false,true})for(var fluid:new net.minecraft.world.level.material.Fluid[]{Fluids.WATER,Fluids.LAVA}) {
            var world=FoundryDismantlingChecks.world(lookup);var dry=block.defaultBlockState();world.blocks.put(pos,dry);
            var basin=new FoundryBasinBlockEntity(pos,dry);basin.setLevel(world);world.entities.put(pos,basin);
            var player=(BucketPlayer)unsafe.allocateInstance(BucketPlayer.class);player.creative=creative;player.inventory=new BucketInventory(player);
            var bucket=fluid==Fluids.WATER?Items.WATER_BUCKET:Items.LAVA_BUCKET;
            player.hand=new ItemStack(bucket);
            check(method.invoke(block,player.hand,dry,world,pos,player,InteractionHand.MAIN_HAND,null)==ItemInteractionResult.CONSUME,"Native server bucket interaction consumed once");
            var contained=world.getBlockState(pos);
            check(FoundryBasinBlock.containedFluid(contained)==fluid&&contained.getFluidState().isEmpty()&&world.waterTicks==0,"Contained fluid never spreads");
            check(contained.getLightEmission()==(fluid==Fluids.LAVA?15:0),"Lava emits fifteen, water zero");
            check(player.hand.is(creative?bucket:Items.BUCKET),"Survival exchanges bucket; Creative retains original");
            if(creative)check(player.inventory.contains(new ItemStack(Items.BUCKET)),"Creative receives empty bucket without consuming filled bucket");
            check(basin.insert(iron,1)==0&&basin.getMoltenAmount()==0,"Contained fluid rejects insertion without consumption");
            var saved=NbtUtils.writeBlockState(contained);
            check(NbtUtils.readBlockState(lookup.lookupOrThrow(Registries.BLOCK),saved)==contained,"Contained state survives save/reload");
            check(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(world,pos).getBlockState()==contained,"Contained state synchronized by native block update");
            player.hand=new ItemStack(fluid==Fluids.WATER?Items.LAVA_BUCKET:Items.WATER_BUCKET);
            check(method.invoke(block,player.hand,contained,world,pos,player,InteractionHand.MAIN_HAND,null)==ItemInteractionResult.FAIL&&world.getBlockState(pos)==contained,"Cannot overwrite occupied Basin with another fluid");
            for(var direction:Direction.values()) {
                check(contained.updateShape(direction,Blocks.WATER.defaultBlockState(),world,pos,pos.relative(direction))==contained,"Submersion preserves contents");
            }
            player.hand=new ItemStack(Items.BUCKET);
            method.invoke(block,player.hand,contained,world,pos,player,InteractionHand.MAIN_HAND,null);
            check(!FoundryBasinBlock.hasContainedFluid(world.getBlockState(pos))&&player.hand.is(creative?Items.BUCKET:bucket),"Retrieve correct fluid with correct bucket exchange");
            check(world.getBlockState(pos).getLightEmission()==0,"Retrieving lava extinguishes");
            check(basin.insert(iron,1)==1,"Drained Basin accepts molten metal");
            player.hand=new ItemStack(bucket);
            check(method.invoke(block,player.hand,world.getBlockState(pos),world,pos,player,InteractionHand.MAIN_HAND,null)==ItemInteractionResult.FAIL,"Cannot delete molten metal with a fluid bucket");
            check(basin.getMoltenAmount()==1,"Rejected bucket preserves molten quantity");
        }
        // Old contained-water saves omit the new level/lava fields and must retain a full water bucket.
        var legacy=NbtUtils.writeBlockState(block.defaultBlockState());
        legacy.getCompound("Properties").putString("contained_water","true");
        legacy.getCompound("Properties").remove("contained_lava");legacy.getCompound("Properties").remove("fluid_level");
        var restored=NbtUtils.readBlockState(lookup.lookupOrThrow(Registries.BLOCK),legacy);
        check(FoundryBasinBlock.containedFluid(restored)==Fluids.WATER&&restored.getValue(FoundryBasinBlock.FLUID_LEVEL)==3,"Legacy contained water preserved at full level");
        dripstone(lookup,method,unsafe);
        // Geometry is the actual cached emitted water mesh, compared against every authored solid/window element.
        var facesField=Class.forName("net.chriskatze.katzencraftmetals.client.renderer.FoundryBasinWaterRenderer").getDeclaredField("FACES");facesField.setAccessible(true);
        float[][] faces=(float[][])facesField.get(null);
        var waterRenderer=Class.forName("net.chriskatze.katzencraftmetals.client.renderer.FoundryBasinWaterRenderer");
        var surfaceMethod=waterRenderer.getDeclaredMethod("surfacePixels",BlockState.class);surfaceMethod.setAccessible(true);
        var vertexMethod=waterRenderer.getDeclaredMethod("vertexYPixels",float[].class,int.class,float.class);vertexMethod.setAccessible(true);
        for(int level=1;level<=3;level++) {
            var state=FoundryBasinBlock.withFluid(block.defaultBlockState(),Fluids.WATER,level);
            float surface=(float)surfaceMethod.invoke(null,state);
            check(surface==3+4*level,"Three fluid surface heights in model pixels");
            for(var face:faces)for(int vertex=0;vertex<4;vertex++) {
                float y=(float)vertexMethod.invoke(null,face,vertex,surface);
                check(y>=3&&y<=surface,"Actual emitted partial-water vertices clipped to cavity and surface");
            }
        }
        var returnsField=waterRenderer.getDeclaredField("WINDOW_TOPS");returnsField.setAccessible(true);
        float[][] returns=(float[][])returnsField.get(null);
        check(returns.length==4,"Four partial-water top returns close the recessed slit surfaces");
        for(var face:returns)for(int level=1;level<=2;level++)for(int vertex=0;vertex<4;vertex++) {
            float y=(float)vertexMethod.invoke(null,face,vertex,3f+4f*level);
            check(y==3+4*level&&face[13]==1,"Recessed partial surface stays coplanar and upward facing");
        }
        var root=java.nio.file.Path.of(System.getProperty("katzencraft.equipment.root"));
        var model=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("src/main/resources/assets/katzencraftmetals/models/block/foundry_basin.json"))).getAsJsonObject();
        check(faces.length==5,"Only surface and four recessed windows emitted, no hidden bottom faces");
        for(float[] face:faces) {
            float ax=face[3]-face[0],ay=face[4]-face[1],az=face[5]-face[2];
            float bx=face[6]-face[0],by=face[7]-face[1],bz=face[8]-face[2];
            check((ay*bz-az*by)*face[12]+(az*bx-ax*bz)*face[13]+(ax*by-ay*bx)*face[14]>0,"Emitted water face winding matches outward normal");
        }
        for(float[] face:faces)for(int v=0;v<4;v++) {
            double x=face[v*3],y=face[v*3+1],z=face[v*3+2];
            check(y>=3&&y<=15,"Water stays between floor and max fill");
            for(var e:model.getAsJsonArray("elements")) {
                var from=e.getAsJsonObject().getAsJsonArray("from");var to=e.getAsJsonObject().getAsJsonArray("to");
                check(!(x>from.get(0).getAsDouble()&&x<to.get(0).getAsDouble()&&y>from.get(1).getAsDouble()&&y<to.get(1).getAsDouble()&&z>from.get(2).getAsDouble()&&z<to.get(2).getAsDouble()),"No emitted water vertex inside wall/glass");
            }
        }
        check(faces[0][1]==15&&faces[1][2]==1.05f&&faces[2][2]==14.95f&&faces[3][0]==1.05f&&faces[4][0]==14.95f,"Top 15/16; recessed window faces have .05-pixel glass clearance");
        var enderman=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("src/generated/resources/data/minecraft/tags/block/enderman_holdable.json"))).getAsJsonObject();
        check((!enderman.has("replace") || !enderman.get("replace").getAsBoolean())&&enderman.getAsJsonArray("remove").size()==4,"Only four Enderman exclusions, no vanilla tag replacement");
        var exclusions=new HashSet<String>();enderman.getAsJsonArray("remove").forEach(value->exclusions.add(value.getAsString()));
        check(exclusions.equals(Set.of("katzencraftmetals:foundry_controller","katzencraftmetals:foundry_tank",
                "katzencraftmetals:foundry_basin","katzencraftmetals:foundry_table")),"Exact Enderman exclusions");
        // Native Detonate event: Creeper filters two blocks; TNT/null explosions retain all block and entity targets.
        var world=FoundryDismantlingChecks.world(lookup);var blocks=List.of(ModBlocks.FOUNDRY_CONTROLLER.get(),ModBlocks.FOUNDRY_TANK.get(),ModBlocks.FOUNDRY_BASIN.get(),ModBlocks.FOUNDRY_TABLE.get(),Blocks.STONE);
        var positions=new ArrayList<BlockPos>();for(int i=0;i<blocks.size();i++){var p=new BlockPos(i,0,0);positions.add(p);world.blocks.put(p,blocks.get(i).defaultBlockState());}
        for(int kind=0;kind<3;kind++) {
            net.minecraft.world.entity.Entity source=kind==0?(net.minecraft.world.entity.Entity)unsafe.allocateInstance(net.minecraft.world.entity.monster.Creeper.class):kind==1?(net.minecraft.world.entity.Entity)unsafe.allocateInstance(net.minecraft.world.entity.item.PrimedTnt.class):null;
            var damage=new net.minecraft.world.damagesource.DamageSource(lookup.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.EXPLOSION));
            var explosion=new Explosion(world,source,damage,new ExplosionDamageCalculator(),0,0,0,3,false,Explosion.BlockInteraction.DESTROY,net.minecraft.core.particles.ParticleTypes.EXPLOSION,net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE);
            explosion.getToBlow().addAll(positions);var entities=new ArrayList<net.minecraft.world.entity.Entity>();if(source!=null)entities.add(source);
            var event=new net.neoforged.neoforge.event.level.ExplosionEvent.Detonate(world,explosion,entities);FoundryExplosionProtection.detonate(event);
            check(event.getAffectedBlocks().equals(kind==0?positions.subList(2,5):positions),"Only Creeper block damage to Controller/Tank filtered");
            check(entities.size()==(source==null?0:1),"Entity damage list unchanged");
        }
        System.out.println("PASS: contained water/lava bucket exchanges Survival/Creative, non-spreading states, molten exclusion, submersion persistence/sync and legacy saves; native-style dripstone checks; authored window-clearance mesh; unchanged Enderman/Creeper protections.");
    }
    private static void dripstone(HolderLookup.Provider lookup, java.lang.reflect.Method use, sun.misc.Unsafe unsafe) throws Exception {
        var block=ModBlocks.FOUNDRY_BASIN.get();var pos=BlockPos.ZERO;var tip=pos.above(3);
        var tipState=Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.DOWN);
        for(var fluid:new net.minecraft.world.level.material.Fluid[]{Fluids.WATER,Fluids.LAVA}) {
            var world=FoundryDismantlingChecks.world(lookup);world.blocks.put(pos,block.defaultBlockState());
            world.blocks.put(tip,tipState);world.blocks.put(tip.above(),Blocks.DRIPSTONE_BLOCK.defaultBlockState());
            world.blocks.put(tip.above(2),fluid==Fluids.WATER?Blocks.WATER.defaultBlockState():Blocks.LAVA.defaultBlockState());
            var chance=fluid.getFluidType().getDripInfo().chance();
            FoundryBasinDripstone.schedule(world.getBlockState(pos),world,pos,chance);
            check(world.scheduled.isEmpty(),"Chance threshold uses vanilla fluid probability");
            FoundryBasinDripstone.schedule(world.getBlockState(pos),world,pos,0);
            check(world.scheduled.get(pos)==block&&world.lastBlockDelay==53,"Valid drip schedules native 50+distance delay");
            check(!FoundryBasinBlock.hasContainedFluid(world.getBlockState(pos)),"Fluid does not appear before scheduled impact");
            int steps=fluid==Fluids.WATER?3:1;
            for(int step=1;step<=steps;step++) {
                FoundryBasinDripstone.fill(world.getBlockState(pos),world,pos);
                var state=world.getBlockState(pos);
                check(FoundryBasinBlock.containedFluid(state)==fluid&&state.getValue(FoundryBasinBlock.FLUID_LEVEL)==(fluid==Fluids.WATER?step:3),"Water collects in thirds, lava fills once");
                var player=(BucketPlayer)unsafe.allocateInstance(BucketPlayer.class);player.inventory=new BucketInventory(player);player.hand=new ItemStack(Items.BUCKET);
                var result=use.invoke(block,player.hand,state,world,pos,player,InteractionHand.MAIN_HAND,null);
                check(step==steps?result==ItemInteractionResult.CONSUME:result==ItemInteractionResult.FAIL,"Only full water levels yield a bucket");
            }
            // Repeat with obstructed path, bad tip, missing source, mud, excessive distance and removed source during delivery.
            for(int bad=0;bad<7;bad++) {
                world.blocks.clear();world.scheduled.clear();world.blocks.put(pos,block.defaultBlockState());
                world.blocks.put(tip,tipState);world.blocks.put(tip.above(),Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                world.blocks.put(tip.above(2),fluid==Fluids.WATER?Blocks.WATER.defaultBlockState():Blocks.LAVA.defaultBlockState());
                if(bad==0)world.blocks.put(pos.above(),Blocks.STONE.defaultBlockState());
                if(bad==1)world.blocks.remove(tip.above(2));
                if(bad==2)world.blocks.put(tip,tipState.setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.UP));
                if(bad==3)world.blocks.put(tip,tipState.setValue(PointedDripstoneBlock.WATERLOGGED,true));
                if(bad==4)world.blocks.put(tip.above(2),Blocks.MUD.defaultBlockState());
                if(bad==5){world.blocks.clear();world.blocks.put(pos,block.defaultBlockState());world.blocks.put(pos.above(11),tipState);world.blocks.put(pos.above(12),Blocks.DRIPSTONE_BLOCK.defaultBlockState());world.blocks.put(pos.above(13),Blocks.WATER.defaultBlockState());}
                if(bad==6){FoundryBasinDripstone.schedule(world.getBlockState(pos),world,pos,0);world.blocks.remove(tip.above(2));}
                FoundryBasinDripstone.schedule(world.getBlockState(pos),world,pos,0);
                FoundryBasinDripstone.fill(world.getBlockState(pos),world,pos);
                check(!FoundryBasinBlock.hasContainedFluid(world.getBlockState(pos)),"Invalidated drip cannot fill Basin: "+bad);
            }
        }
    }

}

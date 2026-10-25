package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTableBlock;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Regression guard: neither casting block exposes standard waterlogging any longer. */
public final class FoundryWaterloggingChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run(HolderLookup.Provider lookup) throws Exception {
        for(boolean table:new boolean[]{false,true}) {
            var block=table?ModBlocks.FOUNDRY_TABLE.get():ModBlocks.FOUNDRY_BASIN.get();
            var world=FoundryDismantlingChecks.world(lookup);var pos=new BlockPos(8,0,0);
            check(!(block instanceof SimpleWaterloggedBlock)&&!(block instanceof LiquidBlockContainer),"No standard fluid-container interface");
            check(!block.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED),"No waterlogged state property");
            for(var direction:Direction.Plane.HORIZONTAL)for(var ambient:new Block[]{Blocks.AIR,Blocks.WATER,Blocks.LAVA}) {
                world.blocks.put(pos,ambient.defaultBlockState());
                var state=block.getStateForPlacement(new DirectionalPlaceContext(world,pos,direction,new ItemStack(block.asItem()),Direction.UP));
                check(state.getFluidState().isEmpty()&&!FoundryBasinBlock.hasContainedFluid(state),"Submerged placement stays an empty casting block");
                if(table)check(state.getValue(FoundryTableBlock.FACING)==direction.getOpposite().getClockWise(),"Table rotation preserved");
            }
            if(table) {
                var itemUse=FoundryBasinBlock.class.getDeclaredMethod("useItemOn",ItemStack.class,
                        net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,
                        BlockPos.class,net.minecraft.world.entity.player.Player.class,net.minecraft.world.InteractionHand.class,
                        net.minecraft.world.phys.BlockHitResult.class);itemUse.setAccessible(true);
                for(var bucket:new Item[]{Items.BUCKET,Items.WATER_BUCKET,Items.LAVA_BUCKET})
                    check(itemUse.invoke(block,new ItemStack(bucket),block.defaultBlockState(),world,pos,null,
                            net.minecraft.world.InteractionHand.MAIN_HAND,null)==net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,"No Table-specific bucket interception");
            }
            var state=block.defaultBlockState();world.blocks.put(pos,state);
            var basin=table?new FoundryTableBlockEntity(pos,state):new FoundryBasinBlockEntity(pos,state);
            basin.setLevel(world);world.entities.put(pos,basin);
            var iron=FoundryDismantlingChecks.id("iron");
            check(basin.insert(iron,basin.getRequiredMoltenAmount())==basin.getRequiredMoltenAmount(),"Empty target casts at original capacity");
            check(world.getBlockState(pos).getLightEmission()==12,"Molten cooling still emits twelve");
            for(int tick=0;tick<basin.getCoolingDuration();tick++)FoundryBasinBlockEntity.serverTick(world,pos,basin.getBlockState(),basin);
            check(basin.isCooled()&&!basin.getResultCopy().isEmpty()&&world.getBlockState(pos).getLightEmission()==0,"Cooled casting extinguishes");
            var restored=NbtUtils.readBlockState(lookup.lookupOrThrow(Registries.BLOCK),NbtUtils.writeBlockState(world.getBlockState(pos)));
            check(restored==world.getBlockState(pos)&&restored.getFluidState().isEmpty(),"State reload never restores waterlogging");
            for(var direction:Direction.values())state.updateShape(direction,Blocks.WATER.defaultBlockState(),world,pos,pos.relative(direction));
            check(world.waterTicks==0,"Neighbor water schedules no fluid ticks");
            basin.takeResult();check(basin.isEmpty(),"Normal extraction preserved");
        }
        System.out.println("PASS: no Basin/Table waterlogging interfaces, properties or fluid states; submerged water/lava placement; all Table rotations; ordinary casting/cooling/extraction, native 12/0 light and persistence; no fluid ticks.");
    }
}

package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.fuel.*;
import net.chriskatze.katzencraftmetals.metal.*;
import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Native Controller fuel lifecycle, NBT/update packets and menu-data paths; no live world required. */
public final class FoundryFuelChecks {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void invalid(Runnable r){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Invalid fuel definition accepted");}
    static final class Controller extends FoundryControllerBlockEntity {
        Controller(){super(BlockPos.ZERO,ModBlocks.FOUNDRY_CONTROLLER.get().defaultBlockState());}
        @Override public void setChanged() {} // Isolate chunk notifications, not fuel behavior.
        @Override public FoundryTankNetwork getOwnedTankNetwork(){return null;}
    }
    public static void run(HolderLookup.Provider registries) throws Exception {
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        var server=(ServerLevel)unsafe.allocateInstance(ServerLevel.class);
        var client=(ServerLevel)unsafe.allocateInstance(ServerLevel.class);
        var access=Level.class.getDeclaredField("registryAccess");access.setAccessible(true);
        var staticRegistries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        access.set(server,staticRegistries);access.set(client,staticRegistries);
        var clientSide=Level.class.getDeclaredField("isClientSide");clientSide.setAccessible(true);clientSide.setBoolean(client,true);
        invalid(()->FoundryFuelCatalog.fuel("minecraft:coal",0));
        invalid(()->FoundryFuelCatalog.fuel("minecraft:coal",-1));
        invalid(()->FoundryFuelCatalog.fuel("minecraft:air",800));
        invalid(()->FoundryFuelCatalog.index(List.of(FoundryFuelCatalog.fuel("minecraft:coal",1),FoundryFuelCatalog.fuel("minecraft:coal",2))));
        check(FoundryFuelCatalog.values().size()==3,"Exactly three configured fuels");
        for(var unknown:List.of(Items.BLAZE_POWDER,Items.NETHER_STAR,Items.LAVA_BUCKET,Items.OAK_LOG,Items.COAL_BLOCK,Items.DIRT)) {
            var c=new Controller();c.setLevel(server);
            check(!FoundryFuelCatalog.isFuel(new ItemStack(unknown))&&!c.getFuelInventory().canPlaceItem(0,new ItemStack(unknown)),"Unknown fuel rejected");
            c.getFuelInventory().setItem(0,new ItemStack(unknown));
            check(!c.getFuelSystem().tryStartBurnCycle()&&c.getFuelInventory().getItem(0).getCount()==1,"Bypassing insertion validation does not burn unknown fuel");
        }
        check(!FoundryFuelCatalog.isFuel(ItemStack.EMPTY)&&!FoundryFuelCatalog.isFuel(null),"Empty fuel rejected");
        for(var definition:FoundryFuelCatalog.values()) {
            var item=BuiltInRegistries.ITEM.getOptional(definition.item()).orElseThrow();
            int ticks=item==Items.BLAZE_ROD?1200:800;
            check(definition.burnTime()==ticks,"Exact configured duration");
            var controller=new Controller();controller.setLevel(server);var fuel=controller.getFuelSystem();
            for(int slot=0;slot<FoundryControllerFuelSystem.FUEL_SLOT_COUNT;slot++)check(controller.getFuelInventory().canPlaceItem(slot,new ItemStack(item)),"Fuel allowed in every unlocked inventory slot");
            controller.getFuelInventory().setItem(2,new ItemStack(item,2));
            check(!controller.getFuelInventory().canPlaceItem(-1,new ItemStack(item))&&!controller.getFuelInventory().canPlaceItem(4,new ItemStack(item)),"Invalid slots rejected");
            check(fuel.getStoredFuelCount()==2&&fuel.hasAvailableFuel(),"Fuel inventory availability");
            check(fuel.tryStartBurnCycle()&&fuel.getBurnTimeRemaining()==ticks&&fuel.getMaxBurnTime()==ticks,"Start exact-duration cycle");
            check(controller.getFuelInventory().getItem(2).getCount()==1&&fuel.tryStartBurnCycle()&&controller.getFuelInventory().getItem(2).getCount()==1,"Consume exactly one item, no extra consumption on repeated start");
            for(int i=0;i<37;i++)fuel.tickBurnTime();
            var saved=new CompoundTag();FoundryControllerPersistence.save(controller,saved,registries);
            var restored=new Controller();FoundryControllerPersistence.load(restored,saved,registries);restored.setLevel(server);
            check(restored.getBurnTimeRemaining()==ticks-37&&restored.getMaxBurnTime()==ticks,"Burn save/load persistence");
            check(restored.getFuelInventory().getItem(0).isEmpty()&&restored.getFuelInventory().getItem(2).is(item)&&restored.getFuelInventory().getItem(2).getCount()==1,"Exact fuel slot preserved");
            var packet=controller.getUpdatePacket();
            var synced=new Controller();synced.setLevel(client);synced.handleUpdateTag(((net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket)packet).getTag(),registries);
            check(synced.getBurnTimeRemaining()==ticks-37&&synced.getMaxBurnTime()==ticks&&synced.getFuelInventory().getItem(2).is(item),"Actual block-entity update packet/tag synchronized");
            var menu=controller.getData();
            check(menu.get(FoundryControllerBlockEntity.BURN_TIME_DATA_INDEX)==ticks-37&&menu.get(FoundryControllerBlockEntity.MAX_BURN_TIME_DATA_INDEX)==ticks,"GUI fuel data indices unchanged");
            int clientCount=synced.getFuelInventory().getItem(2).getCount();
            check(!synced.getFuelSystem().tryStartBurnCycle(),"Client cannot ignite fuel");synced.getFuelSystem().tickBurnTime();
            check(synced.getBurnTimeRemaining()==ticks-37&&synced.getFuelInventory().getItem(2).getCount()==clientCount,"Client cannot decrement or consume");
            for(int i=37;i<ticks;i++)restored.getFuelSystem().tickBurnTime();
            check(!restored.isBurning()&&restored.getBurnTimeRemaining()==0&&restored.getFuelInventory().getItem(2).getCount()==1,"Exact burn exhaustion does not auto-consume idle fuel");
            restored.getFuelSystem().tickBurnTime();check(restored.getBurnTimeRemaining()==0,"No negative burn timer");
            check(restored.getFuelSystem().tryStartBurnCycle()&&restored.getFuelInventory().getItem(2).isEmpty(),"Next operation consumes next item");
            // Use the production recipe-selection path for every fuel/metal pairing.
            var select=FoundryControllerProcessing.class.getDeclaredMethod("selectActiveRecipe",int.class,FoundryMeltingRecipe.class,ItemStack.class);select.setAccessible(true);
            for(var metal:MetalCatalog.values()) {
                var ingotId=metal.forms().entrySet().stream().filter(e->e.getValue()==MetalDefinition.Form.INGOT).findFirst().orElseThrow().getKey();
                var ingot=BuiltInRegistries.ITEM.getOptional(ingotId).orElseThrow();
                var recipe=new FoundryMeltingRecipe(Ingredient.of(ingot),metal.id(),6,metal.meltingTimeFor(ingotId,6));
                select.invoke(controller.getProcessingForPersistence(),0,recipe,new ItemStack(ingot));
                check(controller.getData().get(1)==FoundryControllerProcessing.getProcessingTime(metal.meltingTime()),"Metal catalog duration independent of fuel: "+metal.id());
                var progress=controller.getUpdateTag(registries);progress.putInt("Progress",7);
                var resumed=new Controller();FoundryControllerPersistence.load(resumed,progress,registries);
                check(resumed.getData().get(0)==7&&resumed.getData().get(1)==controller.getData().get(1),"Active melting progress survives save/load");
            }
        }
        // Old active cycles retain exact saved time, even if the catalog duration changed.
        var old=new CompoundTag();old.putInt("BurnTimeRemaining",1500);old.putInt("MaxBurnTime",1600);
        var legacy=new Controller();legacy.getFuelSystem().load(old,registries);
        check(legacy.getBurnTimeRemaining()==1500&&legacy.getMaxBurnTime()==1600,"Existing lit saves preserved");
        // Datagen does not bind furnace fuel data maps. Audit the native defaults
        // used to generate that data instead of treating unbound values as zero fuel.
        Map<Item,Integer> furnace=new HashMap<>();
        net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.buildFuels((key,ticks)->key.ifLeft(item->furnace.put(item,ticks)));
        check(furnace.get(Items.COAL)==1600&&furnace.get(Items.CHARCOAL)==1600&&furnace.get(Items.BLAZE_ROD)==2400,"Vanilla furnace defaults unchanged");
        System.out.println("PASS: three Foundry fuels, exact 800/800/1200 ticks, slot insertion/rejection, one-item consumption/exhaustion, native NBT and update packet synchronization, client authority, GUI data indices, legacy lit saves, all 21 fuel/metal recipe-duration pairings and unchanged furnace durations.");
    }
}

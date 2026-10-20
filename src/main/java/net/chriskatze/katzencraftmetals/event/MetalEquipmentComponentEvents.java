package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.config.BlockRequirements;
import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import java.util.*;

/** Startup-only supported component patches; no item replacement, ticks or stack rewriting. */
@EventBusSubscriber(modid=KatzencraftMetalsMod.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class MetalEquipmentComponentEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void modify(ModifyDefaultComponentsEvent event){
        for(var profile:MetalEquipmentProfiles.activeProfiles())apply(event,profile);
    }
    public static void apply(ModifyDefaultComponentsEvent event,MetalEquipmentProfiles.Profile profile){
        for(var kind:MetalEquipmentProfiles.ToolKind.values()) {
            var id=ResourceLocation.fromNamespaceAndPath(profile.vanilla()?"minecraft":"katzencraftmetals",
                    profile.id()+"_"+kind.name().toLowerCase(Locale.ROOT));
            var optional=BuiltInRegistries.ITEM.getOptional(id);if(optional.isEmpty())continue;
            Item item=optional.get();
            // Keep foreign rules and mining speeds. Only the native tier rejection receives
            // a narrow drop-only exception for explicitly configured vanilla resources.
            var nativeTool=item.components().get(DataComponents.TOOL);
            Tool tool=nativeTool;
            if(nativeTool!=null){
                List<Tool.Rule> rules=new ArrayList<>();
                for(int level=profile.miningLevel()+1;level<=8;level++)rules.add(Tool.Rule.deniesDrops(BlockRequirements.tag(level)));
                var nativeRejection=profile.tier(kind).getIncorrectBlocksForDrops();
                var supported=BlockRequirements.configured().entrySet().stream()
                        .filter(e->e.getKey().getNamespace().equals("minecraft")&&e.getValue()<=profile.miningLevel())
                        .map(e->BuiltInRegistries.BLOCK.getHolder(e.getKey()).orElseThrow()).toList();
                for(var rule:nativeTool.rules()) {
                    if(kind==MetalEquipmentProfiles.ToolKind.PICKAXE&&!supported.isEmpty()
                            &&rule.correctForDrops().equals(Optional.of(false))
                            &&rule.blocks().unwrapKey().filter(nativeRejection::equals).isPresent())
                    {
                        // Honor third-party component denials even if placed after the native tier rule.
                        for(var foreign:nativeTool.rules())
                            if(foreign.correctForDrops().equals(Optional.of(false))
                                    &&!foreign.blocks().unwrapKey().filter(nativeRejection::equals).isPresent())
                                rules.add(new Tool.Rule(foreign.blocks(),Optional.empty(),foreign.correctForDrops()));
                        rules.add(new Tool.Rule(HolderSet.direct(supported),Optional.empty(),Optional.of(true)));
                    }
                    rules.add(profile.vanilla()&&profile.speedOverride()!=null&&rule.speed().isPresent()
                            ?new Tool.Rule(rule.blocks(),Optional.of(profile.speedOverride()),rule.correctForDrops()):rule);
                }
                tool=new Tool(List.copyOf(rules),nativeTool.defaultMiningSpeed(),nativeTool.damagePerBlock());
            }
            Tool resolvedTool=tool;
            var modifiers=item.getDefaultInstance().getAttributeModifiers();
            if(profile.vanilla()&&profile.damageOverride(kind)!=null)
                modifiers=attribute(modifiers,Attributes.ATTACK_DAMAGE,Item.BASE_ATTACK_DAMAGE_ID,
                        profile.damageOverride(kind)-1,EquipmentSlotGroup.MAINHAND);
            if(profile.vanilla()&&profile.attackSpeedOverride(kind)!=null)
                modifiers=attribute(modifiers,Attributes.ATTACK_SPEED,Item.BASE_ATTACK_SPEED_ID,
                        profile.attackSpeedOverride(kind)-4,EquipmentSlotGroup.MAINHAND);
            var resolvedModifiers=modifiers;
            boolean attributes=profile.vanilla()&&(profile.damageOverride(kind)!=null||profile.attackSpeedOverride(kind)!=null);
            event.modify(item,patch->{
                if(resolvedTool!=null)patch.set(DataComponents.TOOL,resolvedTool);
                if(profile.vanilla()&&profile.durabilityOverride(kind)!=null)patch.set(DataComponents.MAX_DAMAGE,profile.durabilityOverride(kind));
                if(attributes)patch.set(DataComponents.ATTRIBUTE_MODIFIERS,resolvedModifiers);
            });
        }
        if(!profile.vanilla())return;
        // Wood/stone have no armor; look up existing objects without inventing any.
        for(var type:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS)){
            var id=ResourceLocation.withDefaultNamespace(profile.id()+"_"+type.getName());
            var optional=BuiltInRegistries.ITEM.getOptional(id);if(optional.isEmpty()||!(optional.get() instanceof ArmorItem))continue;
            var item=optional.get();var modifiers=item.getDefaultInstance().getAttributeModifiers();
            var key=ResourceLocation.withDefaultNamespace("armor."+type.getName());var slot=EquipmentSlotGroup.bySlot(type.getSlot());
            if(profile.defenseOverride(type)!=null)modifiers=attribute(modifiers,Attributes.ARMOR,key,profile.defenseOverride(type),slot);
            if(profile.toughnessOverride()!=null)modifiers=attribute(modifiers,Attributes.ARMOR_TOUGHNESS,key,profile.toughnessOverride(),slot);
            if(profile.knockbackOverride()!=null)modifiers=attribute(modifiers,Attributes.KNOCKBACK_RESISTANCE,key,profile.knockbackOverride(),slot);
            var resolved=modifiers;
            boolean attributes=profile.defenseOverride(type)!=null||profile.toughnessOverride()!=null||profile.knockbackOverride()!=null;
            event.modify(item,patch->{
                if(profile.armorDurabilityOverride(type)!=null)patch.set(DataComponents.MAX_DAMAGE,profile.armorDurabilityOverride(type));
                if(attributes)patch.set(DataComponents.ATTRIBUTE_MODIFIERS,resolved);
            });
        }
    }
    private static ItemAttributeModifiers attribute(ItemAttributeModifiers original,Holder<Attribute> attribute,
                                                     ResourceLocation id,double value,EquipmentSlotGroup slot){
        return original.withModifierAdded(attribute,new AttributeModifier(id,value,AttributeModifier.Operation.ADD_VALUE),slot);
    }
    private MetalEquipmentComponentEvents() {}
}

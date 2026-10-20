package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import java.util.*;
import java.util.function.Supplier;

/** Equipment balance lives here; catalog links never register replacement vanilla items.
 * Damage and attack speed are TOTAL unenchanted player values (base damage 1, speed 4).
 * Null overrides mean inherit, never zero. Vanilla values are resolved from registered items.
 */
public final class MetalEquipmentProfiles {
    public enum ToolKind { SWORD, PICKAXE, AXE, SHOVEL, HOE }
    public record ToolStats(int durability, float speed, float damage, float attacksPerSecond, int enchantability) {}
    public static final Profile WOOD = vanillaEquipment("wooden").miningLevel(2).build();
    public static final Profile STONE = vanillaEquipment("stone").miningLevel(3).build();
    public static final Profile GOLD = vanillaEquipment("golden").miningLevel(4).build();
    public static final Profile IRON = vanillaEquipment("iron").miningLevel(4).build();
    public static final Profile NETHERITE = vanillaEquipment("netherite").miningLevel(7).build();
    public static final Profile COPPER = equipment("copper").miningLevel(4)
            .toolDurability(190).miningSpeed(5).enchantability(12)
            .swordDamage(5).pickaxeDamage(3).axeDamage(7).shovelDamage(3.5f).hoeDamage(1)
            .attackSpeed(ToolKind.SWORD,1.6f).attackSpeed(ToolKind.PICKAXE,1.2f)
            .attackSpeed(ToolKind.AXE,.9f).attackSpeed(ToolKind.SHOVEL,1).attackSpeed(ToolKind.HOE,2)
            .incorrectBlocks(BlockTags.INCORRECT_FOR_STONE_TOOL).repair("minecraft:copper_ingot")
            .armorDefense(2,5,4,1).armorDurability(132)
            .chestplateDurability(192).leggingsDurability(180).bootsDurability(156)
            .armorEnchantability(12).armorToughness(0).knockbackResistance(0)
            // Approved TEMPORARY vanilla artwork. Replace with authored copper layers later.
            .armorLayer("minecraft:iron").equipSound(() -> SoundEvents.ARMOR_EQUIP_IRON).build();
    public static final Profile STEEL = equipment("steel").miningLevel(6)
            .toolDurability(512).miningSpeed(7).enchantability(18).tierAttackBonus(3)
            .swordDamage(7).pickaxeDamage(5).axeDamage(5.5f).shovelDamage(5.5f).hoeDamage(2)
            .attackSpeed(ToolKind.SWORD,1.6f).attackSpeed(ToolKind.PICKAXE,1.2f)
            .attackSpeed(ToolKind.AXE,1).attackSpeed(ToolKind.SHOVEL,1).attackSpeed(ToolKind.HOE,3)
            .incorrectBlocks(BlockTags.INCORRECT_FOR_DIAMOND_TOOL).repair("katzencraftmetals:steel_ingot")
            .armorDefense(2,6,5,2).bodyDefense(5).armorDurability(264)
            .chestplateDurability(384).leggingsDurability(360).bootsDurability(312)
            .armorEnchantability(18).armorToughness(0).knockbackResistance(0)
            .armorLayer("katzencraftmetals:steel").equipSound(() -> SoundEvents.ARMOR_EQUIP_IRON).build();
    public static final Profile MYTHRIL = equipment("mythril").miningLevel(8)
            .toolDurability(1536).miningSpeed(10).enchantability(25).tierAttackBonus(5)
            .swordDamage(10).pickaxeDamage(7).axeDamage(7.5f).shovelDamage(7.5f).hoeDamage(4)
            .attackSpeed(ToolKind.SWORD,1.6f).attackSpeed(ToolKind.PICKAXE,1.2f)
            .attackSpeed(ToolKind.AXE,1).attackSpeed(ToolKind.SHOVEL,1).attackSpeed(ToolKind.HOE,3)
            .incorrectBlocks(BlockTags.INCORRECT_FOR_NETHERITE_TOOL).repair("katzencraftmetals:mythril_ingot")
            .armorDefense(3,8,6,3).bodyDefense(8).armorDurability(440)
            .chestplateDurability(640).leggingsDurability(600).bootsDurability(520)
            .armorEnchantability(25).armorToughness(2).knockbackResistance(0)
            .armorLayer("katzencraftmetals:mythril").equipSound(() -> SoundEvents.ARMOR_EQUIP_DIAMOND).build();
    public static List<Profile> values() { return List.of(WOOD,STONE,GOLD,IRON,NETHERITE,COPPER,STEEL,MYTHRIL); }
    /** Catalog links may replace a profile; wood/stone/netherite do not need fake metal entries. */
    public static List<Profile> activeProfiles() {
        Map<String,Profile> profiles=new LinkedHashMap<>();
        for(var p:values())profiles.put(p.id(),p);
        for(var metal:MetalCatalog.values())if(metal.equipment()!=null)profiles.put(metal.equipment().id(),metal.equipment());
        return List.copyOf(profiles.values());
    }
    public static Profile forMetal(String id) {
        var key=ResourceLocation.parse(id);
        return Objects.requireNonNull(MetalCatalog.get(key).orElseThrow().equipment(),"Equipment profile for "+key);
    }
    public record ArmorStats(int durability,int defense,float toughness,float knockbackResistance,
                             int enchantability,Holder<SoundEvent> equipSound) {}
    private MetalEquipmentProfiles() {}
    public static Builder equipment(String id) { return new Builder(id,false); }
    public static Builder vanillaEquipment(String id) { return new Builder(id,true); }

    public static final class Profile {
        private final String id;
        private final boolean vanilla;
        private final int miningLevel;
        private final Integer durability, enchantability, armorDurability, armorEnchantability;
        private final Float speed, toughness, knockback;
        private final float attackBonus;
        private final Map<ToolKind,Integer> toolDurabilities;
        private final Map<ToolKind,Float> damage, attackSpeeds;
        private final Map<ArmorItem.Type,Integer> defenses, armorDurabilities;
        private final TagKey<Block> incorrect;
        private final Supplier<Ingredient> repair;
        private final Supplier<Holder<SoundEvent>> sound;
        private final ResourceLocation layer;
        private Profile(Builder b) {
            id=b.id; vanilla=b.vanilla; miningLevel=b.miningLevel;
            durability=b.durability; enchantability=b.enchantability; speed=b.speed; attackBonus=b.attackBonus;
            toolDurabilities=Map.copyOf(b.toolDurabilities);damage=Map.copyOf(b.damage);attackSpeeds=Map.copyOf(b.attackSpeeds);
            defenses=Map.copyOf(b.defenses);armorDurabilities=Map.copyOf(b.armorDurabilities);
            armorDurability=b.armorDurability;armorEnchantability=b.armorEnchantability;
            toughness=b.toughness;knockback=b.knockback;incorrect=b.incorrect;repair=b.repair;sound=b.sound;layer=b.layer;
        }
        public String id(){return id;} public boolean vanilla(){return vanilla;} public int miningLevel(){return miningLevel;}
        public float tierAttackBonus(){return attackBonus;}
        public Item vanillaTool(ToolKind kind) {
            if(!vanilla)throw new IllegalStateException("Not vanilla: "+id);
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(id+"_"+kind.name().toLowerCase(Locale.ROOT))).orElseThrow();
        }
        public Item vanillaArmor(ArmorItem.Type type) {
            if(!vanilla||type==ArmorItem.Type.BODY)throw new IllegalArgumentException("No player armor reference: "+id);
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(id+"_"+type.getName())).orElseThrow();
        }
        public Integer durabilityOverride(ToolKind kind){return toolDurabilities.getOrDefault(kind,durability);}
        public Float damageOverride(ToolKind kind){return damage.get(kind);}
        public Float attackSpeedOverride(ToolKind kind){return attackSpeeds.get(kind);}
        public Float speedOverride(){return speed;}
        public Integer armorDurabilityOverride(ArmorItem.Type type){return armorDurabilities.getOrDefault(type,armorDurability);}
        public Integer defenseOverride(ArmorItem.Type type){return defenses.get(type);}
        public Float toughnessOverride(){return toughness;}
        public Float knockbackOverride(){return knockback;}
        public ToolStats tool(ToolKind kind) {
            Tier tier=vanilla?((TieredItem)vanillaTool(kind)).getTier():null;
            Integer d=durabilityOverride(kind);
            float totalDamage=damage.getOrDefault(kind,vanilla?nativeAttribute(vanillaTool(kind),true):1f);
            float totalSpeed=attackSpeeds.getOrDefault(kind,vanilla?nativeAttribute(vanillaTool(kind),false):4f);
            return new ToolStats(d!=null?d:tier.getUses(),speed!=null?speed:tier.getSpeed(),totalDamage,totalSpeed,
                    enchantability!=null?enchantability:tier.getEnchantmentValue());
        }
        private static float nativeAttribute(Item item,boolean damage) {
            var attribute=damage?Attributes.ATTACK_DAMAGE:Attributes.ATTACK_SPEED;
            double value=damage?1:4;
            for(var entry:item.getDefaultInstance().getAttributeModifiers().modifiers())
                if(entry.attribute().equals(attribute)&&entry.modifier().operation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
                    value+=entry.modifier().amount();
            return (float)value;
        }
        public Tier tier(ToolKind kind) {
            if(vanilla)return ((TieredItem)vanillaTool(kind)).getTier();
            var stats=tool(kind);
            return new net.chriskatze.katzencraftmetals.item.FixedToolTier(incorrect,stats.durability(),stats.speed(),attackBonus,stats.enchantability(),repair);
        }
        public Ingredient repairIngredient(){return vanilla?((TieredItem)vanillaTool(ToolKind.PICKAXE)).getTier().getRepairIngredient():repair.get();}
        public Ingredient armorRepairIngredient(){return vanilla?((ArmorItem)vanillaArmor(ArmorItem.Type.HELMET)).getMaterial().value().repairIngredient().get():repair.get();}
        public ArmorStats armor(ArmorItem.Type type){
            ArmorMaterial material=vanilla?((ArmorItem)vanillaArmor(type)).getMaterial().value():null;
            return new ArmorStats(armorDurability(type),defense(type),toughness!=null?toughness:material.toughness(),
                    knockback!=null?knockback:material.knockbackResistance(),armorEnchantability!=null?armorEnchantability:material.enchantmentValue(),
                    sound!=null?sound.get():material.equipSound());
        }
        public int armorDurability(ArmorItem.Type type){var n=armorDurabilityOverride(type);return n!=null?n:vanillaArmor(type).getDefaultInstance().getMaxDamage();}
        public int defense(ArmorItem.Type type){var n=defenses.get(type);return n!=null?n:((ArmorItem)vanillaArmor(type)).getMaterial().value().getDefense(type);}
        public ArmorMaterial armorMaterial() {
            if(vanilla)throw new IllegalStateException("Vanilla materials are references, not replacements");
            var map=new EnumMap<ArmorItem.Type,Integer>(ArmorItem.Type.class);map.putAll(defenses);
            map.putIfAbsent(ArmorItem.Type.BODY,0);
            return new ArmorMaterial(map,armorEnchantability,sound.get(),repair,List.of(new ArmorMaterial.Layer(layer)),toughness,knockback);
        }
    }

    public static final class Builder {
        private final String id; private final boolean vanilla;
        private int miningLevel=1; private Integer durability,enchantability,armorDurability,armorEnchantability;
        private Float speed,toughness,knockback;private float attackBonus;
        private final Map<ToolKind,Integer> toolDurabilities=new EnumMap<>(ToolKind.class);
        private final Map<ToolKind,Float> damage=new EnumMap<>(ToolKind.class),attackSpeeds=new EnumMap<>(ToolKind.class);
        private final Map<ArmorItem.Type,Integer> defenses=new EnumMap<>(ArmorItem.Type.class),armorDurabilities=new EnumMap<>(ArmorItem.Type.class);
        private TagKey<Block> incorrect;private Supplier<Ingredient> repair;private Supplier<Holder<SoundEvent>> sound;private ResourceLocation layer;
        private Builder(String id,boolean vanilla){ResourceLocation.parse(id);this.id=id;this.vanilla=vanilla;}
        private static int positive(int v){if(v<=0)throw new IllegalArgumentException("Durability must be positive");return v;}
        private static float range(float v,float min,float max){if(!Float.isFinite(v)||v<min||v>max)throw new IllegalArgumentException("Invalid equipment property: "+v);return v;}
        public Builder miningLevel(int n){if(n<1||n>8)throw new IllegalArgumentException("Capability must be 1..8");miningLevel=n;return this;}
        public Builder toolDurability(int n){durability=positive(n);return this;}
        public Builder toolDurability(ToolKind k,int n){toolDurabilities.put(k,positive(n));return this;}
        public Builder pickaxeDurability(int n){return toolDurability(ToolKind.PICKAXE,n);}
        public Builder miningSpeed(float n){speed=range(n,.01f,100);return this;}
        public Builder enchantability(int n){range(n,0,100);enchantability=n;return this;}
        public Builder tierAttackBonus(float n){if(vanilla)throw new IllegalArgumentException("Use total damage overrides for vanilla tools");attackBonus=range(n,0,100);return this;}
        public Builder damage(ToolKind k,float n){damage.put(k,range(n,0,100));return this;}
        public Builder swordDamage(float n){return damage(ToolKind.SWORD,n);}
        public Builder pickaxeDamage(float n){return damage(ToolKind.PICKAXE,n);}
        public Builder axeDamage(float n){return damage(ToolKind.AXE,n);}
        public Builder shovelDamage(float n){return damage(ToolKind.SHOVEL,n);}
        public Builder hoeDamage(float n){return damage(ToolKind.HOE,n);}
        public Builder attackSpeed(ToolKind k,float n){attackSpeeds.put(k,range(n,.01f,20));return this;}
        public Builder incorrectBlocks(TagKey<Block> tag){incorrect=Objects.requireNonNull(tag);return this;}
        public Builder repair(String item){var key=ResourceLocation.parse(item);repair=()->Ingredient.of(BuiltInRegistries.ITEM.getOptional(key).orElseThrow());return this;}
        public Builder armorDefense(int h,int c,int l,int b){return defense(ArmorItem.Type.HELMET,h).defense(ArmorItem.Type.CHESTPLATE,c).defense(ArmorItem.Type.LEGGINGS,l).defense(ArmorItem.Type.BOOTS,b);}
        public Builder defense(ArmorItem.Type type,int n){if(vanilla&&type==ArmorItem.Type.BODY)throw new IllegalArgumentException("Vanilla body armor is not a player armor override");range(n,0,100);defenses.put(type,n);return this;}
        public Builder bodyDefense(int n){return defense(ArmorItem.Type.BODY,n);}
        public Builder armorDurability(int n){armorDurability=positive(n);return this;}
        public Builder armorDurability(ArmorItem.Type t,int n){if(vanilla&&t==ArmorItem.Type.BODY)throw new IllegalArgumentException("Vanilla body armor is not a player armor override");armorDurabilities.put(t,positive(n));return this;}
        public Builder helmetDurability(int n){return armorDurability(ArmorItem.Type.HELMET,n);}
        public Builder chestplateDurability(int n){return armorDurability(ArmorItem.Type.CHESTPLATE,n);}
        public Builder leggingsDurability(int n){return armorDurability(ArmorItem.Type.LEGGINGS,n);}
        public Builder bootsDurability(int n){return armorDurability(ArmorItem.Type.BOOTS,n);}
        public Builder armorEnchantability(int n){range(n,0,100);armorEnchantability=n;return this;}
        public Builder armorToughness(float n){toughness=range(n,0,100);return this;}
        public Builder knockbackResistance(float n){knockback=range(n,0,1);return this;}
        public Builder armorLayer(String s){layer=ResourceLocation.parse(s);return this;}
        public Builder equipSound(Supplier<Holder<SoundEvent>> s){sound=Objects.requireNonNull(s);return this;}
        public Profile build(){
            if(vanilla) {
                if((id.equals("wooden")||id.equals("stone"))&&(!defenses.isEmpty()||!armorDurabilities.isEmpty()||armorDurability!=null||toughness!=null||knockback!=null))
                    throw new IllegalArgumentException("No vanilla armor exists for "+id);
                // These are methods/material identity in 1.21.1, not supported mutable components.
                if(enchantability!=null||armorEnchantability!=null||repair!=null||incorrect!=null||sound!=null||layer!=null)
                    throw new IllegalArgumentException("Vanilla enchantability/repair/material overrides have no supported component hook in 1.21.1");
            } else {
                Objects.requireNonNull(durability,"Tool durability");Objects.requireNonNull(speed,"Mining speed");Objects.requireNonNull(enchantability,"Enchantability");
                Objects.requireNonNull(incorrect,"Incorrect blocks tag");Objects.requireNonNull(repair,"Repair ingredient");
                for(var t:ToolKind.values()){Objects.requireNonNull(damage.get(t),"Damage "+t);Objects.requireNonNull(attackSpeeds.get(t),"Attack speed "+t);}
                if(!defenses.isEmpty()){
                    for(var t:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS))Objects.requireNonNull(defenses.get(t),"Defense "+t);
                    Objects.requireNonNull(armorDurability,"Armor durability");Objects.requireNonNull(armorEnchantability,"Armor enchantability");Objects.requireNonNull(toughness,"Toughness");Objects.requireNonNull(knockback,"Knockback resistance");Objects.requireNonNull(sound,"Equip sound");Objects.requireNonNull(layer,"Armor layer");
                }
            }
            return new Profile(this);
        }
    }
}

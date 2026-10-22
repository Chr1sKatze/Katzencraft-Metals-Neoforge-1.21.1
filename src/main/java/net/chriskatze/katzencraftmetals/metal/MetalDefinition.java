package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Registry-independent metadata: safe before deferred items and blocks are constructed. */
public record MetalDefinition(ResourceLocation id, String englishName, int density, int unitsPerOre,
                              Map<String, String> localizedNames, Map<ResourceLocation, Form> forms,
                              Map<String, Map<ResourceLocation, String>> formNameOverrides, StorageBlock storageBlock,
                              MetalFoundrySupport foundry, MetalAlloy alloy, int meltingTime, int ingotCoolingTime,
                              int blockCoolingTime, Map<ResourceLocation, Integer> meltingTimeOverrides,
                              MetalEquipmentProfiles.Profile equipment, Integer oreMiningLevel,
                              Integer referencedStorageMiningLevel, Map<Form,Integer> oreMiningOverrides, java.util.List<MetalOreGeneration> oreGeneration) {
    public enum StorageSound { METAL, COPPER }
    public record StorageBlock(float strength, float resistance, StorageSound sound, int miningLevel) {
        public StorageBlock {
            if (!Float.isFinite(strength) || !Float.isFinite(resistance) || strength < 0 || resistance < 0 || (miningLevel < 1 || miningLevel > 8))
                throw new IllegalArgumentException("Invalid storage block properties");
            Objects.requireNonNull(sound);
        }
    }
    public enum Form {
        INGOT(false, "%s Ingot", "%sbarren"), NUGGET(false, "%s Nugget", "%snugget"),
        RAW(false, "Raw %s", "Roh%s"),
        STORAGE_BLOCK(true, "%s Block", "%sblock"), RAW_BLOCK(true, "Block of Raw %s", "Roh%sblock"),
        ORE(true, "%s Ore", "%serz"), DEEPSLATE_ORE(true, "Deepslate %s Ore", "Tiefenschiefer-%serz"),
        NETHER_ORE(true, "Nether %s Ore", "Nether-%serz"), END_ORE(true, "End %s Ore", "End-%serz");

        public final boolean block;
        private final String english, german;
        Form(boolean block, String english, String german) {
            this.block = block; this.english = english; this.german = german;
        }
        public String name(String locale, String metalName) {
            return (locale.equals("de_de") ? german : english).formatted(metalName);
        }
    }

    public MetalDefinition {
        Objects.requireNonNull(id, "Metal ID");
        oreGeneration = java.util.List.copyOf(oreGeneration);
        for (var entry : oreGeneration) {
            if (forms.values().stream().filter(f -> f == entry.form()).count() != 1)
                throw new IllegalArgumentException("Generation requires one declared " + entry.form() + " for " + id);
        }
        if (new java.util.HashSet<>(oreGeneration).size() != oreGeneration.size())
            throw new IllegalArgumentException("Duplicate ore generation entry for " + id);
        if (oreMiningLevel != null) requireMiningLevel(oreMiningLevel);
        if (referencedStorageMiningLevel != null) requireMiningLevel(referencedStorageMiningLevel);
        oreMiningOverrides.forEach((form,level) -> { if (!isOre(form)) throw new IllegalArgumentException("Not an ore form: "+form); requireMiningLevel(level); });
        oreMiningOverrides = Map.copyOf(oreMiningOverrides);
        if (meltingTime <= 0 || ingotCoolingTime <= 0 || blockCoolingTime <= 0)
            throw new IllegalArgumentException("Metal processing times must be positive: " + id);
        meltingTimeOverrides.forEach((input, ticks) -> {
            Objects.requireNonNull(input);
            if (ticks == null || ticks <= 0) throw new IllegalArgumentException("Invalid melting override: " + input);
        });
        meltingTimeOverrides = Map.copyOf(meltingTimeOverrides);
        boolean ownedStorage = forms.entrySet().stream().anyMatch(entry -> entry.getValue() == Form.STORAGE_BLOCK && entry.getKey().getNamespace().equals("katzencraftmetals"));
        if (ownedStorage && storageBlock == null) throw new IllegalArgumentException("Storage block requires explicit properties: " + id);
        if (!ownedStorage && storageBlock != null) throw new IllegalArgumentException("Properties without an owned storage block: " + id);
        requireName(englishName);
        if (foundry != null && unitsPerOre != MetalAlloy.UNITS_PER_INGOT)
            throw new IllegalArgumentException("Foundry currently requires six units per ingot: " + id);
        if (alloy != null && foundry == null) throw new IllegalArgumentException("Alloy output requires Foundry support: " + id);
        if (density <= 0 || unitsPerOre <= 0) throw new IllegalArgumentException("Invalid metal quantities: " + id);
        localizedNames.forEach((locale, name) -> { requireLocale(locale); requireName(name); });
        if (localizedNames.containsKey("en_us") && !localizedNames.get("en_us").equals(englishName))
            throw new IllegalArgumentException("Conflicting English metal name: " + id);
        localizedNames = Map.copyOf(localizedNames);
        forms = Collections.unmodifiableMap(new LinkedHashMap<>(forms));
        forms.forEach((key, form) -> { Objects.requireNonNull(key); Objects.requireNonNull(form); });
        for (Form standard : new Form[]{Form.INGOT, Form.NUGGET, Form.STORAGE_BLOCK}) {
            if (forms.values().stream().filter(form -> form == standard).count() > 1)
                throw new IllegalArgumentException("Ambiguous standard form: " + standard + " for " + id);
        }
        Map<String, Map<ResourceLocation, String>> overrides = new LinkedHashMap<>();
        for (var entry : formNameOverrides.entrySet()) {
            requireLocale(entry.getKey());
            for (var override : entry.getValue().entrySet()) {
                if (!forms.containsKey(override.getKey())) throw new IllegalArgumentException("Override of absent form: " + override.getKey());
                requireName(override.getValue());
            }
            overrides.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        formNameOverrides = Map.copyOf(overrides);
    }

    public static boolean isOre(Form form) { return form == Form.ORE || form == Form.DEEPSLATE_ORE || form == Form.NETHER_ORE || form == Form.END_ORE; }
    private static void requireMiningLevel(int level) { if (level < 1 || level > 8) throw new IllegalArgumentException("Requirement must be 1..8"); }
    public Integer storageMiningLevel() { return storageBlock != null ? storageBlock.miningLevel() : referencedStorageMiningLevel; }
    public Integer oreMiningLevel(Form form) { return oreMiningOverrides.getOrDefault(form, oreMiningLevel); }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Metal names must not be blank");
    }
    private static void requireLocale(String locale) {
        if (locale == null || !locale.matches("[a-z]{2,3}_[a-z0-9]{2,}")) throw new IllegalArgumentException("Invalid locale: " + locale);
    }
    public String name(String locale) { return localizedNames.getOrDefault(locale, englishName); }
    public String translationKey() { return "metal.katzencraftmetals." + id.getPath(); }
    public String formName(ResourceLocation form, String locale) {
        String override = formNameOverrides.getOrDefault(locale, Map.of()).get(form);
        if (override != null) return override;
        // Unknown grammar falls back to English rather than applying English order to a localized name.
        boolean supported = locale.equals("en_us") || locale.equals("de_de");
        return forms.get(form).name(supported ? locale : "en_us", supported ? name(locale) : englishName);
    }
    /** Ceiling division with wide intermediates; reject unrepresentable processing times. */
    public int scaledMeltingTime(int moltenUnits) {
        if (moltenUnits <= 0) throw new IllegalArgumentException("Melting yield must be positive");
        long ticks = ((long) meltingTime * moltenUnits + 5L) / 6L;
        if (ticks > Integer.MAX_VALUE) throw new IllegalArgumentException("Melting duration overflows: " + id);
        return (int) Math.max(1L, ticks);
    }

    public int meltingTimeFor(ResourceLocation input, int moltenUnits) {
        Integer override = meltingTimeOverrides.get(input);
        return override != null ? override : scaledMeltingTime(moltenUnits);
    }

    /**
     * Standard mod-owned ingot/nugget/storage cube. Density must be supplied explicitly.
     * Defaults: 6 molten units/ingot; storage hardness 5, resistance 6, METAL sound,
     * mining requirement 4 (Iron). Foundry, alloys and specialized forms remain opt-in.
     */
    public static Builder metal(String id, String displayName) {
        ResourceLocation metal = ResourceLocation.fromNamespaceAndPath("katzencraftmetals", id);
        return new Builder(metal, displayName, 0, MetalAlloy.UNITS_PER_INGOT)
                .form(Form.INGOT, metal + "_ingot")
                .form(Form.NUGGET, metal + "_nugget")
                .form(Form.STORAGE_BLOCK, metal + "_block")
                .storageBlock(5.0f, 6.0f, StorageSound.METAL, 4);
    }

    public static Builder builder(String id, String name, int density, int units) {
        return new Builder(ResourceLocation.parse(id), name, density, units);
    }
    public static final class Builder {
        private final ResourceLocation id;
        private final String name;
        private int density;
        private final int units;
        private StorageBlock storageBlock;
        private MetalFoundrySupport foundry;
        private MetalEquipmentProfiles.Profile equipment;
        private Integer oreMiningLevel, referencedStorageMiningLevel;
        private final Map<Form,Integer> oreMiningOverrides = new java.util.EnumMap<>(Form.class);
        private final java.util.List<MetalOreGeneration.Builder> oreGeneration = new java.util.ArrayList<>();
        private MetalOreGeneration.Mode oreMode = MetalOreGeneration.Mode.ADDITIVE;
        private MetalAlloy alloy;
        private int meltingTime = 100, ingotCoolingTime = 100, blockCoolingTime = 100;
        private final Map<ResourceLocation, Integer> meltingTimeOverrides = new LinkedHashMap<>();
        private final Map<String, String> names = new LinkedHashMap<>();
        private final Map<ResourceLocation, Form> forms = new LinkedHashMap<>();
        private final Map<String, Map<ResourceLocation, String>> overrides = new LinkedHashMap<>();
        private Builder(ResourceLocation id, String name, int density, int units) {
            this.id=id; this.name=name; this.density=density; this.units=units;
        }
        /** Required for metal(): positive layering density; heavier molten metals settle lower. */
        public Builder density(int density) {
            if (density <= 0) throw new IllegalArgumentException("Metal density must be positive: " + id);
            this.density = density;
            return this;
        }
        /** Default for all entries without an explicit mode; no entries means vanilla stays intact. */
        public Builder oreGenerationMode(MetalOreGeneration.Mode mode) { oreMode = Objects.requireNonNull(mode); return this; }
        public Builder oreGeneration(Form form, java.util.function.Consumer<MetalOreGeneration.Builder> configure) {
            var entry = new MetalOreGeneration.Builder(form);
            configure.accept(entry); oreGeneration.add(entry); return this;
        }
        public Builder equipment(MetalEquipmentProfiles.Profile profile) { equipment=Objects.requireNonNull(profile);return this; }
        public Builder oreMiningLevel(int level) { requireMiningLevel(level);oreMiningLevel=level;return this; }
        public Builder oreMiningLevel(Form form,int level) { requireMiningLevel(level);if(!isOre(form))throw new IllegalArgumentException("Not an ore form");oreMiningOverrides.put(form,level);return this; }
        public Builder storageMiningLevel(int level) {
            requireMiningLevel(level);
            if(storageBlock == null) referencedStorageMiningLevel=level;
            else storageBlock(storageBlock.strength(),storageBlock.resistance(),storageBlock.sound(),level);
            return this;
        }
        /** Compatibility spelling; only the storage requirement, never tool capability. */
        public Builder miningLevel(int level) { return storageMiningLevel(level); }
        public Builder storageSound(StorageSound sound) {
            var storage = requireStorage();
            return storageBlock(storage.strength(), storage.resistance(), sound, storage.miningLevel());
        }
        public Builder storageStrength(float strength) {
            var storage = requireStorage();
            return storageBlock(strength, storage.resistance(), storage.sound(), storage.miningLevel());
        }
        public Builder storageResistance(float resistance) {
            var storage = requireStorage();
            return storageBlock(storage.strength(), resistance, storage.sound(), storage.miningLevel());
        }
        private StorageBlock requireStorage() {
            if (storageBlock == null) throw new IllegalArgumentException("No mod-owned storage properties to configure: " + id);
            return storageBlock;
        }
        public Builder meltingTime(int ticks) { meltingTime = positiveTime(ticks); return this; }
        public Builder ingotCoolingTime(int ticks) { ingotCoolingTime = positiveTime(ticks); return this; }
        public Builder blockCoolingTime(int ticks) { blockCoolingTime = positiveTime(ticks); return this; }
        /** Explicit input exception; new metals otherwise use proportional melting times. */
        public Builder meltingTimeOverride(String input, int ticks) {
            if (meltingTimeOverrides.putIfAbsent(ResourceLocation.parse(input), positiveTime(ticks)) != null)
                throw new IllegalArgumentException("Duplicate melting time override: " + input);
            return this;
        }
        private int positiveTime(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException("Processing time must be positive: " + id);
            return ticks;
        }
        public Builder localizedName(String locale, String name) {
            if (names.putIfAbsent(locale, name) != null) throw new IllegalArgumentException("Duplicate locale: " + locale);
            return this;
        }
        public Builder form(Form form, String id) {
            if (forms.putIfAbsent(ResourceLocation.parse(id), form) != null) throw new IllegalArgumentException("Duplicate form: " + id);
            return this;
        }
        public Builder formName(String locale, String id, String name) {
            if (overrides.computeIfAbsent(locale, key -> new LinkedHashMap<>()).putIfAbsent(ResourceLocation.parse(id), name) != null)
                throw new IllegalArgumentException("Duplicate form name: " + id);
            return this;
        }
        public Builder storageBlock(float strength, float resistance, StorageSound sound, int miningLevel) {
            storageBlock = new StorageBlock(strength, resistance, sound, miningLevel);
            return this;
        }
        public Builder foundry() { return foundry(MetalFoundrySupport.standard()); }
        public Builder foundry(MetalFoundrySupport support) { foundry = Objects.requireNonNull(support); return this; }
        public Builder alloy(MetalAlloy.Input... inputs) { return alloy(100, inputs); }
        public Builder alloy(int ticks, MetalAlloy.Input... inputs) {
            if (alloy != null) throw new IllegalArgumentException("Duplicate alloy for " + id);
            alloy = new MetalAlloy(java.util.List.of(inputs), ticks);
            return this;
        }
        public MetalDefinition build() {
            if (density <= 0) throw new IllegalArgumentException("Supply a positive .density(...) for " + id);
            return new MetalDefinition(id, name, density, units, names, forms, overrides, storageBlock, foundry, alloy, meltingTime, ingotCoolingTime, blockCoolingTime, meltingTimeOverrides, equipment, oreMiningLevel, referencedStorageMiningLevel, oreMiningOverrides, oreGeneration.stream().map(e -> e.build(oreMode)).toList());
        }
    }
}

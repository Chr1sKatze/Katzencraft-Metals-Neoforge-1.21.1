package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.metal.MetalCatalog;
import net.chriskatze.katzencraftmetals.metal.MetalDefinition;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Standard form naming only: never registers items or replaces vanilla translations. */
public final class MetalLanguageEntries {
    private MetalLanguageEntries() {}
    public static Set<String> locales() {
        Set<String> result = new LinkedHashSet<>();
        result.add("en_us");
        for (var metal : MetalCatalog.values()) {
            metal.localizedNames().keySet().stream().sorted().forEach(result::add);
            metal.formNameOverrides().keySet().stream().sorted().forEach(result::add);
        }
        return java.util.Collections.unmodifiableSet(result);
    }
    public static Map<String, String> entries(List<MetalDefinition> metals, String locale) {
        Map<String, String> result = new LinkedHashMap<>();
        for (var metal : metals) {
            put(result, metal.translationKey(), metal.name(locale));
            metal.forms().forEach((id, form) -> {
                if (!id.getNamespace().equals("katzencraftmetals")) return;
                String key = (form.block ? "block." : "item.") + id.getNamespace() + "." + id.getPath();
                put(result, key, metal.formName(id, locale));
            });
        }
        return java.util.Collections.unmodifiableMap(result);
    }
    private static void put(Map<String, String> entries, String key, String value) {
        if (entries.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Duplicate metal translation: " + key);
    }
}

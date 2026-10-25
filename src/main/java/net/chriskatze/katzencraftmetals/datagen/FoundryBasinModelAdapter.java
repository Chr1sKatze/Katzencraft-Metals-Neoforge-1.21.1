package net.chriskatze.katzencraftmetals.datagen;

import com.google.gson.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Build-time integration only. The authored Blockbench JSON is copied, never rewritten. */
public final class FoundryBasinModelAdapter extends CustomLoaderBuilder<BlockModelBuilder> {
    private final JsonObject adapted;

    public FoundryBasinModelAdapter(BlockModelBuilder parent, ExistingFileHelper files) {
        super(ResourceLocation.parse("neoforge:composite"), parent, files, false);
        try (var reader = files.getResource(ResourceLocation.parse("katzencraftmetals:block/foundry_basin"),
                PackType.CLIENT_RESOURCES, ".json", "models").openAsReader()) {
            adapted = adapt(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException error) { throw new UncheckedIOException("Cannot read authored Foundry Basin model", error); }
    }

    public static JsonObject adapt(JsonObject source) {
        JsonObject root = source.deepCopy();
        root.remove("elements");
        root.addProperty("parent", "minecraft:block/block");
        root.addProperty("loader", "neoforge:composite");
        JsonObject textures = root.getAsJsonObject("textures");
        for (var entry : textures.entrySet()) {
            String path = entry.getValue().getAsString();
            if (!path.startsWith("#") && !path.contains(":"))
                entry.setValue(new JsonPrimitive("katzencraftmetals:" + (path.startsWith("block/") ? path : "block/" + path)));
        }
        JsonObject children = new JsonObject();
        for (boolean windows : new boolean[]{false, true}) {
            JsonObject child = new JsonObject();
            child.addProperty("parent", "minecraft:block/block");
            child.addProperty("ambientocclusion", source.has("ambientocclusion") && source.get("ambientocclusion").getAsBoolean());
            child.addProperty("render_type", windows ? "minecraft:translucent" : "minecraft:cutout");
            child.add("textures", textures.deepCopy());
            JsonArray elements = new JsonArray();
            for (var value : source.getAsJsonArray("elements")) {
                JsonObject element = value.getAsJsonObject().deepCopy();
                JsonObject faces = new JsonObject();
                for (var face : element.getAsJsonObject("faces").entrySet()) {
                    JsonObject definition = face.getValue().getAsJsonObject();
                    String texture = definition.get("texture").getAsString();
                    while (texture.startsWith("#")) texture = textures.get(texture.substring(1)).getAsString();
                    if (texture.endsWith("foundry_basin_window") != windows) continue;
                    if (definition.has("cullface")) {
                        Direction direction = Direction.byName(face.getKey());
                        int axis = direction.getAxis().ordinal();
                        float plane = element.getAsJsonArray(direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? "from" : "to").get(axis).getAsFloat();
                        if (plane != (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0 : 16)) definition.remove("cullface");
                    }
                    faces.add(face.getKey(), definition);
                }
                if (faces.size() > 0) { element.add("faces", faces); elements.add(element); }
            }
            child.add("elements", elements);
            children.add(windows ? "windows" : "frame", child);
        }
        root.add("children", children);
        return root;
    }

    @Override public JsonObject toJson(JsonObject json) {
        adapted.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue().deepCopy()));
        return super.toJson(json);
    }
}

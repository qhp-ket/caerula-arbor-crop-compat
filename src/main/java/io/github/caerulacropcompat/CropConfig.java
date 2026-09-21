package io.github.caerulacropcompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/** Loads the user-editable crop semantic entries and exposes their validated lookup. */
public final class CropConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIRECTORY = "caerula_crop_compat";
    private static final String CONFIG_FILE = "crops.json";
    private static final String PACK_DIRECTORY = "generated_data";
    private static final String DEFAULT_AGE_PROPERTY = "blockstate";

    private static final List<CropConfigEntry> entries = new ArrayList<>();
    private static final Map<ResourceLocation, CropConfigEntry> byBlock = new HashMap<>();
    private static final Set<String> warnings = new HashSet<>();
    private static Path generatedPackDirectory;
    private static boolean loaded;

    private CropConfig() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path directory = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIRECTORY);
        Path file = directory.resolve(CONFIG_FILE);
        generatedPackDirectory = directory.resolve(PACK_DIRECTORY);
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            LOGGER.warn("[Caerula Crop Compat] Could not create config directory {}", directory, exception);
        }

        List<CropConfigEntry> parsed;
        if (Files.exists(file)) {
            parsed = read(file);
        } else {
            parsed = defaultEntries();
            write(file, parsed);
        }
        install(parsed);
        LOGGER.info("[Caerula Crop Compat] Loaded {} crop compatibility entries", entries.size());
    }

    private static List<CropConfigEntry> read(Path file) {
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonObject() || !root.getAsJsonObject().has("crops")
                    || !root.getAsJsonObject().get("crops").isJsonArray()) {
                throw new JsonParseException("expected an object containing a crops array");
            }
            List<CropConfigEntry> result = new ArrayList<>();
            JsonArray crops = root.getAsJsonObject().getAsJsonArray("crops");
            for (int index = 0; index < crops.size(); index++) {
                try {
                    CropConfigEntry entry = CropConfigEntry.fromJson(crops.get(index).getAsJsonObject());
                    if (entry != null) {
                        result.add(entry);
                    }
                } catch (RuntimeException exception) {
                    warnOnce("entry-" + index,
                            "[Caerula Crop Compat] Skipping invalid crop config entry " + index + ": "
                                    + exception.getMessage());
                }
            }
            return result;
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.warn("[Caerula Crop Compat] Could not parse {}; using built-in defaults: {}",
                    file, exception.getMessage());
            return defaultEntries();
        }
    }

    private static void install(List<CropConfigEntry> parsed) {
        entries.clear();
        byBlock.clear();
        for (CropConfigEntry entry : parsed) {
            if (byBlock.putIfAbsent(entry.blockId(), entry) != null) {
                warnOnce("duplicate-" + entry.blockId(),
                        "[Caerula Crop Compat] Duplicate crop block entry for " + entry.blockId()
                                + "; keeping the first entry.");
                continue;
            }
            entries.add(entry);
        }
    }

    private static void write(Path file, List<CropConfigEntry> values) {
        JsonObject root = new JsonObject();
        JsonArray crops = new JsonArray();
        for (CropConfigEntry entry : values) {
            crops.add(entry.toJson());
        }
        root.add("crops", crops);
        try {
            Files.writeString(file, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("[Caerula Crop Compat] Could not write default crop config {}", file, exception);
        }
    }

    private static List<CropConfigEntry> defaultEntries() {
        return List.of(
                new CropConfigEntry(id("caerula_arbor:planted_viviparous_lily"),
                        id("caerula_arbor:planted_viviparous_lily"), DEFAULT_AGE_PROPERTY, -1, true, true, true),
                new CropConfigEntry(id("caerula_arbor:nethersea_potato_plant"),
                        id("caerula_arbor:nethersea_potato"), DEFAULT_AGE_PROPERTY, -1, true, true, true),
                new CropConfigEntry(id("caerula_arbor:nethersea_wheat"),
                        id("caerula_arbor:nethersea_wheat"), DEFAULT_AGE_PROPERTY, -1, true, true, true),
                new CropConfigEntry(id("caerula_arbor:tentacle_plant"),
                        id("caerula_arbor:ocean_peduncle"), DEFAULT_AGE_PROPERTY, -1, true, true, true),
                new CropConfigEntry(id("caerula_arbor:planted_cell"),
                        id("caerula_arbor:ocean_cell"), DEFAULT_AGE_PROPERTY, -1, true, true, true),
                new CropConfigEntry(id("caerula_arbor:planted_fake_egg"),
                        id("caerula_arbor:fake_egg"), DEFAULT_AGE_PROPERTY, -1, true, true, true));
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    public static CropConfigEntry entryFor(Block block) {
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        CropConfigEntry entry = byBlock.get(id);
        return entry != null && entry.enabled() ? entry : null;
    }

    public static IntegerProperty agePropertyFor(BlockState state) {
        CropConfigEntry entry = entryFor(state.getBlock());
        if (entry == null || !(state.getBlock() instanceof CropBlock)) {
            return null;
        }
        Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.ageProperty());
        if (!(property instanceof IntegerProperty age) || !age.getPossibleValues().contains(0)) {
            warnOnce("age-" + entry.blockId(),
                    "[Caerula Crop Compat] Configured age property " + entry.ageProperty()
                            + " is not a valid IntegerProperty containing 0 on " + entry.blockId());
            return null;
        }
        if (entry.maxAge() >= 0 && !age.getPossibleValues().contains(entry.maxAge())) {
            warnOnce("max-" + entry.blockId(),
                    "[Caerula Crop Compat] Configured max_age " + entry.maxAge()
                            + " is not valid for " + entry.blockId());
            return null;
        }
        return age;
    }

    public static int maxAgeFor(Block block, IntegerProperty property) {
        CropConfigEntry entry = entryFor(block);
        if (entry != null && entry.maxAge() >= 0 && property.getPossibleValues().contains(entry.maxAge())) {
            return entry.maxAge();
        }
        return property.getPossibleValues().stream().mapToInt(Integer::intValue).max()
                .orElseThrow(() -> new IllegalStateException("Age property has no possible values on " + block));
    }

    public static Item seedFor(Block block) {
        CropConfigEntry entry = entryFor(block);
        if (entry == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(entry.seedId()).orElseGet(() -> {
            warnOnce("seed-" + entry.blockId(),
                    "[Caerula Crop Compat] Configured seed does not exist: " + entry.seedId());
            return null;
        });
    }

    public static void addDataPack(net.minecraftforge.event.AddPackFindersEvent event) {
        ensureLoaded();
        if (event.getPackType() != PackType.SERVER_DATA || generatedPackDirectory == null) {
            return;
        }
        writeGeneratedPack();
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("caerula_crop_compat_config",
                    net.minecraft.network.chat.Component.literal("Caerula Crop Compat config"), false,
                    name -> new PathPackResources(name, generatedPackDirectory, false),
                    PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    private static void writeGeneratedPack() {
        try {
            Path blocks = generatedPackDirectory.resolve("data/minecraft/tags/blocks");
            Path seeds = generatedPackDirectory.resolve("data/forge/tags/items");
            Files.createDirectories(blocks);
            Files.createDirectories(seeds);
            JsonObject blockTag = new JsonObject();
            blockTag.addProperty("replace", true);
            JsonArray blockValues = new JsonArray();
            JsonObject seedTag = new JsonObject();
            seedTag.addProperty("replace", true);
            JsonArray seedValues = new JsonArray();
            for (CropConfigEntry entry : entries) {
                if (!entry.enabled() || !validForTags(entry)) {
                    continue;
                }
                if (entry.cropTag()) {
                    blockValues.add(entry.blockId().toString());
                }
                if (entry.seedTag()) {
                    seedValues.add(entry.seedId().toString());
                }
            }
            blockTag.add("values", blockValues);
            seedTag.add("values", seedValues);
            Files.writeString(generatedPackDirectory.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":15,\"description\":\"Caerula Crop Compat config\"}}\n",
                    StandardCharsets.UTF_8);
            Files.writeString(blocks.resolve("crops.json"), GSON.toJson(blockTag) + "\n", StandardCharsets.UTF_8);
            Files.writeString(seeds.resolve("seeds.json"), GSON.toJson(seedTag) + "\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("[Caerula Crop Compat] Could not generate config tag pack", exception);
        }
    }

    private static boolean validForTags(CropConfigEntry entry) {
        if (!BuiltInRegistries.BLOCK.containsKey(entry.blockId())) {
            warnOnce("block-" + entry.blockId(),
                    "[Caerula Crop Compat] Skipping entry because block does not exist: " + entry.blockId());
            return false;
        }
        if (!BuiltInRegistries.ITEM.containsKey(entry.seedId())) {
            warnOnce("item-" + entry.seedId(),
                    "[Caerula Crop Compat] Skipping entry because seed does not exist: " + entry.seedId());
            return false;
        }
        Block block = BuiltInRegistries.BLOCK.get(entry.blockId());
        if (!(block instanceof CropBlock)) {
            warnOnce("noncrop-" + entry.blockId(),
                    "[Caerula Crop Compat] Configured crop " + entry.blockId()
                            + " is not a CropBlock and has no transformer support; semantic/tag compatibility was loaded but CropBlock-based automation may not work.");
            return true;
        }
        Property<?> property = block.getStateDefinition().getProperty(entry.ageProperty());
        if (!(property instanceof IntegerProperty age) || !age.getPossibleValues().contains(0)) {
            warnOnce("invalid-age-" + entry.blockId(),
                    "[Caerula Crop Compat] Skipping entry because age_property is not a valid IntegerProperty containing 0: "
                            + entry.blockId() + " -> " + entry.ageProperty());
            return false;
        }
        if (entry.maxAge() >= 0 && !age.getPossibleValues().contains(entry.maxAge())) {
            warnOnce("invalid-max-" + entry.blockId(),
                    "[Caerula Crop Compat] Skipping entry because max_age is outside the property range: "
                            + entry.blockId() + " -> " + entry.maxAge());
            return false;
        }
        return true;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    static void warn(String key, String message) {
        warnOnce(key, message);
    }

    private static void warnOnce(String key, String message) {
        if (warnings.add(key)) {
            LOGGER.warn(message);
        }
    }

    public record CropConfigEntry(ResourceLocation blockId, ResourceLocation seedId, String ageProperty,
            int maxAge, boolean cropTag, boolean seedTag, boolean enabled) {
        private static CropConfigEntry fromJson(JsonObject object) {
            String block = requiredString(object, "block");
            String seed = requiredString(object, "seed");
            ResourceLocation blockId = parseId(block, "block");
            ResourceLocation seedId = parseId(seed, "seed");
            String ageProperty = stringOrDefault(object, "age_property", DEFAULT_AGE_PROPERTY);
            if (ageProperty.isBlank()) {
                throw new IllegalArgumentException("age_property must not be blank");
            }
            int maxAge = intOrDefault(object, "max_age", -1);
            if (maxAge < -1) {
                throw new IllegalArgumentException("max_age must be -1 or non-negative");
            }
            return new CropConfigEntry(blockId, seedId, ageProperty, maxAge,
                    booleanOrDefault(object, "crop_tag", true),
                    booleanOrDefault(object, "seed_tag", true),
                    booleanOrDefault(object, "enabled", true));
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("block", blockId.toString());
            object.addProperty("seed", seedId.toString());
            object.addProperty("age_property", ageProperty);
            object.addProperty("max_age", maxAge);
            object.addProperty("crop_tag", cropTag);
            object.addProperty("seed_tag", seedTag);
            object.addProperty("enabled", enabled);
            return object;
        }

        private static String requiredString(JsonObject object, String name) {
            if (!object.has(name) || !object.get(name).isJsonPrimitive()) {
                throw new IllegalArgumentException(name + " is required");
            }
            return object.get(name).getAsString();
        }

        private static String stringOrDefault(JsonObject object, String name, String fallback) {
            if (!object.has(name)) {
                return fallback;
            }
            if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException(name + " must be a string");
            }
            return object.get(name).getAsString();
        }

        private static int intOrDefault(JsonObject object, String name, int fallback) {
            if (!object.has(name)) {
                return fallback;
            }
            if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException(name + " must be an integer");
            }
            return object.get(name).getAsInt();
        }

        private static boolean booleanOrDefault(JsonObject object, String name, boolean fallback) {
            if (!object.has(name)) {
                return fallback;
            }
            if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException(name + " must be a boolean");
            }
            return object.get(name).getAsBoolean();
        }

        private static ResourceLocation parseId(String value, String field) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null) {
                throw new IllegalArgumentException(field + " is not a valid registry ID: " + value);
            }
            return id;
        }
    }
}

package com.hcbattlegrounds.config;

import com.hcbattlegrounds.models.Vec3i;
import com.hcbattlegrounds.world.ArenaConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ArenaRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Map<String, ArenaConfig> arenas = new ConcurrentHashMap<>();
    private final Path arenasDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ArenaRegistry(Path baseDir) {
        this.arenasDir = baseDir.resolve("arenas");
    }

    public void loadAll() {
        this.arenas.clear();
        if (!Files.exists(this.arenasDir)) {
            try {
                Files.createDirectories(this.arenasDir);
                this.save(ArenaConfig.createDefault("default"));
                LOGGER.at(Level.INFO).log("Created default arena config");
            } catch (IOException e) {
                LOGGER.at(Level.SEVERE).withCause(e).log("Failed to create arenas folder");
                return;
            }
        }
        try (Stream<Path> files = Files.list(this.arenasDir)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(this::loadConfig);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to list arenas folder");
        }
        LOGGER.at(Level.INFO).log("Loaded %d arena configs", this.arenas.size());
    }

    private void loadConfig(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonArenaConfig json = this.gson.fromJson(reader, JsonArenaConfig.class);
            ArenaConfig config = this.toConfig(json);
            this.arenas.put(config.id(), config);
            LOGGER.at(Level.INFO).log("Loaded arena: %s", config.id());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to load arena: %s", file);
        }
    }

    public void save(ArenaConfig config) throws IOException {
        if (!Files.exists(this.arenasDir)) {
            Files.createDirectories(this.arenasDir);
        }
        Path file = this.arenasDir.resolve(config.id() + ".json");
        JsonArenaConfig json = this.toJson(config);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            this.gson.toJson(json, writer);
        }
        this.arenas.put(config.id(), config);
        LOGGER.at(Level.INFO).log("Saved arena: %s", config.id());
    }

    public void delete(String id) throws IOException {
        Path file = this.arenasDir.resolve(id + ".json");
        if (Files.exists(file)) {
            Files.delete(file);
        }
        this.arenas.remove(id);
        LOGGER.at(Level.INFO).log("Deleted arena: %s", id);
    }

    public Optional<ArenaConfig> get(String id) {
        return Optional.ofNullable(this.arenas.get(id));
    }

    public ArenaConfig getOrDefault(String id) {
        return this.arenas.getOrDefault(id, ArenaConfig.createDefault(id));
    }

    public Collection<ArenaConfig> getAll() {
        return this.arenas.values();
    }

    public Set<String> listIds() {
        return this.arenas.keySet();
    }

    public int count() {
        return this.arenas.size();
    }

    private ArenaConfig toConfig(JsonArenaConfig json) {
        ArrayList<Vec3i> flags = new ArrayList<>();
        if (json.flagPositions != null) {
            for (JsonVec3i pos : json.flagPositions) {
                flags.add(new Vec3i(pos.x, pos.y, pos.z));
            }
        }
        return new ArenaConfig(
            json.id,
            json.prefabName != null ? json.prefabName : "FactionWar",
            new Vec3i(json.redSpawn.x, json.redSpawn.y, json.redSpawn.z),
            new Vec3i(json.blueSpawn.x, json.blueSpawn.y, json.blueSpawn.z),
            flags
        );
    }

    private JsonArenaConfig toJson(ArenaConfig config) {
        JsonArenaConfig json = new JsonArenaConfig();
        json.id = config.id();
        json.prefabName = config.prefabName();
        json.redSpawn = new JsonVec3i(config.redSpawn().x(), config.redSpawn().y(), config.redSpawn().z());
        json.blueSpawn = new JsonVec3i(config.blueSpawn().x(), config.blueSpawn().y(), config.blueSpawn().z());
        json.flagPositions = new ArrayList<>();
        for (Vec3i pos : config.flagPositions()) {
            json.flagPositions.add(new JsonVec3i(pos.x(), pos.y(), pos.z()));
        }
        return json;
    }

    public static class JsonArenaConfig {
        public String id;
        public String prefabName;
        public JsonVec3i redSpawn;
        public JsonVec3i blueSpawn;
        public List<JsonVec3i> flagPositions;
    }

    public static class JsonVec3i {
        public int x;
        public int y;
        public int z;

        public JsonVec3i() {
        }

        public JsonVec3i(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

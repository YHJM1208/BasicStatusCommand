package yhjm1208.basicstatuscommand.metrics;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 全服实体 / 区块统计。必须在主线程调用。 */
public final class WorldStats {

    public static final String MONSTER = "monster";
    public static final String ANIMALS = "animals";
    public static final String AMBIENT = "ambient";
    public static final String WATER = "water";
    public static final String VILLAGER = "villager";
    public static final String OTHER = "other";

    private WorldStats() {
    }

    public static EntitySnapshot entities() {
        EntitySnapshot snapshot = new EntitySnapshot();
        for (World world : Bukkit.getWorlds()) {
            WorldEntities perWorld = new WorldEntities(world.getName());
            for (Entity entity : world.getEntities()) {
                String category = category(entity);
                perWorld.total++;
                perWorld.categories.put(category, perWorld.categories.getOrDefault(category, 0) + 1);
                snapshot.total++;
                snapshot.categories.put(category, snapshot.categories.getOrDefault(category, 0) + 1);
            }
            snapshot.worlds.add(perWorld);
        }
        return snapshot;
    }

    public static ChunkSnapshot chunks() {
        ChunkSnapshot snapshot = new ChunkSnapshot();
        for (World world : Bukkit.getWorlds()) {
            int loaded = world.getLoadedChunks().length;
            snapshot.total += loaded;
            snapshot.worlds.add(new WorldChunks(world.getName(), loaded));
        }
        return snapshot;
    }

    private static String category(Entity entity) {
        if (entity instanceof Monster) {
            return MONSTER;
        }
        if (entity instanceof Animals) {
            return ANIMALS;
        }
        if (entity instanceof WaterMob) {
            return WATER;
        }
        if (entity instanceof Villager) {
            return VILLAGER;
        }
        if (entity instanceof Ambient) {
            return AMBIENT;
        }
        return OTHER;
    }

    public static final class EntitySnapshot {
        public int total;
        public final Map<String, Integer> categories = new LinkedHashMap<>();
        public final List<WorldEntities> worlds = new ArrayList<>();
    }

    public static final class WorldEntities {
        public final String world;
        public int total;
        public final Map<String, Integer> categories = new LinkedHashMap<>();

        WorldEntities(String world) {
            this.world = world;
        }
    }

    public static final class ChunkSnapshot {
        public int total;
        public final List<WorldChunks> worlds = new ArrayList<>();
    }

    public static final class WorldChunks {
        public final String world;
        public final int loaded;

        WorldChunks(String world, int loaded) {
            this.world = world;
            this.loaded = loaded;
        }
    }
}

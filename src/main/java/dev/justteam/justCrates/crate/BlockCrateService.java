package dev.justteam.justCrates.crate;

import dev.justteam.justCrates.core.PluginPaths;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class BlockCrateService {

    private final JavaPlugin plugin;
    private final PluginPaths paths;
    private final Map<String, String> bindings = new HashMap<>();
    private final CrateService crateService;
    private BukkitTask particleTask;

    public BlockCrateService(JavaPlugin plugin, PluginPaths paths, CrateService crateService) {
        this.plugin = plugin;
        this.paths = paths;
        this.crateService = crateService;
    }

    public void load() {
        bindings.clear();
        File file = paths.getBlocksFile();
        if (file.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                bindings.put(key, cfg.getString(key));
            }
        }

        if (particleTask != null) {
            particleTask.cancel();
        }
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, 0L, 2L);
    }

    public void save() {
        File file = paths.getBlocksFile();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            cfg.set(entry.getKey(), entry.getValue());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save blocks.yml: " + e.getMessage());
        }
    }

    public void bind(Location location, String crateId) {
        bindings.put(serialize(location), crateId.toLowerCase());
    }

    public void unbind(Location location) {
        bindings.remove(serialize(location));
    }

    public String getCrateId(Location location) {
        return bindings.get(serialize(location));
    }

    private String serialize(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";"
                + location.getBlockZ();
    }

    public Location deserialize(String key) {
        String[] parts = key.split(";");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        return new Location(world, x, y, z);
    }

    private void spawnParticles() {
        if (bindings.isEmpty() || crateService == null) {
            return;
        }

        long millis = System.currentTimeMillis();

        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            Location loc = deserialize(entry.getKey());
            if (loc == null || loc.getWorld() == null) {
                continue;
            }

            // check if chunk is loaded to prevent lag
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }

            // check if block is empty (air), which means the crate was physically removed
            if (loc.getBlock().getType().isAir()) {
                continue;
            }

            String crateId = entry.getValue();
            CrateDefinition crate = crateService.getCrate(crateId);
            if (crate == null || crate.getParticle() == null || crate.getParticle().isBlank()) {
                continue;
            }

            try {
                Particle particle = Particle.valueOf(crate.getParticle());

                // Spawn a dense trail of particles spanning the last 120ms
                for (int i = 0; i < 6; i++) {
                    double t = (millis - (i * 20)) / 1000.0;

                    // Spin 3 times per second
                    double angle = t * Math.PI * 2 * 3.0;

                    // Moves up at 0.75 blocks per second (takes 2s to reach 1.5 blocks)
                    double height = (t * 0.75) % 1.5;
                    if (height < 0)
                        height += 1.5;

                    double radius = 0.65;
                    double xOffset = radius * Math.cos(angle);
                    double zOffset = radius * Math.sin(angle);

                    Location center = loc.clone().add(0.5, height, 0.5);
                    Location particleLoc = center.add(xOffset, 0, zOffset);

                    // Spawn a single particle without spread
                    loc.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
                }
            } catch (Exception ignored) {
                // Invalid particle name, ignore
            }
        }
    }
}

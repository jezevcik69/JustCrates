package dev.justteam.justCrates.crate;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BlockCrateService {

    private final JavaPlugin plugin;
    private final PluginPaths paths;
    private final Map<String, String> bindings = new HashMap<>();
    private final Map<String, List<UUID>> hologramEntities = new HashMap<>();
    private final CrateService crateService;
    private BukkitTask particleTask;
    private BukkitTask hologramTask;

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

        if (hologramTask != null) {
            hologramTask.cancel();
        }
        clearAllHolograms();
        hologramTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshHolograms, 0L, 20L);
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
        String key = serialize(location);
        bindings.put(key, crateId.toLowerCase());
        refreshHologram(key);
    }

    public void unbind(Location location) {
        String key = serialize(location);
        bindings.remove(key);
        removeHologram(key);
    }

    public int unbindAll(String crateId) {
        if (crateId == null || crateId.isBlank()) {
            return 0;
        }
        String normalized = crateId.toLowerCase(Locale.ROOT);
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            if (normalized.equals(entry.getValue())) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            bindings.remove(key);
            removeHologram(key);
        }
        return toRemove.size();
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

    public void refreshHolograms() {
        if (bindings.isEmpty() || crateService == null) {
            clearAllHolograms();
            return;
        }

        List<String> staleKeys = new ArrayList<>();
        for (String key : hologramEntities.keySet()) {
            if (!bindings.containsKey(key)) {
                staleKeys.add(key);
            }
        }
        for (String key : staleKeys) {
            removeHologram(key);
        }

        for (String key : bindings.keySet()) {
            refreshHologram(key);
        }
    }

    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (hologramTask != null) {
            hologramTask.cancel();
            hologramTask = null;
        }
        clearAllHolograms();
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
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }
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
                for (int i = 0; i < 6; i++) {
                    double t = (millis - (i * 20)) / 1000.0;
                    double angle = t * Math.PI * 2 * 3.0;
                    double height = (t * 0.75) % 1.5;
                    if (height < 0)
                        height += 1.5;

                    double radius = 0.65;
                    double xOffset = radius * Math.cos(angle);
                    double zOffset = radius * Math.sin(angle);

                    Location center = loc.clone().add(0.5, height, 0.5);
                    Location particleLoc = center.add(xOffset, 0, zOffset);
                    loc.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void refreshHologram(String key) {
        Location loc = deserialize(key);
        if (loc == null || loc.getWorld() == null) {
            removeHologram(key);
            return;
        }

        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        if (loc.getBlock().getType().isAir()) {
            removeHologram(key);
            return;
        }

        String crateId = bindings.get(key);
        CrateDefinition crate = crateId == null ? null : crateService.getCrate(crateId);
        if (crate == null) {
            removeHologram(key);
            return;
        }

        List<String> lines = resolveHologramLines(crate);
        if (lines.isEmpty()) {
            removeHologram(key);
            return;
        }

        List<ArmorStand> existing = getHologramStands(key);
        if (existing.size() != lines.size()) {
            recreateHologram(key, loc, lines);
            return;
        }

        boolean changed = false;
        for (int i = 0; i < existing.size(); i++) {
            ArmorStand stand = existing.get(i);
            if (!stand.getLocation().getChunk().isLoaded()) {
                continue;
            }
            String line = Text.color(lines.get(i));
            if (!line.equals(stand.getCustomName())) {
                stand.setCustomName(line);
                changed = true;
            }
            Location target = hologramLineLocation(loc, lines.size(), i);
            if (stand.getLocation().distanceSquared(target) > 0.0001D) {
                stand.teleport(target);
                changed = true;
            }
        }

        if (changed) {
            List<UUID> ids = new ArrayList<>();
            for (ArmorStand stand : existing) {
                ids.add(stand.getUniqueId());
            }
            hologramEntities.put(key, ids);
        }
    }

    private void recreateHologram(String key, Location crateLoc, List<String> lines) {
        removeHologram(key);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineIndex = i;
            Location lineLoc = hologramLineLocation(crateLoc, lines.size(), i);
            ArmorStand stand = crateLoc.getWorld().spawn(lineLoc, ArmorStand.class, armorStand -> {
                armorStand.setInvisible(true);
                armorStand.setMarker(true);
                armorStand.setGravity(false);
                armorStand.setSmall(true);
                armorStand.setInvulnerable(true);
                armorStand.setSilent(true);
                armorStand.setPersistent(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setCustomName(Text.color(lines.get(lineIndex)));
            });
            ids.add(stand.getUniqueId());
        }
        hologramEntities.put(key, ids);
    }

    private Location hologramLineLocation(Location crateLoc, int lineCount, int lineIndex) {
        double topOffset = 1.75 + (lineCount - 1) * 0.25;
        double y = crateLoc.getY() + topOffset - (lineIndex * 0.25);
        return new Location(crateLoc.getWorld(), crateLoc.getX() + 0.5, y, crateLoc.getZ() + 0.5);
    }

    private List<String> resolveHologramLines(CrateDefinition crate) {
        List<String> raw = crate.getHologramLines();
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : raw) {
            if (line == null || line.isBlank()) {
                continue;
            }
            lines.add(line.replace("%crate_name%", crate.getName()).replace("%crate_id%", crate.getId()));
        }
        return lines;
    }

    private List<ArmorStand> getHologramStands(String key) {
        List<UUID> ids = hologramEntities.get(key);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ArmorStand> stands = new ArrayList<>();
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof ArmorStand stand && !stand.isDead()) {
                stands.add(stand);
            }
        }
        return stands;
    }

    private void removeHologram(String key) {
        List<UUID> ids = hologramEntities.remove(key);
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    private void clearAllHolograms() {
        List<String> keys = new ArrayList<>(hologramEntities.keySet());
        for (String key : keys) {
            removeHologram(key);
        }
        hologramEntities.clear();
    }
}


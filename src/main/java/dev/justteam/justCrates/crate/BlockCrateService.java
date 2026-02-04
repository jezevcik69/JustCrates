package dev.justteam.justCrates.crate;

import dev.justteam.justCrates.core.PluginPaths;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class BlockCrateService {

    private final JavaPlugin plugin;
    private final PluginPaths paths;
    private final Map<String, String> bindings = new HashMap<>();

    public BlockCrateService(JavaPlugin plugin, PluginPaths paths) {
        this.plugin = plugin;
        this.paths = paths;
    }

    public void load() {
        bindings.clear();
        File file = paths.getBlocksFile();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            bindings.put(key, cfg.getString(key));
        }
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
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
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
}

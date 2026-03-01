package dev.justteam.justCrates.core;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PluginPaths {
    private final JavaPlugin plugin;
    private final File dataFolder;
    private final File cratesFolder;
    private final File keysFolder;
    private final File hologramsFolder;
    private final File blocksFile;

    public PluginPaths(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.cratesFolder = new File(dataFolder, "crates");
        this.keysFolder = new File(dataFolder, "keys");
        this.hologramsFolder = new File(dataFolder, "holograms");
        this.blocksFile = new File(dataFolder, "blocks.yml");
    }

    public void ensure() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
        }
        if (!keysFolder.exists()) {
            keysFolder.mkdirs();
        }
        if (!hologramsFolder.exists()) {
            hologramsFolder.mkdirs();
        }

        copyDefault("config.yml");

        if (cratesFolder.listFiles((dir, name) -> name.endsWith(".yml")) == null
                || cratesFolder.listFiles((dir, name) -> name.endsWith(".yml")).length == 0) {
            copyDefault("crates/example.yml");
        }
        if (keysFolder.listFiles((dir, name) -> name.endsWith(".yml")) == null
                || keysFolder.listFiles((dir, name) -> name.endsWith(".yml")).length == 0) {
            copyDefault("keys/example.yml");
        }
    }

    private void copyDefault(String path) {
        File target = new File(dataFolder, path);
        if (target.exists()) {
            return;
        }
        target.getParentFile().mkdirs();
        plugin.saveResource(path, false);
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public File getCratesFolder() {
        return cratesFolder;
    }

    public File getKeysFolder() {
        return keysFolder;
    }

    public File getHologramsFolder() {
        return hologramsFolder;
    }

    public File getBlocksFile() {
        return blocksFile;
    }
}
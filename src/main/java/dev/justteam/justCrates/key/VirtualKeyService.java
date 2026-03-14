package dev.justteam.justCrates.key;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.YamlFile;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class VirtualKeyService {
    private final JavaPlugin plugin;
    private final KeyService keyService;
    private final File file;
    private final YamlFile yaml;

    public VirtualKeyService(JavaPlugin plugin, PluginPaths paths, KeyService keyService) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.file = new File(paths.getDataFolder(), "virtual-keys.yml");
        ensureFile();
        this.yaml = new YamlFile(file);
    }

    public void reload() {
        yaml.reload();
    }

    public void save() {
        yaml.save();
    }

    public int getKeys(UUID playerId, String keyId) {
        return yaml.getConfig().getInt(path(playerId, keyId), 0);
    }

    public int getPhysicalKeys(Player player, String keyId) {
        if (player == null || keyId == null || keyId.isBlank()) {
            return 0;
        }
        return countPhysicalKeys(player, keyId);
    }

    public int getTotalKeys(Player player, String keyId) {
        if (player == null || keyId == null || keyId.isBlank()) {
            return 0;
        }
        return countPhysicalKeys(player, keyId) + getKeys(player.getUniqueId(), keyId);
    }

    public int getPhysicalKeys(HumanEntity player, String keyId) {
        return player instanceof Player onlinePlayer ? getPhysicalKeys(onlinePlayer, keyId) : 0;
    }

    public int getTotalKeys(HumanEntity player, String keyId) {
        return player instanceof Player onlinePlayer ? getTotalKeys(onlinePlayer, keyId) : 0;
    }

    public void addKeys(UUID playerId, String keyId, int amount) {
        if (amount <= 0) {
            return;
        }
        YamlConfiguration cfg = yaml.getConfig();
        int current = cfg.getInt(path(playerId, keyId), 0);
        cfg.set(path(playerId, keyId), current + amount);
        yaml.save();
    }

    public void setKeys(UUID playerId, String keyId, int amount) {
        if (playerId == null || keyId == null || keyId.isBlank()) {
            return;
        }
        YamlConfiguration cfg = yaml.getConfig();
        cfg.set(path(playerId, keyId), amount > 0 ? amount : null);
        yaml.save();
    }

    public boolean takeKeys(UUID playerId, String keyId, int amount) {
        if (amount <= 0) {
            return false;
        }
        YamlConfiguration cfg = yaml.getConfig();
        int current = cfg.getInt(path(playerId, keyId), 0);
        if (current < amount) {
            return false;
        }
        int next = current - amount;
        cfg.set(path(playerId, keyId), next > 0 ? next : null);
        yaml.save();
        return true;
    }

    public int clearKeys(UUID playerId, String keyId) {
        if (playerId == null || keyId == null || keyId.isBlank()) {
            return 0;
        }
        YamlConfiguration cfg = yaml.getConfig();
        int current = cfg.getInt(path(playerId, keyId), 0);
        if (current <= 0) {
            return 0;
        }
        cfg.set(path(playerId, keyId), null);
        yaml.save();
        return current;
    }

    private int countPhysicalKeys(Player player, String keyId) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getAmount() <= 0) {
                continue;
            }
            if (keyService.isKey(item, keyId)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private int removePhysicalKeys(Player player, String keyId, int amount) {
        Inventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getAmount() <= 0) {
                continue;
            }
            if (!keyService.isKey(item, keyId)) {
                continue;
            }
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) {
                contents[i] = null;
            }
            remaining -= take;
            if (remaining <= 0) {
                break;
            }
        }
        inv.setContents(contents);
        return amount - remaining;
    }

    private String path(UUID playerId, String keyId) {
        return "players." + playerId.toString() + "." + keyId.toLowerCase();
    }

    private void ensureFile() {
        if (file.exists()) {
            return;
        }
        file.getParentFile().mkdirs();
        try {
            if (!file.createNewFile()) {
                return;
            }
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create virtual keys file: " + e.getMessage());
        }
    }
}

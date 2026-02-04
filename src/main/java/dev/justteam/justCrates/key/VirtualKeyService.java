package dev.justteam.justCrates.key;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.YamlFile;
import org.bukkit.configuration.file.YamlConfiguration;
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
    private YamlFile yaml;

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

    public boolean hasKeys(UUID playerId, String keyId, int amount) {
        return getKeys(playerId, keyId) >= amount;
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

    public boolean convertFromInventory(Player player, String keyId, int amount) {
        if (amount <= 0) {
            return false;
        }
        int available = countPhysicalKeys(player, keyId);
        if (available < amount) {
            return false;
        }
        removePhysicalKeys(player, keyId, amount);
        addKeys(player.getUniqueId(), keyId, amount);
        return true;
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

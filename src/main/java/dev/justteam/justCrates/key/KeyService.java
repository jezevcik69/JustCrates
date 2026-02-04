package dev.justteam.justCrates.key;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.item.ItemDefinition;
import dev.justteam.justCrates.item.ItemFactory;
import dev.justteam.justCrates.provider.ProviderRegistry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class KeyService {

    private final JavaPlugin plugin;
    private final ProviderRegistry providerRegistry;
    private final PluginPaths paths;
    private final KeyRegistry registry;
    private final ItemFactory itemFactory;

    public KeyService(JavaPlugin plugin, ProviderRegistry providerRegistry, PluginPaths paths) {
        this.plugin = plugin;
        this.providerRegistry = providerRegistry;
        this.paths = paths;
        this.registry = new KeyRegistry();
        this.itemFactory = new ItemFactory(plugin, providerRegistry);
    }

    public void loadAll() {
        registry.clear();
        File[] files = paths.getKeysFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String id = cfg.getString("id", file.getName().replace(".yml", "")).toLowerCase();
            String name = cfg.getString("display.name", "&eKey " + id);
            List<String> lore = cfg.getStringList("display.lore");
            ItemStack itemStack = cfg.getItemStack("itemstack");
            ItemDefinition itemDef = ItemDefinition.fromSection(cfg.getConfigurationSection("item"));

            KeyDefinition key = new KeyDefinition(id, name, lore, itemDef, itemStack);
            registry.register(key);
        }

        plugin.getLogger().info("Loaded keys: " + registry.all().size());
    }

    public KeyDefinition getKey(String id) {
        return registry.get(id);
    }

    public List<KeyDefinition> getKeys() {
        return List.copyOf(registry.all());
    }

    public ItemStack createKeyItem(KeyDefinition key) {
        if (key == null) {
            return null;
        }

        ItemStack stack = null;
        if (key.getItemStack() != null) {
            stack = key.getItemStack().clone();
        } else if (key.getItemDefinition() != null) {
            stack = itemFactory.createItem(key.getItemDefinition());
        }

        if (stack == null) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (key.getName() != null) {
                meta.setDisplayName(Text.color(key.getName()));
            }
            if (key.getLore() != null && !key.getLore().isEmpty()) {
                meta.setLore(key.getLore().stream().map(Text::color).toList());
            }
            stack.setItemMeta(meta);
        }

        itemFactory.markKey(stack, key.getId());
        return stack;
    }

    public boolean isKey(ItemStack stack, String keyId) {
        return itemFactory.isKey(stack, keyId);
    }

    public boolean saveKeyFromItem(String id, ItemStack stack) {
        if (id == null || id.isBlank() || stack == null) {
            return false;
        }

        File file = new File(paths.getKeysFolder(), id.toLowerCase() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", id.toLowerCase());
        cfg.set("itemstack", stack);

        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save key: " + e.getMessage());
            return false;
        }
    }
}

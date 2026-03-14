package dev.justteam.justCrates.key;

import dev.justteam.justCrates.core.PluginPaths;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.item.ItemDefinition;
import dev.justteam.justCrates.item.ItemFactory;
import dev.justteam.justCrates.provider.ProviderRegistry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            boolean virtual = cfg.getBoolean("virtual", false);

            KeyDefinition key = new KeyDefinition(id, name, lore, itemDef, itemStack, virtual);
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

    public boolean isVirtualKey(ItemStack stack) {
        return itemFactory.isVirtualKey(stack);
    }

    public ItemStack createVirtualKeyItem(KeyDefinition key) {
        if (key == null) {
            return null;
        }
        ItemStack stack = createKeyItem(key);
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = meta.hasDisplayName() ? meta.getDisplayName() : Text.color(key.getName());
            meta.setDisplayName(Text.color("&d\u2726 ") + name + Text.color(" &7(Virtual)"));
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(Text.color("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
            lore.add(Text.color("&dVirtual Key"));
            lore.add(Text.color("&7Can be stored in chests"));
            stack.setItemMeta(meta);
        }
        itemFactory.markVirtualKey(stack, key.getId());
        return stack;
    }

    public String resolveKeyId(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        for (KeyDefinition key : registry.all()) {
            if (itemFactory.isKey(stack, key.getId())) {
                return key.getId();
            }
        }
        return null;
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

    public boolean createDefaultKey(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        String normalized = id.toLowerCase(Locale.ROOT);
        File file = new File(paths.getKeysFolder(), normalized + ".yml");
        if (file.exists()) {
            return false;
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", normalized);
        cfg.set("display.name", "&e" + normalized + " key");
        cfg.set("display.lore", List.of("&7Configure this key in editor"));
        cfg.set("item.material", "TRIPWIRE_HOOK");
        cfg.set("item.amount", 1);
        cfg.set("item.custom-model-data", 0);
        cfg.set("item.provider", "");
        cfg.set("item.provider-item", "");
        cfg.set("virtual", false);

        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create key: " + e.getMessage());
            return false;
        }
    }

    public boolean updateKeyItemStack(String id, ItemStack stack) {
        if (id == null || id.isBlank() || stack == null) {
            return false;
        }

        File file = new File(paths.getKeysFolder(), id.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return false;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("itemstack", stack);
        cfg.set("item", null);

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : "&e" + id + " key";
            List<String> lore = meta.hasLore() ? meta.getLore() : List.of("&7Configured from GUI editor");
            cfg.set("display.name", displayName);
            cfg.set("display.lore", lore != null ? new ArrayList<>(lore) : List.of("&7Configured from GUI editor"));
        }

        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update key item: " + e.getMessage());
            return false;
        }
    }

    public boolean updateKeyDisplayName(String id, String displayName) {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()) {
            return false;
        }

        File file = new File(paths.getKeysFolder(), id.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return false;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("display.name", displayName);

        ItemStack itemStack = cfg.getItemStack("itemstack");
        if (itemStack != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Text.color(displayName));
                itemStack.setItemMeta(meta);
                cfg.set("itemstack", itemStack);
            }
        }

        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update key name: " + e.getMessage());
            return false;
        }
    }

    public boolean updateKeyLore(String id, List<String> loreLines) {
        if (id == null || id.isBlank()) {
            return false;
        }

        File file = new File(paths.getKeysFolder(), id.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return false;
        }

        List<String> lore = loreLines == null ? new ArrayList<>() : new ArrayList<>(loreLines);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("display.lore", lore);

        ItemStack itemStack = cfg.getItemStack("itemstack");
        if (itemStack != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (lore.isEmpty()) {
                    meta.setLore(null);
                } else {
                    meta.setLore(lore.stream().map(Text::color).toList());
                }
                itemStack.setItemMeta(meta);
                cfg.set("itemstack", itemStack);
            }
        }

        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update key lore: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteKey(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        File file = new File(paths.getKeysFolder(), id.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return false;
        }
        return file.delete();
    }

    public boolean updateKeyVirtualMode(String id, boolean virtual) {
        if (id == null || id.isBlank()) {
            return false;
        }

        File file = new File(paths.getKeysFolder(), id.toLowerCase(Locale.ROOT) + ".yml");
        if (!file.exists()) {
            return false;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("virtual", virtual);
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update key virtual mode: " + e.getMessage());
            return false;
        }
    }

    public boolean givePhysicalKeys(Player target, KeyDefinition key, int amount) {
        if (target == null || key == null || amount <= 0) {
            return false;
        }

        ItemStack template = createKeyItem(key);
        if (template == null) {
            return false;
        }

        int maxStackSize = Math.max(1, template.getMaxStackSize());
        int remaining = amount;

        while (remaining > 0) {
            int chunkSize = Math.min(maxStackSize, remaining);
            ItemStack chunk = template.clone();
            chunk.setAmount(chunkSize);

            var leftovers = target.getInventory().addItem(chunk);
            for (ItemStack leftover : leftovers.values()) {
                if (leftover != null && leftover.getAmount() > 0) {
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                }
            }

            remaining -= chunkSize;
        }

        return true;
    }

    public int clearPhysicalKeys(Player target, String keyId, boolean includeEnderChest) {
        if (target == null || keyId == null || keyId.isBlank()) {
            return 0;
        }

        int removed = clearPhysicalKeys(target.getInventory(), keyId);
        if (includeEnderChest) {
            removed += clearPhysicalKeys(target.getEnderChest(), keyId);
        }
        target.updateInventory();
        return removed;
    }

    private int clearPhysicalKeys(Inventory inventory, String keyId) {
        if (inventory == null) {
            return 0;
        }

        ItemStack[] contents = inventory.getContents();
        int removed = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getAmount() <= 0) {
                continue;
            }
            if (!isKey(item, keyId)) {
                continue;
            }
            removed += item.getAmount();
            contents[i] = null;
        }
        inventory.setContents(contents);
        return removed;
    }
}

package dev.justteam.justCrates.item;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.provider.ItemProvider;
import dev.justteam.justCrates.provider.ProviderRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ItemFactory {
    private final JavaPlugin plugin;
    private final ProviderRegistry providerRegistry;
    private final NamespacedKey keyIdTag;
    private final NamespacedKey virtualKeyTag;

    public ItemFactory(JavaPlugin plugin, ProviderRegistry providerRegistry) {
        this.plugin = plugin;
        this.providerRegistry = providerRegistry;
        this.keyIdTag = new NamespacedKey(plugin, "key_id");
        this.virtualKeyTag = new NamespacedKey(plugin, "virtual_key");
    }

    public ItemStack createItem(ItemDefinition definition) {
        if (definition == null) {
            return null;
        }

        ItemStack stack = null;
        if (!definition.getProvider().isEmpty()) {
            ItemProvider provider = providerRegistry.getProvider(definition.getProvider());
            if (provider != null && provider.isAvailable()) {
                stack = provider.createItem(definition);
            } else {
                plugin.getLogger().warning("Provider not available: " + definition.getProvider());
            }
        }

        if (stack == null) {
            stack = new ItemStack(definition.getMaterial(), definition.getAmount());
        }

        applyMeta(stack, definition);
        return stack;
    }

    public ItemStack applyMeta(ItemStack stack, ItemDefinition definition) {
        if (stack == null) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        if (definition.getName() != null) {
            meta.setDisplayName(Text.color(definition.getName()));
        }
        List<String> lore = definition.getLore();
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(Text::color).toList());
        }
        if (definition.getCustomModelData() != null && definition.getCustomModelData() > 0) {
            meta.setCustomModelData(definition.getCustomModelData());
        }

        stack.setItemMeta(meta);
        stack.setAmount(definition.getAmount());
        return stack;
    }

    public ItemStack markKey(ItemStack stack, String keyId) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.getPersistentDataContainer().set(keyIdTag, PersistentDataType.STRING, keyId.toLowerCase());
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isKey(ItemStack stack, String keyId) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        String stored = stack.getItemMeta().getPersistentDataContainer().get(keyIdTag, PersistentDataType.STRING);
        return stored != null && stored.equalsIgnoreCase(keyId);
    }

    public ItemStack markVirtualKey(ItemStack stack, String keyId) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.getPersistentDataContainer().set(keyIdTag, PersistentDataType.STRING, keyId.toLowerCase());
        meta.getPersistentDataContainer().set(virtualKeyTag, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isVirtualKey(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        Byte val = stack.getItemMeta().getPersistentDataContainer().get(virtualKeyTag, PersistentDataType.BYTE);
        return val != null && val == 1;
    }
}
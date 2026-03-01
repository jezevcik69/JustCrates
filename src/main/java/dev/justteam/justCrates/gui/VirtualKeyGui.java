package dev.justteam.justCrates.gui;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class VirtualKeyGui {

    private static final String TITLE = "&8Virtual Keys \u2022 Converter";

    private VirtualKeyGui() {
    }

    public static void open(JavaPlugin plugin, Player player, KeyService keyService, VirtualKeyService virtualKeyService) {
        List<KeyDefinition> keys = keyService.getKeys();
        int rows = Math.max(1, (int) Math.ceil(keys.size() / 7.0));
        rows = Math.min(6, rows);
        int size = rows * 9;

        VirtualKeyMenuHolder holder = new VirtualKeyMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, size, Text.color(TITLE));
        holder.setInventory(inv);

        fill(inv, Material.BLACK_STAINED_GLASS_PANE, " ");
        NamespacedKey idKey = new NamespacedKey(plugin, "virtual_key_id");

        int index = 0;
        for (KeyDefinition key : keys) {
            int row = index / 7;
            int col = index % 7;
            int slot = row * 9 + (col + 1);
            ItemStack item = keyService.createKeyItem(key);
            if (item == null) {
                index++;
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                int amount = virtualKeyService.getKeys(player.getUniqueId(), key.getId());
                lore.add(Text.color("&7Virtual: &e" + amount));
                lore.add(Text.color("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
                lore.add(Text.color("&bLeft-click: Convert to virtual key item"));
                lore.add(Text.color("&7Virtual keys can be stored in chests"));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, key.getId().toLowerCase());
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            index++;
            if (index >= rows * 7) {
                break;
            }
        }

        player.openInventory(inv);
    }

    public static String extractKeyId(JavaPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        NamespacedKey idKey = new NamespacedKey(plugin, "virtual_key_id");
        return stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    private static void fill(Inventory inv, Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, stack);
        }
    }
}

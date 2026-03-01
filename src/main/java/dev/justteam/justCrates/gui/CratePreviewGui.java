package dev.justteam.justCrates.gui;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.CrateDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class CratePreviewGui {
    private CratePreviewGui() {}

    public static void open(Player player, CrateDefinition crate) {
        List<ItemStack> rewards = RewardPreview.buildPreview(crate);
        int size = 54;

        CratePreviewHolder holder = new CratePreviewHolder(crate.getId());
        Inventory inv = Bukkit.createInventory(holder, size, Text.color("&8Preview \u2022 " + crate.getName()));
        holder.setInventory(inv);

        fillGradientBorder(inv);

        int index = 0;
        int[] contentSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        for (int slot : contentSlots) {
            if (index >= rewards.size()) {
                break;
            }
            inv.setItem(slot, rewards.get(index++));
        }

        player.openInventory(inv);
    }

    private static void fillGradientBorder(Inventory inv) {
        ItemStack dark = pane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack corner = pane(Material.CYAN_STAINED_GLASS_PANE);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, dark);
        }
        int[] corners = {0, 8, 45, 53};
        for (int c : corners) {
            inv.setItem(c, corner);
        }
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, accent);
            inv.setItem(row * 9 + 8, accent);
        }
        int[] contentSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        for (int slot : contentSlots) {
            inv.setItem(slot, null);
        }
        inv.setItem(3, accent);
        inv.setItem(5, accent);
    }

    private static ItemStack pane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
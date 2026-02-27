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

    private CratePreviewGui() {
    }

    public static void open(Player player, CrateDefinition crate) {
        List<ItemStack> rewards = RewardPreview.buildPreview(crate);
        int size = 27;

        CratePreviewHolder holder = new CratePreviewHolder(crate.getId());
        Inventory inv = Bukkit.createInventory(holder, size, Text.color("&8Preview • " + crate.getName()));
        holder.setInventory(inv);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE, " ");
        inv.setItem(4, infoItem(crate));

        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 1; col <= 7; col++) {
                if (index >= rewards.size()) {
                    break;
                }
                int slot = row * 9 + col;
                inv.setItem(slot, rewards.get(index++));
            }
        }

        player.openInventory(inv);
    }

    private static ItemStack infoItem(CrateDefinition crate) {
        ItemStack stack = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color("&b" + crate.getName()));
            meta.setLore(List.of(
                Text.color("&7ID: &f" + crate.getId()),
                Text.color("&7Rewards preview"),
                Text.color("&eLeft-click crate to open this menu")
            ));
            stack.setItemMeta(meta);
        }
        return stack;
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

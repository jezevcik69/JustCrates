package dev.justteam.justCrates.gui;

import dev.justteam.justCrates.core.PreviewGuiSettings;
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

    public static void open(Player player, CrateDefinition crate, PreviewGuiSettings settings) {
        List<ItemStack> rewards = RewardPreview.buildPreview(crate);
        int size = settings.getSize();
        String title = settings.getTitle()
                .replace("%crate%", crate.getName())
                .replace("%crate_id%", crate.getId());

        CratePreviewHolder holder = new CratePreviewHolder(crate.getId());
        Inventory inv = Bukkit.createInventory(holder, size, Text.color(Text.toSmallCaps(title)));
        holder.setInventory(inv);

        fillGradientBorder(inv, settings);

        int index = 0;
        for (int slot : settings.getContentSlots()) {
            if (index >= rewards.size()) {
                break;
            }
            inv.setItem(slot, rewards.get(index++));
        }

        player.openInventory(inv);
    }

    private static void fillGradientBorder(Inventory inv, PreviewGuiSettings settings) {
        ItemStack dark = pane(settings.getDarkMaterial(), settings.getPaneName());
        ItemStack accent = pane(settings.getAccentMaterial(), settings.getPaneName());
        ItemStack corner = pane(settings.getCornerMaterial(), settings.getPaneName());

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, dark);
        }
        for (int slot : settings.getCornerSlots()) {
            inv.setItem(slot, corner);
        }
        for (int slot : settings.getAccentSlots()) {
            inv.setItem(slot, accent);
        }
        for (int slot : settings.getContentSlots()) {
            inv.setItem(slot, null);
        }
    }

    private static ItemStack pane(Material material, String paneName) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = paneName == null || paneName.isBlank() ? " " : paneName;
            meta.setDisplayName(Text.color(name));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}

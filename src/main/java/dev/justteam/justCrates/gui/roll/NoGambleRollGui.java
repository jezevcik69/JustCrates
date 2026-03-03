package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.core.Messages;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.gui.RewardPreview;
import dev.justteam.justCrates.reward.RewardDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class NoGambleRollGui {
    private NoGambleRollGui() {}

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        List<RewardDefinition> rewards = crate.getRewards();
        if (rewards.isEmpty()) {
            player.sendMessage(Messages.get("hologram-roll-no-rewards"));
            return;
        }

        int innerSlots = rewards.size();
        int rows = Math.max(3, (int) Math.ceil((innerSlots + 7.0) / 7.0) + 2);
        if (rows > 6) rows = 6;
        int invSize = rows * 9;

        NoGambleInventoryHolder holder = new NoGambleInventoryHolder(crate, crateService);
        Inventory inv = Bukkit.createInventory(holder, invSize, Text.color(crate.getRollDefinition().getTitle()));

        ItemStack dark = pane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack corner = pane(Material.CYAN_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < invSize; i++) {
            if (i < 9 || i >= invSize - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, dark);
            }
        }
        inv.setItem(0, corner);
        inv.setItem(8, corner);
        inv.setItem(invSize - 9, corner);
        inv.setItem(invSize - 1, corner);
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, accent);
            inv.setItem(row * 9 + 8, accent);
        }

        int rewardIdx = 0;
        for (int slot = 0; slot < invSize && rewardIdx < rewards.size(); slot++) {
            if (slot < 9 || slot >= invSize - 9 || slot % 9 == 0 || slot % 9 == 8) {
                continue;
            }
            RewardDefinition reward = rewards.get(rewardIdx);
            ItemStack preview = RewardPreview.create(reward);
            if (preview != null) {
                ItemMeta meta = preview.getItemMeta();
                if (meta != null) {
                    java.util.List<String> lore = meta.getLore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(meta.getLore());
                    lore.add(Text.color(Text.toSmallCaps(Messages.raw("nogamble-click-claim"))));
                    meta.setLore(lore);
                    preview.setItemMeta(meta);
                }
                inv.setItem(slot, preview);
            }
            rewardIdx++;
        }

        player.openInventory(inv);
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

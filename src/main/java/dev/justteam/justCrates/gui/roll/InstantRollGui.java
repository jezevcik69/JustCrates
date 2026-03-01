package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.gui.RewardPreview;
import dev.justteam.justCrates.reward.RewardDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Instant roll: immediately shows the reward without animation.
 */
public final class InstantRollGui {

    private InstantRollGui() {
    }

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        RewardDefinition reward = crateService.rollReward(crate);
        if (reward == null) {
            return;
        }

        Inventory inv = Bukkit.createInventory(new RollInventoryHolder(), 27, Text.color(crate.getRollDefinition().getTitle()));

        // Fill with gradient border
        ItemStack dark = pane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack corner = pane(Material.CYAN_STAINED_GLASS_PANE);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, dark);
        }
        inv.setItem(0, corner);
        inv.setItem(8, corner);
        inv.setItem(18, corner);
        inv.setItem(26, corner);
        inv.setItem(9, accent);
        inv.setItem(17, accent);

        // Show reward in center
        ItemStack display = RewardPreview.create(reward);
        if (display != null) {
            inv.setItem(13, display);
        }

        player.openInventory(inv);
        crateService.giveReward(player, reward);

        String soundName = plugin.getConfig().getString("sounds.reward", "entity.player.levelup");
        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }

        Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 40L);
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

    private static Sound parseSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(name.toUpperCase().replace(".", "_"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

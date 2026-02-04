package dev.justteam.justCrates.gui;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.crate.RollDefinition;
import dev.justteam.justCrates.reward.RewardDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class RollGui {

    private RollGui() {
    }

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        RollDefinition roll = crate.getRollDefinition();
        int size = Math.max(9, Math.min(54, roll.getSize()));
        String title = Text.color(roll.getTitle());

        Inventory inv = Bukkit.createInventory(null, size, title);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE, " ");

        player.openInventory(inv);

        List<ItemStack> preview = RewardPreview.buildPreview(crate);
        if (preview.isEmpty()) {
            return;
        }

        int center = size / 2;
        int ticks = roll.getDurationTicks();
        int interval = Math.max(1, roll.getTickInterval());

        new BukkitRunnable() {
            int elapsed = 0;
            int index = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                inv.setItem(center, preview.get(index % preview.size()));
                player.updateInventory();

                playRollSound(plugin, player);

                index++;
                elapsed += interval;
                if (elapsed >= ticks) {
                    RewardDefinition reward = crateService.rollReward(crate);
                    if (reward != null) {
                        ItemStack display = RewardPreview.create(reward);
                        if (display != null) {
                            inv.setItem(center, display);
                        }
                        crateService.giveReward(player, reward);
                    }
                    playRewardSound(plugin, player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
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

    private static void playRollSound(JavaPlugin plugin, Player player) {
        String soundName = plugin.getConfig().getString("sounds.roll", "ui.button.click");
        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.7f, 1.2f);
        }
    }

    private static void playRewardSound(JavaPlugin plugin, Player player) {
        String soundName = plugin.getConfig().getString("sounds.reward", "entity.player.levelup");
        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
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

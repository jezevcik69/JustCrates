package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.crate.RollDefinition;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class RouletteRollGui {
    private static final int[] PERIMETER = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        17, 26, 35, 44,
        53, 52, 51, 50, 49, 48, 47, 46, 45,
        36, 27, 18, 9
    };
    private static final int MARKER_INDEX = 4;

    private RouletteRollGui() {}

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        RollDefinition roll = crate.getRollDefinition();
        String title = Text.color(roll.getTitle());

        RollInventoryHolder holder = new RollInventoryHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        ItemStack dark = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, dark);
        }
        inv.setItem(22, pane(Material.CYAN_STAINED_GLASS_PANE));
        ItemStack marker = pane(Material.YELLOW_STAINED_GLASS_PANE);
        inv.setItem(13, marker);

        player.openInventory(inv);

        List<ItemStack> preview = RewardPreview.buildPreview(crate);
        if (preview.isEmpty()) {
            return;
        }

        RewardDefinition finalReward = crateService.rollReward(crate);

        int totalTicks = roll.getDurationTicks();
        int baseInterval = Math.max(1, roll.getTickInterval());

        new BukkitRunnable() {
            int elapsed = 0;
            int offset = 0;
            int currentInterval = baseInterval;
            int ticksSinceLastShift = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                ticksSinceLastShift++;
                elapsed++;

                if (ticksSinceLastShift >= currentInterval) {
                    ticksSinceLastShift = 0;
                    for (int i = 0; i < PERIMETER.length; i++) {
                        int itemIndex = (i + offset) % preview.size();
                        inv.setItem(PERIMETER[i], preview.get(itemIndex));
                    }
                    offset++;

                    player.updateInventory();
                    playTickSound(plugin, player);
                    double progress = (double) elapsed / totalTicks;
                    if (progress > 0.4) {
                        currentInterval = baseInterval + (int) ((progress - 0.4) * 14);
                    }
                }

                if (elapsed >= totalTicks) {
                    if (finalReward != null) {
                        ItemStack display = RewardPreview.create(finalReward);
                        if (display != null) {
                            inv.setItem(PERIMETER[MARKER_INDEX], display);
                            inv.setItem(22, display);
                        }
                        crateService.giveReward(player, finalReward);
                    }
                    player.updateInventory();
                    playRewardSound(plugin, player);
                    holder.setFinished(true);
                    cancel();
                    Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 40L);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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

    private static void playTickSound(JavaPlugin plugin, Player player) {
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
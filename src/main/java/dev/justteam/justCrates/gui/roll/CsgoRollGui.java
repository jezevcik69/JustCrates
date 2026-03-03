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

public final class CsgoRollGui {
    private CsgoRollGui() {}

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        RollDefinition roll = crate.getRollDefinition();
        String title = Text.color(roll.getTitle());

        RollInventoryHolder holder = new RollInventoryHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        ItemStack dark = pane(Material.BLUE_STAINED_GLASS_PANE);
        ItemStack accent = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack corner = pane(Material.CYAN_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, dark);
        }
        inv.setItem(0, corner);
        inv.setItem(4, accent);
        inv.setItem(8, corner);
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, dark);
        }
        inv.setItem(18, corner);
        inv.setItem(22, accent);
        inv.setItem(26, corner);
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, pane(Material.BLACK_STAINED_GLASS_PANE));
        }

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
                    for (int i = 9; i < 17; i++) {
                        inv.setItem(i, inv.getItem(i + 1));
                    }
                    inv.setItem(17, preview.get(offset % preview.size()));
                    offset++;

                    player.updateInventory();
                    playTickSound(plugin, player);
                    double progress = (double) elapsed / totalTicks;
                    if (progress > 0.5) {
                        currentInterval = baseInterval + (int) ((progress - 0.5) * 12);
                    }
                }

                if (elapsed >= totalTicks) {
                    if (finalReward != null) {
                        ItemStack display = RewardPreview.create(finalReward);
                        if (display != null) {
                            inv.setItem(13, display);
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
package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.BlockCrateService;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.crate.RollDefinition;
import dev.justteam.justCrates.gui.RewardPreview;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HologramRollGui {
    private HologramRollGui() {}

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService, Block block, BlockCrateService blockCrateService) {
        if (block == null) {
            // Fallback to CSGO if no block
            CsgoRollGui.open(plugin, player, crate, crateService);
            return;
        }

        List<RewardDefinition> rewards = crate.getRewards();
        if (rewards.isEmpty()) {
            player.sendMessage(Text.chat("&cThis crate has no rewards."));
            return;
        }

        RewardDefinition finalReward = crateService.rollReward(crate);
        if (finalReward == null) {
            return;
        }

        RollDefinition roll = crate.getRollDefinition();
        int totalTicks = roll.getDurationTicks();
        int baseInterval = Math.max(1, roll.getTickInterval());

        Location blockLoc = block.getLocation();

        // Remove existing hologram
        if (blockCrateService != null) {
            blockCrateService.hideHologram(blockLoc);
        }

        // Spawn two armor stands: one for item display name, one for reward name
        Location iconLoc = new Location(blockLoc.getWorld(), blockLoc.getX() + 0.5, blockLoc.getY() + 2.0, blockLoc.getZ() + 0.5);
        Location nameLoc = new Location(blockLoc.getWorld(), blockLoc.getX() + 0.5, blockLoc.getY() + 1.7, blockLoc.getZ() + 0.5);

        ArmorStand iconStand = blockLoc.getWorld().spawn(iconLoc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(" ");
        });

        ArmorStand nameStand = blockLoc.getWorld().spawn(nameLoc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(" ");
        });

        // Build preview names
        List<String> rewardNames = new ArrayList<>();
        List<String> rewardIcons = new ArrayList<>();
        for (RewardDefinition reward : rewards) {
            String name = resolveRewardName(reward);
            String icon = resolveRewardIcon(reward);
            rewardNames.add(name);
            rewardIcons.add(icon);
        }

        new BukkitRunnable() {
            int elapsed = 0;
            int offset = 0;
            int currentInterval = baseInterval;
            int ticksSinceLastShift = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup();
                    cancel();
                    return;
                }

                ticksSinceLastShift++;
                elapsed++;

                if (ticksSinceLastShift >= currentInterval) {
                    ticksSinceLastShift = 0;

                    int idx = offset % rewardNames.size();
                    iconStand.setCustomName(Text.color(rewardIcons.get(idx)));
                    nameStand.setCustomName(Text.color(rewardNames.get(idx)));
                    offset++;

                    // Play tick sound
                    String soundName = plugin.getConfig().getString("sounds.roll", "ui.button.click");
                    Sound sound = parseSound(soundName);
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, 0.7f, 1.2f);
                    }

                    double progress = (double) elapsed / totalTicks;
                    if (progress > 0.5) {
                        currentInterval = baseInterval + (int) ((progress - 0.5) * 12);
                    }
                }

                if (elapsed >= totalTicks) {
                    // Show final reward
                    String finalName = resolveRewardName(finalReward);
                    String finalIcon = resolveRewardIcon(finalReward);
                    iconStand.setCustomName(Text.color(finalIcon));
                    nameStand.setCustomName(Text.color(finalName));

                    crateService.giveReward(player, finalReward);

                    String soundName = plugin.getConfig().getString("sounds.reward", "entity.player.levelup");
                    Sound sound = parseSound(soundName);
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    }

                    cancel();

                    // Remove stands and restore hologram after 3 seconds
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        cleanup();
                    }, 40L);
                }
            }

            private void cleanup() {
                if (!iconStand.isDead()) iconStand.remove();
                if (!nameStand.isDead()) nameStand.remove();
                // Restore hologram
                if (blockCrateService != null) {
                    blockCrateService.showHologram(blockLoc);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static String resolveRewardName(RewardDefinition reward) {
        if (reward.getType() == RewardType.COMMAND) {
            if (reward.getPreviewName() != null && !reward.getPreviewName().isBlank()) {
                return reward.getPreviewName();
            }
            return "&cCommand Reward";
        }
        if (reward.getItemStack() != null && reward.getItemStack().hasItemMeta()
                && reward.getItemStack().getItemMeta().hasDisplayName()) {
            return reward.getItemStack().getItemMeta().getDisplayName();
        }
        if (reward.getItemDefinition() != null) {
            if (reward.getItemDefinition().getName() != null && !reward.getItemDefinition().getName().isBlank()) {
                return reward.getItemDefinition().getName();
            }
            if (reward.getItemDefinition().getMaterial() != null) {
                return "&f" + formatMaterial(reward.getItemDefinition().getMaterial().name());
            }
        }
        return "&fReward";
    }

    private static String resolveRewardIcon(RewardDefinition reward) {
        Material mat = null;
        if (reward.getType() == RewardType.ITEM) {
            if (reward.getItemStack() != null) {
                mat = reward.getItemStack().getType();
            } else if (reward.getItemDefinition() != null && reward.getItemDefinition().getMaterial() != null) {
                mat = reward.getItemDefinition().getMaterial();
            }
        }
        if (mat == null && reward.getPreviewMaterial() != null && !reward.getPreviewMaterial().isBlank()) {
            try {
                mat = Material.valueOf(reward.getPreviewMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (mat == null) {
            mat = Material.CHEST;
        }
        // Use colored block name as "icon" representation
        return "&e\u2B50 &6" + formatMaterial(mat.name()) + " &e\u2B50";
    }

    private static String formatMaterial(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
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

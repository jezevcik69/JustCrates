package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.core.Messages;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.BlockCrateService;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.crate.RollDefinition;
import dev.justteam.justCrates.gui.RewardPreview;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public final class HologramRollGui {
    private HologramRollGui() {}

    private static final Set<UUID> rollingPlayers = new HashSet<>();
    private static final Set<String> rollingLocations = new HashSet<>();

    public static boolean isRolling(Player player) {
        return rollingPlayers.contains(player.getUniqueId());
    }

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService, Block block, BlockCrateService blockCrateService) {
        if (block == null) {
            CsgoRollGui.open(plugin, player, crate, crateService);
            return;
        }

        List<RewardDefinition> rewards = crate.getRewards();
        if (rewards.isEmpty()) {
            player.sendMessage(Messages.get("hologram-roll-no-rewards"));
            return;
        }

        String locationKey = block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();

        if (rollingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(Messages.get("crate-already-rolling"));
            return;
        }
        if (rollingLocations.contains(locationKey)) {
            player.sendMessage(Messages.get("crate-already-rolling"));
            return;
        }

        RewardDefinition finalReward = crateService.rollReward(crate);
        if (finalReward == null) {
            return;
        }

        rollingPlayers.add(player.getUniqueId());
        rollingLocations.add(locationKey);

        RollDefinition roll = crate.getRollDefinition();
        int totalTicks = roll.getDurationTicks();
        int baseInterval = Math.max(1, roll.getTickInterval());

        Location blockLoc = block.getLocation();
        double heightOffset = plugin.getConfig().getDouble("hologram.height-offset", 1.75);

        if (blockCrateService != null) {
            blockCrateService.hideHologram(blockLoc);
        }

        double nameY = blockLoc.getY() + heightOffset;
        double iconY = nameY + 0.6;

        Location nameLoc = new Location(blockLoc.getWorld(), blockLoc.getX() + 0.5, nameY, blockLoc.getZ() + 0.5);
        Location iconLoc = new Location(blockLoc.getWorld(), blockLoc.getX() + 0.5, iconY, blockLoc.getZ() + 0.5);

        ItemDisplay iconDisplay = blockLoc.getWorld().spawn(iconLoc, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setSilent(true);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
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

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                online.showEntity(plugin, iconDisplay);
                online.showEntity(plugin, nameStand);
            } else {
                online.hideEntity(plugin, iconDisplay);
                online.hideEntity(plugin, nameStand);
            }
        }

        List<String> rewardNames = new ArrayList<>();
        List<ItemStack> rewardIcons = new ArrayList<>();
        for (RewardDefinition reward : rewards) {
            String name = resolveRewardName(reward);
            ItemStack icon = resolveRewardIcon(reward);
            rewardNames.add(name);
            rewardIcons.add(icon);
        }

        new BukkitRunnable() {
            int elapsed = 0;
            int offset = 0;
            int currentInterval = baseInterval;
            int ticksSinceLastShift = 0;
            float rotationAngle = 0f;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup();
                    cancel();
                    return;
                }

                ticksSinceLastShift++;
                elapsed++;

                rotationAngle += 0.3f;
                Transformation transform = new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(rotationAngle, 0f, 1f, 0f),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                );
                iconDisplay.setTransformation(transform);

                if (ticksSinceLastShift >= currentInterval) {
                    ticksSinceLastShift = 0;

                    int idx = offset % rewardNames.size();
                    iconDisplay.setItemStack(rewardIcons.get(idx));
                    nameStand.setCustomName(Text.color(rewardNames.get(idx)));
                    offset++;

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
                    String finalName = resolveRewardName(finalReward);
                    ItemStack finalIcon = resolveRewardIcon(finalReward);
                    iconDisplay.setItemStack(finalIcon);
                    nameStand.setCustomName(Text.color(finalName));

                    crateService.giveReward(player, finalReward);

                    String soundName = plugin.getConfig().getString("sounds.reward", "entity.player.levelup");
                    Sound sound = parseSound(soundName);
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    }

                    cancel();

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        cleanup();
                    }, 40L);
                }
            }

            private void cleanup() {
                if (!iconDisplay.isDead()) iconDisplay.remove();
                if (!nameStand.isDead()) nameStand.remove();
                rollingPlayers.remove(player.getUniqueId());
                rollingLocations.remove(locationKey);
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

    private static ItemStack resolveRewardIcon(RewardDefinition reward) {
        ItemStack preview = RewardPreview.create(reward);
        return preview == null ? null : preview.clone();
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

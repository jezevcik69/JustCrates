package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.reward.RewardDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class NoGambleInventoryHolder implements InventoryHolder {
    private final CrateDefinition crate;
    private final CrateService crateService;
    private boolean claimed = false;

    public NoGambleInventoryHolder(CrateDefinition crate, CrateService crateService) {
        this.crate = crate;
        this.crateService = crateService;
    }

    public void handleClick(Player player, int rawSlot) {
        if (claimed) {
            return;
        }
        int size = crate.getRewards().size();
        int rewardIndex = slotToRewardIndex(rawSlot, size);
        if (rewardIndex < 0 || rewardIndex >= size) {
            return;
        }
        claimed = true;
        RewardDefinition reward = crate.getRewards().get(rewardIndex);
        crateService.giveReward(player, reward);
        player.closeInventory();
    }

    private int slotToRewardIndex(int slot, int totalRewards) {
        int index = 0;
        int rows = Math.max(3, (int) Math.ceil((totalRewards + 7.0) / 7.0) + 2);
        int invSize = rows * 9;
        for (int s = 0; s < invSize; s++) {
            if (s < 9 || s >= invSize - 9 || s % 9 == 0 || s % 9 == 8) {
                continue;
            }
            if (s == slot) {
                return index;
            }
            index++;
            if (index >= totalRewards) {
                break;
            }
        }
        return -1;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
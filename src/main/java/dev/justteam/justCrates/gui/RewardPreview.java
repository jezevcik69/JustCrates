package dev.justteam.justCrates.gui;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.reward.RewardDefinition;
import dev.justteam.justCrates.reward.RewardType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class RewardPreview {
    private RewardPreview() {}

    public static List<ItemStack> buildPreview(CrateDefinition crate) {
        List<ItemStack> list = new ArrayList<>();
        for (RewardDefinition reward : crate.getRewards()) {
            ItemStack preview = create(reward);
            if (preview != null) {
                list.add(preview);
            }
        }
        return list;
    }

    public static ItemStack create(RewardDefinition reward) {
        if (reward.getType() == RewardType.ITEM) {
            if (reward.getItemStack() != null) {
                return reward.getItemStack().clone();
            }
            if (reward.getItemDefinition() != null) {
                Material material = reward.getItemDefinition().getMaterial();
                ItemStack stack = new ItemStack(material, reward.getItemDefinition().getAmount());
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    if (reward.getItemDefinition().getName() != null) {
                        meta.setDisplayName(Text.color(reward.getItemDefinition().getName()));
                    }
                    if (reward.getItemDefinition().getLore() != null) {
                        meta.setLore(reward.getItemDefinition().getLore().stream().map(Text::color).toList());
                    }
                    if (reward.getItemDefinition().getCustomModelData() != null) {
                        meta.setCustomModelData(reward.getItemDefinition().getCustomModelData());
                    }
                    stack.setItemMeta(meta);
                }
                return stack;
            }
        }

        Material material = Material.PAPER;
        if (reward.getPreviewMaterial() != null && !reward.getPreviewMaterial().isBlank()) {
            try {
                material = Material.valueOf(reward.getPreviewMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                material = Material.PAPER;
            }
        }
        ItemStack paper = new ItemStack(material);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            String name = reward.getPreviewName() == null || reward.getPreviewName().isBlank()
                    ? "&eCommand Reward"
                    : reward.getPreviewName();
            meta.setDisplayName(Text.color(name));
            if (reward.getPreviewLore() != null && !reward.getPreviewLore().isEmpty()) {
                meta.setLore(reward.getPreviewLore().stream().map(Text::color).toList());
            }
            paper.setItemMeta(meta);
        }
        return paper;
    }
}
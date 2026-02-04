package dev.justteam.justCrates.reward;

import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RewardDefinition {

    private final RewardType type;
    private final int weight;
    private final List<String> commands;
    private final ItemDefinition itemDefinition;
    private final ItemStack itemStack;

    public RewardDefinition(RewardType type, int weight, List<String> commands, ItemDefinition itemDefinition, ItemStack itemStack) {
        this.type = type;
        this.weight = weight;
        this.commands = commands;
        this.itemDefinition = itemDefinition;
        this.itemStack = itemStack;
    }

    public RewardType getType() {
        return type;
    }

    public int getWeight() {
        return weight;
    }

    public List<String> getCommands() {
        return commands;
    }

    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}

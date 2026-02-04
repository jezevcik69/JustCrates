package dev.justteam.justCrates.key;

import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class KeyDefinition {

    private final String id;
    private final String name;
    private final List<String> lore;
    private final ItemDefinition itemDefinition;
    private final ItemStack itemStack;

    public KeyDefinition(String id, String name, List<String> lore, ItemDefinition itemDefinition, ItemStack itemStack) {
        this.id = id;
        this.name = name;
        this.lore = lore;
        this.itemDefinition = itemDefinition;
        this.itemStack = itemStack;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getColoredName() {
        return Text.color(name);
    }
}

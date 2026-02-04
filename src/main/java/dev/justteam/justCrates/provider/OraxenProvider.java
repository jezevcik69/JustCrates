package dev.justteam.justCrates.provider;

import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

public final class OraxenProvider implements ItemProvider {
    @Override
    public String id() {
        return "oraxen";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ItemStack createItem(ItemDefinition definition) {
        return null;
    }
}

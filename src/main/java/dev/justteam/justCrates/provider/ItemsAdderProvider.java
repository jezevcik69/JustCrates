package dev.justteam.justCrates.provider;

import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

public final class ItemsAdderProvider implements ItemProvider {
    @Override
    public String id() {
        return "itemsadder";
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


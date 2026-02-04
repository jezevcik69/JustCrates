package dev.justteam.justCrates.provider;

import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

public final class NexoProvider implements ItemProvider {
    @Override
    public String id() {
        return "nexo";
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

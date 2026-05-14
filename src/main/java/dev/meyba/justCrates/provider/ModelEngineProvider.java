package dev.meyba.justCrates.provider;

import dev.meyba.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

public final class ModelEngineProvider implements ItemProvider {
    @Override
    public String id() {
        return "modelengine";
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
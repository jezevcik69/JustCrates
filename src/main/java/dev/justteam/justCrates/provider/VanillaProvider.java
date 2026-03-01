package dev.justteam.justCrates.provider;

import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

public final class VanillaProvider implements ItemProvider {
    @Override
    public String id() {
        return "vanilla";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ItemStack createItem(ItemDefinition definition) {
        if (definition == null || definition.getMaterial() == null) {
            return null;
        }
        return new ItemStack(definition.getMaterial(), definition.getAmount());
    }
}
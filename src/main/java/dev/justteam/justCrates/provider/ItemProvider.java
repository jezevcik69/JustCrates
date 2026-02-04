package dev.justteam.justCrates.provider;

import dev.justteam.justCrates.item.ItemDefinition;
import org.bukkit.inventory.ItemStack;

public interface ItemProvider {
    String id();
    boolean isAvailable();
    ItemStack createItem(ItemDefinition definition);
}

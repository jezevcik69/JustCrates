package dev.justteam.justCrates.editor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class EditorMenuHolder implements InventoryHolder {
    private final EditorMenuType type;
    private final String crateId;

    public EditorMenuHolder(EditorMenuType type, String crateId) {
        this.type = type;
        this.crateId = crateId;
    }

    public EditorMenuType getType() {
        return type;
    }

    public String getCrateId() {
        return crateId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
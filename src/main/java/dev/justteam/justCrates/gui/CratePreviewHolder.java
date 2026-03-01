package dev.justteam.justCrates.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class CratePreviewHolder implements InventoryHolder {

    private final String crateId;
    private Inventory inventory;

    public CratePreviewHolder(String crateId) {
        this.crateId = crateId;
    }

    public String getCrateId() {
        return crateId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}


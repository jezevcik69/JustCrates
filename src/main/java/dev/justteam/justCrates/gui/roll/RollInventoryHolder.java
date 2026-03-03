package dev.justteam.justCrates.gui.roll;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class RollInventoryHolder implements InventoryHolder {
    private boolean finished = false;

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

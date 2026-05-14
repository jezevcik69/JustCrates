package dev.meyba.justCrates.listener;

import dev.meyba.justCrates.JustCrates;
import dev.meyba.justCrates.core.Messages;
import dev.meyba.justCrates.gui.CratePreviewHolder;
import dev.meyba.justCrates.gui.VirtualKeyGui;
import dev.meyba.justCrates.gui.VirtualKeyMenuHolder;
import dev.meyba.justCrates.gui.roll.NoGambleInventoryHolder;
import dev.meyba.justCrates.gui.roll.RollInventoryHolder;
import dev.meyba.justCrates.key.KeyDefinition;
import dev.meyba.justCrates.key.KeyService;
import dev.meyba.justCrates.key.VirtualKeyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class GuiListener implements Listener {
    private final JustCrates plugin;
    private final VirtualKeyService virtualKeys;

    public GuiListener(JustCrates plugin, VirtualKeyService virtualKeys) {
        this.plugin = plugin;
        this.virtualKeys = virtualKeys;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof CratePreviewHolder || holder instanceof RollInventoryHolder) {
            event.setCancelled(true);
            return;
        }

        if (holder instanceof NoGambleInventoryHolder noGamble) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                noGamble.handleClick(player, event.getRawSlot());
            }
            return;
        }

        if (!(holder instanceof VirtualKeyMenuHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String keyId = VirtualKeyGui.extractKeyId(plugin, event.getCurrentItem());
        if (keyId == null || keyId.isBlank()) return;

        KeyService keyService = plugin.getKeyService();
        KeyDefinition keyDef = keyService.getKey(keyId);
        if (keyDef == null) return;

        if (!consumeKey(player, keyService, keyId)) {
            player.sendMessage(Messages.get("key-not-in-inventory"));
            return;
        }

        ItemStack virtualKey = keyService.createVirtualKeyItem(keyDef);
        if (virtualKey != null) player.getInventory().addItem(virtualKey);

        player.sendMessage(Messages.get("key-converted-virtual"));
        VirtualKeyGui.open(plugin, player, keyService, virtualKeys);
    }

    private boolean consumeKey(Player player, KeyService keyService, String keyId) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getAmount() <= 0) continue;
            if (!keyService.isKey(item, keyId) || keyService.isVirtualKey(item)) continue;

            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) contents[i] = null;
            player.getInventory().setContents(contents);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RollInventoryHolder roll)) return;
        if (roll.isFinished() || !(event.getPlayer() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.openInventory(inv);
        }, 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CratePreviewHolder
                || holder instanceof RollInventoryHolder
                || holder instanceof VirtualKeyMenuHolder
                || holder instanceof NoGambleInventoryHolder) {
            event.setCancelled(true);
        }
    }
}

package dev.justteam.justCrates.listener;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.gui.CratePreviewHolder;
import dev.justteam.justCrates.gui.VirtualKeyGui;
import dev.justteam.justCrates.gui.VirtualKeyMenuHolder;
import dev.justteam.justCrates.gui.roll.NoGambleInventoryHolder;
import dev.justteam.justCrates.gui.roll.RollInventoryHolder;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
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
    private final VirtualKeyService virtualKeyService;

    public GuiListener(JustCrates plugin, VirtualKeyService virtualKeyService) {
        this.plugin = plugin;
        this.virtualKeyService = virtualKeyService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CratePreviewHolder) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof RollInventoryHolder) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof NoGambleInventoryHolder noGambleHolder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            noGambleHolder.handleClick(player, event.getRawSlot());
            return;
        }
        if (!(holder instanceof VirtualKeyMenuHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        String keyId = VirtualKeyGui.extractKeyId(plugin, clicked);
        if (keyId == null || keyId.isBlank()) {
            return;
        }

        KeyService keyService = plugin.getKeyService();
        KeyDefinition keyDef = keyService.getKey(keyId);
        if (keyDef == null) {
            return;
        }
        boolean found = false;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getAmount() <= 0) {
                continue;
            }
            if (keyService.isKey(item, keyId) && !keyService.isVirtualKey(item)) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    contents[i] = null;
                }
                found = true;
                break;
            }
        }
        if (!found) {
            player.sendMessage(Text.chat("&cYou do not have this key in inventory."));
            return;
        }
        player.getInventory().setContents(contents);
        ItemStack virtualKey = keyService.createVirtualKeyItem(keyDef);
        if (virtualKey != null) {
            player.getInventory().addItem(virtualKey);
        }

        player.sendMessage(Text.chat("&aKey converted to virtual key item."));
        VirtualKeyGui.open(plugin, player, keyService, virtualKeyService);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof RollInventoryHolder rollHolder) {
            if (!rollHolder.isFinished() && event.getPlayer() instanceof Player player) {
                Inventory inv = event.getInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.openInventory(inv);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CratePreviewHolder || holder instanceof RollInventoryHolder
                || holder instanceof VirtualKeyMenuHolder || holder instanceof NoGambleInventoryHolder) {
            event.setCancelled(true);
        }
    }
}

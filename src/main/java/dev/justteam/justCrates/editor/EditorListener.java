package dev.justteam.justCrates.editor;

import dev.justteam.justCrates.core.Text;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

public final class EditorListener implements Listener {

    private final EditorService editorService;

    public EditorListener(EditorService editorService) {
        this.editorService = editorService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof EditorMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack cursor = event.getCursor();
        int rawSlot = event.getRawSlot();
        boolean rightClick = event.isRightClick();
        editorService.handleMenuClick(player, holder, clicked, cursor, rawSlot, rightClick);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EditorMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!editorService.hasPendingInput(player)) {
            return;
        }
        String message = event.getMessage();
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(editorService.getPlugin(), () -> {
            editorService.handleChatInput(player, message);
        });
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.startsWith("/justcrates _editchat ")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (!editorService.hasPendingInput(player)) {
                return;
            }
            String input = message.substring("/justcrates _editchat ".length());
            Bukkit.getScheduler().runTask(editorService.getPlugin(), () -> {
                editorService.handleChatInput(player, input);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (editorService.isInUnbindMode(player)) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
                return;
            }
            Block block = event.getClickedBlock();
            if (block == null) {
                return;
            }
            event.setCancelled(true);
            editorService.clearUnbindMode(player);
            editorService.getBlockCrateService().unbind(block.getLocation());
            editorService.getBlockCrateService().save();
            player.sendMessage(Text.chat("&aCrate block unbound."));
            return;
        }

        if (!editorService.isInBindMode(player)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        String crateId = editorService.getBindCrateId(player);
        editorService.clearBindMode(player);
        if (crateId == null) {
            return;
        }
        editorService.getBlockCrateService().bind(block.getLocation(), crateId);
        editorService.getBlockCrateService().save();
        player.sendMessage(Text.chat("&aBlock bound to crate: " + crateId));
        editorService.openCrateEditor(player, crateId);
    }
}


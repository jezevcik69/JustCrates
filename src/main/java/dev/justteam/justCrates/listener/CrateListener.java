package dev.justteam.justCrates.listener;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.gui.CratePreviewGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class CrateListener implements Listener {

    private final JustCrates plugin;

    public CrateListener(JustCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        String crateId = plugin.getBlockCrateService().getCrateId(event.getClickedBlock().getLocation());
        if (crateId == null) {
            return;
        }

        CrateDefinition crate = plugin.getCrateService().getCrate(crateId);
        if (crate == null) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            CratePreviewGui.open(event.getPlayer(), crate);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getCrateService().openCrate(event.getPlayer(), crate, event.getClickedBlock());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String crateId = plugin.getBlockCrateService().getCrateId(event.getBlock().getLocation());
        if (crateId == null) {
            return;
        }
        CrateDefinition crate = plugin.getCrateService().getCrate(crateId);
        if (crate == null) {
            return;
        }
        event.setCancelled(true);
        CratePreviewGui.open(event.getPlayer(), crate);
    }
}


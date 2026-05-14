package dev.meyba.justCrates.listener;

import dev.meyba.justCrates.JustCrates;
import dev.meyba.justCrates.crate.CrateDefinition;
import dev.meyba.justCrates.gui.CratePreviewGui;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class CrateListener implements Listener {
    private final JustCrates plugin;

    public CrateListener(JustCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;

        CrateDefinition crate = resolveCrate(event.getClickedBlock().getLocation());
        if (crate == null) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            CratePreviewGui.open(event.getPlayer(), crate, plugin.getPreviewGuiSettings());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getCrateService().openCrate(event.getPlayer(), crate, event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        CrateDefinition crate = resolveCrate(event.getBlock().getLocation());
        if (crate == null) return;
        event.setCancelled(true);
        CratePreviewGui.open(event.getPlayer(), crate, plugin.getPreviewGuiSettings());
    }

    private CrateDefinition resolveCrate(org.bukkit.Location location) {
        String id = plugin.getBlockCrateService().getCrateId(location);
        return id == null ? null : plugin.getCrateService().getCrate(id);
    }
}

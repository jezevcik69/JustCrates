package dev.meyba.justCrates.gui.roll;

import dev.meyba.justCrates.crate.BlockCrateService;
import dev.meyba.justCrates.crate.CrateDefinition;
import dev.meyba.justCrates.crate.CrateService;
import dev.meyba.justCrates.crate.RollType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RollGuiFactory {
    private RollGuiFactory() {}

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        open(plugin, player, crate, crateService, null, null);
    }

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService, Block block, BlockCrateService blockCrateService) {
        RollType type = crate.getRollDefinition().getRollType();
        switch (type) {
            case CSGO -> CsgoRollGui.open(plugin, player, crate, crateService);
            case ROULETTE -> RouletteRollGui.open(plugin, player, crate, crateService);
            case INSTANT -> InstantRollGui.open(plugin, player, crate, crateService);
            case NO_GAMBLE -> NoGambleRollGui.open(plugin, player, crate, crateService);
            case HOLOGRAM -> HologramRollGui.open(plugin, player, crate, crateService, block, blockCrateService);
        }
    }
}
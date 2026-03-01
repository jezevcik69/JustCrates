package dev.justteam.justCrates.gui.roll;

import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.crate.RollType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RollGuiFactory {

    private RollGuiFactory() {
    }

    public static void open(JavaPlugin plugin, Player player, CrateDefinition crate, CrateService crateService) {
        RollType type = crate.getRollDefinition().getRollType();
        switch (type) {
            case CSGO -> CsgoRollGui.open(plugin, player, crate, crateService);
            case ROULETTE -> RouletteRollGui.open(plugin, player, crate, crateService);
            case INSTANT -> InstantRollGui.open(plugin, player, crate, crateService);
        }
    }
}

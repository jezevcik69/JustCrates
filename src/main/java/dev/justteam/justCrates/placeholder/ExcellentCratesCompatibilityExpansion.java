package dev.justteam.justCrates.placeholder;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExcellentCratesCompatibilityExpansion extends PlaceholderExpansion {
    private final JustCrates plugin;
    private final CrateService crateService;

    public ExcellentCratesCompatibilityExpansion(JustCrates plugin, CrateService crateService) {
        this.plugin = plugin;
        this.crateService = crateService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "excellentcrates";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("keys_")) {
            return resolveOpenings(player, params.substring(5));
        }

        if (params.startsWith("openings_available_")) {
            return resolveOpenings(player, params.substring(19));
        }

        if (params.startsWith("cooldown_")) {
            return resolveCooldown(player, params.substring(9));
        }

        if (params.startsWith("crate_name_")) {
            String crateId = params.substring(11);
            CrateDefinition crate = crateService.getCrate(crateId);
            return crate != null ? crate.getName() : "Unknown";
        }

        return null;
    }

    private String resolveOpenings(OfflinePlayer player, String crateId) {
        if (player == null) {
            return "0";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "0";
        }

        CrateDefinition crate = crateService.getCrate(crateId);
        return crate == null ? "0" : String.valueOf(crateService.getAvailableOpens(onlinePlayer, crate));
    }

    private String resolveCooldown(OfflinePlayer player, String crateId) {
        if (player == null) {
            return "0";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "0";
        }

        CrateDefinition crate = crateService.getCrate(crateId);
        return crate == null ? "0" : String.valueOf(crateService.getRemainingCooldown(onlinePlayer, crate));
    }
}

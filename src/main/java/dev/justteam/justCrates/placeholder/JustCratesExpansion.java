package dev.justteam.justCrates.placeholder;

import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.key.VirtualKeyService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JustCratesExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final CrateService crateService;
    private final VirtualKeyService virtualKeyService;

    public JustCratesExpansion(JavaPlugin plugin, CrateService crateService, VirtualKeyService virtualKeyService) {
        this.plugin = plugin;
        this.crateService = crateService;
        this.virtualKeyService = virtualKeyService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "justcrates";
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
        if (params.startsWith("keys_") && player != null) {
            String keyId = params.substring(5);
            if (virtualKeyService != null) {
                return String.valueOf(virtualKeyService.getKeys(player.getUniqueId(), keyId));
            }
            return "0";
        }

        if (params.startsWith("crate_name_")) {
            String crateId = params.substring(11);
            CrateDefinition crate = crateService.getCrate(crateId);
            return crate != null ? crate.getName() : "Unknown";
        }

        return null;
    }
}
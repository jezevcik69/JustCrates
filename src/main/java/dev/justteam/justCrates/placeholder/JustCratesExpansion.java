package dev.justteam.justCrates.placeholder;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.crate.CrateDefinition;
import dev.justteam.justCrates.crate.CrateService;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.VirtualKeyService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JustCratesExpansion extends PlaceholderExpansion {
    private final JustCrates plugin;
    private final CrateService crateService;
    private final VirtualKeyService virtualKeyService;

    public JustCratesExpansion(JustCrates plugin, CrateService crateService, VirtualKeyService virtualKeyService) {
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
        if (params.startsWith("crate_keys_")) {
            return resolveCrateKeyAmount(player, params.substring(11), KeyAmountType.TOTAL);
        }

        if (params.startsWith("crate_virtual_keys_")) {
            return resolveCrateKeyAmount(player, params.substring(19), KeyAmountType.VIRTUAL);
        }

        if (params.startsWith("crate_physical_keys_")) {
            return resolveCrateKeyAmount(player, params.substring(20), KeyAmountType.PHYSICAL);
        }

        if (params.startsWith("crate_key_id_")) {
            return resolveCrateKeyId(params.substring(13));
        }

        if (params.startsWith("crate_key_name_")) {
            return resolveCrateKeyName(params.substring(15));
        }

        if (params.startsWith("crate_cooldown_")) {
            return resolveCrateCooldown(player, params.substring(15));
        }

        if (params.startsWith("crate_can_open_")) {
            return resolveCanOpen(player, params.substring(15));
        }

        if (params.startsWith("keys_") && player != null) {
            String keyId = params.substring(5);
            if (virtualKeyService != null) {
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    return String.valueOf(virtualKeyService.getTotalKeys(onlinePlayer, keyId));
                }
                return String.valueOf(virtualKeyService.getKeys(player.getUniqueId(), keyId));
            }
            return "0";
        }

        if (params.startsWith("virtual_keys_") && player != null) {
            String keyId = params.substring(13);
            if (virtualKeyService != null) {
                return String.valueOf(virtualKeyService.getKeys(player.getUniqueId(), keyId));
            }
            return "0";
        }

        if (params.startsWith("physical_keys_") && player != null) {
            String keyId = params.substring(14);
            if (virtualKeyService != null) {
                Player onlinePlayer = player.getPlayer();
                return String.valueOf(onlinePlayer != null ? virtualKeyService.getPhysicalKeys(onlinePlayer, keyId) : 0);
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

    private @Nullable String resolveCrateKeyAmount(OfflinePlayer player, String crateId, KeyAmountType type) {
        if (player == null || virtualKeyService == null) {
            return "0";
        }

        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null || crate.getKeyId() == null || crate.getKeyId().isBlank()) {
            return "0";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return type == KeyAmountType.VIRTUAL
                    ? String.valueOf(virtualKeyService.getKeys(player.getUniqueId(), crate.getKeyId()))
                    : "0";
        }

        return switch (type) {
            case TOTAL -> String.valueOf(virtualKeyService.getTotalKeys(onlinePlayer, crate.getKeyId()));
            case VIRTUAL -> String.valueOf(virtualKeyService.getKeys(player.getUniqueId(), crate.getKeyId()));
            case PHYSICAL -> String.valueOf(virtualKeyService.getPhysicalKeys(onlinePlayer, crate.getKeyId()));
        };
    }

    private @Nullable String resolveCrateKeyId(String crateId) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null || crate.getKeyId() == null || crate.getKeyId().isBlank()) {
            return "";
        }
        return crate.getKeyId();
    }

    private @Nullable String resolveCrateKeyName(String crateId) {
        CrateDefinition crate = crateService.getCrate(crateId);
        if (crate == null || crate.getKeyId() == null || crate.getKeyId().isBlank()) {
            return "";
        }

        KeyDefinition key = plugin.getKeyService().getKey(crate.getKeyId());
        return key != null ? key.getName() : crate.getKeyId();
    }

    private @Nullable String resolveCrateCooldown(OfflinePlayer player, String crateId) {
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

    private @Nullable String resolveCanOpen(OfflinePlayer player, String crateId) {
        if (player == null) {
            return "false";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "false";
        }

        CrateDefinition crate = crateService.getCrate(crateId);
        return String.valueOf(crate != null && crateService.canOpen(onlinePlayer, crate));
    }

    private enum KeyAmountType {
        TOTAL,
        VIRTUAL,
        PHYSICAL
    }
}

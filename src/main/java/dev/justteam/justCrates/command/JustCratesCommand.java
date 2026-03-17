package dev.justteam.justCrates.command;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.core.Messages;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.KeyService;
import dev.justteam.justCrates.key.VirtualKeyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class JustCratesCommand implements CommandExecutor, TabCompleter {
    private final JustCrates plugin;

    public JustCratesCommand(JustCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("key")) {
            return handleKeyCommand(sender, label, args);
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!sender.hasPermission("justcrates.admin")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                plugin.getEditorService().openMainMenu(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                sendHelp(sender);
                return true;
            }
            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Messages.get("player-only"));
                    return true;
                }
                if (!sender.hasPermission("justcrates.admin")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                plugin.getEditorService().openMainMenu(player);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("justcrates.admin")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                Bukkit.getScheduler().cancelTasks(plugin);
                plugin.reloadAllData();
                sender.sendMessage(Messages.get("reloaded"));
                return true;
            }
            case "key" -> {
                if (args.length > 1) {
                    String keySubCommand = args[1].toLowerCase(Locale.ROOT);
                    String[] keyArgs = args.length > 2 ? slice(args, 2) : new String[0];
                    return switch (keySubCommand) {
                        case "give" -> handleCrateKeyGiveCommand(sender, label, keyArgs);
                        case "set" -> handleCrateKeySetCommand(sender, label, keyArgs);
                        case "clear" -> handleCrateKeyClearCommand(sender, label, keyArgs);
                        case "remove" -> handleCrateKeyRemoveCommand(sender, label, keyArgs);
                        default -> handleKeyCommand(sender, label, slice(args, 1));
                    };
                }
                return handleKeyCommand(sender, label, new String[0]);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean handleKeyCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Messages.get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Messages.get("key-usage", "%value%", label));
            return true;
        }
        String id = args[0].toLowerCase(Locale.ROOT);
        Player target = sender instanceof Player p ? p : null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage(Messages.get("player-not-found"));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        KeyDefinition key = plugin.getKeyService().getKey(id);
        if (key == null) {
            sender.sendMessage(Messages.get("key-not-found"));
            return true;
        }
        if (key.isVirtual()) {
            VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
            if (virtualKeyService == null) {
                sender.sendMessage(Messages.get("virtual-key-service-unavailable"));
                return true;
            }
            virtualKeyService.addKeys(target.getUniqueId(), key.getId(), amount);
            sender.sendMessage(Messages.get("gave-virtual-key"));
            return true;
        }
        ItemStack item = plugin.getKeyService().createKeyItem(key);
        if (item == null) {
            sender.sendMessage(Messages.get("failed-build-key"));
            return true;
        }
        if (!plugin.getKeyService().givePhysicalKeys(target, key, amount)) {
            sender.sendMessage(Messages.get("failed-build-key"));
            return true;
        }
        sender.sendMessage(Messages.get("gave-key"));
        return true;
    }

    private boolean handleCrateKeyGiveCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Messages.get("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Messages.get("key-give-usage", "%value%", label));
            return true;
        }

        String targetArg = args[0];
        String keyId = args[1].toLowerCase(Locale.ROOT);

        int amount;
        try {
            amount = Math.max(1, Integer.parseInt(args[2]));
        } catch (NumberFormatException ignored) {
            sender.sendMessage(Messages.get("amount-must-be-number"));
            return true;
        }

        KeyDefinition key = plugin.getKeyService().getKey(keyId);
        if (key == null) {
            sender.sendMessage(Messages.get("key-not-found"));
            return true;
        }
        VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
        if (key.isVirtual() && virtualKeyService == null) {
            sender.sendMessage(Messages.get("virtual-key-service-unavailable"));
            return true;
        }

        if (targetArg.equalsIgnoreCase("all")) {
            int given = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (key.isVirtual()) {
                    virtualKeyService.addKeys(online.getUniqueId(), keyId, amount);
                    given++;
                } else {
                    if (!plugin.getKeyService().givePhysicalKeys(online, key, amount)) {
                        continue;
                    }
                    given++;
                }
            }
            sender.sendMessage(Messages.get(
                    "given-key-to-all",
                    "%amount%", String.valueOf(amount),
                    "%value%", key.isVirtual() ? "virtual key" : "key",
                    "%key%", keyId,
                    "%count%", String.valueOf(given)));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(Messages.get("player-not-found"));
            return true;
        }

        if (key.isVirtual()) {
            virtualKeyService.addKeys(target.getUniqueId(), keyId, amount);
            sender.sendMessage(Messages.get(
                    "given-key-to-player",
                    "%amount%", String.valueOf(amount),
                    "%value%", "virtual key",
                    "%key%", keyId,
                    "%player%", target.getName()));
            return true;
        }
        if (!plugin.getKeyService().givePhysicalKeys(target, key, amount)) {
            sender.sendMessage(Messages.get("failed-build-key"));
            return true;
        }
        sender.sendMessage(Messages.get(
                "given-key-to-player",
                "%amount%", String.valueOf(amount),
                "%value%", "key",
                "%key%", keyId,
                "%player%", target.getName()));
        return true;
    }

    private boolean handleCrateKeySetCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Messages.get("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Messages.get("key-set-usage", "%value%", label));
            return true;
        }

        String targetArg = args[0];
        String keyId = args[1].toLowerCase(Locale.ROOT);

        int amount;
        try {
            amount = Math.max(0, Integer.parseInt(args[2]));
        } catch (NumberFormatException ignored) {
            sender.sendMessage(Messages.get("amount-must-be-number"));
            return true;
        }

        KeyDefinition key = plugin.getKeyService().getKey(keyId);
        if (key == null) {
            sender.sendMessage(Messages.get("key-not-found"));
            return true;
        }

        if (targetArg.equalsIgnoreCase("all")) {
            int affected = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (setPlayerKeyAmount(online, key, amount)) {
                    affected++;
                }
            }
            sender.sendMessage(Messages.get(
                    "set-key-to-all",
                    "%amount%", String.valueOf(amount),
                    "%key%", keyId,
                    "%count%", String.valueOf(affected)));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(Messages.get("player-not-found"));
            return true;
        }
        if (!setPlayerKeyAmount(target, key, amount)) {
            sender.sendMessage(Messages.get("failed-build-key"));
            return true;
        }
        sender.sendMessage(Messages.get(
                "set-key-to-player",
                "%amount%", String.valueOf(amount),
                "%key%", keyId,
                "%player%", target.getName()));
        return true;
    }

    private boolean handleCrateKeyClearCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Messages.get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Messages.get("key-clear-usage", "%value%", label));
            return true;
        }

        String targetArg = args[0];
        String keyId = args[1].toLowerCase(Locale.ROOT);
        KeyDefinition key = plugin.getKeyService().getKey(keyId);
        if (key == null) {
            sender.sendMessage(Messages.get("key-not-found"));
            return true;
        }

        KeyService keyService = plugin.getKeyService();
        VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
        if (targetArg.equalsIgnoreCase("all")) {
            int affected = 0;
            int removed = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                int playerRemoved = keyService.clearPhysicalKeys(online, keyId, true);
                if (virtualKeyService != null) {
                    playerRemoved += virtualKeyService.clearKeys(online.getUniqueId(), keyId);
                }
                if (playerRemoved > 0) {
                    affected++;
                    removed += playerRemoved;
                }
            }
            sender.sendMessage(Messages.get(
                    "cleared-key-from-all",
                    "%amount%", String.valueOf(removed),
                    "%key%", keyId,
                    "%count%", String.valueOf(affected)));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(Messages.get("player-not-found"));
            return true;
        }
        int removed = keyService.clearPhysicalKeys(target, keyId, true);
        if (virtualKeyService != null) {
            removed += virtualKeyService.clearKeys(target.getUniqueId(), keyId);
        }
        sender.sendMessage(Messages.get(
                "cleared-key-from-player",
                "%amount%", String.valueOf(removed),
                "%key%", keyId,
                "%player%", target.getName()));
        return true;
    }

    private boolean handleCrateKeyRemoveCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Messages.get("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Messages.get("key-remove-usage", "%value%", label));
            return true;
        }

        String targetArg = args[0];
        String keyId = args[1].toLowerCase(Locale.ROOT);

        int amount;
        try {
            amount = Math.max(1, Integer.parseInt(args[2]));
        } catch (NumberFormatException ignored) {
            sender.sendMessage(Messages.get("amount-must-be-number"));
            return true;
        }

        KeyDefinition key = plugin.getKeyService().getKey(keyId);
        if (key == null) {
            sender.sendMessage(Messages.get("key-not-found"));
            return true;
        }

        VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
        if (virtualKeyService == null) {
            sender.sendMessage(Messages.get("virtual-key-service-unavailable"));
            return true;
        }

        if (targetArg.equalsIgnoreCase("all")) {
            int affected = 0;
            int removed = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                int playerRemoved = virtualKeyService.removeKeys(online.getUniqueId(), keyId, amount);
                if (playerRemoved > 0) {
                    affected++;
                    removed += playerRemoved;
                }
            }
            sender.sendMessage(Messages.get(
                    "removed-key-from-all",
                    "%amount%", String.valueOf(removed),
                    "%key%", keyId,
                    "%count%", String.valueOf(affected)));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(Messages.get("player-not-found"));
            return true;
        }
        int removed = virtualKeyService.removeKeys(target.getUniqueId(), keyId, amount);
        sender.sendMessage(Messages.get(
                "removed-key-from-player",
                "%amount%", String.valueOf(removed),
                "%key%", keyId,
                "%player%", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("key")) {
            return completeLegacyKeyCommand(args);
        }
        return completeJustCratesCommand(args);
    }

    private List<String> completeJustCratesCommand(String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("help", "editor", "reload", "key"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("key")) {
            return filterByPrefix(List.of("give", "set", "clear", "remove"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("key")
                && isKeyActionRequiringTarget(args[1])) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            for (Player online : Bukkit.getOnlinePlayers()) {
                suggestions.add(online.getName());
            }
            return filterByPrefix(suggestions, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("key")
                && isKeyActionRequiringTarget(args[1])) {
            List<String> keyIds = plugin.getKeyService().getKeys().stream().map(KeyDefinition::getId).toList();
            return filterByPrefix(keyIds, args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("key")
                && (args[1].equalsIgnoreCase("give")
                || args[1].equalsIgnoreCase("set")
                || args[1].equalsIgnoreCase("remove"))) {
            return filterByPrefix(List.of("1", "3", "5", "10", "16", "32", "64"), args[4]);
        }
        return Collections.emptyList();
    }

    private List<String> completeLegacyKeyCommand(String[] args) {
        if (args.length == 1) {
            List<String> keyIds = plugin.getKeyService().getKeys().stream().map(KeyDefinition::getId).toList();
            return filterByPrefix(keyIds, args[0]);
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filterByPrefix(names, args[1]);
        }
        if (args.length == 3) {
            return filterByPrefix(List.of("1", "3", "5", "10", "16", "32", "64"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        if (prefix == null) {
            return values;
        }
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                out.add(value);
            }
        }
        return out;
    }

    private String[] slice(String[] input, int from) {
        if (from >= input.length) {
            return new String[0];
        }
        String[] out = new String[input.length - from];
        System.arraycopy(input, from, out, 0, out.length);
        return out;
    }

    private boolean setPlayerKeyAmount(Player target, KeyDefinition key, int amount) {
        if (target == null || key == null) {
            return false;
        }

        String keyId = key.getId();
        if (key.isVirtual()) {
            VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
            if (virtualKeyService == null) {
                return false;
            }
            virtualKeyService.setKeys(target.getUniqueId(), keyId, amount);
            return true;
        }

        plugin.getKeyService().clearPhysicalKeys(target, keyId, true);
        if (amount <= 0) {
            return true;
        }
        return plugin.getKeyService().givePhysicalKeys(target, key, amount);
    }

    private boolean isKeyActionRequiringTarget(String action) {
        return action.equalsIgnoreCase("give")
                || action.equalsIgnoreCase("set")
                || action.equalsIgnoreCase("clear")
                || action.equalsIgnoreCase("remove");
    }

    private void sendHelp(CommandSender sender) {
        sendHelpLine(sender, "help-header");
        sendHelpLine(sender, "help-title");
        sendHelpLine(sender, "help-header");
        sender.sendMessage("");
        sendHelpLine(sender, "help-section-crates");
        sendHelpLine(sender, "help-cmd-crate");
        sendHelpLine(sender, "help-cmd-crate-help");
        sendHelpLine(sender, "help-cmd-crate-editor");
        sendHelpLine(sender, "help-cmd-crate-reload");
        sender.sendMessage("");
        sendHelpLine(sender, "help-section-keys");
        sendHelpLine(sender, "help-cmd-crate-key-give");
        sendHelpLine(sender, "help-cmd-crate-key-set");
        sendHelpLine(sender, "help-cmd-crate-key-clear");
        sendHelpLine(sender, "help-cmd-crate-key-remove");
        sendHelpLine(sender, "help-cmd-key");
        sender.sendMessage("");
        sendHelpLine(sender, "help-also");
        sendHelpLine(sender, "help-aliases");
        sendHelpLine(sender, "help-footer");
        sender.sendMessage("");
        sendHelpLine(sender, "help-header");
        sender.sendMessage("");
    }

    private void sendHelpLine(CommandSender sender, String key) {
        sender.sendMessage(Text.color(Messages.raw(key)));
    }
}

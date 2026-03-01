package dev.justteam.justCrates.command;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.key.KeyDefinition;
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
                    sender.sendMessage(Text.chat("&cNo permission."));
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
                    sender.sendMessage(Text.chat("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("justcrates.admin")) {
                    sender.sendMessage(Text.chat("&cNo permission."));
                    return true;
                }
                plugin.getEditorService().openMainMenu(player);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("justcrates.admin")) {
                    sender.sendMessage(Text.chat("&cNo permission."));
                    return true;
                }

                // Forcefully clear any stuck or overlapping tasks from previous reloads
                Bukkit.getScheduler().cancelTasks(plugin);

                plugin.reloadConfig();
                plugin.getKeyService().loadAll();
                plugin.getCrateService().loadAll();
                plugin.getBlockCrateService().load();
                if (plugin.getVirtualKeyService() != null) {
                    plugin.getVirtualKeyService().reload();
                }
                sender.sendMessage(Text.chat("&aReloaded."));
                return true;
            }
            case "key" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("give")) {
                    return handleCrateKeyGiveCommand(sender, label, args.length > 2 ? slice(args, 2) : new String[0]);
                }
                return handleKeyCommand(sender, label, args.length > 1 ? slice(args, 1) : new String[0]);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean handleKeyCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Text.chat("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.chat("&cUsage: /" + label + " <id> [player] [amount]"));
            return true;
        }
        String id = args[0].toLowerCase(Locale.ROOT);
        Player target = sender instanceof Player p ? p : null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage(Text.chat("&cPlayer not found."));
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
            sender.sendMessage(Text.chat("&cKey not found."));
            return true;
        }
        if (key.isVirtual()) {
            VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
            virtualKeyService.addKeys(target.getUniqueId(), key.getId(), amount);
            sender.sendMessage(Text.chat("&aGave virtual key."));
            return true;
        }
        ItemStack item = plugin.getKeyService().createKeyItem(key);
        if (item == null) {
            sender.sendMessage(Text.chat("&cFailed to build key item."));
            return true;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(Text.chat("&aGave key."));
        return true;
    }

    private boolean handleCrateKeyGiveCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Text.chat("&cNo permission."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Text.chat("&cUsage: /" + label + " key give <player|all> <key-id> <amount>"));
            return true;
        }

        String targetArg = args[0];
        String keyId = args[1].toLowerCase(Locale.ROOT);

        int amount;
        try {
            amount = Math.max(1, Integer.parseInt(args[2]));
        } catch (NumberFormatException ignored) {
            sender.sendMessage(Text.chat("&cAmount must be a number."));
            return true;
        }

        KeyDefinition key = plugin.getKeyService().getKey(keyId);
        if (key == null) {
            sender.sendMessage(Text.chat("&cKey not found."));
            return true;
        }

        if (targetArg.equalsIgnoreCase("all")) {
            int given = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (key.isVirtual()) {
                    plugin.getVirtualKeyService().addKeys(online.getUniqueId(), keyId, amount);
                    given++;
                } else {
                    ItemStack item = plugin.getKeyService().createKeyItem(key);
                    if (item == null) {
                        continue;
                    }
                    item.setAmount(amount);
                    online.getInventory().addItem(item);
                    given++;
                }
            }
            sender.sendMessage(
                    Text.chat("&aGiven &f" + amount + "x &a" + (key.isVirtual() ? "virtual key" : "key") + " &7(" + keyId
                            + ") &ato &f" + given + " &aplayer(s)."));
            return true;
        }

        Player target = Bukkit.getPlayer(targetArg);
        if (target == null) {
            sender.sendMessage(Text.chat("&cPlayer not found."));
            return true;
        }

        if (key.isVirtual()) {
            plugin.getVirtualKeyService().addKeys(target.getUniqueId(), keyId, amount);
            sender.sendMessage(
                    Text.chat("&aGiven &f" + amount + "x &avirtual key &7(" + keyId + ") &ato &f" + target.getName() + "&a."));
            return true;
        }
        ItemStack item = plugin.getKeyService().createKeyItem(key);
        if (item == null) {
            sender.sendMessage(Text.chat("&cFailed to build key item."));
            return true;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(
                Text.chat("&aGiven &f" + amount + "x &akey &7(" + keyId + ") &ato &f" + target.getName() + "&a."));
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
            return filterByPrefix(List.of("give"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("give")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            for (Player online : Bukkit.getOnlinePlayers()) {
                suggestions.add(online.getName());
            }
            return filterByPrefix(suggestions, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("give")) {
            List<String> keyIds = plugin.getKeyService().getKeys().stream().map(KeyDefinition::getId).toList();
            return filterByPrefix(keyIds, args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("give")) {
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.chat("&#7EB7E5&lhelp menu"));
        sender.sendMessage(Text.chat("&7/crate"));
        sender.sendMessage(Text.chat("&7/crate help"));
        sender.sendMessage(Text.chat("&7/crate editor"));
        sender.sendMessage(Text.chat("&7/crate reload"));
        sender.sendMessage(Text.chat("&7/crate key give <player|all> <key-id> <amount>"));
        sender.sendMessage(Text.chat("&7/key <id> [player] [amount]"));
    }
}

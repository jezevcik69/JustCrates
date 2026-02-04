package dev.justteam.justCrates.command;

import dev.justteam.justCrates.JustCrates;
import dev.justteam.justCrates.core.Text;
import dev.justteam.justCrates.gui.VirtualKeyGui;
import dev.justteam.justCrates.key.KeyDefinition;
import dev.justteam.justCrates.key.VirtualKeyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class JustCratesCommand implements CommandExecutor {

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
                    sender.sendMessage(Text.color("&cNo permission."));
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
            case "reload" -> {
                if (!sender.hasPermission("justcrates.admin")) {
                    sender.sendMessage(Text.color("&cNo permission."));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getKeyService().loadAll();
                plugin.getCrateService().loadAll();
                plugin.getBlockCrateService().load();
                if (plugin.getVirtualKeyService() != null) {
                    plugin.getVirtualKeyService().reload();
                }
                sender.sendMessage(Text.color("&aReloaded."));
                return true;
            }
            case "key" -> {
                return handleKeyCommand(sender, label, args.length > 1 ? slice(args, 1) : new String[0]);
            }
            case "vkey" -> {
                return handleVirtualKeyCommand(sender, args.length > 1 ? slice(args, 1) : new String[0]);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean handleKeyCommand(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(Text.color("&cNo permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Text.color("&cUsage: /" + label + " <id> [player] [amount]"));
            return true;
        }
        String id = args[0].toLowerCase(Locale.ROOT);
        Player target = sender instanceof Player p ? p : null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            sender.sendMessage(Text.color("&cPlayer not found."));
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
            sender.sendMessage(Text.color("&cKey not found."));
            return true;
        }
        ItemStack item = plugin.getKeyService().createKeyItem(key);
        if (item == null) {
            sender.sendMessage(Text.color("&cFailed to build key item."));
            return true;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(Text.color("&aGave key."));
        return true;
    }

    private boolean handleVirtualKeyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color("&cOnly players can use this command."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            VirtualKeyGui.open(plugin, player, plugin.getKeyService(), plugin.getVirtualKeyService());
            return true;
        }

        String id = args[0].toLowerCase(Locale.ROOT);
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        VirtualKeyService virtualKeyService = plugin.getVirtualKeyService();
        if (plugin.getKeyService().getKey(id) == null) {
            sender.sendMessage(Text.color("&cKey not found."));
            return true;
        }

        boolean converted = virtualKeyService.convertFromInventory(player, id, amount);
        if (!converted) {
            sender.sendMessage(Text.color("&cYou do not have enough keys in inventory."));
            return true;
        }

        sender.sendMessage(Text.color("&aConverted " + amount + " key(s) to virtual."));
        return true;
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
        sender.sendMessage(Text.color("&eJustCrates commands:"));
        sender.sendMessage(Text.color("&7/justcrates reload"));
        sender.sendMessage(Text.color("&7/justcrates (opens editor)"));
        sender.sendMessage(Text.color("&7/justcrates vkey [gui|<id> [amount]]"));
        sender.sendMessage(Text.color("&7/key <id> [player] [amount]"));
    }
}

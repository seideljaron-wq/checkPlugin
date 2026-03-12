package de.javakuba.check.command;

import de.javakuba.check.CheckPlugin;
import de.javakuba.check.manager.C;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CheckAdminCommand implements CommandExecutor, TabCompleter {

    private final CheckPlugin plugin;

    public CheckAdminCommand(CheckPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        // Only owner or players with check.admin permission
        boolean isOwner = player.getName().equalsIgnoreCase(plugin.getConfigManager().getOwner());
        if (!isOwner && !player.hasPermission("check.admin")) {
            player.sendMessage(C.prefix() + C.red("No permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { player.sendMessage(C.prefix() + C.yellow("Usage: /check-admin add <player>")); return true; }
                String name = args[1].startsWith("@") ? args[1].substring(1) : args[1];
                if (plugin.getConfigManager().addPlayer(name)) {
                    player.sendMessage(C.prefix() + C.green("✔ ") + C.aqua(name) + C.green(" can now use /check."));
                    Player t = Bukkit.getPlayerExact(name);
                    if (t != null) t.sendMessage(C.prefix() + C.green("You were granted check access by ") + C.aqua(player.getName()) + C.green("."));
                } else {
                    player.sendMessage(C.prefix() + C.yellow(name + " already has check access."));
                }
            }
            case "remove" -> {
                if (args.length < 2) { player.sendMessage(C.prefix() + C.yellow("Usage: /check-admin remove <player>")); return true; }
                String name = args[1].startsWith("@") ? args[1].substring(1) : args[1];
                if (name.equalsIgnoreCase(plugin.getConfigManager().getOwner())) {
                    player.sendMessage(C.prefix() + C.red("Can't remove the owner."));
                    return true;
                }
                if (plugin.getConfigManager().removePlayer(name)) {
                    player.sendMessage(C.prefix() + C.red("✖ ") + C.aqua(name) + C.red("'s check access removed."));
                    Player t = Bukkit.getPlayerExact(name);
                    if (t != null) t.sendMessage(C.prefix() + C.red("Your check access was removed by ") + C.aqua(player.getName()) + C.red("."));
                } else {
                    player.sendMessage(C.prefix() + C.yellow(name + " doesn't have check access."));
                }
            }
            case "list" -> {
                List<String> allowed = plugin.getConfigManager().getAllowedPlayers();
                player.sendMessage(C.gold("─── Check Admins ───"));
                for (String name : allowed) {
                    boolean owner = name.equalsIgnoreCase(plugin.getConfigManager().getOwner());
                    player.sendMessage(C.gray("  • ") + C.aqua(name) + (owner ? C.yellow(" [Owner]") : ""));
                }
            }
            case "reload" -> {
                plugin.getConfigManager().load();
                player.sendMessage(C.prefix() + C.green("✔ Config reloaded."));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(C.gold("─── /check-admin ───"));
        player.sendMessage(C.yellow("  /check-admin add <player>"));
        player.sendMessage(C.yellow("  /check-admin remove <player>"));
        player.sendMessage(C.yellow("  /check-admin list"));
        player.sendMessage(C.yellow("  /check-admin reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("add", "remove", "list", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) result.add(p.getName());
            }
        }
        return result;
    }
}

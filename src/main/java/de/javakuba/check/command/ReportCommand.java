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

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final CheckPlugin plugin;

    public ReportCommand(CheckPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can report.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(C.prefix() + C.gray("Usage: ") + C.yellow("/report <player> <reason>"));
            return true;
        }

        String targetName = args[0].startsWith("@") ? args[0].substring(1) : args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(C.prefix() + C.red("Player '" + targetName + "' is not online."));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String result = plugin.getReportManager().addReport(player, target, reason);

        switch (result) {
            case "ok" ->
                player.sendMessage(C.prefix() + C.green("Report submitted against ")
                        + C.aqua(target.getName()) + C.green(". Thank you."));
            case "self" ->
                player.sendMessage(C.prefix() + C.red("You cannot report yourself."));
            default -> {
                long secs = Long.parseLong(result.split(":")[1]);
                player.sendMessage(C.prefix() + C.red("Please wait ")
                        + C.yellow(secs + "s") + C.red(" before reporting again."));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    result.add(p.getName());
            }
        } else if (args.length == 2) {
            List<String> reasons = List.of("Hacking", "Cheating", "Xray", "Fly", "Killaura", "Speed");
            for (String r : reasons) {
                if (r.toLowerCase().startsWith(args[1].toLowerCase())) result.add(r);
            }
        }
        return result;
    }
}

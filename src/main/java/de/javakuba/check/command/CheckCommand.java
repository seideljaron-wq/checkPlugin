package de.javakuba.check.command;

import de.javakuba.check.CheckPlugin;
import de.javakuba.check.gui.ReportGUI;
import de.javakuba.check.manager.C;
import de.javakuba.check.manager.SessionManager.CheckSession;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CheckCommand implements CommandExecutor, TabCompleter {

    private final CheckPlugin plugin;

    public CheckCommand(CheckPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        if (!plugin.getConfigManager().isAllowed(player.getName())) {
            player.sendMessage(C.prefix() + C.red("You don't have permission."));
            return true;
        }

        switch (label.toLowerCase()) {

            case "check" -> {
                if (args.length == 0) {

                    if (plugin.getReportManager().getTotalReports() == 0) {
                        player.sendMessage(C.prefix() + C.green("No reports pending. ✔"));
                        return true;
                    }
                    ReportGUI.open(player, plugin, 0);
                    return true;
                }

                String targetName = args[0].startsWith("@") ? args[0].substring(1) : args[0];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    player.sendMessage(C.prefix() + C.red("Player '" + targetName + "' is not online."));
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage(C.prefix() + C.red("You can't check yourself."));
                    return true;
                }
                if (plugin.getSessionManager().isChecking(player)) {
                    player.sendMessage(C.prefix() + C.red("You're already in a check session. Use /check-finished first."));
                    return true;
                }
                plugin.getSessionManager().startSession(player, target);
            }


            case "check-back" -> {
                if (!plugin.getSessionManager().isChecking(player)) {
                    player.sendMessage(C.prefix() + C.red("You are not in a check session."));
                    return true;
                }
                plugin.getSessionManager().finishSession(player);
            }


            case "check-finished" -> {
                if (!plugin.getSessionManager().isChecking(player)) {
                    player.sendMessage(C.prefix() + C.red("You are not in a check session."));
                    return true;
                }
                CheckSession session = plugin.getSessionManager().endAndGetSession(player);
                if (session != null) {

                    plugin.getReportManager().removeReport(session.targetUUID());
                    player.sendMessage(C.prefix() + C.gray("Check finished. Report for ")
                            + C.aqua(session.targetName()) + C.gray(" cleared."));
                }
            }


            case "check-punish" -> {
                if (!plugin.getSessionManager().isChecking(player)) {
                    player.sendMessage(C.prefix() + C.red("You are not in a check session."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(C.prefix() + C.yellow("Usage: /check-punish <duration> <reason>"));
                    player.sendMessage(C.prefix() + C.gray("Duration: 1h, 12h, 1d, 7d, 30d, perm"));
                    return true;
                }

                String durationStr = args[0];
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                CheckSession session = plugin.getSessionManager().endAndGetSession(player);
                if (session == null) return true;


                plugin.getReportManager().removeReport(session.targetUUID());


                if (durationStr.equalsIgnoreCase("perm")) {
                    banPlayer(session.targetUUID(), session.targetName(), reason, null, player.getName());
                    player.sendMessage(C.prefix() + C.red("Permanently banned ")
                            + C.aqua(session.targetName()) + C.red(" for: ") + C.white(reason));
                } else {
                    Duration dur = parseDuration(durationStr);
                    if (dur == null) {
                        player.sendMessage(C.prefix() + C.red("Invalid duration. Use: 1h, 12h, 1d, 7d, 30d, perm"));

                        return true;
                    }
                    Date expires = Date.from(Instant.now().plus(dur));
                    banPlayer(session.targetUUID(), session.targetName(), reason, expires, player.getName());
                    player.sendMessage(C.prefix() + C.red("Banned ")
                            + C.aqua(session.targetName())
                            + C.red(" for " + durationStr + " — ") + C.white(reason));
                }


                Player target = Bukkit.getPlayer(session.targetUUID());
                if (target != null) {
                    target.kickPlayer(C.color("&cYou have been banned.\n&7Reason: &f" + reason
                            + "\n&7Duration: &f" + durationStr));
                }


                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getConfigManager().isAllowed(p.getName())) {
                        p.sendMessage(C.prefix() + C.aqua(player.getName())
                                + C.gray(" banned ") + C.red(session.targetName())
                                + C.gray(" (" + durationStr + "): ") + C.white(reason));
                    }
                }
            }
        }
        return true;
    }

    private void banPlayer(java.util.UUID uuid, String name, String reason, Date expires, String bannedBy) {

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        banList.addBan(name, reason, expires, bannedBy);
    }

    private Duration parseDuration(String s) {
        try {
            if (s.endsWith("h")) return Duration.ofHours(Long.parseLong(s.replace("h", "")));
            if (s.endsWith("d")) return Duration.ofDays(Long.parseLong(s.replace("d", "")));
            if (s.endsWith("m")) return Duration.ofMinutes(Long.parseLong(s.replace("m", "")));
        } catch (NumberFormatException ignored) {}
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (alias.equalsIgnoreCase("check") && args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    result.add(p.getName());
            }
        }
        if (alias.equalsIgnoreCase("check-punish") && args.length == 1) {
            result.addAll(List.of("1h", "12h", "1d", "7d", "30d", "perm"));
        }
        if (alias.equalsIgnoreCase("check-punish") && args.length == 2) {
            result.addAll(List.of("Hacking", "Cheating", "Xray", "Fly", "Killaura", "Speed"));
        }
        return result;
    }
}

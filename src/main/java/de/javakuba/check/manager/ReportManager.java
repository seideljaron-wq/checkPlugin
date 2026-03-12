package de.javakuba.check.manager;

import de.javakuba.check.CheckPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReportManager {

    private final CheckPlugin plugin;

    // Reported UUID -> ReportEntry (one entry per reported player, can have multiple reporters)
    private final LinkedHashMap<UUID, ReportEntry> reports = new LinkedHashMap<>();

    // Cooldown: reporter UUID -> last report time
    private final Map<UUID, Instant> reportCooldown = new HashMap<>();
    private static final int COOLDOWN_SECONDS = 60;

    public ReportManager(CheckPlugin plugin) {
        this.plugin = plugin;
    }

    public String addReport(Player reporter, Player reported, String reason) {
        // Cooldown check
        Instant last = reportCooldown.get(reporter.getUniqueId());
        if (last != null) {
            long secondsSince = ChronoUnit.SECONDS.between(last, Instant.now());
            if (secondsSince < COOLDOWN_SECONDS) {
                return "cooldown:" + (COOLDOWN_SECONDS - secondsSince);
            }
        }

        // Can't report yourself
        if (reporter.getUniqueId().equals(reported.getUniqueId())) {
            return "self";
        }

        reportCooldown.put(reporter.getUniqueId(), Instant.now());

        ReportEntry entry = reports.computeIfAbsent(
                reported.getUniqueId(),
                uuid -> new ReportEntry(uuid, reported.getName())
        );
        entry.addReport(reporter.getName(), reason);

        // Notify online staff
        notifyStaff(reporter.getName(), reported.getName(), reason, entry.getReportCount());

        return "ok";
    }

    private void notifyStaff(String reporter, String reported, String reason, int totalReports) {
        String msg = C.prefix()
                + C.gray("New report: ")
                + C.red(reported)
                + C.gray(" by ")
                + C.aqua(reporter)
                + C.gray(" — ")
                + C.yellow(reason)
                + C.gray(" (" + totalReports + " total). Use ")
                + C.gold("/checks")
                + C.gray(" to view.");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfigManager().isAllowed(p.getName())) {
                p.sendMessage(msg);
            }
        }
        plugin.getLogger().info("[Report] " + reported + " reported by " + reporter + ": " + reason);
    }

    public void removeReport(UUID reportedUUID) {
        reports.remove(reportedUUID);
    }

    public Collection<ReportEntry> getAllReports() {
        return reports.values();
    }

    public ReportEntry getReport(UUID uuid) {
        return reports.get(uuid);
    }

    public boolean hasReport(UUID uuid) {
        return reports.containsKey(uuid);
    }

    public int getTotalReports() {
        return reports.size();
    }

    public void startExpiryTask() {
        int expiryMins = plugin.getConfigManager().getReportExpiryMinutes();
        if (expiryMins <= 0) return;

        // Check every minute
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Instant cutoff = Instant.now().minus(expiryMins, ChronoUnit.MINUTES);
            reports.entrySet().removeIf(e -> e.getValue().getFirstReportTime().isBefore(cutoff));
        }, 1200L, 1200L);
    }
}

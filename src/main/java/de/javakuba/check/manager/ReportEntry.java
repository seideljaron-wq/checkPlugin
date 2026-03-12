package de.javakuba.check.manager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportEntry {

    private final UUID   reportedUUID;
    private final String reportedName;

    // Multiple reporters
    private final List<ReporterInfo> reporters = new ArrayList<>();

    public ReportEntry(UUID reportedUUID, String reportedName) {
        this.reportedUUID = reportedUUID;
        this.reportedName = reportedName;
    }

    public void addReport(String reporterName, String reason) {
        reporters.add(new ReporterInfo(reporterName, reason, Instant.now()));
    }

    public UUID   getReportedUUID() { return reportedUUID; }
    public String getReportedName() { return reportedName; }
    public List<ReporterInfo> getReporters() { return reporters; }
    public int getReportCount()     { return reporters.size(); }

    // Returns the most recent reason
    public String getLatestReason() {
        if (reporters.isEmpty()) return "Unknown";
        return reporters.get(reporters.size() - 1).reason();
    }

    public Instant getFirstReportTime() {
        if (reporters.isEmpty()) return Instant.now();
        return reporters.get(0).time();
    }

    public record ReporterInfo(String name, String reason, Instant time) {}
}

package me.koyere.antiafkplus.analytics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.events.PlayerAFKPatternDetectedEvent;
import me.koyere.antiafkplus.events.PlayerAFKStateChangeEvent;
import me.koyere.antiafkplus.platform.PlatformScheduler;

/**
 * Collects AFK session and pattern detection data in memory, then exports
 * it to disk as daily/weekly reports in JSON or CSV format.
 *
 * Enabled by analytics.enabled: true in config.yml.
 * Reports are saved to  plugins/AntiAFKPlus/analytics/
 */
public class AnalyticsManager implements Listener {

    /** Maximum records kept in memory per list (oldest are dropped first). */
    private static final int MAX_IN_MEMORY = 5_000;

    /** How often (ticks) the report-due check runs — every 10 minutes. */
    private static final long REPORT_CHECK_TICKS = 12_000L;

    private final AntiAFKPlus plugin;
    private final Logger logger;
    private final File analyticsDir;

    // Open AFK sessions: UUID → session-start timestamp
    private final Map<UUID, Long> openSessions = new ConcurrentHashMap<>();

    // Completed sessions and pattern events (thread-safe lists)
    private final List<SessionRecord> completedSessions = new CopyOnWriteArrayList<>();
    private final List<PatternRecord> patternRecords    = new CopyOnWriteArrayList<>();

    // Report scheduling
    private volatile LocalDate lastDailyReportDate = null;
    private PlatformScheduler.ScheduledTask reportCheckTask;

    // ===================== Inner Records =====================

    /** An individual completed AFK session. */
    private record SessionRecord(
            UUID uuid,
            String playerName,
            long startMs,
            long endMs,
            String reason,
            boolean wasManual) {

        double durationMinutes() { return (endMs - startMs) / 60_000.0; }
    }

    /** A single pattern-detection event. */
    private record PatternRecord(
            UUID uuid,
            String playerName,
            long timestampMs,
            String patternType,
            double confidence) {}

    // ===================== Constructor =====================

    public AnalyticsManager(AntiAFKPlus plugin) {
        this.plugin       = plugin;
        this.logger       = plugin.getLogger();
        this.analyticsDir = new File(plugin.getDataFolder(), "analytics");
        if (!analyticsDir.exists() && !analyticsDir.mkdirs()) {
            logger.warning("Could not create analytics directory.");
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        scheduleReportTask();
        logger.info("§aAnalytics Manager initialized.");
    }

    private void scheduleReportTask() {
        // Runs on the main thread — safe to read Bukkit state if needed
        this.reportCheckTask = plugin.getPlatformScheduler()
                .runTaskTimer(this::checkAndGenerateReports, REPORT_CHECK_TICKS, REPORT_CHECK_TICKS);
    }

    // ===================== Event Handlers =====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAFKStateChange(PlayerAFKStateChangeEvent event) {
        if (!plugin.getConfig().getBoolean("analytics.collect-player-statistics", true)) return;

        Player player = event.getPlayer();
        UUID uuid     = player.getUniqueId();

        if (event.isEnteringAFK()) {
            openSessions.put(uuid, System.currentTimeMillis());
        } else if (event.isLeavingAFK()) {
            Long startMs = openSessions.remove(uuid);
            if (startMs == null) return;
            String reason = event.getReason().name().toLowerCase();
            addSession(new SessionRecord(
                    uuid, player.getName(), startMs,
                    System.currentTimeMillis(), reason, event.wasManual()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPatternDetected(PlayerAFKPatternDetectedEvent event) {
        if (!plugin.getConfig().getBoolean("analytics.collect-pattern-data", true)) return;
        Player player = event.getPlayer();
        addPattern(new PatternRecord(
                player.getUniqueId(),
                player.getName(),
                System.currentTimeMillis(),
                event.getPatternType().name().toLowerCase(),
                event.getConfidence()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("analytics.collect-player-statistics", true)) return;
        UUID uuid    = event.getPlayer().getUniqueId();
        Long startMs = openSessions.remove(uuid);
        if (startMs == null) return;
        addSession(new SessionRecord(
                uuid, event.getPlayer().getName(),
                startMs, System.currentTimeMillis(), "player_quit", false));
    }

    // ===================== List Management =====================

    private void addSession(SessionRecord record) {
        completedSessions.add(record);
        if (completedSessions.size() > MAX_IN_MEMORY) {
            // Drop the oldest 10 % when limit is exceeded
            completedSessions.subList(0, MAX_IN_MEMORY / 10).clear();
        }
    }

    private void addPattern(PatternRecord record) {
        patternRecords.add(record);
        if (patternRecords.size() > MAX_IN_MEMORY) {
            patternRecords.subList(0, MAX_IN_MEMORY / 10).clear();
        }
    }

    // ===================== Report Scheduling =====================

    private void checkAndGenerateReports() {
        if (!plugin.getConfig().getBoolean("analytics.enabled", false)) return;

        LocalDate today = LocalDate.now();

        if (plugin.getConfig().getBoolean("analytics.generate-daily-reports", true)) {
            if (lastDailyReportDate == null || lastDailyReportDate.isBefore(today)) {
                // Generate report for "yesterday" when crossing midnight, or for today on first run
                LocalDate reportDate = (lastDailyReportDate != null) ? today.minusDays(1) : today;
                generateReport(reportDate, false);
                lastDailyReportDate = today;
            }
        }
    }

    /** Generates a report for the given date. {@code isWeekly} switches naming/content. */
    private void generateReport(LocalDate date, boolean isWeekly) {
        if (completedSessions.isEmpty() && patternRecords.isEmpty()) return;

        String format   = plugin.getConfig().getString("analytics.export-format", "JSON").toUpperCase();
        String prefix   = isWeekly ? "weekly_" : "daily_";
        String filename = prefix + date + "." + format.toLowerCase();
        File   outFile  = new File(analyticsDir, filename);

        try {
            switch (format) {
                case "CSV"  -> writeCsv(outFile, date);
                default     -> writeJson(outFile, date);
            }
            logger.info("§aAnalytics report saved: " + filename);
        } catch (IOException e) {
            logger.warning("§cFailed to write analytics report '" + filename + "': " + e.getMessage());
        }
    }

    // ===================== JSON Writer =====================

    private void writeJson(File file, LocalDate date) throws IOException {
        List<SessionRecord> sessions = new ArrayList<>(completedSessions);
        List<PatternRecord> patterns = new ArrayList<>(patternRecords);

        StringBuilder sb = new StringBuilder("{\n");
        sb.append("  \"date\": \"").append(date).append("\",\n");
        sb.append("  \"generatedAt\": \"").append(LocalDateTime.now()).append("\",\n");
        sb.append("  \"summary\": {\n")
          .append("    \"uniquePlayers\": ").append(countUniquePlayers(sessions)).append(",\n")
          .append("    \"totalSessions\": ").append(sessions.size()).append(",\n")
          .append("    \"totalAfkMinutes\": ").append(String.format("%.1f", sumMinutes(sessions))).append(",\n")
          .append("    \"mostCommonReason\": \"").append(escapeJson(mostCommonReason(sessions))).append("\",\n")
          .append("    \"patternDetections\": ").append(patterns.size()).append("\n")
          .append("  },\n");

        sb.append("  \"sessions\": [\n");
        for (int i = 0; i < sessions.size(); i++) {
            SessionRecord s = sessions.get(i);
            sb.append("    {")
              .append("\"player\":\"").append(escapeJson(s.playerName())).append("\",")
              .append("\"durationMinutes\":").append(String.format("%.2f", s.durationMinutes())).append(",")
              .append("\"reason\":\"").append(escapeJson(s.reason())).append("\",")
              .append("\"wasManual\":").append(s.wasManual()).append("}");
            if (i < sessions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"patternDetections\": [\n");
        for (int i = 0; i < patterns.size(); i++) {
            PatternRecord p = patterns.get(i);
            sb.append("    {")
              .append("\"player\":\"").append(escapeJson(p.playerName())).append("\",")
              .append("\"type\":\"").append(escapeJson(p.patternType())).append("\",")
              .append("\"confidence\":").append(String.format("%.2f", p.confidence())).append("}");
            if (i < patterns.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n");

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.print(sb);
        }
    }

    // ===================== CSV Writer =====================

    private void writeCsv(File file, LocalDate date) throws IOException {
        List<SessionRecord> sessions = new ArrayList<>(completedSessions);
        List<PatternRecord> patterns = new ArrayList<>(patternRecords);

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("# AntiAFKPlus Analytics — " + date);
            pw.println("type,player,durationMinutes,reason,wasManual,patternType,confidence");
            for (SessionRecord s : sessions) {
                pw.printf("session,\"%s\",%.2f,\"%s\",%b,,%n",
                        s.playerName().replace("\"", ""), s.durationMinutes(),
                        s.reason().replace("\"", ""), s.wasManual());
            }
            for (PatternRecord p : patterns) {
                pw.printf("pattern,\"%s\",,,\"%s\",%.2f%n",
                        p.playerName().replace("\"", ""),
                        p.patternType().replace("\"", ""), p.confidence());
            }
        }
    }

    // ===================== Public API =====================

    /**
     * Returns formatted lines suitable for sending to a CommandSender
     * via {@code /afkplus analytics}.
     */
    public List<String> buildAdminSummary() {
        List<SessionRecord> sessions = new ArrayList<>(completedSessions);
        List<PatternRecord> patterns = new ArrayList<>(patternRecords);

        List<String> lines = new ArrayList<>();
        lines.add(plugin.getConfigManager().getMessage(
                "analytics.summary-header", "&6=== AntiAFK+ Analytics Summary ==="));
        lines.add(plugin.getConfigManager().getMessage(
                "analytics.total-players", "&7Unique players tracked: &e{count}")
                .replace("{count}", String.valueOf(countUniquePlayers(sessions))));
        lines.add(plugin.getConfigManager().getMessage(
                "analytics.total-sessions", "&7Total AFK sessions: &e{count}")
                .replace("{count}", String.valueOf(sessions.size())));
        lines.add(plugin.getConfigManager().getMessage(
                "analytics.total-afk-time", "&7Total AFK time: &e{time}")
                .replace("{time}", String.format("%.1f min", sumMinutes(sessions))));
        lines.add(plugin.getConfigManager().getMessage(
                "analytics.most-common-reason", "&7Most common AFK reason: &e{reason}")
                .replace("{reason}", mostCommonReason(sessions)));
        lines.add(plugin.getConfigManager().getMessage(
                "analytics.pattern-detections", "&7Pattern detections (memory): &e{count}")
                .replace("{count}", String.valueOf(patterns.size())));
        return lines;
    }

    /**
     * Forces an immediate export to the analytics folder and returns the filename,
     * or {@code null} if the export failed.
     */
    public String exportNow() {
        String format   = plugin.getConfig().getString("analytics.export-format", "JSON").toUpperCase();
        String filename = "export_" + LocalDate.now() + "_" + System.currentTimeMillis() + "." + format.toLowerCase();
        File   outFile  = new File(analyticsDir, filename);
        try {
            switch (format) {
                case "CSV"  -> writeCsv(outFile, LocalDate.now());
                default     -> writeJson(outFile, LocalDate.now());
            }
            return filename;
        } catch (IOException e) {
            logger.warning("Manual analytics export failed: " + e.getMessage());
            return null;
        }
    }

    // ===================== Aggregation Helpers =====================

    private int countUniquePlayers(List<SessionRecord> sessions) {
        Set<UUID> uuids = new HashSet<>();
        for (SessionRecord s : sessions) uuids.add(s.uuid());
        return uuids.size();
    }

    private double sumMinutes(List<SessionRecord> sessions) {
        double total = 0;
        for (SessionRecord s : sessions) total += s.durationMinutes();
        return total;
    }

    private String mostCommonReason(List<SessionRecord> sessions) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SessionRecord s : sessions) counts.merge(s.reason(), 1, Integer::sum);
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    // ===================== Lifecycle =====================

    public void shutdown() {
        if (reportCheckTask != null && !reportCheckTask.isCancelled()) {
            reportCheckTask.cancel();
        }
        // Flush a final snapshot if there is collected data
        if (plugin.getConfig().getBoolean("analytics.generate-daily-reports", true)
                && (!completedSessions.isEmpty() || !patternRecords.isEmpty())) {
            generateReport(LocalDate.now(), false);
        }
        openSessions.clear();
        completedSessions.clear();
        patternRecords.clear();
    }
}

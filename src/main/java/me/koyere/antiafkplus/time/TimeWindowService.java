package me.koyere.antiafkplus.time;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.TimeWindowSettings;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Provides time-window evaluation so AFK actions can be conditionally disabled
 * during configurable hours. Thread-safe and cache-friendly (minute-level granularity).
 */
public class TimeWindowService {

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public enum WindowBehavior {
        DEFAULT,
        SKIP_ACTIONS,
        EXTEND_THRESHOLD,
        MESSAGE_ONLY
    }

    private final AntiAFKPlus plugin;
    private final Logger logger;
    private final boolean featureEnabled;
    private final ZoneId zoneId;
    private final List<WindowRange> ranges;
    private final WindowBehavior insideBehavior;
    private final WindowBehavior outsideBehavior;
    private final long extendMillis;
    private final String bypassPermission;
    private final Clock clock;

    private volatile CachedEvaluation cachedEvaluation;

    public TimeWindowService(AntiAFKPlus plugin, TimeWindowSettings settings) {
        this(plugin, settings, Clock.systemDefaultZone());
    }

    public TimeWindowService(AntiAFKPlus plugin, TimeWindowSettings settings, Clock clock) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.clock = clock;
        this.featureEnabled = settings.enabled();
        this.zoneId = resolveZoneId(settings.timezoneId());
        this.ranges = Collections.unmodifiableList(parseRanges(settings.ranges()));
        this.insideBehavior = parseBehavior(settings.behaviorInside(), WindowBehavior.SKIP_ACTIONS);
        this.outsideBehavior = parseBehavior(settings.behaviorOutside(), WindowBehavior.DEFAULT);
        this.extendMillis = Math.max(0L, settings.extendSeconds() * 1000L);
        this.bypassPermission = settings.bypassPermission() == null ? "" : settings.bypassPermission().trim();
        this.cachedEvaluation = null;

        if (featureEnabled && ranges.isEmpty()) {
            logger.warning("[AFK Windows] Feature enabled but no valid ranges parsed; disabling.");
        }
    }

    public WindowEvaluation evaluate() {
        return evaluate(Instant.now(clock));
    }

    public WindowEvaluation evaluate(Instant instant) {
        if (!featureEnabled || ranges.isEmpty()) {
            return WindowEvaluation.disabled(zoneId);
        }

        ZonedDateTime zoned = instant.atZone(zoneId);
        int currentMinute = zoned.getHour() * 60 + zoned.getMinute();

        CachedEvaluation cached = cachedEvaluation;
        if (cached != null && cached.minuteOfDay == currentMinute) {
            return cached.evaluation;
        }

        boolean inside = isMinuteInside(currentMinute);
        WindowBehavior behavior = inside ? insideBehavior : outsideBehavior;
        Duration timeUntilChange = calculateTimeUntilChange(currentMinute, zoned);
        String nextChangeDisplay = formatNextChange(zoned, timeUntilChange);

        WindowEvaluation evaluation = new WindowEvaluation(
                true,
                inside,
                behavior,
                timeUntilChange,
                nextChangeDisplay,
                zoneId,
                extendMillis,
                bypassPermission
        );
        cachedEvaluation = new CachedEvaluation(currentMinute, evaluation);
        return evaluation;
    }

    private Duration calculateTimeUntilChange(int currentMinute, ZonedDateTime base) {
        if (ranges.isEmpty()) {
            return Duration.ZERO;
        }
        int bestDelta = MINUTES_PER_DAY;
        for (WindowRange range : ranges) {
            int deltaStart = computeDelta(currentMinute, range.startMinute());
            int deltaEnd = computeDelta(currentMinute, range.endMinute());
            bestDelta = Math.min(bestDelta, Math.min(deltaStart, deltaEnd));
        }
        return Duration.ofMinutes(bestDelta);
    }

    private int computeDelta(int currentMinute, int boundaryMinute) {
        int delta = boundaryMinute - currentMinute;
        if (delta <= 0) {
            delta += MINUTES_PER_DAY;
        }
        return delta;
    }

    private String formatNextChange(ZonedDateTime base, Duration delta) {
        if (delta.isZero()) {
            return DISPLAY_FORMAT.format(base.truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
        }
        ZonedDateTime next = base.plus(delta).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        return DISPLAY_FORMAT.format(next);
    }

    private boolean isMinuteInside(int minute) {
        for (WindowRange range : ranges) {
            if (range.contains(minute)) {
                return true;
            }
        }
        return false;
    }

    private List<WindowRange> parseRanges(List<String> rawRanges) {
        if (rawRanges == null || rawRanges.isEmpty()) {
            return Collections.emptyList();
        }

        List<WindowRange> parsed = new ArrayList<>();
        for (String entry : rawRanges) {
            WindowRange range = parseRange(entry);
            if (range != null) {
                parsed.add(range);
            }
        }

        parsed.sort(Comparator.comparingInt(WindowRange::startMinute));
        return parsed;
    }

    private WindowRange parseRange(String entry) {
        if (entry == null) {
            return null;
        }
        String trimmed = entry.trim();
        if (trimmed.isEmpty() || !trimmed.contains("-")) {
            logger.warning("[AFK Windows] Invalid range '" + entry + "'. Expected format HH:MM-HH:MM");
            return null;
        }
        String[] parts = trimmed.split("-");
        if (parts.length != 2) {
            logger.warning("[AFK Windows] Invalid range '" + entry + "'. Expected format HH:MM-HH:MM");
            return null;
        }

        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            return new WindowRange(start.getHour() * 60 + start.getMinute(),
                    end.getHour() * 60 + end.getMinute());
        } catch (Exception ex) {
            logger.warning("[AFK Windows] Unable to parse range '" + entry + "': " + ex.getMessage());
            return null;
        }
    }

    private WindowBehavior parseBehavior(String value, WindowBehavior fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return WindowBehavior.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warning("[AFK Windows] Unknown behavior '" + value + "', falling back to " + fallback);
            return fallback;
        }
    }

    private ZoneId resolveZoneId(String id) {
        if (id == null || id.isBlank() || "SERVER".equalsIgnoreCase(id.trim())) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(id.trim());
        } catch (Exception ex) {
            logger.warning("[AFK Windows] Invalid timezone '" + id + "', using server default.");
            return ZoneId.systemDefault();
        }
    }

    private record WindowRange(int startMinute, int endMinute) {
        boolean contains(int minute) {
            if (startMinute == endMinute) {
                return true; // full-day range
            }
            if (startMinute < endMinute) {
                return minute >= startMinute && minute < endMinute;
            }
            // wrap-around (eg. 22:00-02:00)
            return minute >= startMinute || minute < endMinute;
        }
    }

    private record CachedEvaluation(int minuteOfDay, WindowEvaluation evaluation) {}

    public record WindowEvaluation(
            boolean featureEnabled,
            boolean insideWindow,
            WindowBehavior behavior,
            Duration timeUntilNextChange,
            String nextChangeDisplay,
            ZoneId zoneId,
            long extendMillis,
            String bypassPermission
    ) {
        public static WindowEvaluation disabled(ZoneId zone) {
            return new WindowEvaluation(false, false, WindowBehavior.DEFAULT,
                    Duration.ZERO, "", zone, 0L, "");
        }

        public boolean shouldSkipActions() {
            return behavior == WindowBehavior.SKIP_ACTIONS || behavior == WindowBehavior.MESSAGE_ONLY;
        }
    }
}

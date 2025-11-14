package me.koyere.antiafkplus.config;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the AFK window configuration loaded from config.yml.
 */
public record TimeWindowSettings(
        boolean enabled,
        String timezoneId,
        List<String> ranges,
        String behaviorInside,
        String behaviorOutside,
        long extendSeconds,
        String bypassPermission
) {
    public static TimeWindowSettings disabled() {
        return new TimeWindowSettings(false, "SERVER", Collections.emptyList(),
                "DEFAULT", "DEFAULT", 0L, "");
    }
}

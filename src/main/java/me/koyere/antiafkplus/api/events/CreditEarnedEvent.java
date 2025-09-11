package me.koyere.antiafkplus.api.events;

import org.bukkit.entity.Player;

import java.time.Instant;

/**
 * API event fired when a player earns AFK credits.
 */
public class CreditEarnedEvent {
    private final Player player;
    private final long creditsEarned;
    private final long totalCredits;
    private final Instant timestamp;

    public CreditEarnedEvent(Player player, long creditsEarned, long totalCredits, Instant timestamp) {
        this.player = player;
        this.creditsEarned = creditsEarned;
        this.totalCredits = totalCredits;
        this.timestamp = timestamp;
    }

    public Player getPlayer() { return player; }
    public long getCreditsEarned() { return creditsEarned; }
    public long getTotalCredits() { return totalCredits; }
    public Instant getTimestamp() { return timestamp; }
}


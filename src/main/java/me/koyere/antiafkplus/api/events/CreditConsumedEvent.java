package me.koyere.antiafkplus.api.events;

import org.bukkit.entity.Player;
import java.time.Instant;

/**
 * API event fired when a player consumes AFK credits.
 */
public class CreditConsumedEvent {
    private final Player player;
    private final long creditsConsumed;
    private final long remainingCredits;
    private final Instant timestamp;

    public CreditConsumedEvent(Player player, long creditsConsumed, long remainingCredits, Instant timestamp) {
        this.player = player;
        this.creditsConsumed = creditsConsumed;
        this.remainingCredits = remainingCredits;
        this.timestamp = timestamp;
    }

    public Player getPlayer() { return player; }
    public long getCreditsConsumed() { return creditsConsumed; }
    public long getRemainingCredits() { return remainingCredits; }
    public Instant getTimestamp() { return timestamp; }
}


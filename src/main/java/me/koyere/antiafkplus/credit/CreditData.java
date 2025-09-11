package me.koyere.antiafkplus.credit;

import me.koyere.antiafkplus.platform.PlatformScheduler;
import org.bukkit.Location;

import java.time.Instant;
import java.util.UUID;

/**
 * CreditData almacena el estado de créditos de un jugador y
 * los datos de una sesión de consumo activa (si existe).
 */
public class CreditData {
    public final UUID playerId;

    // Saldo actual (en minutos)
    private long balanceMinutes;

    // Datos de consumo activo
    private boolean consuming;
    private Instant consumingSince;
    private long minutesConsumedInSession;
    private PlatformScheduler.ScheduledTask consumeTask; // tarea por-jugador (1/min)
    private Location originalLocation; // guardado al inicio de consumo para retorno futuro
    private boolean inAfkZone;
    private long lastReturnAtEpochSeconds;
    private boolean lowCreditWarned;
    private long lastDecayWarningEpochDay;

    // Metadatos
    private Instant lastEarnedAt;

    public CreditData(UUID playerId) {
        this.playerId = playerId;
    }

    public long getBalanceMinutes() {
        return balanceMinutes;
    }

    public void setBalanceMinutes(long balanceMinutes) {
        this.balanceMinutes = Math.max(0, balanceMinutes);
    }

    public void addMinutes(long minutes) {
        if (minutes > 0) {
            this.balanceMinutes += minutes;
            this.lastEarnedAt = Instant.now();
        }
    }

    public boolean consumeOneMinute() {
        if (balanceMinutes <= 0) return false;
        balanceMinutes -= 1;
        minutesConsumedInSession += 1;
        return true;
    }

    public boolean isConsuming() {
        return consuming;
    }

    public void startConsuming(Location originalLocation) {
        this.consuming = true;
        this.consumingSince = Instant.now();
        this.minutesConsumedInSession = 0;
        this.originalLocation = originalLocation;
    }

    public void stopConsuming() {
        this.consuming = false;
        this.consumingSince = null;
        this.minutesConsumedInSession = 0;
        cancelConsumeTask();
    }

    public Instant getLastEarnedAt() {
        return lastEarnedAt;
    }
    public void setLastEarnedAt(Instant instant) { this.lastEarnedAt = instant; }

    public PlatformScheduler.ScheduledTask getConsumeTask() {
        return consumeTask;
    }

    public void setConsumeTask(PlatformScheduler.ScheduledTask consumeTask) {
        cancelConsumeTask();
        this.consumeTask = consumeTask;
    }

    public void cancelConsumeTask() {
        if (this.consumeTask != null && !this.consumeTask.isCancelled()) {
            this.consumeTask.cancel();
        }
        this.consumeTask = null;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public boolean isInAfkZone() { return inAfkZone; }
    public void setInAfkZone(boolean inAfkZone) { this.inAfkZone = inAfkZone; }

    public long getLastReturnAtEpochSeconds() { return lastReturnAtEpochSeconds; }
    public void setLastReturnAtEpochSeconds(long epochSeconds) { this.lastReturnAtEpochSeconds = epochSeconds; }

    public boolean isLowCreditWarned() { return lowCreditWarned; }
    public void setLowCreditWarned(boolean lowCreditWarned) { this.lowCreditWarned = lowCreditWarned; }

    public long getLastDecayWarningEpochDay() { return lastDecayWarningEpochDay; }
    public void setLastDecayWarningEpochDay(long lastDecayWarningEpochDay) { this.lastDecayWarningEpochDay = lastDecayWarningEpochDay; }
}

package me.koyere.antiafkplus.afk;

import java.util.UUID;

/**
 * AFKData stores individual player AFK status and timing.
 */
public class AFKData {

    private boolean isAFK;
    private boolean isManual;
    private long afkStartTime;
    private long manualAFKStartTime;
    private final UUID playerUUID;

    public AFKData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.isAFK = false;
        this.isManual = false;
        this.afkStartTime = 0L;
        this.manualAFKStartTime = 0L;
    }

    public boolean isAFK() {
        return isAFK;
    }

    public void setAFK(boolean afk) {
        this.isAFK = afk;
    }

    public boolean isManual() {
        return isManual;
    }

    public void setManual(boolean manual) {
        this.isManual = manual;
    }

    public long getAfkStartTime() {
        return afkStartTime;
    }

    public void setAfkStartTime(long afkStartTime) {
        this.afkStartTime = afkStartTime;
    }

    public long getManualAFKStartTime() {
        return manualAFKStartTime;
    }

    public void setManualAFKStartTime(long time) {
        this.manualAFKStartTime = time;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }
}

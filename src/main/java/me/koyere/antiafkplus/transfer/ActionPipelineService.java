package me.koyere.antiafkplus.transfer;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActionPipelineService - Fase 4 (parcial)
 * Ejecuta una lista de pasos (TITLE, SUBTITLE, SOUND, MESSAGE, WAIT, TRANSFER)
 * secuencialmente, Folia-safe, cancelable si deja de estar AFK.
 */
public class ActionPipelineService {

    private final AntiAFKPlus plugin;
    private final Map<UUID, RunningPipeline> running = new ConcurrentHashMap<>();

    public ActionPipelineService(AntiAFKPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning(Player player) {
        return player != null && running.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        if (player == null) return;
        RunningPipeline rp = running.remove(player.getUniqueId());
        if (rp != null && rp.currentTask != null) {
            rp.currentTask.cancel();
        }
    }

    /**
     * Ejecuta el pipeline definido en config (server-transfer.actions) para un jugador.
     * Si las acciones no incluyen TRANSFER, ejecuta onComplete al final (para transferencia por defecto).
     */
    public void startPipelineFromConfig(Player player, Runnable onComplete) {
        List<String> raw = plugin.getConfig().getStringList("server-transfer.actions");
        List<Step> steps = parseSteps(raw, player);
        boolean includesTransfer = steps.stream().anyMatch(s -> s.type == StepType.TRANSFER);
        startPipeline(player, steps, includesTransfer ? null : onComplete);
    }

    public void startPipeline(Player player, List<Step> steps, Runnable onComplete) {
        if (player == null || !player.isOnline() || steps == null || steps.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        UUID uuid = player.getUniqueId();
        cancel(player);
        RunningPipeline rp = new RunningPipeline();
        running.put(uuid, rp);

        runNext(player.getUniqueId(), steps, 0, onComplete);
    }

    private void runNext(UUID uuid, List<Step> steps, int index, Runnable onComplete) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            cancelByUuid(uuid);
            return;
        }
        if (!plugin.getAfkManager().isAFK(player)) {
            cancelByUuid(uuid);
            return;
        }

        if (index >= steps.size()) {
            cancelByUuid(uuid);
            if (onComplete != null) {
                plugin.getPlatformScheduler().runTaskForEntity(player, onComplete);
            }
            return;
        }

        Step step = steps.get(index);
        Runnable scheduleNext = () -> runNext(uuid, steps, index + 1, onComplete);

        switch (step.type) {
            case WAIT: {
                long ticks = Math.max(1, step.ticks);
                setCurrentTask(uuid, plugin.getPlatformScheduler().runTaskLater(scheduleNext, ticks));
                break;
            }
            case TITLE: {
                plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.sendTitle(step.text, "", 5, 15, 5));
                setCurrentTask(uuid, plugin.getPlatformScheduler().runTaskLater(scheduleNext, 1));
                break;
            }
            case SUBTITLE: {
                plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.sendTitle("", step.text, 5, 15, 5));
                setCurrentTask(uuid, plugin.getPlatformScheduler().runTaskLater(scheduleNext, 1));
                break;
            }
            case MESSAGE: {
                String msg = color(step.text);
                plugin.getPlatformScheduler().runTaskForEntity(player, () -> player.sendMessage(msg));
                setCurrentTask(uuid, plugin.getPlatformScheduler().runTaskLater(scheduleNext, 1));
                break;
            }
            case SOUND: {
                plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
                    try {
                        Sound s = Sound.valueOf(step.soundName);
                        player.playSound(player.getLocation(), s, step.volume, step.pitch);
                    } catch (IllegalArgumentException ignored) {}
                });
                setCurrentTask(uuid, plugin.getPlatformScheduler().runTaskLater(scheduleNext, 1));
                break;
            }
            case TRANSFER: {
                plugin.getPlatformScheduler().runTaskForEntity(player, () -> {
                    if (plugin.getServerTransferService() != null) {
                        plugin.getServerTransferService().transferPlayer(player, step.transferTarget);
                    }
                });
                setCurrentTask(uuid, plugin.getPlatformScheduler().runTaskLater(scheduleNext, 1));
                break;
            }
        }
    }

    private void setCurrentTask(UUID uuid, PlatformScheduler.ScheduledTask task) {
        RunningPipeline rp = running.get(uuid);
        if (rp != null) {
            if (rp.currentTask != null) rp.currentTask.cancel();
            rp.currentTask = task;
        }
    }

    private void cancelByUuid(UUID uuid) {
        RunningPipeline rp = running.remove(uuid);
        if (rp != null && rp.currentTask != null) {
            rp.currentTask.cancel();
        }
    }

    private List<Step> parseSteps(List<String> raw, Player player) {
        List<Step> out = new ArrayList<>();
        if (raw == null) return out;
        for (String r : raw) {
            if (r == null) continue;
            String line = r.trim();
            if (line.isEmpty()) continue;
            int idx = line.indexOf(':');
            String key = idx >= 0 ? line.substring(0, idx).trim().toUpperCase() : line.toUpperCase();
            String val = idx >= 0 ? line.substring(idx + 1).trim() : "";
            switch (key) {
                case "TITLE":
                    out.add(Step.title(color(val)));
                    break;
                case "SUBTITLE":
                    out.add(Step.subtitle(color(val)));
                    break;
                case "MESSAGE":
                    out.add(Step.message(color(val)));
                    break;
                case "WAIT": {
                    long ticks = parseWait(val);
                    out.add(Step.waitTicks(ticks));
                    break;
                }
                case "SOUND": {
                    // Formato: NAME[,volume[,pitch]]
                    String[] parts = val.split(",");
                    String name = (parts.length > 0 ? parts[0].trim() : "ENTITY_EXPERIENCE_ORB_PICKUP");
                    float vol = parts.length > 1 ? safeFloat(parts[1], 1.0f) : 1.0f;
                    float pit = parts.length > 2 ? safeFloat(parts[2], 1.0f) : 1.0f;
                    out.add(Step.sound(name, vol, pit));
                    break;
                }
                case "TRANSFER": {
                    String target = val.isEmpty() ? plugin.getConfig().getString("server-transfer.target-server", "") : val;
                    out.add(Step.transfer(target));
                    break;
                }
                default:
                    // Ignorar claves desconocidas para robustez
                    break;
            }
        }
        return out;
    }

    private long parseWait(String val) {
        String v = val.trim().toLowerCase();
        try {
            if (v.endsWith("ms")) {
                long ms = Long.parseLong(v.substring(0, v.length() - 2).trim());
                return Math.max(1, ms / 50L);
            }
            if (v.endsWith("s")) {
                long s = Long.parseLong(v.substring(0, v.length() - 1).trim());
                return Math.max(1, s * 20L);
            }
            long ticks = Long.parseLong(v);
            return Math.max(1, ticks);
        } catch (NumberFormatException e) {
            return 20L; // 1s por defecto
        }
    }

    private float safeFloat(String s, float def) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return def; }
    }

    private String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public enum StepType { TITLE, SUBTITLE, MESSAGE, SOUND, WAIT, TRANSFER }

    private static class RunningPipeline {
        PlatformScheduler.ScheduledTask currentTask;
    }

    public static class Step {
        public final StepType type;
        public final String text;
        public final String soundName;
        public final float volume;
        public final float pitch;
        public final long ticks;
        public final String transferTarget;

        private Step(StepType type, String text, String soundName, float volume, float pitch, long ticks, String transferTarget) {
            this.type = type;
            this.text = text;
            this.soundName = soundName;
            this.volume = volume;
            this.pitch = pitch;
            this.ticks = ticks;
            this.transferTarget = transferTarget;
        }

        public static Step title(String text) { return new Step(StepType.TITLE, text, null, 0f, 0f, 0L, null); }
        public static Step subtitle(String text) { return new Step(StepType.SUBTITLE, text, null, 0f, 0f, 0L, null); }
        public static Step message(String text) { return new Step(StepType.MESSAGE, text, null, 0f, 0f, 0L, null); }
        public static Step waitTicks(long ticks) { return new Step(StepType.WAIT, null, null, 0f, 0f, ticks, null); }
        public static Step sound(String name, float vol, float pit) { return new Step(StepType.SOUND, null, name, vol, pit, 0L, null); }
        public static Step transfer(String target) { return new Step(StepType.TRANSFER, null, null, 0f, 0f, 0L, target); }
    }
}

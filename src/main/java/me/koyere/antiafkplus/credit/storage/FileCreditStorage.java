package me.koyere.antiafkplus.credit.storage;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.credit.CreditData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistencia simple en archivo YAML (ligera y por defecto).
 */
public class FileCreditStorage implements CreditStorage {
    private final AntiAFKPlus plugin;
    private final File storageFile;

    public FileCreditStorage(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "credits.yml");
        if (!this.storageFile.getParentFile().exists()) {
            this.storageFile.getParentFile().mkdirs();
        }
    }

    @Override
    public Map<UUID, CreditData> loadAll() {
        Map<UUID, CreditData> map = new HashMap<>();
        try {
            if (!storageFile.exists()) return map;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
            var section = yaml.getConfigurationSection("players");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(key);
                        long bal = yaml.getLong("players." + key + ".balance", 0);
                        long lastEarned = yaml.getLong("players." + key + ".lastEarnedAt", 0);
                        CreditData data = new CreditData(id);
                        data.setBalanceMinutes(bal);
                        if (lastEarned > 0) {
                            data.setLastEarnedAt(Instant.ofEpochMilli(lastEarned));
                        }
                        map.put(id, data);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    @Override
    public void saveAll(Map<UUID, CreditData> data) {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<UUID, CreditData> e : data.entrySet()) {
                String base = "players." + e.getKey();
                yaml.set(base + ".balance", e.getValue().getBalanceMinutes());
                var le = e.getValue().getLastEarnedAt();
                yaml.set(base + ".lastEarnedAt", le != null ? le.toEpochMilli() : 0L);
            }
            yaml.save(storageFile);
        } catch (IOException ignored) {}
    }

    @Override
    public void saveOne(UUID uuid, CreditData data) {
        try {
            YamlConfiguration yaml = storageFile.exists() ? YamlConfiguration.loadConfiguration(storageFile) : new YamlConfiguration();
            String base = "players." + uuid;
            yaml.set(base + ".balance", data.getBalanceMinutes());
            var le = data.getLastEarnedAt();
            yaml.set(base + ".lastEarnedAt", le != null ? le.toEpochMilli() : 0L);
            yaml.save(storageFile);
        } catch (IOException ignored) {}
    }

    @Override
    public void close() { /* no-op */ }
}

package me.koyere.antiafkplus.credit.storage;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.credit.CreditData;
import me.koyere.antiafkplus.api.data.CreditTransaction;
import me.koyere.antiafkplus.api.data.CreditTransactionType;

import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistencia SQL ligera sin dependencias externas.
 * Requiere que el driver JDBC esté presente en el classpath del servidor:
 * - SQLite: org.sqlite.JDBC
 * - MySQL: com.mysql.cj.jdbc.Driver
 * Si el driver no está disponible o falla la conexión, debe usarse el fallback de archivo.
 */
public class SqlCreditStorage implements CreditStorage {
    private final AntiAFKPlus plugin;
    private final String jdbcUrl;
    private final String user;
    private final String pass;
    private final String tablePrefix;
    private Connection conn;

    public enum DbType { SQLITE, MYSQL }

    public SqlCreditStorage(AntiAFKPlus plugin, DbType type, String host, int port, String database, String user, String pass, String tablePrefix) throws Exception {
        this.plugin = plugin;
        this.user = user;
        this.pass = pass;
        this.tablePrefix = tablePrefix != null ? tablePrefix : "afkplus_";

        if (type == DbType.SQLITE) {
            // Driver: org.sqlite.JDBC
            try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException e) { throw new Exception("SQLite driver not found"); }
            String file = new java.io.File(plugin.getDataFolder(), "credits.sqlite").getAbsolutePath();
            this.jdbcUrl = "jdbc:sqlite:" + file;
        } else {
            // Driver: com.mysql.cj.jdbc.Driver
            try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new Exception("MySQL driver not found"); }
            this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database);
        }
        connect();
        ensureSchema();
    }

    private void connect() throws SQLException {
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            this.conn = DriverManager.getConnection(jdbcUrl);
        } else {
            this.conn = DriverManager.getConnection(jdbcUrl, user, pass);
        }
        this.conn.setAutoCommit(true);
    }

    private void ensureSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            String credits = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "credits (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "balance INT NOT NULL, " +
                    "last_earned_at BIGINT DEFAULT 0, " +
                    "updated_at BIGINT NOT NULL)";
            st.executeUpdate(credits);

            // Tabla de transacciones (autoincrement según motor)
            final boolean sqlite = jdbcUrl.startsWith("jdbc:sqlite:");
            String idCol = sqlite ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "BIGINT PRIMARY KEY AUTO_INCREMENT";
            String tx = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "credit_tx (" +
                    "id " + idCol + "," +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "type VARCHAR(16) NOT NULL, " +
                    "amount INT NOT NULL, " +
                    "balance_after INT NOT NULL, " +
                    "note VARCHAR(255), " +
                    "ts BIGINT NOT NULL," +
                    "INDEX idx_uuid_ts (uuid, ts)" +
                    ")";
            // SQLite no soporta INDEX dentro de CREATE TABLE de la misma forma; crear índice aparte
            if (sqlite) {
                tx = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "credit_tx (" +
                        "id " + idCol + "," +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "type VARCHAR(16) NOT NULL, " +
                        "amount INT NOT NULL, " +
                        "balance_after INT NOT NULL, " +
                        "note VARCHAR(255), " +
                        "ts BIGINT NOT NULL" +
                        ")";
            }
            st.executeUpdate(tx);
            if (sqlite) {
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_" + tablePrefix + "credit_tx_uuid_ts ON " + tablePrefix + "credit_tx(uuid, ts)");
            }
        }
    }

    @Override
    public Map<UUID, CreditData> loadAll() {
        Map<UUID, CreditData> map = new HashMap<>();
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT uuid,balance,last_earned_at FROM " + tablePrefix + "credits");
            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString(1));
                    long bal = rs.getInt(2);
                    long last = rs.getLong(3);
                    CreditData data = new CreditData(id);
                    data.setBalanceMinutes(bal);
                    if (last > 0) data.setLastEarnedAt(Instant.ofEpochMilli(last));
                    map.put(id, data);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQL loadAll failed: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void saveAll(Map<UUID, CreditData> data) {
        String sql = "INSERT INTO " + tablePrefix + "credits(uuid,balance,last_earned_at,updated_at) VALUES(?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE balance=VALUES(balance), last_earned_at=VALUES(last_earned_at), updated_at=VALUES(updated_at)";
        // SQLite no soporta ON DUPLICATE KEY UPDATE, usamos UPSERT
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            sql = "INSERT INTO " + tablePrefix + "credits(uuid,balance,last_earned_at,updated_at) VALUES(?,?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance, last_earned_at=excluded.last_earned_at, updated_at=excluded.updated_at";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, CreditData> e : data.entrySet()) {
                ps.setString(1, e.getKey().toString());
                ps.setInt(2, (int) e.getValue().getBalanceMinutes());
                ps.setLong(3, e.getValue().getLastEarnedAt() != null ? e.getValue().getLastEarnedAt().toEpochMilli() : 0L);
                ps.setLong(4, now);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQL saveAll failed: " + e.getMessage());
        }
    }

    @Override
    public void saveOne(UUID uuid, CreditData d) {
        String sql = "INSERT INTO " + tablePrefix + "credits(uuid,balance,last_earned_at,updated_at) VALUES(?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE balance=VALUES(balance), last_earned_at=VALUES(last_earned_at), updated_at=VALUES(updated_at)";
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            sql = "INSERT INTO " + tablePrefix + "credits(uuid,balance,last_earned_at,updated_at) VALUES(?,?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance, last_earned_at=excluded.last_earned_at, updated_at=excluded.updated_at";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, (int) d.getBalanceMinutes());
            ps.setLong(3, d.getLastEarnedAt() != null ? d.getLastEarnedAt().toEpochMilli() : 0L);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQL saveOne failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsHistory() { return true; }

    @Override
    public void recordTransaction(UUID uuid, CreditTransactionType type, long amountMinutes, long balanceAfter, String note, long timestampMillis) {
        String sql = "INSERT INTO " + tablePrefix + "credit_tx(uuid,type,amount,balance_after,note,ts) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.setInt(3, (int) amountMinutes);
            ps.setInt(4, (int) balanceAfter);
            ps.setString(5, note);
            ps.setLong(6, timestampMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQL recordTransaction failed: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<CreditTransaction> getHistory(UUID uuid, int limit) {
        java.util.List<CreditTransaction> list = new java.util.ArrayList<>();
        String sql = "SELECT type,amount,balance_after,note,ts FROM " + tablePrefix + "credit_tx WHERE uuid=? ORDER BY ts DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, Math.max(1, limit));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CreditTransactionType type = CreditTransactionType.valueOf(rs.getString(1));
                long amount = rs.getInt(2);
                long bal = rs.getInt(3);
                String note = rs.getString(4);
                long ts = rs.getLong(5);
                list.add(new CreditTransaction(type, amount, bal, java.time.Instant.ofEpochMilli(ts), note));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQL getHistory failed: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignored) {}
    }
}

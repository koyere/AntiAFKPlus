package me.koyere.antiafkplus.credit.storage;

import me.koyere.antiafkplus.credit.CreditData;
import me.koyere.antiafkplus.api.data.CreditTransaction;
import me.koyere.antiafkplus.api.data.CreditTransactionType;

import java.util.Map;
import java.util.UUID;

/**
 * Abstracción de persistencia para el sistema de créditos.
 * Implementaciones: archivo (YAML) y SQL (SQLite/MySQL) opcional.
 */
public interface CreditStorage extends AutoCloseable {
    /** Carga todos los saldos persistidos en memoria. */
    Map<UUID, CreditData> loadAll();

    /** Guarda todos los saldos actuales. Implementación puede ser incremental. */
    void saveAll(Map<UUID, CreditData> data);

    /** Guarda un jugador concreto. */
    void saveOne(UUID uuid, CreditData data);

    /** Indica si el backend soporta historial de transacciones. */
    default boolean supportsHistory() { return false; }

    /** Registra una transacción si el backend lo soporta. */
    default void recordTransaction(UUID uuid, CreditTransactionType type, long amountMinutes, long balanceAfter, String note, long timestampMillis) {}

    /** Devuelve el historial (más reciente primero) si el backend lo soporta. */
    default java.util.List<CreditTransaction> getHistory(UUID uuid, int limit) { return java.util.Collections.emptyList(); }

    /** Cierra recursos. */
    @Override
    void close();
}

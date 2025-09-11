package me.koyere.antiafkplus.api.data;

import java.time.Instant;

/**
 * Representa una transacción de créditos AFK.
 */
public class CreditTransaction {
    private final CreditTransactionType type;
    private final long amountMinutes;      // cantidad positiva (earn/admin_give) o negativa (consume/admin_take)
    private final long balanceAfter;       // saldo resultante después de aplicar la transacción
    private final Instant timestamp;       // momento de la transacción
    private final String note;             // nota opcional

    public CreditTransaction(CreditTransactionType type, long amountMinutes, long balanceAfter, Instant timestamp, String note) {
        this.type = type;
        this.amountMinutes = amountMinutes;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
        this.note = note;
    }

    public CreditTransactionType getType() { return type; }
    public long getAmountMinutes() { return amountMinutes; }
    public long getBalanceAfter() { return balanceAfter; }
    public Instant getTimestamp() { return timestamp; }
    public String getNote() { return note; }
}


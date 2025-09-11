package me.koyere.antiafkplus.api.data;

/**
 * Tipos de transacciones del sistema de créditos AFK.
 */
public enum CreditTransactionType {
    EARN,           // Créditos ganados automáticamente
    CONSUME,        // Consumo por estar AFK (automático)
    SET,            // Asignación directa de saldo (API/administración)
    RESET,          // Reseteo a 0 (administración/decay)
    DECAY,          // Expiración automática
    ADMIN_GIVE,     // Otorgados por admin
    ADMIN_TAKE      // Retirados por admin
}


package me.koyere.antiafkplus.integrations;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.koyere.antiafkplus.AntiAFKPlus;

/**
 * Optional Vault economy integration via reflection.
 * Allows charging players for AFK time or buying credits with in-game currency.
 */
public class VaultIntegration {

    private final AntiAFKPlus plugin;
    private final Logger logger;
    private boolean available = false;
    private Object economy; // net.milkbowl.vault.economy.Economy instance

    public VaultIntegration(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        detectVault();
    }

    private void detectVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp != null) {
                this.economy = rsp.getProvider();
                this.available = true;
                logger.info("Vault economy integration enabled.");
            }
        } catch (ClassNotFoundException e) {
            // Vault not available
        } catch (Exception e) {
            logger.warning("Failed to initialize Vault integration: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available && economy != null;
    }

    /**
     * Gets a player's economy balance.
     */
    public double getBalance(Player player) {
        if (!isAvailable()) return 0.0;
        try {
            Method getBalance = economy.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            return (double) getBalance.invoke(economy, player);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Withdraws money from a player's account.
     * @return true if successful
     */
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable() || amount <= 0) return false;
        try {
            Method withdrawPlayer = economy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = withdrawPlayer.invoke(economy, player, amount);
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            return (boolean) transactionSuccess.invoke(response);
        } catch (Exception e) {
            logger.warning("Vault withdraw failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deposits money into a player's account.
     * @return true if successful
     */
    public boolean deposit(Player player, double amount) {
        if (!isAvailable() || amount <= 0) return false;
        try {
            Method depositPlayer = economy.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = depositPlayer.invoke(economy, player, amount);
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            return (boolean) transactionSuccess.invoke(response);
        } catch (Exception e) {
            logger.warning("Vault deposit failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player has enough money.
     */
    public boolean has(Player player, double amount) {
        if (!isAvailable()) return false;
        try {
            Method has = economy.getClass().getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
            return (boolean) has.invoke(economy, player, amount);
        } catch (Exception e) {
            return false;
        }
    }
}

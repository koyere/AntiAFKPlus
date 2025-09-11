package me.koyere.antiafkplus.command;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.credit.CreditManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /afkback - retorna al jugador desde la zona AFK a su ubicación original.
 */
public class AFKBackCommand implements CommandExecutor {

    private final AntiAFKPlus plugin;
    private final CreditManager creditManager;

    public AFKBackCommand(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.creditManager = plugin.getCreditManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        if (creditManager == null || !creditManager.isEnabled()) {
            player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.errors.system-disabled", "&cCredit system is disabled.")));
            return true;
        }
        if (!player.hasPermission("antiafkplus.credit.return")) {
            player.sendMessage(color(plugin.getConfigManager().getMessage("no-permission", "&cNo permission.")));
            return true;
        }

        CreditManager.ReturnResult result = creditManager.returnFromAFKZone(player);
        switch (result) {
            case SUCCESS:
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.return.success", "&aReturned.")));
                break;
            case TOO_FAR:
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.return.too-far", "&cToo far from AFK zone.")));
                break;
            case UNSAFE_LOCATION:
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.return.unsafe-location", "&cOriginal location unsafe.")));
                break;
            case NOT_IN_ZONE:
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.return.not-in-zone", "&cNot in AFK zone.")));
                break;
            case NO_SAVED_LOCATION:
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.return.no-saved-location", "&cNo saved location.")));
                break;
            case COOLDOWN:
                long cooldown = plugin.getConfig().getLong("credit-system.return-command.cooldown-seconds", 10);
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.return.cooldown", "&cCooldown {seconds}s")
                        .replace("{seconds}", String.valueOf(cooldown))));
                break;
            case SYSTEM_DISABLED:
            default:
                player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.errors.system-disabled", "&cCredit system is disabled.")));
        }
        return true;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}

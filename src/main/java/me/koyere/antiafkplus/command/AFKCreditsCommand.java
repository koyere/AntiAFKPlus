package me.koyere.antiafkplus.command;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.credit.CreditManager;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /afkcredits [reload] - muestra información de créditos o recarga la configuración.
 */
public class AFKCreditsCommand implements CommandExecutor {

    private final AntiAFKPlus plugin;
    private final CreditManager creditManager;

    public AFKCreditsCommand(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.creditManager = plugin.getCreditManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("antiafkplus.reload")) {
                sender.sendMessage(color(plugin.getConfigManager().getMessage("no-permission", "&cNo permission.")));
                return true;
            }
            plugin.getConfigManager().reloadConfiguration();
            sender.sendMessage(color(plugin.getConfigManager().getMessage("config-reloaded", "&aReloaded.")));
            return true;
        }

        // Admin subcommands: history/give/take/set/reset
        if (args.length >= 1 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take") ||
                args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset"))) {
            if (!sender.hasPermission("antiafkplus.credit.admin")) {
                sender.sendMessage(color(plugin.getConfigManager().getMessage("no-permission", "&cNo permission.")));
                return true;
            }

            // history
            if (args[0].equalsIgnoreCase("history")) {
                if (args.length < 2) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.usage.history", "&cUsage: /afkcredits history <player> [limit]")));
                    return true;
                }
                if (plugin.getCreditManager() == null || !plugin.getCreditManager().isHistoryAvailable()) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.history.unavailable", "&eHistory is only available with SQL backend enabled.")));
                    return true;
                }
                var target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("player-not-found", "&cPlayer not found.").replace("{player}", args[1])));
                    return true;
                }
                int limit = 10;
                if (args.length >= 3) {
                    try { limit = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                var history = plugin.getCreditManager().getHistory(target, Math.min(Math.max(limit, 1), 50));
                if (history.isEmpty()) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.history.empty", "&7No credit transactions found.")));
                    return true;
                }
                sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.history.header", "&6=== AFK Credit History ===")
                        .replace("{player}", target.getName())
                        .replace("{count}", String.valueOf(history.size()))));
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
                for (var tx : history) {
                    String time = fmt.format(tx.getTimestamp());
                    String note = tx.getNote() == null ? "" : tx.getNote();
                    String line = plugin.getConfigManager().getMessage("credit-system.history.entry", "&7- {time} | {type} | {delta}m | bal: {balance}m | {note}")
                            .replace("{time}", time)
                            .replace("{type}", tx.getType().name())
                            .replace("{delta}", String.valueOf(tx.getAmountMinutes()))
                            .replace("{balance}", String.valueOf(tx.getBalanceAfter()))
                            .replace("{note}", note);
                    sender.sendMessage(color(line));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("reset")) {
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /afkcredits reset <player>"));
                    return true;
                }
                var target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("player-not-found", "&cPlayer not found.").replace("{player}", args[1])));
                    return true;
                }
                if (plugin.getCreditManager() != null) {
                    plugin.getCreditManager().adminReset(target);
                }
                sender.sendMessage(color("&aCredits reset for &f" + target.getName()));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.usage.modify", "&cUsage: /afkcredits <give|take|set> <player> <minutes>")));
                return true;
            }
            var target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color(plugin.getConfigManager().getMessage("player-not-found", "&cPlayer not found.").replace("{player}", args[1])));
                return true;
            }
            long minutes;
            try { minutes = Long.parseLong(args[2]); } catch (NumberFormatException e) {
                sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.usage.minutes-number", "&cMinutes must be a number")));
                return true;
            }
            if (minutes < 0) minutes = 0;
            var cm = plugin.getCreditManager();
            if (cm == null) {
                sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.errors.system-disabled", "&cCredit system is disabled.")));
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "give":
                    cm.adminGive(target, minutes);
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.admin.gave", "&aGave {minutes}m to {player}")
                            .replace("{minutes}", String.valueOf(minutes))
                            .replace("{player}", target.getName())));
                    break;
                case "take":
                    cm.adminTake(target, minutes);
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.admin.took", "&eTook {minutes}m from {player}")
                            .replace("{minutes}", String.valueOf(minutes))
                            .replace("{player}", target.getName())));
                    break;
                case "set":
                    cm.adminSet(target, minutes);
                    sender.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.admin.set", "&aSet {player} balance to {minutes}m")
                            .replace("{minutes}", String.valueOf(minutes))
                            .replace("{player}", target.getName())));
                    break;
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        if (creditManager == null || !creditManager.isEnabled()) {
            player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.errors.system-disabled", "&cCredit system is disabled.")));
            return true;
        }

        long bal = creditManager.getBalance(player);
        long max = creditManager.getMaxCredits(player);
        String ratio = creditManager.getRatioString(player);
        long hours = bal / 60;
        long rem = bal % 60;

        player.sendMessage(color("&6=== AFK Credits ==="));
        player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.status.balance", "&7AFK Credits: &f{minutes}m &8(&f{hours}h {remaining_minutes}m&8)")
                .replace("{minutes}", String.valueOf(bal))
                .replace("{hours}", String.valueOf(hours))
                .replace("{remaining_minutes}", String.valueOf(rem))));
        player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.status.ratio", "&7Credit Ratio: &f{active}:{credit}")
                .replace("{active}", ratio.split(":" )[0])
                .replace("{credit}", ratio.split(":" )[1])));
        player.sendMessage(color(plugin.getConfigManager().getMessage("credit-system.status.max-credits", "&7Maximum Credits: &f{max}m")
                .replace("{max}", String.valueOf(max))));
        return true;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}

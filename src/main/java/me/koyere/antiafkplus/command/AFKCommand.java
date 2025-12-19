package me.koyere.antiafkplus.command;

import me.koyere.antiafkplus.AntiAFKPlus;
import me.koyere.antiafkplus.config.ConfigManager; // Import ConfigManager
import me.koyere.antiafkplus.afk.AFKManager;      // Import AFKManager
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // Not directly needed if all messages come from ConfigManager
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // For tab completion
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /afk command and its subcommands (e.g., /afk list, /afk status).
 */
public class AFKCommand implements CommandExecutor, TabCompleter { // Implement TabCompleter

    private final AntiAFKPlus plugin;
    private final AFKManager afkManager;
    private final ConfigManager configManager;

    /**
     * Constructor for AFKCommand.
     * @param plugin The main plugin instance.
     */
    public AFKCommand(AntiAFKPlus plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAfkManager(); // Get AFKManager from plugin instance
        this.configManager = plugin.getConfigManager(); // Get ConfigManager from plugin instance
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Subcommand: /afk list
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            String listPermission = configManager.getListCommandPermission();
            if (!sender.hasPermission(listPermission)) {
                sender.sendMessage(configManager.getMessage("no-permission", "&cYou do not have permission to use this command."));
                return true;
            }

            sender.sendMessage(configManager.getMessage("afk-list-header", "&6Currently AFK players:"));
            int afkCount = 0;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (afkManager.isAFK(onlinePlayer)) {
                    sender.sendMessage(configManager.getMessage("afk-list-format", "&e- {player}")
                            .replace("{player}", onlinePlayer.getName()));
                    afkCount++;
                }
            }

            if (afkCount == 0) {
                sender.sendMessage(configManager.getMessage("afk-list-empty", "&7No players are currently AFK."));
            }
            return true;
        }

        // Subcommand: /afk status [player]
        if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
            // Assuming a general permission for status checks. You can make this more specific.
            if (!sender.hasPermission("antiafkplus.status.check")) {
                sender.sendMessage(configManager.getMessage("no-permission", "&cYou do not have permission to use this command."));
                return true;
            }

            Player targetPlayer;
            if (args.length == 1) { // /afk status (checks self)
                if (!(sender instanceof Player)) {
                    sender.sendMessage(configManager.getMessage("player-only-command", "&cConsole must specify a player: /afk status <player>"));
                    return true;
                }
                targetPlayer = (Player) sender;
                boolean isSelfAfk = afkManager.isAFK(targetPlayer);
                String statusText;
                if (isSelfAfk) {
                    if (afkManager.isManuallyAFK(targetPlayer)) {
                        statusText = plugin.getLocalizationManager().getMessage(targetPlayer, "placeholder-status-manual-afk");
                    } else {
                        statusText = plugin.getLocalizationManager().getMessage(targetPlayer, "placeholder-status-auto-afk");
                    }
                } else {
                    statusText = plugin.getLocalizationManager().getMessage(targetPlayer, "placeholder-status-active");
                }
                String statusMessage = configManager.getMessageByFullPath("messages.afk-status-self", "&fYou are currently: {status}")
                        .replace("{status}", statusText);
                sender.sendMessage(statusMessage);

            } else { // /afk status <player_name>
                targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(configManager.getMessage("player-not-found", "&cPlayer '{player}' not found.").replace("{player}", args[1]));
                    return true;
                }
                boolean isTargetAfk = afkManager.isAFK(targetPlayer);
                String statusKey = isTargetAfk ? "afk-status-target-afk" : "afk-status-target-active";
                String defaultMessage = isTargetAfk ? "&c{player} is currently AFK." : "&a{player} is currently active.";
                sender.sendMessage(configManager.getMessage(statusKey, defaultMessage)
                        .replace("{player}", targetPlayer.getName()));
            }
            return true;
        }

        // Main command: /afk (toggle manual AFK)
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessage("player-only-command", "&cThis command can only be used by players."));
                return true;
            }
            Player player = (Player) sender;

            // Permission check for /afk toggle
            if (!player.hasPermission("antiafkplus.afk")) {
                player.sendMessage(configManager.getMessage("no-permission", "&cYou do not have permission to use this command."));
                return true;
            }

            boolean isNowAfk = afkManager.toggleManualAFK(player);

            // AFKManager now handles the broadcast messages if broadcast-afk-state-changes is true.
            // So, we don't need to broadcast from here.
            // We only need to send player-specific feedback, like titles.

            if (isNowAfk) {
                // Player was not AFK, now they are manually AFK.
                // AFKManager's markAsAFK (called by toggleManualAFK) sends the broadcast if configured.
                // Send a title to the player.
                // Consider making title messages configurable.
                player.sendTitle(
                        configManager.getMessage("manual-afk-title-on", "&eYou are now AFK"),
                        configManager.getMessage("manual-afk-subtitle-on", "&7Move to return"),
                        10, 70, 20); // fadeIn, stay, fadeOut ticks
            } else {
                // Player was manually AFK, now they are not.
                // AFKManager's unmarkAsAFK (called by toggleManualAFK) sends the broadcast if configured.
                // Send a title to the player.
                player.sendTitle(
                        configManager.getMessage("manual-afk-title-off", "&aYou are no longer AFK"),
                        configManager.getMessage("manual-afk-subtitle-off", "&7Welcome back!"),
                        10, 70, 20);
            }
            return true;
        }

        // If no subcommands matched and args were provided, it's an unknown subcommand or incorrect usage.
        sender.sendMessage(configManager.getMessage("incorrect-usage", "&cIncorrect usage. Try /afk or /afk <list|status> [player]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> subCommands = List.of("list", "status");

        if (args.length == 1) {
            for (String subCmd : subCommands) {
                if (subCmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    // Check permissions for tab completion
                    if (subCmd.equalsIgnoreCase("list") && sender.hasPermission(configManager.getListCommandPermission())) {
                        completions.add(subCmd);
                    } else if (subCmd.equalsIgnoreCase("status") && sender.hasPermission("antiafkplus.status.check")) {
                        completions.add(subCmd);
                    } else if (!subCmd.equalsIgnoreCase("list") && !subCmd.equalsIgnoreCase("status")) {
                        // For /afk toggle itself, no specific sub-command, but it's an option.
                        // This part is tricky as "" is not a typical sub-command.
                        // Usually, if args[0] is empty, it means the player typed "/afk "
                    }
                }
            }
            // If the sender can use /afk (toggle), and args[0] is empty or matches nothing,
            // this means they might just be typing /afk.
            // For now, stick to completing defined subcommands.
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("status") && sender.hasPermission("antiafkplus.status.check")) {
                // Complete online player names
                String input = args[1].toLowerCase();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                        if (sender instanceof Player && !((Player) sender).canSee(onlinePlayer) && Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket")) {
                            // Basic vanish check example, you might need a more robust solution
                            // or rely on permissions if you don't want to list vanished players.
                            continue;
                        }
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }
        return completions.stream().sorted().collect(Collectors.toList());
    }
}
package online.yudream.minecraft.bukkit.command;

import online.yudream.minecraft.bukkit.YudreamMinecraftPlugin;
import online.yudream.minecraft.bukkit.api.HttpResult;
import online.yudream.minecraft.bukkit.util.PlayerSummaryParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class YudreamCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "status", "sync", "queue", "help");

    private final YudreamMinecraftPlugin plugin;

    public YudreamCommand(YudreamMinecraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase();
        if ("reload".equals(subcommand)) {
            plugin.reloadBridge();
            sender.sendMessage(prefix() + "Config reloaded.");
            return true;
        }
        if ("status".equals(subcommand)) {
            status(sender);
            return true;
        }
        if ("sync".equals(subcommand)) {
            plugin.syncOnlinePlayers();
            sender.sendMessage(prefix() + "Queued join reports for current online players.");
            return true;
        }
        if ("queue".equals(subcommand)) {
            sender.sendMessage(prefix() + "Pending reports: " + plugin.getReportQueue().size());
            return true;
        }
        help(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return new ArrayList<String>();
        }
        String prefix = args[0].toLowerCase();
        List<String> results = new ArrayList<String>();
        for (String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(prefix)) {
                results.add(subcommand);
            }
        }
        return results;
    }

    private void status(final CommandSender sender) {
        if (!plugin.canReport()) {
            sender.sendMessage(prefix() + ChatColor.RED + "Not configured or disabled.");
            return;
        }
        sender.sendMessage(prefix() + "Checking remote players...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                String computedMessage;
                try {
                    HttpResult result = plugin.getApiClient().players(1, 100);
                    if (result.isSuccess()) {
                        PlayerSummaryParser.Summary summary = PlayerSummaryParser.parse(result.getBody());
                        computedMessage = prefix() + "Remote players: total=" + summary.getTotal()
                                + ", online=" + summary.getOnline()
                                + ", afk=" + summary.getAfk()
                                + ", http=" + result.getStatusCode();
                    } else {
                        computedMessage = prefix() + ChatColor.RED + "Remote status failed: HTTP "
                                + result.getStatusCode() + " " + trim(result.getBody());
                    }
                } catch (Exception e) {
                    computedMessage = prefix() + ChatColor.RED + "Remote status failed: " + e.getMessage();
                }
                final String message = computedMessage;
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        sender.sendMessage(message);
                    }
                });
            }
        });
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(prefix() + "/" + label + " reload");
        sender.sendMessage(prefix() + "/" + label + " status");
        sender.sendMessage(prefix() + "/" + label + " sync");
        sender.sendMessage(prefix() + "/" + label + " queue");
    }

    private static String prefix() {
        return ChatColor.AQUA + "[YuDream] " + ChatColor.RESET;
    }

    private static String trim(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 160 ? body : body.substring(0, 160) + "...";
    }
}

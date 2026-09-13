package online.yudream.minecraft.bukkit;

import online.yudream.minecraft.bukkit.afk.AfkTracker;
import online.yudream.minecraft.bukkit.api.YudreamApiClient;
import online.yudream.minecraft.bukkit.command.YudreamCommand;
import online.yudream.minecraft.bukkit.compat.PaperChatCompatibility;
import online.yudream.minecraft.bukkit.compat.ServerCompatibility;
import online.yudream.minecraft.bukkit.config.YudreamConfig;
import online.yudream.minecraft.bukkit.event.PlayerActivityListener;
import online.yudream.minecraft.bukkit.report.PlayerEventType;
import online.yudream.minecraft.bukkit.report.PlayerEventPayload;
import online.yudream.minecraft.bukkit.report.ReportQueue;
import online.yudream.minecraft.bukkit.util.PlayerSnapshots;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class YudreamMinecraftPlugin extends JavaPlugin {

    private YudreamConfig settings;
    private YudreamApiClient apiClient;
    private ReportQueue reportQueue;
    private AfkTracker afkTracker;
    private int snapshotTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info(ServerCompatibility.describe());
        reloadBridge();

        getServer().getPluginManager().registerEvents(new PlayerActivityListener(this), this);
        if (PaperChatCompatibility.register(this)) {
            getLogger().info("Paper AsyncChatEvent compatibility is active.");
        }
        PluginCommand command = getCommand("yudreammc");
        if (command != null) {
            YudreamCommand executor = new YudreamCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        if (settings.isSyncOnlineOnEnable()) {
            syncOnlinePlayers();
        }
        snapshotTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(
                this, this::reportPlayerSnapshot, 20L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        if (snapshotTaskId >= 0) {
            getServer().getScheduler().cancelTask(snapshotTaskId);
            snapshotTaskId = -1;
        }
        if (afkTracker != null) {
            afkTracker.stop();
        }
        if (settings != null && settings.isReportQuitOnDisable() && reportQueue != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                reportQueue.submit(PlayerEventType.QUIT, PlayerSnapshots.from(player));
            }
        }
        if (reportQueue != null && canReport()) {
            reportQueue.submitSnapshot(Collections.<PlayerEventPayload>emptyList(), System.currentTimeMillis());
        }
        if (reportQueue != null) {
            reportQueue.shutdown(settings == null ? 5000L : settings.getFlushTimeoutMs());
        }
    }

    public synchronized void reloadBridge() {
        reloadConfig();
        settings = YudreamConfig.load(this);
        apiClient = new YudreamApiClient(settings);

        if (reportQueue != null) {
            reportQueue.shutdown(settings.getFlushTimeoutMs());
        }
        reportQueue = new ReportQueue(this, apiClient, settings);

        if (afkTracker != null) {
            afkTracker.stop();
        }
        afkTracker = new AfkTracker(this, reportQueue, settings);
        afkTracker.start();

        if (!settings.isConfigured()) {
            getLogger().warning("YuDream bridge is not fully configured. Set base-url, server-id and api-key in config.yml.");
        }
        if (!settings.isEnabled()) {
            getLogger().warning("YuDream bridge is disabled by config.");
        }
    }

    public void syncOnlinePlayers() {
        if (!canReport()) {
            return;
        }
        for (Player player : getServer().getOnlinePlayers()) {
            reportQueue.submit(PlayerEventType.JOIN, PlayerSnapshots.from(player));
            afkTracker.markOnline(player);
        }
        reportPlayerSnapshot();
    }

    private void reportPlayerSnapshot() {
        if (!canReport() || reportQueue == null) {
            return;
        }
        long observedAt = System.currentTimeMillis();
        List<PlayerEventPayload> players = new ArrayList<PlayerEventPayload>();
        for (Player player : getServer().getOnlinePlayers()) {
            players.add(PlayerSnapshots.from(player, observedAt));
        }
        reportQueue.submitSnapshot(players, observedAt);
    }

    public boolean canReport() {
        return settings != null && settings.isEnabled() && settings.isConfigured();
    }

    public YudreamConfig getSettings() {
        return settings;
    }

    public YudreamApiClient getApiClient() {
        return apiClient;
    }

    public ReportQueue getReportQueue() {
        return reportQueue;
    }

    public AfkTracker getAfkTracker() {
        return afkTracker;
    }
}

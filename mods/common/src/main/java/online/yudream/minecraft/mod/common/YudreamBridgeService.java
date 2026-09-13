package online.yudream.minecraft.mod.common;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class YudreamBridgeService {

    private static final long SNAPSHOT_INTERVAL_MS = 60_000L;

    private final Path configPath;
    private final LogSink logger;
    private final Map<UUID, PlayerActivityState> states = new ConcurrentHashMap<>();
    private BridgeConfig config;
    private YudreamApiClient apiClient;
    private ReportQueue reportQueue;
    private long lastAfkCheckAt;
    private long lastSnapshotAt;

    public YudreamBridgeService(Path configPath, LogSink logger) {
        this.configPath = configPath;
        this.logger = logger;
    }

    public synchronized void start() {
        reload();
    }

    public synchronized void reload() {
        if (reportQueue != null) {
            reportQueue.shutdown(currentFlushTimeout());
        }
        config = BridgeConfigLoader.load(configPath, logger);
        apiClient = new YudreamApiClient(config);
        reportQueue = new ReportQueue(logger, apiClient, config);
        lastAfkCheckAt = 0L;
        lastSnapshotAt = 0L;
        if (!config.isConfigured()) {
            logger.warn("YuDream bridge is not fully configured. Edit " + configPath + " and set base-url, server-id and api-key.");
        }
        if (!config.isEnabled()) {
            logger.warn("YuDream bridge is disabled by config.");
        }
    }

    public synchronized void stop(Collection<PlayerIdentity> onlinePlayers) {
        if (canReport()) {
            reportQueue.submitSnapshot(Collections.<PlayerEventPayload>emptyList(), System.currentTimeMillis());
        }
        if (config != null && config.isReportQuitOnStop() && reportQueue != null) {
            for (PlayerIdentity player : onlinePlayers) {
                reportQueue.submit(PlayerEventType.QUIT, player.payload(System.currentTimeMillis()));
            }
        }
        states.clear();
        if (reportQueue != null) {
            reportQueue.shutdown(currentFlushTimeout());
            reportQueue = null;
        }
    }

    public void syncOnline(Collection<PlayerIdentity> onlinePlayers) {
        if (!canReport()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (PlayerIdentity player : onlinePlayers) {
            states.put(player.getUuid(), new PlayerActivityState(now, false));
            reportQueue.submit(PlayerEventType.JOIN, player.payload(now));
        }
        reportSnapshot(onlinePlayers, now);
    }

    public void playerJoined(PlayerIdentity player) {
        if (!canReport()) {
            return;
        }
        long now = System.currentTimeMillis();
        states.put(player.getUuid(), new PlayerActivityState(now, false));
        reportQueue.submit(PlayerEventType.JOIN, player.payload(now));
    }

    public void playerQuit(PlayerIdentity player) {
        if (!canReport()) {
            return;
        }
        reportQueue.submit(PlayerEventType.QUIT, player.payload(System.currentTimeMillis()));
        states.remove(player.getUuid());
    }

    public void markActive(PlayerIdentity player) {
        if (!canReport() || !config.isAfkEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        PlayerActivityState state = states.get(player.getUuid());
        if (state == null) {
            states.put(player.getUuid(), new PlayerActivityState(now, false));
            return;
        }
        if (state.isAfk()) {
            reportQueue.submit(PlayerEventType.AFK_END, player.payload(now));
        }
        states.put(player.getUuid(), new PlayerActivityState(now, false));
    }

    public void tick(Collection<PlayerIdentity> onlinePlayers) {
        if (!canReport()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) {
            reportSnapshot(onlinePlayers, now);
        }
        if (!config.isAfkEnabled()) {
            return;
        }
        if (now - lastAfkCheckAt < config.getAfkCheckIntervalMs()) {
            return;
        }
        lastAfkCheckAt = now;
        for (PlayerIdentity player : onlinePlayers) {
            PlayerActivityState state = states.get(player.getUuid());
            if (state == null) {
                states.put(player.getUuid(), new PlayerActivityState(now, false));
                continue;
            }
            if (!state.isAfk() && now - state.getLastActiveAt() >= config.getAfkTimeoutMs()) {
                states.put(player.getUuid(), new PlayerActivityState(state.getLastActiveAt(), true));
                reportQueue.submit(PlayerEventType.AFK_START, player.payload(now));
            }
        }
    }

    private void reportSnapshot(Collection<PlayerIdentity> onlinePlayers, long observedAt) {
        java.util.List<PlayerEventPayload> players = onlinePlayers.stream()
                .map(player -> player.payload(observedAt))
                .collect(java.util.stream.Collectors.toList());
        reportQueue.submitSnapshot(players, observedAt);
        lastSnapshotAt = observedAt;
    }

    public void statusAsync(Consumer<String> callback) {
        BridgeConfig snapshot = config;
        YudreamApiClient client = apiClient;
        if (snapshot == null || !snapshot.isEnabled() || !snapshot.isConfigured() || client == null) {
            callback.accept("YuDream bridge is not configured or disabled.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                HttpResult result = client.players(1, 100);
                if (result.isSuccess()) {
                    PlayerSummaryParser.Summary summary = PlayerSummaryParser.parse(result.getBody());
                    callback.accept("Remote players: total=" + summary.getTotal()
                            + ", online=" + summary.getOnline()
                            + ", afk=" + summary.getAfk()
                            + ", http=" + result.getStatusCode());
                } else {
                    callback.accept("Remote status failed: HTTP " + result.getStatusCode() + " " + trim(result.getBody()));
                }
            } catch (Exception e) {
                callback.accept("Remote status failed: " + e.getMessage());
            }
        });
    }

    public int queueSize() {
        return reportQueue == null ? 0 : reportQueue.size();
    }

    public BridgeConfig getConfig() {
        return config == null ? BridgeConfig.defaults() : config;
    }

    public boolean shouldSyncOnlineOnStart() {
        return config != null && config.isSyncOnlineOnStart();
    }

    private boolean canReport() {
        return config != null && config.isEnabled() && config.isConfigured() && reportQueue != null;
    }

    private long currentFlushTimeout() {
        return config == null ? 5000L : config.getFlushTimeoutMs();
    }

    private static String trim(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 160 ? body : body.substring(0, 160) + "...";
    }
}

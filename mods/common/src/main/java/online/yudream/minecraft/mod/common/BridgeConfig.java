package online.yudream.minecraft.mod.common;

public final class BridgeConfig {

    private final boolean enabled;
    private final String baseUrl;
    private final String serverId;
    private final String apiKey;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int retryAttempts;
    private final long retryDelayMs;
    private final int queueCapacity;
    private final boolean logQueued;
    private final boolean logAttempts;
    private final boolean logSuccess;
    private final boolean logFailures;
    private final boolean logPayload;
    private final boolean afkEnabled;
    private final long afkTimeoutMs;
    private final long afkCheckIntervalMs;
    private final boolean syncOnlineOnStart;
    private final boolean reportQuitOnStop;
    private final long flushTimeoutMs;

    public BridgeConfig(boolean enabled,
                        String baseUrl,
                        String serverId,
                        String apiKey,
                        int connectTimeoutMs,
                        int readTimeoutMs,
                        int retryAttempts,
                        long retryDelayMs,
                        int queueCapacity,
                        boolean logQueued,
                        boolean logAttempts,
                        boolean logSuccess,
                        boolean logFailures,
                        boolean logPayload,
                        boolean afkEnabled,
                        long afkTimeoutMs,
                        long afkCheckIntervalMs,
                        boolean syncOnlineOnStart,
                        boolean reportQuitOnStop,
                        long flushTimeoutMs) {
        this.enabled = enabled;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.serverId = trim(serverId);
        this.apiKey = trim(apiKey);
        this.connectTimeoutMs = positive(connectTimeoutMs, 5000);
        this.readTimeoutMs = positive(readTimeoutMs, 8000);
        this.retryAttempts = Math.max(retryAttempts, 1);
        this.retryDelayMs = positive(retryDelayMs, 1500L);
        this.queueCapacity = Math.max(queueCapacity, 1);
        this.logQueued = logQueued;
        this.logAttempts = logAttempts;
        this.logSuccess = logSuccess;
        this.logFailures = logFailures;
        this.logPayload = logPayload;
        this.afkEnabled = afkEnabled;
        this.afkTimeoutMs = positive(afkTimeoutMs, 300_000L);
        this.afkCheckIntervalMs = positive(afkCheckIntervalMs, 30_000L);
        this.syncOnlineOnStart = syncOnlineOnStart;
        this.reportQuitOnStop = reportQuitOnStop;
        this.flushTimeoutMs = positive(flushTimeoutMs, 5000L);
    }

    public static BridgeConfig defaults() {
        return new BridgeConfig(true, "http://127.0.0.1:8080", "", "",
                5000, 8000, 3, 1500L, 1000,
                false, false, true, true, false,
                true, 300_000L, 30_000L,
                true, false, 5000L);
    }

    public boolean isConfigured() {
        return !baseUrl.isEmpty() && !serverId.isEmpty() && !apiKey.isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getServerId() {
        return serverId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public boolean isLogQueued() {
        return logQueued;
    }

    public boolean isLogAttempts() {
        return logAttempts;
    }

    public boolean isLogSuccess() {
        return logSuccess;
    }

    public boolean isLogFailures() {
        return logFailures;
    }

    public boolean isLogPayload() {
        return logPayload;
    }

    public boolean isAfkEnabled() {
        return afkEnabled;
    }

    public long getAfkTimeoutMs() {
        return afkTimeoutMs;
    }

    public long getAfkCheckIntervalMs() {
        return afkCheckIntervalMs;
    }

    public boolean isSyncOnlineOnStart() {
        return syncOnlineOnStart;
    }

    public boolean isReportQuitOnStop() {
        return reportQuitOnStop;
    }

    public long getFlushTimeoutMs() {
        return flushTimeoutMs;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        String result = trim(value);
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static long positive(long value, long fallback) {
        return value > 0 ? value : fallback;
    }
}

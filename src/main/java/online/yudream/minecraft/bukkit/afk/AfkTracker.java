package online.yudream.minecraft.bukkit.afk;

import online.yudream.minecraft.bukkit.config.YudreamConfig;
import online.yudream.minecraft.bukkit.report.PlayerEventType;
import online.yudream.minecraft.bukkit.report.ReportQueue;
import online.yudream.minecraft.bukkit.util.PlayerSnapshots;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AfkTracker {

    private final JavaPlugin plugin;
    private final ReportQueue reportQueue;
    private final YudreamConfig config;
    private final Map<UUID, State> states = new ConcurrentHashMap<UUID, State>();
    private BukkitTask task;

    public AfkTracker(JavaPlugin plugin, ReportQueue reportQueue, YudreamConfig config) {
        this.plugin = plugin;
        this.reportQueue = reportQueue;
        this.config = config;
    }

    public void start() {
        if (!config.isAfkEnabled()) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                checkAfk();
            }
        }, config.getAfkCheckIntervalTicks(), config.getAfkCheckIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        states.clear();
    }

    public void markOnline(Player player) {
        states.put(player.getUniqueId(), new State(System.currentTimeMillis(), false));
    }

    public void markOffline(Player player) {
        states.remove(player.getUniqueId());
    }

    public void markActive(Player player) {
        if (!config.isAfkEnabled()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        State state = states.get(uuid);
        if (state == null) {
            states.put(uuid, new State(now, false));
            return;
        }
        if (state.afk) {
            reportQueue.submit(PlayerEventType.AFK_END, PlayerSnapshots.from(player, now));
        }
        states.put(uuid, new State(now, false));
    }

    public boolean isAfk(Player player) {
        State state = states.get(player.getUniqueId());
        return state != null && state.afk;
    }

    private void checkAfk() {
        if (!config.isAfkEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            State state = states.get(player.getUniqueId());
            if (state == null) {
                states.put(player.getUniqueId(), new State(now, false));
                continue;
            }
            if (!state.afk && now - state.lastActiveAt >= config.getAfkTimeoutMs()) {
                states.put(player.getUniqueId(), new State(state.lastActiveAt, true));
                reportQueue.submit(PlayerEventType.AFK_START, PlayerSnapshots.from(player, now));
            }
        }
    }

    private static final class State {
        private final long lastActiveAt;
        private final boolean afk;

        private State(long lastActiveAt, boolean afk) {
            this.lastActiveAt = lastActiveAt;
            this.afk = afk;
        }
    }
}

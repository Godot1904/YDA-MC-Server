package online.yudream.minecraft.bukkit.event;

import online.yudream.minecraft.bukkit.YudreamMinecraftPlugin;
import online.yudream.minecraft.bukkit.afk.AfkTracker;
import online.yudream.minecraft.bukkit.report.PlayerEventType;
import online.yudream.minecraft.bukkit.util.PlayerSnapshots;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public final class PlayerActivityListener implements Listener {

    private final YudreamMinecraftPlugin plugin;

    public PlayerActivityListener(YudreamMinecraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.canReport()) {
            return;
        }
        plugin.getAfkTracker().markOnline(event.getPlayer());
        plugin.getReportQueue().submit(PlayerEventType.JOIN, PlayerSnapshots.from(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.canReport()) {
            return;
        }
        plugin.getReportQueue().submit(PlayerEventType.QUIT, PlayerSnapshots.from(event.getPlayer()));
        plugin.getAfkTracker().markOffline(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getSettings().isResetOnMove() && movedBlock(event.getFrom(), event.getTo())) {
            active(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
        if (!plugin.getSettings().isResetOnChat()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                active(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.getSettings().isResetOnCommand()) {
            active(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getSettings().isResetOnInteract()) {
            active(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        active(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        active(event.getPlayer());
    }

    private void active(Player player) {
        if (!plugin.canReport()) {
            return;
        }
        AfkTracker tracker = plugin.getAfkTracker();
        if (tracker != null) {
            tracker.markActive(player);
        }
    }

    private static boolean movedBlock(Location from, Location to) {
        return to != null
                && (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ());
    }
}

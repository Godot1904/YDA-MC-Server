package online.yudream.minecraft.bukkit.compat;

import online.yudream.minecraft.bukkit.YudreamMinecraftPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.logging.Level;

public final class PaperChatCompatibility {

    private static final String PAPER_ASYNC_CHAT_EVENT = "io.papermc.paper.event.player.AsyncChatEvent";

    private PaperChatCompatibility() {
    }

    public static boolean register(final YudreamMinecraftPlugin plugin) {
        final Class<? extends Event> eventClass;
        try {
            eventClass = Class.forName(PAPER_ASYNC_CHAT_EVENT).asSubclass(Event.class);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Could not inspect Paper chat event compatibility", t);
            return false;
        }

        Listener listener = new Listener() {
        };
        EventExecutor executor = new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                if (!plugin.canReport() || !plugin.getSettings().isResetOnChat()) {
                    return;
                }
                final Player player = player(event);
                if (player == null) {
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (player.isOnline() && plugin.getAfkTracker() != null) {
                            plugin.getAfkTracker().markActive(player);
                        }
                    }
                });
            }
        };
        plugin.getServer().getPluginManager().registerEvent(eventClass, listener, EventPriority.MONITOR, executor, plugin, true);
        return true;
    }

    private static Player player(Event event) {
        try {
            Method method = event.getClass().getMethod("getPlayer");
            Object player = method.invoke(event);
            return player instanceof Player ? (Player) player : null;
        } catch (Exception e) {
            return null;
        }
    }
}

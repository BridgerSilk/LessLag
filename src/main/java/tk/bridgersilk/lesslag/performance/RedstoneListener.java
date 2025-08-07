package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.Plugin;

public class RedstoneListener implements Listener {

    private final boolean clocksOnly;
    private final double tpsThreshold;

    public RedstoneListener(Plugin plugin, boolean clocksOnly, double tpsThreshold) {
        this.clocksOnly = clocksOnly;
        this.tpsThreshold = tpsThreshold;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        if (TPSUtil.getTPS() >= tpsThreshold) return;

        if (clocksOnly) {
            if (Math.abs(event.getOldCurrent() - event.getNewCurrent()) > 0) {
                event.setNewCurrent(0);
            }
        } else {
            event.setNewCurrent(0);
        }
    }

    public void unregister() {
        BlockRedstoneEvent.getHandlerList().unregister(this);
    }
}

package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.plugin.Plugin;

public class FluidListener implements Listener {

	private final Plugin plugin;
	private final FileConfiguration config;
    private final double tpsThreshold;

	public FluidListener(Plugin plugin, double tpsThreshold) {
		this.plugin = plugin;
        this.tpsThreshold = tpsThreshold;
		this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onFluidFlow(BlockFromToEvent event) {
		if (!config.getBoolean("performance_controls.disable_fluids.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		Block block = event.getBlock();
		String typeName = block.getType().name();
		if (typeName.contains("WATER") || typeName.contains("LAVA")) {
			event.setCancelled(true);
		}
	}

    public void unregister() {
        BlockFromToEvent.getHandlerList().unregister(this);
    }
}

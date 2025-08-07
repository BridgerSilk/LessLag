package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.plugin.Plugin;

public class ExplosionListener implements Listener {

	private final Plugin plugin;
	private final FileConfiguration config;
    private final double tpsThreshold;

	public ExplosionListener(Plugin plugin, double tpsThreshold) {
		this.plugin = plugin;
        this.tpsThreshold = tpsThreshold;
		this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (!config.getBoolean("performance_controls.disable_explosions.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		event.setCancelled(true);
	}

    public void unregister() {
        ExplosionPrimeEvent.getHandlerList().unregister(this);
    }
}

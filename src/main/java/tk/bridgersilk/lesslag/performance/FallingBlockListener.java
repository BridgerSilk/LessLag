package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;

public class FallingBlockListener implements Listener {

	private final Plugin plugin;
	private final FileConfiguration config;
    private final double tpsThreshold;

	public FallingBlockListener(Plugin plugin, double tpsThreshold) {
		this.plugin = plugin;
        this.tpsThreshold = tpsThreshold;
		this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (!config.getBoolean("performance_controls.disable_falling_blocks.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		Block block = event.getBlock();
		Material type = block.getType();
		if (type == Material.SAND || type == Material.GRAVEL || type.name().endsWith("CONCRETE_POWDER")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent event) {
		if (!config.getBoolean("performance_controls.disable_falling_blocks.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		if (event.getEntityType() == EntityType.FALLING_BLOCK) {
			event.setCancelled(true);
		}
	}

    public void unregister() {
        BlockPhysicsEvent.getHandlerList().unregister(this);
        EntitySpawnEvent.getHandlerList().unregister(this);
    }
}

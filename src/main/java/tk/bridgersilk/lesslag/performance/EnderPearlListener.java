package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class EnderPearlListener implements Listener {

	private final Plugin plugin;
	private final FileConfiguration config;
    private final double tpsThreshold;

	public EnderPearlListener(Plugin plugin, double tpsThreshold) {
		this.plugin = plugin;
        this.tpsThreshold = tpsThreshold;
		this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPearlLaunch(ProjectileLaunchEvent event) {
		if (!config.getBoolean("performance_controls.disable_ender_pearls.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		if (event.getEntity() instanceof EnderPearl) {
			event.getEntity().remove();
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPearlUse(PlayerInteractEvent event) {
        String prefix = Bukkit.getPluginManager().getPlugin("LessLag").getConfig().getString("settings.prefix");

		if (!config.getBoolean("performance_controls.disable_ender_pearls.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() == Material.ENDER_PEARL) {
			event.setCancelled(true);
			player.sendMessage(prefix + "§cEnder pearls are disabled due to low TPS.");
		}
	}

    public void unregister() {
        PlayerInteractEvent.getHandlerList().unregister(this);
        ProjectileLaunchEvent.getHandlerList().unregister(this);
    }
}

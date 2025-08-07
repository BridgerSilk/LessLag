package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

public class CommandBlockListener implements Listener {

	private final Plugin plugin;
	private final FileConfiguration config;
    private final double tpsThreshold;

	public CommandBlockListener(Plugin plugin, double tpsThreshold) {
		this.plugin = plugin;
        this.tpsThreshold = tpsThreshold;
		this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onCommandBlockExecute(ServerCommandEvent event) {
		if (!config.getBoolean("performance_controls.disable_command_blocks.enabled")) return;

		double tps = Bukkit.getServer().getTPS()[0];
		if (tps > tpsThreshold) return;

		if (event.getSender() instanceof BlockCommandSender) {
			event.setCancelled(true);
		}
	}

    public void unregister() {
        ServerCommandEvent.getHandlerList().unregister(this);
    }
}

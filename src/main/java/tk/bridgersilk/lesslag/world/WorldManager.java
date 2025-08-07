package tk.bridgersilk.lesslag.world;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import tk.bridgersilk.lesslag.LessLag;

public class WorldManager {

	private final LessLag plugin;

	private final Map<String, Long> worldActivity = new ConcurrentHashMap<>();

	public WorldManager(LessLag plugin) {
		this.plugin = plugin;

		for (World world : Bukkit.getWorlds()) {
			worldActivity.put(world.getName(), System.currentTimeMillis());
		}

		startAutoUnloadTask();
		startLowTpsUnloadTask();
		startActivityTracker();
	}

	private void startActivityTracker() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			for (World world : Bukkit.getWorlds()) {
				if (!world.getPlayers().isEmpty()) {
					worldActivity.put(world.getName(), System.currentTimeMillis());
				}
			}
		}, 20L, 20L);
	}

	private void startAutoUnloadTask() {
		if (!plugin.getConfig().getBoolean("world_management.auto_unload.enabled")) return;

		long checkInterval = plugin.getConfig().getLong("settings.tps_check_interval", 20L);

		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			int inactivityMinutes = plugin.getConfig().getInt("world_management.auto_unload.inactivity_minutes", 5);
			long inactivityThreshold = inactivityMinutes * 60_000L;

			List<String> excluded = plugin.getConfig().getStringList("world_management.auto_unload.excluded_worlds");

			for (World world : Bukkit.getWorlds()) {
				if (excluded.contains(world.getName())) continue;

				Long lastActive = worldActivity.getOrDefault(world.getName(), System.currentTimeMillis());
				if (System.currentTimeMillis() - lastActive >= inactivityThreshold && world.getPlayers().isEmpty()) {
					unloadWorld(world, "auto-unload (inactive)");
				}
			}
		}, checkInterval, checkInterval);
	}

	private void startLowTpsUnloadTask() {
		if (!plugin.getConfig().getBoolean("world_management.force_unload_on_low_tps.enabled")) return;

		long checkInterval = plugin.getConfig().getLong("settings.tps_check_interval", 20L);
        String prefix = plugin.getConfig().getString("settings.prefix");

		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			double criticalTps = plugin.getConfig().getDouble("settings.critical_tps_threshold", 10.0);
			double currentTps = Bukkit.getServer().getTPS()[0];

			if (currentTps < criticalTps) {
				String transferWorldName = plugin.getConfig().getString("world_management.force_unload_on_low_tps.player_transfer_world", "world");
				World transferWorld = Bukkit.getWorld(transferWorldName);

				if (transferWorld == null) {
					plugin.getLogger().warning("Transfer world '" + transferWorldName + "' not found. Skipping unload.");
					return;
				}

				for (World world : Bukkit.getWorlds()) {
					if (world.getName().equalsIgnoreCase(transferWorldName)) continue;

					for (Player p : world.getPlayers()) {
						p.teleport(transferWorld.getSpawnLocation());
						p.sendMessage(prefix + "§cServer lag detected. You have been moved to §b" + transferWorld.getName());
					}

					unloadWorld(world, "low TPS emergency");
				}
			}
		}, checkInterval, checkInterval);
	}

	private void unloadWorld(World world, String reason) {
		int entities = world.getEntities().size();
		int chunks = world.getLoadedChunks().length;
		int players = world.getPlayers().size();
		double sizeMb = getWorldSizeInMb(world);

        String prefix = plugin.getConfig().getString("settings.prefix");

		String message = String.format("§b%s §7| Entities: §f%d §7| Chunks: §f%d §7| Players: §f%d §7| Size: §f%.2f MB",
				world.getName(), entities, chunks, players, sizeMb);

        boolean saveWorld = plugin.getConfig().getBoolean("world_management.auto_unload.save_world");
		boolean success = Bukkit.unloadWorld(world, saveWorld);
		if (success) {
			for (Player admin : Bukkit.getOnlinePlayers()) {
				if (admin.hasPermission("lesslag.admin")) {
					admin.sendMessage(prefix + "§eUnloaded world: " + message + " §7(Reason: " + reason + ")");
				}
			}
		}
	}

	private double getWorldSizeInMb(World world) {
		File folder = world.getWorldFolder();
		return bytesToMb(getFolderSize(folder));
	}

	private long getFolderSize(File folder) {
		if (!folder.exists()) return 0L;
		if (folder.isFile()) return folder.length();

		long length = 0;
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				length += getFolderSize(file);
			}
		}
		return length;
	}

	private double bytesToMb(long bytes) {
		return bytes / 1024.0 / 1024.0;
	}

	public void disable() {
		Bukkit.getScheduler().cancelTasks(plugin);
		worldActivity.clear();
	}
}

package tk.bridgersilk.lesslag.entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class EntityManager {

	private final Plugin plugin;
	private BukkitTask chunkAndWorldCheckTask;
	private BukkitTask smartRemovalTask;

	private int maxEntitiesPerChunk;
	private int maxEntitiesPerWorld;
	private boolean disableNaturalSpawnOnLimit;
	private boolean killExcessEntities;
	private boolean smartRemovalEnabled;
	private List<String> smartRemovalWhitelist;

	public EntityManager(Plugin plugin) {
		this.plugin = plugin;
		loadConfigValues();
		startTasks();
	}

	public void loadConfigValues() {
		FileConfiguration config = plugin.getConfig();
		this.maxEntitiesPerChunk = config.getInt("entity_management.max_entities_per_chunk", 50);
		this.maxEntitiesPerWorld = config.getInt("entity_management.max_entities_per_world", 5000);
		this.disableNaturalSpawnOnLimit = config.getBoolean("entity_management.disable_natural_spawn_on_limit", true);
		this.killExcessEntities = config.getBoolean("entity_management.kill_excess_entities", true);

		this.smartRemovalEnabled = config.getBoolean("entity_management.smart_entity_removal.enabled", true);
		this.smartRemovalWhitelist = config.getStringList("entity_management.smart_entity_removal.whitelist");
	}

	private void startTasks() {
		chunkAndWorldCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkEntities, 0L, 200L);

		smartRemovalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::smartEntityRemoval, 0L, 6000L);
	}

	public void stopTasks() {
		if (chunkAndWorldCheckTask != null) chunkAndWorldCheckTask.cancel();
		if (smartRemovalTask != null) smartRemovalTask.cancel();
	}

	public void reload() {
		loadConfigValues();
		stopTasks();
		startTasks();
	}

	private void checkEntities() {
		for (World world : Bukkit.getWorlds()) {
			int totalEntities = getNonPlayerEntities(world);

			if (totalEntities >= maxEntitiesPerWorld) {
				if (disableNaturalSpawnOnLimit) {
					world.setSpawnFlags(false, false);
				}
				if (killExcessEntities) {
					killExcessEntitiesInWorld(world, totalEntities - maxEntitiesPerWorld);
				}
			} else {
				world.setSpawnFlags(true, true);
			}

			for (Chunk chunk : world.getLoadedChunks()) {
				int entityCount = getNonPlayerEntities(chunk);
				if (entityCount >= maxEntitiesPerChunk) {
					if (disableNaturalSpawnOnLimit) {
						// todo
					}
					if (killExcessEntities) {
						killExcessEntitiesInChunk(chunk, entityCount - maxEntitiesPerChunk);
					}
				}
			}
		}
	}

	private void smartEntityRemoval() {
		if (!smartRemovalEnabled) return;

		Map<String, AtomicInteger> entityCountMap = new HashMap<>();

        String prefix = plugin.getConfig().getString("settings.prefix");

		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (entity instanceof Player) continue;
				String type = entity.getType().name();
				if (smartRemovalWhitelist.contains(type)) continue;

				entityCountMap.computeIfAbsent(type, k -> new AtomicInteger()).incrementAndGet();
			}
		}

		String mostFrequent = entityCountMap.entrySet().stream()
				.max(Comparator.comparingInt(e -> e.getValue().get()))
				.map(Map.Entry::getKey)
				.orElse(null);

		if (mostFrequent != null) {
            int removed = 0;
			for (World world : Bukkit.getWorlds()) {
				for (Entity entity : world.getEntitiesByClass(Entity.class)) {
					if (entity.getType().name().equals(mostFrequent) && !(entity instanceof Player)) {
						entity.remove();
                        removed++;
					}
				}
			}
			Bukkit.getLogger().info(prefix + "Smart entity removal executed: Removed all " + mostFrequent + " entities.");
            if (removed > 0) {
				notifyAdmins("§eSmart removal: §cRemoved §b" + removed + " §centities of type §b" + mostFrequent + " §cacross all worlds.");
			}
		}
	}

	/* -------------------------- Helper Methods -------------------------- */

	private int getNonPlayerEntities(World world) {
		int count = 0;
		for (Entity entity : world.getEntities()) {
			if (!(entity instanceof Player)) count++;
		}
		return count;
	}

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isOverEntityLimit(Chunk chunk, World world) {
        int chunkEntities = getNonPlayerEntities(chunk);
        int worldEntities = getNonPlayerEntities(world);
        return chunkEntities >= maxEntitiesPerChunk || worldEntities >= maxEntitiesPerWorld;
    }

	private int getNonPlayerEntities(Chunk chunk) {
		int count = 0;
		for (Entity entity : chunk.getEntities()) {
			if (!(entity instanceof Player)) count++;
		}
		return count;
	}

	private void killExcessEntitiesInWorld(World world, int excess) {
		if (excess <= 0) return;
		int removed = 0;
		String lastType = "";

		for (Entity entity : world.getEntities()) {
			if (removed >= excess) break;
			if (entity instanceof Player) continue;
            
            lastType = entity.getType().name();
			entity.remove();
			removed++;
		}

        if (removed > 0) {
			notifyAdmins("§eWorld cap removal: §cRemoved §b" + removed + " §centities of type §b" + lastType +
					" §cin world §b" + world.getName() + "§c.");
		}
	}

	private void killExcessEntitiesInChunk(Chunk chunk, int excess) {
		if (excess <= 0) return;
		int removed = 0;
        String lastType = "";

		for (Entity entity : chunk.getEntities()) {
			if (removed >= excess) break;
			if (entity instanceof Player) continue;

            lastType = entity.getType().name();
			entity.remove();
			removed++;
		}

        if (removed > 0) {
			Location loc = chunk.getBlock(0, 0, 0).getLocation();
			notifyAdmins("§eChunk cap removal: §cRemoved §b" + removed + " §centities of type §b" + lastType +
					" §cin world §b" + loc.getWorld().getName() +
					" §7(Chunk X:" + chunk.getX() + "§7, Z:" + chunk.getZ() + "§7).");
		}
	}

    private void notifyAdmins(String message) {
        String prefix = plugin.getConfig().getString("settings.prefix");

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.hasPermission("lesslag.admin")) {
				player.sendMessage(prefix + message);
			}
		}
	}
}

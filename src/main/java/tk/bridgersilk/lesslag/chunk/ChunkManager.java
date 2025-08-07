package tk.bridgersilk.lesslag.chunk;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ChunkManager implements Listener {

    private final Plugin plugin;
    private final FileConfiguration config;

    private boolean throttlingEnabled;
    private int maxChunksPerSecond;
    private int disableDurationSeconds;

    private boolean chunkLoadingDisabled = false;
    private int chunksLoadedThisSecond = 0;
    private final Set<Chunk> recentlyUnloadedChunks = new HashSet<>();

    public ChunkManager(Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadConfigValues();
        startChunkCounterResetTask();
    }

    private void loadConfigValues() {
        throttlingEnabled = config.getBoolean("chunk_loading.throttle_on_fast_player_movement.enabled", true);
        maxChunksPerSecond = config.getInt("chunk_loading.throttle_on_fast_player_movement.max_chunks_per_second", 30);
        disableDurationSeconds = config.getInt("chunk_loading.throttle_on_fast_player_movement.disable_duration_seconds", 5);
    }

    private void startChunkCounterResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                chunksLoadedThisSecond = 0;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!throttlingEnabled) return;

        if (chunkLoadingDisabled) {
            Chunk chunk = event.getChunk();

            if (!recentlyUnloadedChunks.contains(chunk)) {
                recentlyUnloadedChunks.add(chunk);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (chunk.isLoaded()) {
                        boolean success = chunk.unload(true);
                        if (!success) {
                            chunk.unload(false);
                        }
                    }
                });

                Bukkit.getScheduler().runTaskLater(plugin, () -> 
                    recentlyUnloadedChunks.remove(chunk), 40L);
            }
            return;
        }

        chunksLoadedThisSecond++;

        if (chunksLoadedThisSecond > maxChunksPerSecond) {
            triggerThrottle(event.getWorld(), event.isNewChunk());
        }
    }

    private void triggerThrottle(World world, boolean newChunk) {
        if (chunkLoadingDisabled) return;

        chunkLoadingDisabled = true;
        String info = "§cChunk loading temporarily disabled for §b" + disableDurationSeconds +
                "§cs in world: §b" + world.getName() +
                "§c | Reason: High chunk load rate (§b" + chunksLoadedThisSecond + "§c chunks/sec)";
        notifyAdmins(info);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            chunkLoadingDisabled = false;
            notifyAdmins("§aChunk loading re-enabled in world: §b" + world.getName());
        }, disableDurationSeconds * 20L);
    }

    private void notifyAdmins(String message) {
        String prefix = Bukkit.getPluginManager().getPlugin("LessLag").getConfig().getString("settings.prefix");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("lesslag.admin")) {
                player.sendMessage(prefix + message);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        recentlyUnloadedChunks.remove(event.getChunk());
    }

    public void disable() {
        ChunkLoadEvent.getHandlerList().unregister(this);
        ChunkUnloadEvent.getHandlerList().unregister(this);
    }
}

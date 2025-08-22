package tk.bridgersilk.lesslag;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import tk.bridgersilk.lesslag.chunk.ChunkManager;
import tk.bridgersilk.lesslag.entity.CommandControlListener;
import tk.bridgersilk.lesslag.entity.EntityManager;
import tk.bridgersilk.lesslag.entity.SpawnControlListener;
import tk.bridgersilk.lesslag.item.ItemManagement;
import tk.bridgersilk.lesslag.performance.PerformanceManager;
import tk.bridgersilk.lesslag.player.ChatListener;
import tk.bridgersilk.lesslag.player.PlayerJoinListener;
import tk.bridgersilk.lesslag.player.PlayerManager;
import tk.bridgersilk.lesslag.player.PlayerTeleportListener;
import tk.bridgersilk.lesslag.system.ConfigManager;
import tk.bridgersilk.lesslag.system.MainCommand;
import tk.bridgersilk.lesslag.system.Profiler;
import tk.bridgersilk.lesslag.world.WorldManager;

public class LessLag extends JavaPlugin {

    private ConfigManager configManager;
    private Profiler profiler;
    private WorldManager worldManager;
    private ItemManagement itemManagement;
    private EntityManager entityManager;
    private PlayerManager playerManager;
    private PerformanceManager performanceManager;
    private ChunkManager chunkManager;

    private ProtocolManager protocolManager;

    @Override
    public void onLoad() {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        
        this.profiler = new Profiler(this);

        this.worldManager = new WorldManager(this);

        int pluginId = 27003;
        Metrics metrics = new Metrics(this, pluginId);
        
        new MainCommand(this);

        itemManagement = new ItemManagement(this);

        entityManager = new EntityManager(this);

        performanceManager = new PerformanceManager(this);

        chunkManager = new ChunkManager(this);

        Bukkit.getPluginManager().registerEvents(new SpawnControlListener(entityManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandControlListener(entityManager), this);

        playerManager = new PlayerManager(this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(playerManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerTeleportListener(playerManager), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(playerManager), this);

        getLogger().info("LessLag enabled! Config loaded and Profiler initialized.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LessLag disabled!");
        if (worldManager != null) {
            worldManager.disable();
        }
        if (itemManagement != null) {
            itemManagement.disable();
        }
        if (entityManager != null) entityManager.stopTasks();
        if (performanceManager != null) performanceManager.disable();
        if (playerManager != null) playerManager.disable();
        if (chunkManager != null) chunkManager.disable();
    }

    
    public ConfigManager getConfigManager() {
        return configManager;
    }

    
    public Profiler getProfiler() {
        return profiler;
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reloadConfig();

        if (profiler != null) {
            profiler.disable();
        }
        if (worldManager != null) {
            worldManager.disable();
        }
        if (itemManagement != null) {
            itemManagement.disable();
        }
        if (entityManager != null) {
            entityManager.stopTasks();
        }
        if (playerManager != null) {
            playerManager.disable();
        }

        if (chunkManager != null) chunkManager.disable();
        chunkManager = new ChunkManager(this);

        if (performanceManager != null) performanceManager.disable();

        
        profiler = new Profiler(this);
        worldManager = new WorldManager(this);

        itemManagement = new ItemManagement(this);
        entityManager = new EntityManager(this);

        playerManager = new PlayerManager(this);
        performanceManager = new PerformanceManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(playerManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerTeleportListener(playerManager), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(playerManager), this);

        Bukkit.getPluginManager().registerEvents(new SpawnControlListener(entityManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandControlListener(entityManager), this);

        getLogger().info("Plugin reloaded. Features updated with the latest config.");
    }
}

package tk.bridgersilk.lesslag.performance;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class PerformanceManager {

    private final Plugin plugin;

    private boolean redstoneEnabled;
    private boolean redstoneClocksOnly;
    private double redstoneTpsThreshold;

    private boolean fallingBlocksEnabled;
    private double fallingBlocksTpsThreshold;

    private boolean fluidsEnabled;
    private double fluidsTpsThreshold;

    private boolean explosionsEnabled;
    private double explosionsTpsThreshold;

    private boolean enderPearlsEnabled;
    private double enderPearlsTpsThreshold;

    private boolean commandBlocksEnabled;
    private double commandBlocksTpsThreshold;

    private boolean mobAiDisableWhenNoPlayers;
    private int mobAiRadius;

    private RedstoneListener redstoneListener;
    private FallingBlockListener fallingBlockListener;
    private FluidListener fluidListener;
    private ExplosionListener explosionListener;
    private EnderPearlListener enderPearlListener;
    private CommandBlockListener commandBlockListener;
    private MobAIListener mobAIListener;

    private BukkitTask aiCheckTask;

    public PerformanceManager(Plugin plugin) {
        this.plugin = plugin;
        reloadConfig();
        registerListeners();
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        redstoneEnabled = config.getBoolean("performance_controls.disable_redstone.enabled");
        redstoneClocksOnly = config.getBoolean("performance_controls.disable_redstone.clocks_only");
        redstoneTpsThreshold = config.getDouble("performance_controls.disable_redstone.disable_below_tps");

        fallingBlocksEnabled = config.getBoolean("performance_controls.disable_falling_blocks.enabled");
        fallingBlocksTpsThreshold = config.getDouble("performance_controls.disable_falling_blocks.disable_below_tps");

        fluidsEnabled = config.getBoolean("performance_controls.disable_fluids.enabled");
        fluidsTpsThreshold = config.getDouble("performance_controls.disable_fluids.disable_below_tps");

        explosionsEnabled = config.getBoolean("performance_controls.disable_explosions.enabled");
        explosionsTpsThreshold = config.getDouble("performance_controls.disable_explosions.disable_below_tps");

        enderPearlsEnabled = config.getBoolean("performance_controls.disable_ender_pearls.enabled");
        enderPearlsTpsThreshold = config.getDouble("performance_controls.disable_ender_pearls.disable_below_tps");

        commandBlocksEnabled = config.getBoolean("performance_controls.disable_command_blocks.enabled");
        commandBlocksTpsThreshold = config.getDouble("performance_controls.disable_command_blocks.disable_below_tps");

        mobAiDisableWhenNoPlayers = config.getBoolean("mob_ai.disable_ai_when_no_players_nearby.enabled");
        mobAiRadius = config.getInt("mob_ai.disable_ai_when_no_players_nearby.radius");
    }

    private void registerListeners() {
        if (redstoneEnabled) redstoneListener = new RedstoneListener(plugin, redstoneClocksOnly, redstoneTpsThreshold);
        if (fallingBlocksEnabled) fallingBlockListener = new FallingBlockListener(plugin, fallingBlocksTpsThreshold);
        if (fluidsEnabled) fluidListener = new FluidListener(plugin, fluidsTpsThreshold);
        if (explosionsEnabled) explosionListener = new ExplosionListener(plugin, explosionsTpsThreshold);
        if (enderPearlsEnabled) enderPearlListener = new EnderPearlListener(plugin, enderPearlsTpsThreshold);
        if (commandBlocksEnabled) commandBlockListener = new CommandBlockListener(plugin, commandBlocksTpsThreshold);
        if (mobAiDisableWhenNoPlayers) mobAIListener = new MobAIListener(plugin, mobAiRadius);
    }

    public void disable() {
        if (redstoneListener != null) redstoneListener.unregister();
        if (fallingBlockListener != null) fallingBlockListener.unregister();
        if (fluidListener != null) fluidListener.unregister();
        if (explosionListener != null) explosionListener.unregister();
        if (enderPearlListener != null) enderPearlListener.unregister();
        if (commandBlockListener != null) commandBlockListener.unregister();
        if (mobAIListener != null) mobAIListener.unregister();

        if (aiCheckTask != null) aiCheckTask.cancel();
    }
}

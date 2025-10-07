package tk.bridgersilk.lesslag.performance;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TickSpeedListener {
    private final Plugin plugin;
    private final FileConfiguration config;
    private final double tpsThreshold;
    private final double decreaseTo;
    private boolean changeTickSpeed;
    private Map<World, Integer> tickSpeedMap;

    public TickSpeedListener(Plugin plugin, int decreaseTo, double tpsThreshold) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.tpsThreshold = tpsThreshold;
        this.decreaseTo = decreaseTo;
        this.changeTickSpeed = false;
        this.tickSpeedMap = new HashMap<>();
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!config.getBoolean("performance_controls.decrease_tickspeed.enabled")) return;

                double tps = Bukkit.getServer().getTPS()[0];

                if (tps < tpsThreshold) {
                    if (!changeTickSpeed) {
                        tickSpeedMap.clear();
                        for (World world : Bukkit.getWorlds()) {
                            Integer current = world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED);
                            if (current == null) {
                                Integer def = world.getGameRuleDefault(GameRule.RANDOM_TICK_SPEED);
                                current = (def != null) ? def : 3; // fallback
                            }
                            tickSpeedMap.put(world, current);
                            world.setGameRule(GameRule.RANDOM_TICK_SPEED, (int) decreaseTo);
                        }
                        changeTickSpeed = true;
                    }
                } else {
                    if (changeTickSpeed) {
                        for (World world : Bukkit.getWorlds()) {
                            Integer original = tickSpeedMap.get(world);
                            if (original == null) {
                                Integer def = world.getGameRuleDefault(GameRule.RANDOM_TICK_SPEED);
                                original = (def != null) ? def : 3;
                            }
                            world.setGameRule(GameRule.RANDOM_TICK_SPEED, original);
                        }
                        changeTickSpeed = false;
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }
}

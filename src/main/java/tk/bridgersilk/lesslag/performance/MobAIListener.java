package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MobAIListener implements Listener {

	private final Plugin plugin;
	private final FileConfiguration config;
    private final int mobAiRadius;

	public MobAIListener(Plugin plugin, int mobAiRadius) {
		this.plugin = plugin;
        this.mobAiRadius = mobAiRadius;
		this.config = plugin.getConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
		startTask();
	}

	private void startTask() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!config.getBoolean("mob_ai.disable_ai_when_no_players_nearby.enabled")) return;

				for (Entity entity : Bukkit.getWorlds().stream().flatMap(w -> w.getEntities().stream()).toList()) {
					if (!(entity instanceof LivingEntity)) continue;
					LivingEntity mob = (LivingEntity) entity;

					boolean hasNearby = mob.getNearbyEntities(mobAiRadius, mobAiRadius, mobAiRadius).stream()
							.anyMatch(e -> e instanceof Player);

					mob.setAI(hasNearby);
				}
			}
		}.runTaskTimer(plugin, 100L, 100L);
	}

	@EventHandler
	public void onMobSpawn(CreatureSpawnEvent event) {
		if (!config.getBoolean("mob_ai.disable_ai_when_no_players_nearby.enabled")) return;

		LivingEntity mob = event.getEntity();

		boolean hasNearby = mob.getNearbyEntities(mobAiRadius, mobAiRadius, mobAiRadius).stream()
				.anyMatch(e -> e instanceof Player);

		mob.setAI(hasNearby);
	}

    public void unregister() {
        CreatureSpawnEvent.getHandlerList().unregister(this);
    }
}

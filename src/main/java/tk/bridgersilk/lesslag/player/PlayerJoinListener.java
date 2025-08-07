package tk.bridgersilk.lesslag.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final PlayerManager manager;

    public PlayerJoinListener(PlayerManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String prefix = Bukkit.getPluginManager().getPlugin("LessLag").getConfig().getString("settings.prefix");

        if (!world.getName().equalsIgnoreCase(manager.getFallbackWorldName()) &&
                world.getPlayers().size() >= manager.getMaxPlayersPerWorld()) {
            World fallback = Bukkit.getWorld(manager.getFallbackWorldName());
            if (fallback != null) {
                player.teleport(fallback.getSpawnLocation());
                player.sendMessage(prefix + "§cThe world was full, you've been teleported to the fallback world.");
            }
        }
    }
}

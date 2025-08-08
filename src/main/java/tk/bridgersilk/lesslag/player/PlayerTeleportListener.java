package tk.bridgersilk.lesslag.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    private final PlayerManager manager;

    public PlayerTeleportListener(PlayerManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World targetWorld = event.getTo().getWorld();
        String prefix = Bukkit.getPluginManager().getPlugin("LessLag").getConfig().getString("settings.prefix");

        if (!event.getFrom().getWorld().equals(targetWorld) &&
                !targetWorld.getName().equalsIgnoreCase(manager.getFallbackWorldName()) &&
                targetWorld.getPlayers().size() >= manager.getMaxPlayersPerWorld() &&
                !player.hasPermission("lesslag.admin")) {
            event.setCancelled(true);
            player.sendMessage(prefix + "§cYou can't teleport to this world because it's full.");
        }
    }
}

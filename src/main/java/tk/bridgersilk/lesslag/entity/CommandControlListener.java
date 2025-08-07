package tk.bridgersilk.lesslag.entity;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandControlListener implements Listener {

    private final EntityManager entityManager;

    public CommandControlListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String prefix = entityManager.getPlugin().getConfig().getString("settings.prefix");

        if (event.getMessage().toLowerCase().startsWith("/summon")) {
            Location loc = event.getPlayer().getLocation();
            Chunk chunk = loc.getChunk();
            World world = loc.getWorld();

            if (entityManager.isOverEntityLimit(chunk, world)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(prefix + "§cEntity limit reached! You can't summon entities here.");
            }
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase();
        if (command.startsWith("summon")) {
            if (event.getSender() instanceof BlockCommandSender) {
                BlockCommandSender cmdBlock = (BlockCommandSender) event.getSender();
                Location loc = cmdBlock.getBlock().getLocation();
                Chunk chunk = loc.getChunk();
                World world = loc.getWorld();

                if (entityManager.isOverEntityLimit(chunk, world)) {
                    event.setCancelled(true);
                    Bukkit.getLogger().info("Summon command from command block blocked due to entity limit.");
                }
            }
        }
    }
}

package tk.bridgersilk.lesslag.entity;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnControlListener implements Listener {

    private final EntityManager entityManager;

    public SpawnControlListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void onPlayerUseSpawnEgg(PlayerInteractEvent event) {
        String prefix = entityManager.getPlugin().getConfig().getString("settings.prefix");

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR)
            return;

        ItemStack item = event.getItem();
        if (item == null) return;

        if (item.getType().name().endsWith("_SPAWN_EGG")) {
            Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : event.getPlayer().getLocation();
            Chunk chunk = loc.getChunk();
            World world = loc.getWorld();

            if (entityManager.isOverEntityLimit(chunk, world)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(prefix + "§cEntity limit reached! You can't spawn more entities here.");
            }
        }
    }
}

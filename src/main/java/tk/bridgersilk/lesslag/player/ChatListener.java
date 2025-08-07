package tk.bridgersilk.lesslag.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final PlayerManager manager;

    public ChatListener(PlayerManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = Bukkit.getPluginManager().getPlugin("LessLag").getConfig().getString("settings.prefix");

        if (manager.isChatMuted() && !player.hasPermission("lesslag.admin")) {
            event.setCancelled(true);
            long remaining = (manager.chatMutedUntil - System.currentTimeMillis()) / 1000;
            player.sendMessage(prefix + "§cChat is currently muted due to spam. §7(" + remaining + "s left)");
            return;
        }

        manager.incrementMessageCount(player);
    }
}

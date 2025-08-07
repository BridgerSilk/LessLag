package tk.bridgersilk.lesslag.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

public class PlayerManager {

	private final Plugin plugin;
	private final ProtocolManager protocolManager;
	private PacketAdapter packetListener;

	private int maxPlayersPerWorld;
	private String fallbackWorldName;

	private boolean packetSpamEnabled;
	private int packetLimit;
	private String packetAction;
	private String packetActionReason;

	private boolean chatSpamEnabled;
	private int maxMessagesPerSecond;
	private int muteDurationSeconds;

	private final Map<Player, Integer> packetCounts = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> playerMessageCounts = new ConcurrentHashMap<>();
	private int totalMessagesPerSecond = 0;
	private boolean chatMuted = false;
	public long chatMutedUntil = 0;

	private BukkitTask scheduledTask;

	public PlayerManager(Plugin plugin) {
		this.plugin = plugin;
		this.protocolManager = ProtocolLibrary.getProtocolManager();
		reloadConfig();
		startScheduledTasks();
		registerPacketListener();
	}

	public void reloadConfig() {
		FileConfiguration config = plugin.getConfig();
		maxPlayersPerWorld = config.getInt("player_management.max_players_per_world");
		fallbackWorldName = config.getString("player_management.fallback_world", "lobby");

		packetSpamEnabled = config.getBoolean("player_management.packet_spam_protection.enabled", true);
		packetLimit = config.getInt("player_management.packet_spam_protection.packet_limit", 1000);
		packetAction = config.getString("player_management.packet_spam_protection.action", "kick");
		packetActionReason = config.getString("player_management.packet_spam_protection.action_reason", "§cYou're sending way too many packets! Trying to crash the server!?");

		chatSpamEnabled = config.getBoolean("player_management.chat_spam_protection.enabled", true);
		maxMessagesPerSecond = config.getInt("player_management.chat_spam_protection.max_messages_per_second", 6);
		muteDurationSeconds = config.getInt("player_management.chat_spam_protection.mute_duration_seconds", 5);
	}

	private void startScheduledTasks() {
		scheduledTask = new BukkitRunnable() {
			@Override
			public void run() {
				handleWorldPlayerLimits();
				handlePacketSpam();
				handleChatSpamReset();
			}
		}.runTaskTimer(plugin, 20L, 20L);
	}

	private void registerPacketListener() {
		if (packetListener != null) {
			protocolManager.removePacketListener(packetListener);
		}

		packetListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.values()) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				if (!packetSpamEnabled) return;

				Player player = event.getPlayer();
				packetCounts.put(player, packetCounts.getOrDefault(player, 0) + 1);
			}
		};

		protocolManager.addPacketListener(packetListener);
	}

	public void disable() {
		if (scheduledTask != null) {
			scheduledTask.cancel();
		}
		if (packetListener != null) {
			protocolManager.removePacketListener(packetListener);
		}
	}

	private void handleWorldPlayerLimits() {
		String prefix = plugin.getConfig().getString("settings.prefix");
		for (World world : Bukkit.getWorlds()) {
			if (world.getName().equalsIgnoreCase(fallbackWorldName)) continue;

			int playerCount = world.getPlayers().size();
			if (playerCount > maxPlayersPerWorld) {
				int excess = playerCount - maxPlayersPerWorld;
				List<Player> players = new ArrayList<>(world.getPlayers());
				Collections.shuffle(players);
				World fallback = Bukkit.getWorld(fallbackWorldName);
				if (fallback == null) continue;

				for (int i = 0; i < excess; i++) {
					Player p = players.get(i);
					p.teleport(fallback.getSpawnLocation());
					p.sendMessage(prefix + "§cThe world was over capacity. You've been moved to the fallback world.");
				}
			}
		}
	}

	private void handlePacketSpam() {
		if (!packetSpamEnabled) return;
		for (Map.Entry<Player, Integer> entry : packetCounts.entrySet()) {
			Player player = entry.getKey();
			int packets = entry.getValue();

			if (packets > packetLimit) {
				if (packetAction.equalsIgnoreCase("kick")) {
					player.kickPlayer(packetActionReason);
				} else if (packetAction.equalsIgnoreCase("ban")) {
					player.banPlayer(packetActionReason);
				}
				notifyAdmins(player.getName(), packets);
			}
		}
		packetCounts.clear();
	}

	private void notifyAdmins(String playerName, int packets) {
		String prefix = plugin.getConfig().getString("settings.prefix");
		String action = packetAction.equalsIgnoreCase("kick") ? "kicked" : "banned";

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.hasPermission("lesslag.admin")) {
				p.sendMessage(prefix + "§ePlayer Packet Limit: §b" + playerName +
						" §cgot §b" + action + " §cfor sending too many packets. §7(" + packets + " packets/second)");
			}
		}
	}

	public boolean isChatMuted() {
		return chatMuted && System.currentTimeMillis() < chatMutedUntil;
	}

	public void incrementMessageCount(Player player) {
		playerMessageCounts.put(player.getUniqueId(),
				playerMessageCounts.getOrDefault(player.getUniqueId(), 0) + 1);
		totalMessagesPerSecond++;

		if (chatSpamEnabled && totalMessagesPerSecond > maxMessagesPerSecond && !chatMuted) {
			chatMuted = true;
			chatMutedUntil = System.currentTimeMillis() + (muteDurationSeconds * 1000L);

			String prefix = plugin.getConfig().getString("settings.prefix");
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.hasPermission("lesslag.admin")) {
					p.sendMessage(prefix + "§cChat has been muted for §b" + muteDurationSeconds + "s §cdue to spam.");
				}
			}
		}
	}

	private void handleChatSpamReset() {
		playerMessageCounts.clear();
		totalMessagesPerSecond = 0;

		if (chatMuted && System.currentTimeMillis() >= chatMutedUntil) {
			chatMuted = false;
			String prefix = plugin.getConfig().getString("settings.prefix");
			Bukkit.broadcastMessage(prefix + "§aChat has been unmuted.");
		}
	}

	public int getMaxPlayersPerWorld() {
		return maxPlayersPerWorld;
	}

	public String getFallbackWorldName() {
		return fallbackWorldName;
	}
}

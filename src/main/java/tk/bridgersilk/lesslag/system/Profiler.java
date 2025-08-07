package tk.bridgersilk.lesslag.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import tk.bridgersilk.lesslag.LessLag;

public class Profiler {
	private final LessLag plugin;
	private final BossBar bossBar;
	private final Set<Player> viewers;
	private final FileConfiguration config;

	private long totalIncomingPackets = 0;
	private long totalOutgoingPackets = 0;

	private boolean blinkToggle = false;
	private final boolean enabled;
	private final int updateIntervalTicks;
	private final boolean showCPU;
	private final boolean showRAM;
	private final boolean showPackets;

	public Profiler(LessLag plugin) {
		this.plugin = plugin;
		this.config = plugin.getConfig();

		this.enabled = config.getBoolean("profiler.enabled", true);
		this.updateIntervalTicks = config.getInt("profiler.update_interval_ticks", 20);
		this.showCPU = config.getBoolean("profiler.show_cpu_usage", true);
		this.showRAM = config.getBoolean("profiler.show_ram_usage", true);
		this.showPackets = config.getBoolean("profiler.show_packets", true);

		this.bossBar = Bukkit.createBossBar("§eProfiler", BarColor.BLUE, BarStyle.SEGMENTED_10);
		this.viewers = new HashSet<>();

		if (enabled) {
			registerPacketListeners();
			startUpdating();
		}
	}

	private PacketType[] getAllClientPackets() {
		List<PacketType> packets = new ArrayList<>();
		for (Field field : PacketType.Play.Client.class.getDeclaredFields()) {
			if (PacketType.class.isAssignableFrom(field.getType())) {
				try {
					packets.add((PacketType) field.get(null));
				} catch (IllegalAccessException ignored) {}
			}
		}
		return packets.toArray(new PacketType[0]);
	}

	private PacketType[] getAllServerPackets() {
		List<PacketType> packets = new ArrayList<>();
		for (Field field : PacketType.Play.Server.class.getDeclaredFields()) {
			if (PacketType.class.isAssignableFrom(field.getType())) {
				try {
					packets.add((PacketType) field.get(null));
				} catch (IllegalAccessException ignored) {}
			}
		}
		return packets.toArray(new PacketType[0]);
	}

	private void registerPacketListeners() {
		if (!showPackets) return;

		Plugin pl = plugin;
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(pl, getAllClientPackets()) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				totalIncomingPackets++;
			}
		});
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(pl, getAllServerPackets()) {
			@Override
			public void onPacketSending(PacketEvent event) {
				totalOutgoingPackets++;
			}
		});
	}

	private void startUpdating() {
		Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			blinkToggle = !blinkToggle;

			double tps = Bukkit.getServer().getTPS()[0];
			double mspt = Bukkit.getAverageTickTime();
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

			double cpuLoad = showCPU
					? (osBean.getSystemLoadAverage() / osBean.getAvailableProcessors()) * 10
					: 0;
			long usedMem = showRAM
					? (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
					: 0;
			long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;

			for (Player player : viewers) {
				int ping = getPing(player);

				boolean criticalCpu = cpuLoad > 90;
				boolean highCpu = cpuLoad > 70;
				boolean criticalRam = usedMem > maxMem * 0.9;
				boolean highRam = usedMem > maxMem * 0.75;
				boolean criticalTps = tps < 10;
				boolean highTps = tps < 15;

				BarColor color;
				if ((showCPU && (criticalCpu || highCpu)) ||
					(showRAM && (criticalRam || highRam)) ||
					criticalTps) {
					color = (criticalCpu || criticalRam || criticalTps)
							? (blinkToggle ? BarColor.RED : BarColor.PINK)
							: BarColor.YELLOW;
				} else {
					color = BarColor.BLUE;
				}

				StringBuilder title = new StringBuilder();
				title.append("§bTPS: ").append(colorString(tps, highTps, criticalTps)).append(String.format("%.2f", tps));
				title.append(" §7| §bMSPT: ").append(colorString(mspt, highTps, criticalTps)).append(String.format("%.2f", mspt));

				if (showCPU)
					title.append(" §7| §bCPU: ").append(colorString(cpuLoad, highCpu, criticalCpu)).append(String.format("%.0f%%", cpuLoad));
				if (showRAM)
					title.append(" §7| §bRAM: ").append(colorString(usedMem, highRam, criticalRam)).append(String.format("%d/%d MB", usedMem, maxMem));
				title.append(" §7| §bPing: §f").append(ping).append("ms");

				if (showPackets)
					title.append(" §7| §bPackets In: §f").append(totalIncomingPackets)
						 .append(" §7| §bPackets Out: §f").append(totalOutgoingPackets);

				bossBar.setTitle(title.toString());
				bossBar.setColor(color);
			}
		}, updateIntervalTicks, updateIntervalTicks);
	}

	private int getPing(Player player) {
		try {
			return player.getPing();
		} catch (NoSuchMethodError e) {
			return 0;
		}
	}

	private String colorString(double value, boolean isHigh, boolean isCritical) {
		if (isCritical) return "§c";
		if (isHigh) return "§e";
		return "§f";
	}

	public void toggleProfiler(CommandSender sender) {
		String prefix = config.getString("settings.prefix");

		if (!(sender instanceof Player)) {
			sender.sendMessage(prefix + "§cOnly players can view the profiler.");
			return;
		}
		Player player = (Player) sender;
		if (viewers.contains(player)) {
			bossBar.removePlayer(player);
			viewers.remove(player);
			player.sendMessage(prefix + "§cProfiler disabled.");
		} else {
			bossBar.addPlayer(player);
			viewers.add(player);
			player.sendMessage(prefix + "§aProfiler enabled.");
		}
	}

    public void disable() {
        for (Player player : viewers) {
            bossBar.removePlayer(player);
        }
        viewers.clear();

        Bukkit.getScheduler().cancelTasks(plugin);

        ProtocolLibrary.getProtocolManager().getPacketListeners().stream()
            .filter(listener -> listener.getPlugin().equals(plugin))
            .forEach(ProtocolLibrary.getProtocolManager()::removePacketListener);

        totalIncomingPackets = 0;
        totalOutgoingPackets = 0;
    }
}

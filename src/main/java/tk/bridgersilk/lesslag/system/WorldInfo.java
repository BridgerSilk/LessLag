package tk.bridgersilk.lesslag.system;

import java.io.File;
import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import tk.bridgersilk.lesslag.LessLag;

public class WorldInfo {
	private final DecimalFormat df = new DecimalFormat("#.##");

    private final FileConfiguration config;

    public WorldInfo(LessLag plugin) {
        this.config = plugin.getConfig();
    }

	public void sendWorldInfo(CommandSender sender) {
        String prefix = config.getString("settings.prefix");
        
		sender.sendMessage(prefix + "§e--- World Information ---");

		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName();
			int entityCount = world.getEntities().size();
			int loadedChunks = world.getLoadedChunks().length;
			int players = world.getPlayers().size();
			double fileSizeMB = getWorldFolderSize(world.getWorldFolder()) / 1024.0 / 1024.0;


			sender.sendMessage(prefix + String.format(
					"§b%s §7| Entities: §f%d §7| Chunks: §f%d §7| Players: §f%d §7| Size: §f%s MB",
					worldName, entityCount, loadedChunks, players, df.format(fileSizeMB)
			));
		}
	}

	private long getWorldFolderSize(File folder) {
		long length = 0;
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					length += file.length();
				} else {
					length += getWorldFolderSize(file);
				}
			}
		}
		return length;
	}
}

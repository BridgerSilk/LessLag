package tk.bridgersilk.lesslag.system;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import tk.bridgersilk.lesslag.LessLag;

public class ConfigManager {
	private final LessLag plugin;
	private File configFile;
	private FileConfiguration config;

	public ConfigManager(LessLag plugin) {
		this.plugin = plugin;
		createConfig();
	}

	private void createConfig() {
		File folder = new File(plugin.getDataFolder(), "");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		configFile = new File(plugin.getDataFolder(), "config.yml");

		if (!configFile.exists()) {
			plugin.saveResource("config.yml", false);
		}

		config = YamlConfiguration.loadConfiguration(configFile);
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public void saveConfig() {
		try {
			config.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reloadConfig() {
		config = YamlConfiguration.loadConfiguration(configFile);
	}
}

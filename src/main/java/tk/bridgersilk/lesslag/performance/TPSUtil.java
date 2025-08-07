package tk.bridgersilk.lesslag.performance;

import org.bukkit.Bukkit;

public class TPSUtil {
    public static double getTPS() {
        return Bukkit.getServer().getTPS()[0];
    }
}

package dev.meyba.justCrates.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionUtil {
    private final Plugin plugin;
    private final String user;
    private final String repo;

    public VersionUtil(Plugin plugin, String user, String repo) {
        this.plugin = plugin;
        this.user = user;
        this.repo = repo;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String current = plugin.getDescription().getVersion();
                String latest = fetchLatest();
                if (latest == null || current.equals(latest)) return;
                plugin.getLogger().warning("New version available! " + current + " -> " + latest);
                plugin.getLogger().warning("Download: https://github.com/" + user + "/" + repo);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private String fetchLatest() throws Exception {
        URL url = new URL("https://raw.githubusercontent.com/" + user + "/" + repo + "/refs/heads/main/build.gradle");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        if (conn.getResponseCode() != 200) return null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (!t.startsWith("version =")) continue;
                String v = t.substring("version =".length()).trim();
                if ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\""))) {
                    v = v.substring(1, v.length() - 1);
                }
                if (!v.isEmpty()) return v;
            }
        } finally {
            conn.disconnect();
        }
        return null;
    }
}

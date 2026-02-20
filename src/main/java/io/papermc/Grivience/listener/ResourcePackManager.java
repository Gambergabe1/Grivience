package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ResourcePackManager implements Listener {
    private final GriviencePlugin plugin;
    private ResourcePackServer localServer;
    private boolean enabled;
    private String url;
    private String hash;
    private boolean required;
    private Component prompt;

    public ResourcePackManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("resource-pack.enabled", false);
        url = config.getString("resource-pack.url", "");
        required = config.getBoolean("resource-pack.required", false);
        hash = normalizeHash(config.getString("resource-pack.hash", ""));
        String promptText = config.getString("resource-pack.prompt", "");
        prompt = promptText == null || promptText.isBlank() ? null : Component.text(org.bukkit.ChatColor.translateAlternateColorCodes('&', promptText));
        maybeStartLocalServer(config, false);

        // Auto-start local host if enabled but no URL was provided.
        if (enabled && (url == null || url.isBlank())) {
            maybeStartLocalServer(config, true);
        }

        if (enabled && (url == null || url.isBlank())) {
            plugin.getLogger().warning("Resource pack enabled but URL is empty and local host unavailable.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        applyPack(player);
    }

    public void applyPack(Player player) {
        if (!enabled) {
            return;
        }
        if (url == null || url.isBlank()) {
            plugin.getLogger().warning("Resource pack enabled but URL is empty.");
            return;
        }
        try {
            if (prompt != null) {
                player.setResourcePack(url, hash, required, prompt);
            } else {
                player.setResourcePack(url, hash, required);
            }
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to send resource pack to " + player.getName() + ": " + ex.getMessage());
        }
    }

    public void shutdown() {
        if (localServer != null) {
            localServer.stop();
        }
    }

    private void maybeStartLocalServer(FileConfiguration config, boolean force) {
        boolean localEnabled = config.getBoolean("resource-pack.local.enabled", false) || force;
        if (!localEnabled) {
            return;
        }
        String fileName = config.getString("resource-pack.local.file", "resource-pack.zip");
        File packFile = new File(plugin.getDataFolder(), fileName);
        if (!packFile.exists()) {
            plugin.getLogger().warning("Local resource pack enabled but file '" + packFile.getAbsolutePath() + "' is missing.");
            return;
        }
        String host = config.getString("resource-pack.local.host", Bukkit.getIp());
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        int port = config.getInt("resource-pack.local.port", 8765);
        try {
            if (localServer == null) {
                localServer = new ResourcePackServer(plugin);
            }
            url = localServer.serve(packFile, host, port);
            if (hash == null || hash.isBlank()) {
                hash = sha1(packFile);
            }
            enabled = true;
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to start local resource pack server: " + ex.getMessage());
        }
    }

    private String sha1(File file) {
        try (var in = new java.io.FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException ex) {
            plugin.getLogger().warning("Failed to compute resource pack hash: " + ex.getMessage());
            return null;
        }
    }

    private String normalizeHash(String input) {
        if (input == null) {
            return null;
        }
        String clean = input.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.isBlank()) {
            return null;
        }
        if (clean.length() != 40) {
            plugin.getLogger().warning("Resource pack hash must be 40 hex chars (SHA-1). Provided: '" + input + "'.");
            return null;
        }
        return clean.toLowerCase();
    }
}

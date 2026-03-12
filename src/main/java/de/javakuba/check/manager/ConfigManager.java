package de.javakuba.check.manager;

import de.javakuba.check.CheckPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final CheckPlugin plugin;
    private String owner;
    private List<String> allowedPlayers;
    private int reportExpiryMinutes;

    public ConfigManager(CheckPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        owner = cfg.getString("owner", "javakuba");
        allowedPlayers = new ArrayList<>(cfg.getStringList("allowed-players"));
        if (!containsIgnoreCase(allowedPlayers, owner)) allowedPlayers.add(owner);
        reportExpiryMinutes = cfg.getInt("report-expiry-minutes", 60);
    }

    public void save() {
        plugin.getConfig().set("allowed-players", allowedPlayers);
        plugin.saveConfig();
    }

    public boolean isAllowed(String playerName) {
        return playerName.equalsIgnoreCase(owner) || containsIgnoreCase(allowedPlayers, playerName);
    }

    public boolean addPlayer(String playerName) {
        if (isAllowed(playerName)) return false;
        allowedPlayers.add(playerName);
        save();
        return true;
    }

    public boolean removePlayer(String playerName) {
        if (playerName.equalsIgnoreCase(owner)) return false;
        boolean removed = allowedPlayers.removeIf(p -> p.equalsIgnoreCase(playerName));
        if (removed) save();
        return removed;
    }

    public List<String> getAllowedPlayers() { return new ArrayList<>(allowedPlayers); }
    public String getOwner()               { return owner; }
    public int getReportExpiryMinutes()    { return reportExpiryMinutes; }

    private boolean containsIgnoreCase(List<String> list, String val) {
        return list.stream().anyMatch(s -> s.equalsIgnoreCase(val));
    }
}

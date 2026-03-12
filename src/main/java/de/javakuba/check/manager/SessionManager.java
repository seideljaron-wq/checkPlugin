package de.javakuba.check.manager;

import de.javakuba.check.CheckPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private final CheckPlugin plugin;
    private final Map<UUID, CheckSession> sessions = new HashMap<>();

    public SessionManager(CheckPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isChecking(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public CheckSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void startSession(Player checker, Player target) {
        sessions.put(checker.getUniqueId(),
                new CheckSession(checker.getLocation(), checker.getGameMode(),
                        target.getUniqueId(), target.getName()));
        checker.setGameMode(GameMode.SPECTATOR);
        checker.teleport(target.getLocation());
        checker.sendMessage(C.prefix()
                + C.gray("Now checking ") + C.aqua(target.getName())
                + C.gray(". Use ") + C.yellow("/check-finished") + C.gray(" or ")
                + C.yellow("/check-punish <duration> <reason>") + C.gray("."));
    }

    public void startOfflineSession(Player checker, UUID targetUUID, String targetName) {
        sessions.put(checker.getUniqueId(),
                new CheckSession(checker.getLocation(), checker.getGameMode(), targetUUID, targetName));
        checker.setGameMode(GameMode.SPECTATOR);
    }

    public boolean finishSession(Player checker) {
        CheckSession session = sessions.remove(checker.getUniqueId());
        if (session == null) return false;
        restore(checker, session);
        checker.sendMessage(C.prefix() + C.gray("Session ended. No action on ") + C.aqua(session.targetName()) + C.gray("."));
        return true;
    }

    public CheckSession endAndGetSession(Player checker) {
        CheckSession session = sessions.remove(checker.getUniqueId());
        if (session == null) return null;
        restore(checker, session);
        return session;
    }

    private void restore(Player checker, CheckSession session) {
        checker.setGameMode(session.previousGameMode());
        checker.teleport(session.returnLocation());
    }

    public void returnAll() {
        for (Map.Entry<UUID, CheckSession> entry : sessions.entrySet()) {
            Player p = plugin.getServer().getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                restore(p, entry.getValue());
                p.sendMessage(C.prefix() + C.gray("Session ended (plugin reload/shutdown)."));
            }
        }
        sessions.clear();
    }

    public record CheckSession(Location returnLocation, GameMode previousGameMode,
                               UUID targetUUID, String targetName) {}
}

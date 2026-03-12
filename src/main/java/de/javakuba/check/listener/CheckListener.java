package de.javakuba.check.listener;

import de.javakuba.check.CheckPlugin;
import de.javakuba.check.gui.ReportGUI;
import de.javakuba.check.manager.C;
import de.javakuba.check.manager.ReportEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class CheckListener implements Listener {

    private final CheckPlugin plugin;
    private static final String GUI_PREFIX = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Reports";


    private final java.util.Map<UUID, Integer> currentPage = new java.util.HashMap<>();

    public CheckListener(CheckPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player staff)) return;
        String title = e.getView().getTitle();
        if (!title.startsWith(GUI_PREFIX)) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        int slot = e.getRawSlot();
        int page = currentPage.getOrDefault(staff.getUniqueId(), 0);


        if (slot == 49) { staff.closeInventory(); return; }
        if (slot == 53) { 
            ReportGUI.open(staff, plugin, page);
            return;
        }
        if (slot == 48) { 
            page = Math.max(0, page - 1);
            currentPage.put(staff.getUniqueId(), page);
            ReportGUI.open(staff, plugin, page);
            return;
        }
        if (slot == 50) { 
            page++;
            currentPage.put(staff.getUniqueId(), page);
            ReportGUI.open(staff, plugin, page);
            return;
        }


        if (slot >= 45) return; 

        UUID reportedUUID = ReportGUI.getClickedUUID(staff.getUniqueId(), slot);
        if (reportedUUID == null) return;

        ReportEntry entry = plugin.getReportManager().getReport(reportedUUID);
        if (entry == null) {
            staff.sendMessage(C.prefix() + C.red("That report no longer exists."));
            staff.closeInventory();
            return;
        }

        staff.closeInventory();


        Player target = Bukkit.getPlayer(reportedUUID);
        if (target == null) {
  
            OfflinePlayer offline = Bukkit.getOfflinePlayer(reportedUUID);
            staff.sendMessage(C.prefix() + C.yellow(entry.getReportedName())
                    + C.gray(" is offline. Starting check session anyway."));
      
            staff.sendMessage(C.prefix() + C.gray("Use ") + C.yellow("/check-finished")
                    + C.gray(" to close the session."));
            plugin.getSessionManager().startOfflineSession(staff, reportedUUID, entry.getReportedName());
            return;
        }

        plugin.getSessionManager().startSession(staff, target);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        String title = e.getView().getTitle();
        if (!title.startsWith(GUI_PREFIX)) return;
        ReportGUI.cleanup(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (plugin.getSessionManager().isChecking(player)) {
            plugin.getSessionManager().endAndGetSession(player); 
        }
        currentPage.remove(player.getUniqueId());
    }
}

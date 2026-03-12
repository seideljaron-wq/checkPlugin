package de.javakuba.check.gui;

import de.javakuba.check.CheckPlugin;
import de.javakuba.check.manager.C;
import de.javakuba.check.manager.ReportEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportGUI {

    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Reports";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yy").withZone(ZoneId.systemDefault());

    // Map from inventory title hash → ordered list of reported UUIDs shown in that page
    // We re-open fresh each time, so we keep a per-player cache of the last opened GUI slots
    private static final Map<UUID, List<UUID>> openGUISlots = new HashMap<>();

    private ReportGUI() {}

    public static void open(Player staff, CheckPlugin plugin, int page) {
        Collection<ReportEntry> all = plugin.getReportManager().getAllReports();
        List<ReportEntry> entries = new ArrayList<>(all);
        // Newest first (LinkedHashMap insertion order = oldest first → reverse)
        Collections.reverse(entries);

        int pageSize = 45; // 5 rows for reports, last row for navigation
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = GUI_TITLE_PREFIX + ChatColor.DARK_GRAY + " [" + (page + 1) + "/" + totalPages + "]";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = page * pageSize;
        int end   = Math.min(start + pageSize, entries.size());

        List<UUID> slotMap = new ArrayList<>();
        for (int i = start; i < end; i++) {
            slotMap.add(entries.get(i).getReportedUUID());
            inv.setItem(i - start, buildSkull(entries.get(i), plugin));
        }
        openGUISlots.put(staff.getUniqueId(), slotMap);

        // Navigation row (row 6)
        fillBorder(inv, pageSize, totalPages, page);

        staff.openInventory(inv);
    }

    private static ItemStack buildSkull(ReportEntry entry, CheckPlugin plugin) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        // Set skull owner (async if possible)
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getReportedUUID()));

        boolean online = Bukkit.getPlayer(entry.getReportedUUID()) != null;

        // Display name
        meta.setDisplayName((online ? ChatColor.GREEN : ChatColor.RED) + "" + ChatColor.BOLD
                + entry.getReportedName()
                + ChatColor.RESET + ChatColor.GRAY + " (" + (online ? "Online" : "Offline") + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "────────────────────");
        lore.add(ChatColor.YELLOW + "Reports: " + ChatColor.WHITE + entry.getReportCount());
        lore.add("");

        // Show up to 3 most recent reports
        List<ReportEntry.ReporterInfo> reporters = entry.getReporters();
        int shown = Math.min(3, reporters.size());
        for (int i = reporters.size() - 1; i >= reporters.size() - shown; i--) {
            ReportEntry.ReporterInfo r = reporters.get(i);
            lore.add(ChatColor.GRAY + "► " + ChatColor.AQUA + r.name()
                    + ChatColor.GRAY + " @ " + ChatColor.WHITE + TIME_FMT.format(r.time()));
            lore.add(ChatColor.GRAY + "  Reason: " + ChatColor.WHITE + r.reason());
        }
        if (reporters.size() > 3) {
            lore.add(ChatColor.DARK_GRAY + "  ... +" + (reporters.size() - 3) + " more");
        }

        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "────────────────────");
        lore.add(ChatColor.GREEN + "▶ Click to check " + entry.getReportedName());

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private static void fillBorder(Inventory inv, int pageSize, int totalPages, int currentPage) {
        // Gray glass pane filler
        ItemStack filler = buildNamedItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "─");
        for (int i = pageSize; i < 54; i++) inv.setItem(i, filler);

        // Prev page
        if (currentPage > 0) {
            inv.setItem(48, buildNamedItem(Material.ARROW,
                    ChatColor.YELLOW + "◀ Previous page",
                    ChatColor.GRAY + "Page " + currentPage + "/" + totalPages));
        }
        // Next page
        if (currentPage < totalPages - 1) {
            inv.setItem(50, buildNamedItem(Material.ARROW,
                    ChatColor.YELLOW + "Next page ▶",
                    ChatColor.GRAY + "Page " + (currentPage + 2) + "/" + totalPages));
        }
        // Close button
        inv.setItem(49, buildNamedItem(Material.BARRIER, ChatColor.RED + "Close"));
        // Refresh button
        inv.setItem(53, buildNamedItem(Material.LIME_DYE, ChatColor.GREEN + "Refresh"));
    }

    private static ItemStack buildNamedItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines.length > 0) meta.setLore(Arrays.asList(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns the reported UUID at the clicked slot for a given player's open GUI, or null. */
    public static UUID getClickedUUID(UUID staffUUID, int slot) {
        List<UUID> slots = openGUISlots.get(staffUUID);
        if (slots == null || slot >= slots.size()) return null;
        return slots.get(slot);
    }

    public static void cleanup(UUID staffUUID) {
        openGUISlots.remove(staffUUID);
    }
}

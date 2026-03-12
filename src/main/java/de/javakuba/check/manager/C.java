package de.javakuba.check.manager;

import org.bukkit.ChatColor;

public final class C {
    private C() {}

    public static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    public static String prefix()  { return ChatColor.DARK_GRAY + "[" + ChatColor.RED + "Check" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET; }
    public static String gray(String s)   { return ChatColor.GRAY    + s + ChatColor.RESET; }
    public static String red(String s)    { return ChatColor.RED     + s + ChatColor.RESET; }
    public static String green(String s)  { return ChatColor.GREEN   + s + ChatColor.RESET; }
    public static String yellow(String s) { return ChatColor.YELLOW  + s + ChatColor.RESET; }
    public static String aqua(String s)   { return ChatColor.AQUA    + s + ChatColor.RESET; }
    public static String gold(String s)   { return ChatColor.GOLD    + s + ChatColor.RESET; }
    public static String white(String s)  { return ChatColor.WHITE   + s + ChatColor.RESET; }
}

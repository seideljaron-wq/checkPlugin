package de.javakuba.check;

import de.javakuba.check.command.*;
import de.javakuba.check.listener.CheckListener;
import de.javakuba.check.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

public class CheckPlugin extends JavaPlugin {

    private static CheckPlugin instance;

    private ConfigManager  configManager;
    private ReportManager  reportManager;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager  = new ConfigManager(this);
        reportManager  = new ReportManager(this);
        sessionManager = new SessionManager(this);


        ReportCommand reportCmd = new ReportCommand(this);
        getCommand("report").setExecutor(reportCmd);
        getCommand("report").setTabCompleter(reportCmd);

        CheckCommand checkCmd = new CheckCommand(this);
        getCommand("check").setExecutor(checkCmd);
        getCommand("check").setTabCompleter(checkCmd);
        getCommand("check-back").setExecutor(checkCmd);
        getCommand("check-finished").setExecutor(checkCmd);
        getCommand("check-punish").setExecutor(checkCmd);

        CheckAdminCommand adminCmd = new CheckAdminCommand(this);
        getCommand("check-admin").setExecutor(adminCmd);
        getCommand("check-admin").setTabCompleter(adminCmd);

        getServer().getPluginManager().registerEvents(new CheckListener(this), this);


        reportManager.startExpiryTask();

        getLogger().info("CheckPlugin enabled!");
    }

    @Override
    public void onDisable() {
        sessionManager.returnAll();
    }

    public static CheckPlugin getInstance() { return instance; }

    public ConfigManager  getConfigManager()  { return configManager; }
    public ReportManager  getReportManager()  { return reportManager; }
    public SessionManager getSessionManager() { return sessionManager; }
}

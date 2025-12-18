package de.b3ncloud.itemhunter;

import de.b3ncloud.itemhunter.commands.ItemHuntCommand;
import de.b3ncloud.itemhunter.gui.ChallengeGUI;
import de.b3ncloud.itemhunter.gui.ItemsGUI;
import de.b3ncloud.itemhunter.listeners.AchievementListener;
import de.b3ncloud.itemhunter.listeners.ItemPickupListener;
import de.b3ncloud.itemhunter.listeners.MobKillListener;
import de.b3ncloud.itemhunter.listeners.PlayerConnectionListener;
import de.b3ncloud.itemhunter.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * ItemHunter - Ultimate Challenge Plugin
 * 
 * Spieler müssen alle Items finden, alle Mobs töten und alle Achievements erreichen!
 * Timer pausiert automatisch wenn niemand online ist.
 * 
 * Mit Folia-Support via Reflection.
 * 
 * @version 2.0.0
 * @author b3ncloud
 */
public class ItemHunter extends JavaPlugin {

    private static ItemHunter instance;
    
    // Manager
    private ItemManager itemManager;
    private MobManager mobManager;
    private AchievementManager achievementManager;
    private TimerManager timerManager;
    private TablistManager tablistManager;
    
    // GUI
    private ItemsGUI itemsGUI;
    private ChallengeGUI challengeGUI;
    
    private BossBar progressBar;
    private String prefix;
    
    private boolean showTimerActionbar;
    private boolean showProgressBossbar;
    private boolean showTablist;
    
    // Folia-Support
    private boolean isFolia = false;
    private Object globalRegionScheduler;
    private Method foliaRunAtFixedRate;
    private Method foliaRunDelayed;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Folia-Erkennung
        detectFolia();
        
        // Config laden
        saveDefaultConfig();
        loadConfiguration();
        
        // Manager initialisieren
        itemManager = new ItemManager(this);
        mobManager = new MobManager(this);
        achievementManager = new AchievementManager(this);
        timerManager = new TimerManager(this);
        tablistManager = new TablistManager(this);
        
        // GUI initialisieren
        itemsGUI = new ItemsGUI(this);
        challengeGUI = new ChallengeGUI(this);
        
        // BossBar erstellen
        progressBar = Bukkit.createBossBar(
                ChatColor.GOLD + "Challenge: " + ChatColor.WHITE + "0%",
                BarColor.YELLOW,
                BarStyle.SEGMENTED_10
        );
        progressBar.setVisible(false);
        
        // Listener registrieren
        getServer().getPluginManager().registerEvents(new ItemPickupListener(this), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(this), this);
        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(itemsGUI, this);
        getServer().getPluginManager().registerEvents(challengeGUI, this);
        
        // Commands registrieren
        ItemHuntCommand commandHandler = new ItemHuntCommand(this);
        getCommand("itemhunt").setExecutor(commandHandler);
        getCommand("itemhunt").setTabCompleter(commandHandler);
        
        // Tasks starten
        startTasks();
        
        // Gespeicherten Zustand laden
        timerManager.loadState();
        itemManager.loadProgress();
        mobManager.loadProgress();
        achievementManager.loadProgress();
        
        getLogger().info("═══════════════════════════════════════════════");
        getLogger().info("  🎯 ItemHunter v" + getDescription().getVersion() + " aktiviert!");
        getLogger().info("  📦 Items: " + itemManager.getTotalItems());
        getLogger().info("  ☠ Mobs: " + mobManager.getTotalMobs());
        getLogger().info("  ★ Achievements: " + achievementManager.getTotalAdvancements());
        getLogger().info("  Folia-Support: " + (isFolia ? "✓ Aktiviert" : "✗ Bukkit-Modus"));
        getLogger().info("═══════════════════════════════════════════════");
    }
    
    @Override
    public void onDisable() {
        // Zustand speichern
        if (timerManager != null) timerManager.saveState();
        if (itemManager != null) itemManager.saveProgress();
        if (mobManager != null) mobManager.saveProgress();
        if (achievementManager != null) achievementManager.saveProgress();
        
        // BossBar entfernen
        if (progressBar != null) {
            progressBar.removeAll();
        }
        
        getLogger().info("🎯 ItemHunter deaktiviert!");
    }
    
    private void loadConfiguration() {
        prefix = getConfig().getString("prefix", "&8[&6ItemHunt&8] &r").replace("&", "§");
        showTimerActionbar = getConfig().getBoolean("show-timer-actionbar", true);
        showProgressBossbar = getConfig().getBoolean("show-progress-bossbar", true);
        showTablist = getConfig().getBoolean("show-tablist", true);
    }
    
    /**
     * Erkennt ob wir auf Folia laufen und initialisiert die Reflection-Methoden
     */
    private void detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            
            Method getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            globalRegionScheduler = getGlobalRegionScheduler.invoke(null);
            
            Class<?> schedulerClass = globalRegionScheduler.getClass();
            foliaRunAtFixedRate = schedulerClass.getMethod("runAtFixedRate", 
                    org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class);
            foliaRunDelayed = schedulerClass.getMethod("runDelayed",
                    org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, TimeUnit.class);
            
            getLogger().info("Folia erkannt - verwende RegionScheduler");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Standard Bukkit/Paper erkannt");
        } catch (Exception e) {
            isFolia = false;
            getLogger().warning("Folia-Erkennung fehlgeschlagen: " + e.getMessage());
        }
    }
    
    private void startTasks() {
        // Timer & ActionBar Update (jede Sekunde)
        Runnable timerTask = () -> {
            if (!timerManager.isRunning()) return;
            
            timerManager.tick();
            
            // ActionBar mit Gesamtfortschritt
            if (showTimerActionbar) {
                int totalFound = itemManager.getFoundCount() + mobManager.getKilledCount() + achievementManager.getCompletedCount();
                int totalRequired = itemManager.getTotalItems() + mobManager.getTotalMobs() + achievementManager.getTotalAdvancements();
                double progress = totalRequired > 0 ? (double) totalFound / totalRequired * 100 : 0;
                
                String timerText = ChatColor.GOLD + "⏱ " + ChatColor.WHITE + timerManager.getFormattedTime() + 
                        ChatColor.DARK_GRAY + " │ " + 
                        ChatColor.GREEN + "📦" + itemManager.getFoundCount() + 
                        ChatColor.DARK_GRAY + " │ " +
                        ChatColor.RED + "☠" + mobManager.getKilledCount() + 
                        ChatColor.DARK_GRAY + " │ " +
                        ChatColor.LIGHT_PURPLE + "★" + achievementManager.getCompletedCount() +
                        ChatColor.DARK_GRAY + " │ " +
                        ChatColor.YELLOW + String.format("%.1f", progress) + "%";
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(timerText));
                }
            }
            
            // Tablist aktualisieren
            if (showTablist) {
                tablistManager.updateTablist();
            }
            
            updateBossBar();
        };
        
        Runnable saveTask = () -> {
            timerManager.saveState();
            itemManager.saveProgress();
            mobManager.saveProgress();
            achievementManager.saveProgress();
        };
        
        if (isFolia) {
            try {
                java.util.function.Consumer<Object> timerConsumer = (task) -> timerTask.run();
                java.util.function.Consumer<Object> saveConsumer = (task) -> saveTask.run();
                
                foliaRunAtFixedRate.invoke(globalRegionScheduler, this, timerConsumer, 1L, 1L, TimeUnit.SECONDS);
                foliaRunAtFixedRate.invoke(globalRegionScheduler, this, saveConsumer, 5L * 60, 5L * 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().severe("Folia-Scheduler fehlgeschlagen: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(this, timerTask, 20L, 20L);
            Bukkit.getScheduler().runTaskTimer(this, saveTask, 6000L, 6000L);
        }
    }
    
    /**
     * Führt eine Aufgabe verzögert aus (Folia-kompatibel)
     */
    public void runTaskLater(Runnable task, long delayTicks) {
        if (isFolia) {
            try {
                java.util.function.Consumer<Object> consumer = (t) -> task.run();
                long delayMs = delayTicks * 50;
                foliaRunDelayed.invoke(globalRegionScheduler, this, consumer, delayMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                getLogger().severe("Folia runTaskLater fehlgeschlagen: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
        }
    }
    
    public boolean isFolia() {
        return isFolia;
    }
    
    public void updateBossBar() {
        if (!showProgressBossbar || !timerManager.isRunning()) {
            progressBar.setVisible(false);
            return;
        }
        
        // Gesamtfortschritt
        int totalFound = itemManager.getFoundCount() + mobManager.getKilledCount() + achievementManager.getCompletedCount();
        int totalRequired = itemManager.getTotalItems() + mobManager.getTotalMobs() + achievementManager.getTotalAdvancements();
        double progress = totalRequired > 0 ? (double) totalFound / totalRequired : 0;
        
        progressBar.setTitle(ChatColor.GOLD + "🎯 Challenge: " + 
                ChatColor.GREEN + "📦" + itemManager.getFoundCount() + "/" + itemManager.getTotalItems() + " " +
                ChatColor.RED + "☠" + mobManager.getKilledCount() + "/" + mobManager.getTotalMobs() + " " +
                ChatColor.LIGHT_PURPLE + "★" + achievementManager.getCompletedCount() + "/" + achievementManager.getTotalAdvancements() + " " +
                ChatColor.YELLOW + "(" + String.format("%.1f", progress * 100) + "%)");
        progressBar.setProgress(Math.min(1.0, progress));
        
        // Farbe je nach Fortschritt
        if (progress >= 0.75) {
            progressBar.setColor(BarColor.GREEN);
        } else if (progress >= 0.5) {
            progressBar.setColor(BarColor.YELLOW);
        } else if (progress >= 0.25) {
            progressBar.setColor(BarColor.PINK);
        } else {
            progressBar.setColor(BarColor.RED);
        }
        
        progressBar.setVisible(true);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!progressBar.getPlayers().contains(player)) {
                progressBar.addPlayer(player);
            }
        }
    }
    
    /**
     * Prüft ob die gesamte Challenge abgeschlossen ist
     */
    public void checkChallengeComplete() {
        if (!timerManager.isActive()) return;
        
        boolean itemsComplete = itemManager.getFoundCount() >= itemManager.getTotalItems();
        boolean mobsComplete = mobManager.getKilledCount() >= mobManager.getTotalMobs();
        boolean achievementsComplete = achievementManager.getCompletedCount() >= achievementManager.getTotalAdvancements();
        
        if (itemsComplete && mobsComplete && achievementsComplete) {
            challengeComplete();
        }
    }
    
    private void challengeComplete() {
        String time = timerManager.getFormattedTime();
        
        timerManager.stop();
        progressBar.removeAll();
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════════════════════════════════");
        Bukkit.broadcastMessage(ChatColor.GREEN + "              🎉 " + ChatColor.BOLD + "CHALLENGE GESCHAFFT!" + ChatColor.GREEN + " 🎉");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.WHITE + "   📦 Alle " + ChatColor.GREEN + itemManager.getTotalItems() + ChatColor.WHITE + " Items gesammelt!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "   ☠ Alle " + ChatColor.RED + mobManager.getTotalMobs() + ChatColor.WHITE + " Mobs getötet!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "   ★ Alle " + ChatColor.LIGHT_PURPLE + achievementManager.getTotalAdvancements() + ChatColor.WHITE + " Achievements erreicht!");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.WHITE + "   ⏱ Zeit: " + ChatColor.AQUA + time);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════════════════════════════════");
        Bukkit.broadcastMessage("");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendTitle(
                    ChatColor.GREEN + "🎉 GESCHAFFT! 🎉",
                    ChatColor.WHITE + "Alle Ziele in " + ChatColor.AQUA + time,
                    10, 100, 20
            );
        }
    }
    
    // ============ GETTER ============
    
    public static ItemHunter getInstance() {
        return instance;
    }
    
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    public MobManager getMobManager() {
        return mobManager;
    }
    
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    public TimerManager getTimerManager() {
        return timerManager;
    }
    
    public TablistManager getTablistManager() {
        return tablistManager;
    }
    
    public ItemsGUI getItemsGUI() {
        return itemsGUI;
    }
    
    public ChallengeGUI getChallengeGUI() {
        return challengeGUI;
    }
    
    public BossBar getProgressBar() {
        return progressBar;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getMessage(String path) {
        String message = getConfig().getString("messages." + path, "&cNachricht nicht gefunden: " + path);
        return message.replace("&", "§");
    }
}

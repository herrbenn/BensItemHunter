package de.b3ncloud.itemhunter.managers;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Verwaltet den Timer der Challenge
 * Pausiert automatisch wenn keine Spieler online sind
 */
public class TimerManager {

    private final ItemHunter plugin;
    
    // Timer Status
    private boolean running = false;
    private boolean paused = false;
    
    // Zeit in Sekunden
    private long elapsedSeconds = 0;
    
    private File timerFile;
    private FileConfiguration timerConfig;
    
    public TimerManager(ItemHunter plugin) {
        this.plugin = plugin;
        this.timerFile = new File(plugin.getDataFolder(), "timer.yml");
    }
    
    // ============ TIMER KONTROLLE ============
    
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        paused = false;
        elapsedSeconds = 0;
        
        // Challenge starten Nachricht
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "    🎯 " + ChatColor.BOLD + "ITEM HUNT GESTARTET!" + ChatColor.YELLOW + " 🎯");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.WHITE + "    Findet alle " + ChatColor.GOLD + plugin.getItemManager().getTotalItems() + ChatColor.WHITE + " Items!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "    Benutze " + ChatColor.WHITE + "/itemhunt items" + ChatColor.GRAY + " für die Item-Liste");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        Bukkit.broadcastMessage("");
        
        // Sound
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
            player.sendTitle(
                    ChatColor.GOLD + "🎯 ITEM HUNT 🎯",
                    ChatColor.WHITE + "Finde alle Items!",
                    10, 60, 20
            );
        }
        
        plugin.updateBossBar();
    }
    
    public void stop() {
        running = false;
        paused = false;
        
        plugin.getProgressBar().setVisible(false);
    }
    
    public void pause() {
        if (!running || paused) return;
        
        paused = true;
        
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.YELLOW + "⏸ Timer pausiert bei " + getFormattedTime());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }
    
    public void resume() {
        if (!running || !paused) return;
        
        paused = false;
        
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GREEN + "▶ Timer fortgesetzt! Aktuelle Zeit: " + getFormattedTime());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }
    
    public void tick() {
        if (!running || paused) return;
        
        elapsedSeconds++;
    }
    
    public void reset() {
        running = false;
        paused = false;
        elapsedSeconds = 0;
        
        // Timer-Datei löschen
        if (timerFile.exists()) {
            timerFile.delete();
        }
        
        plugin.getProgressBar().setVisible(false);
    }
    
    // ============ AUTO PAUSE (keine Spieler online) ============
    
    public void checkAutoPause() {
        if (!running) return;
        
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            if (!paused) {
                paused = true;
                plugin.getLogger().info("Timer automatisch pausiert (keine Spieler online)");
            }
        }
    }
    
    public void checkAutoResume() {
        if (!running) return;
        
        if (!Bukkit.getOnlinePlayers().isEmpty() && paused) {
            paused = false;
            plugin.getLogger().info("Timer automatisch fortgesetzt (Spieler online)");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "▶ Timer fortgesetzt! Zeit: " + getFormattedTime());
            }
        }
    }
    
    // ============ GETTER ============
    
    public boolean isRunning() {
        return running && !paused;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public boolean isActive() {
        return running;
    }
    
    public long getElapsedSeconds() {
        return elapsedSeconds;
    }
    
    public String getFormattedTime() {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    public String getDetailedTime() {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(" Stunde").append(hours != 1 ? "n" : "").append(" ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append(" Minute").append(minutes != 1 ? "n" : "").append(" ");
        }
        sb.append(seconds).append(" Sekunde").append(seconds != 1 ? "n" : "");
        
        return sb.toString();
    }
    
    // ============ SPEICHERN & LADEN ============
    
    public void saveState() {
        timerConfig = new YamlConfiguration();
        
        timerConfig.set("running", running);
        timerConfig.set("paused", paused);
        timerConfig.set("elapsed-seconds", elapsedSeconds);
        
        try {
            timerConfig.save(timerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte Timer-Zustand nicht speichern!");
            e.printStackTrace();
        }
    }
    
    public void loadState() {
        if (!timerFile.exists()) return;
        
        timerConfig = YamlConfiguration.loadConfiguration(timerFile);
        
        running = timerConfig.getBoolean("running", false);
        paused = timerConfig.getBoolean("paused", false);
        elapsedSeconds = timerConfig.getLong("elapsed-seconds", 0);
        
        if (running) {
            // Timer war aktiv, pausieren da Server gerade gestartet
            paused = true;
            plugin.getLogger().info("Timer-Zustand geladen: " + getFormattedTime() + " (pausiert)");
        }
    }
}

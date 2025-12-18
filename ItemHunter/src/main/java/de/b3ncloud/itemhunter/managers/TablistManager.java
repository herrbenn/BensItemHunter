package de.b3ncloud.itemhunter.managers;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Verwaltet die Tablist mit TPS, Ping und Challenge-Fortschritt
 */
public class TablistManager {

    private final ItemHunter plugin;
    private Scoreboard scoreboard;
    
    // Folia-Support
    private boolean isFolia;
    
    public TablistManager(ItemHunter plugin) {
        this.plugin = plugin;
        this.isFolia = plugin.isFolia();
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }
    
    /**
     * Aktualisiert die Tablist für alle Spieler
     */
    public void updateTablist() {
        double tps = getTPS();
        String tpsColor = getTpsColor(tps);
        String tpsDisplay = tpsColor + String.format("%.1f", Math.min(20.0, tps));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTablist(player, tpsDisplay);
        }
    }
    
    /**
     * Aktualisiert die Tablist für einen einzelnen Spieler
     */
    public void updatePlayerTablist(Player player, String tpsDisplay) {
        int ping = getPlayerPing(player);
        String pingColor = getPingColor(ping);
        
        // Header
        StringBuilder header = new StringBuilder();
        header.append("\n");
        header.append(ChatColor.GOLD).append("★ ").append(ChatColor.WHITE).append(ChatColor.BOLD).append("HerrrBennn").append(ChatColor.GOLD).append(" ★\n");
        header.append(ChatColor.GRAY).append("Item Hunt Challenge\n");
        header.append("\n");
        
        // Footer mit Stats
        StringBuilder footer = new StringBuilder();
        footer.append("\n");
        
        // TPS & Ping Zeile
        footer.append(ChatColor.GRAY).append("TPS: ").append(tpsDisplay);
        footer.append(ChatColor.DARK_GRAY).append(" │ ");
        footer.append(ChatColor.GRAY).append("Ping: ").append(pingColor).append(ping).append("ms");
        footer.append("\n\n");
        
        // Challenge-Fortschritt wenn aktiv
        if (plugin.getTimerManager().isActive()) {
            String time = plugin.getTimerManager().getFormattedTime();
            boolean paused = plugin.getTimerManager().isPaused();
            
            footer.append(ChatColor.AQUA).append("⏱ ").append(ChatColor.WHITE).append(time);
            if (paused) {
                footer.append(ChatColor.RED).append(" (PAUSIERT)");
            }
            footer.append("\n\n");
            
            // Items
            int foundItems = plugin.getItemManager().getFoundCount();
            int totalItems = plugin.getItemManager().getTotalItems();
            double itemProgress = totalItems > 0 ? (double) foundItems / totalItems * 100 : 0;
            footer.append(ChatColor.GREEN).append("📦 Items: ");
            footer.append(ChatColor.WHITE).append(foundItems).append("/").append(totalItems);
            footer.append(ChatColor.GRAY).append(" (").append(String.format("%.0f", itemProgress)).append("%)\n");
            
            // Mobs
            int killedMobs = plugin.getMobManager().getKilledCount();
            int totalMobs = plugin.getMobManager().getTotalMobs();
            double mobProgress = totalMobs > 0 ? (double) killedMobs / totalMobs * 100 : 0;
            footer.append(ChatColor.RED).append("☠ Mobs: ");
            footer.append(ChatColor.WHITE).append(killedMobs).append("/").append(totalMobs);
            footer.append(ChatColor.GRAY).append(" (").append(String.format("%.0f", mobProgress)).append("%)\n");
            
            // Achievements
            int completedAdv = plugin.getAchievementManager().getCompletedCount();
            int totalAdv = plugin.getAchievementManager().getTotalAdvancements();
            double advProgress = totalAdv > 0 ? (double) completedAdv / totalAdv * 100 : 0;
            footer.append(ChatColor.LIGHT_PURPLE).append("★ Achievements: ");
            footer.append(ChatColor.WHITE).append(completedAdv).append("/").append(totalAdv);
            footer.append(ChatColor.GRAY).append(" (").append(String.format("%.0f", advProgress)).append("%)\n");
            
            // Gesamtfortschritt
            int totalFound = foundItems + killedMobs + completedAdv;
            int totalRequired = totalItems + totalMobs + totalAdv;
            double totalProgress = totalRequired > 0 ? (double) totalFound / totalRequired * 100 : 0;
            footer.append("\n");
            footer.append(ChatColor.GOLD).append("Gesamt: ");
            footer.append(getProgressBar(totalProgress, 20));
            footer.append(ChatColor.YELLOW).append(" ").append(String.format("%.1f", totalProgress)).append("%");
        } else {
            footer.append(ChatColor.GRAY).append("Keine Challenge aktiv\n");
            footer.append(ChatColor.GRAY).append("Starte mit ").append(ChatColor.WHITE).append("/itemhunt start");
        }
        
        footer.append("\n");
        
        player.setPlayerListHeaderFooter(header.toString(), footer.toString());
    }
    
    /**
     * Erstellt einen Fortschrittsbalken
     */
    private String getProgressBar(double percentage, int length) {
        int filled = (int) (percentage / 100 * length);
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.DARK_GRAY).append("[");
        
        for (int i = 0; i < filled; i++) {
            if (percentage >= 75) {
                bar.append(ChatColor.GREEN);
            } else if (percentage >= 50) {
                bar.append(ChatColor.YELLOW);
            } else if (percentage >= 25) {
                bar.append(ChatColor.GOLD);
            } else {
                bar.append(ChatColor.RED);
            }
            bar.append("█");
        }
        
        bar.append(ChatColor.GRAY);
        for (int i = 0; i < empty; i++) {
            bar.append("░");
        }
        
        bar.append(ChatColor.DARK_GRAY).append("]");
        return bar.toString();
    }
    
    /**
     * Holt die Server-TPS
     */
    private double getTPS() {
        try {
            // Paper/Folia haben eine direkte Methode
            Method getTPSMethod = Bukkit.class.getMethod("getTPS");
            double[] tps = (double[]) getTPSMethod.invoke(null);
            return tps[0]; // 1-Minuten TPS
        } catch (Exception e) {
            // Fallback für Spigot
            try {
                Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
                Field tpsField = server.getClass().getField("recentTps");
                double[] tps = (double[]) tpsField.get(server);
                return tps[0];
            } catch (Exception e2) {
                return 20.0; // Fallback
            }
        }
    }
    
    /**
     * Holt den Ping eines Spielers
     */
    private int getPlayerPing(Player player) {
        try {
            // Paper hat getPing() direkt
            Method getPingMethod = player.getClass().getMethod("getPing");
            return (int) getPingMethod.invoke(player);
        } catch (Exception e) {
            // Fallback via Reflection
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                Field pingField = handle.getClass().getField("ping");
                return pingField.getInt(handle);
            } catch (Exception e2) {
                return 0;
            }
        }
    }
    
    /**
     * Gibt die Farbe basierend auf TPS zurück
     */
    private String getTpsColor(double tps) {
        if (tps >= 19.5) return ChatColor.GREEN.toString();
        if (tps >= 18.0) return ChatColor.YELLOW.toString();
        if (tps >= 15.0) return ChatColor.GOLD.toString();
        return ChatColor.RED.toString();
    }
    
    /**
     * Gibt die Farbe basierend auf Ping zurück
     */
    private String getPingColor(int ping) {
        if (ping <= 50) return ChatColor.GREEN.toString();
        if (ping <= 100) return ChatColor.YELLOW.toString();
        if (ping <= 200) return ChatColor.GOLD.toString();
        return ChatColor.RED.toString();
    }
    
    /**
     * Setzt den Tablist-Namen eines Spielers mit Prefix/Suffix
     */
    public void setPlayerTabName(Player player) {
        // Optional: Team-basierte Namen für Prefixes
        String teamName = "ih_" + player.getName().substring(0, Math.min(12, player.getName().length()));
        
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        // Prefix mit Status
        if (plugin.getTimerManager().isActive()) {
            team.setPrefix(ChatColor.GRAY + "[" + ChatColor.GREEN + "✓" + ChatColor.GRAY + "] ");
        } else {
            team.setPrefix(ChatColor.GRAY + "");
        }
        
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }
    
    /**
     * Entfernt einen Spieler aus dem Tablist-Team
     */
    public void removePlayerTabName(Player player) {
        String teamName = "ih_" + player.getName().substring(0, Math.min(12, player.getName().length()));
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }
}

package de.b3ncloud.itemhunter.listeners;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener für Spieler-Verbindungs-Events
 * Pausiert/Resumiert Timer automatisch
 */
public class PlayerConnectionListener implements Listener {

    private final ItemHunter plugin;
    
    public PlayerConnectionListener(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Timer fortsetzen wenn Challenge aktiv
        plugin.getTimerManager().checkAutoResume();
        
        // Spieler zur BossBar hinzufügen
        if (plugin.getTimerManager().isActive()) {
            plugin.getProgressBar().addPlayer(player);
            
            // Info über laufende Challenge
            int found = plugin.getItemManager().getFoundCount();
            int total = plugin.getItemManager().getTotalItems();
            String time = plugin.getTimerManager().getFormattedTime();
            
            player.sendMessage("");
            player.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "🎯 Item Hunt Challenge läuft!");
            player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Fortschritt: " + 
                    ChatColor.GREEN + found + ChatColor.GRAY + "/" + ChatColor.WHITE + total + " Items");
            player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Zeit: " + ChatColor.AQUA + time);
            player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Benutze " + ChatColor.WHITE + 
                    "/itemhunt items" + ChatColor.GRAY + " für die Item-Liste");
            player.sendMessage("");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Prüfen ob Timer pausiert werden muss
        // Das muss einen Tick später passieren, da der Spieler noch in der Liste ist
        plugin.runTaskLater(() -> {
            plugin.getTimerManager().checkAutoPause();
        }, 1L);
    }
}

package de.b3ncloud.itemhunter.listeners;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * Listener für Achievement/Advancement Events
 */
public class AchievementListener implements Listener {

    private final ItemHunter plugin;
    
    public AchievementListener(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        // Challenge muss laufen
        if (!plugin.getTimerManager().isRunning()) return;
        
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        
        // Prüfen ob dieses Achievement noch gebraucht wird
        plugin.getAchievementManager().completeAdvancement(player, advancement);
    }
}

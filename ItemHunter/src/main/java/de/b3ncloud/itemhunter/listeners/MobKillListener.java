package de.b3ncloud.itemhunter.listeners;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener für Mob-Kill Events
 */
public class MobKillListener implements Listener {

    private final ItemHunter plugin;
    
    public MobKillListener(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Challenge muss laufen
        if (!plugin.getTimerManager().isRunning()) return;
        
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        // Muss von einem Spieler getötet worden sein
        if (killer == null) return;
        
        EntityType type = entity.getType();
        
        // Prüfen ob dieser Mob noch gebraucht wird
        plugin.getMobManager().killedMob(killer, type);
    }
}

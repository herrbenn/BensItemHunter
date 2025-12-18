package de.b3ncloud.itemhunter.listeners;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Item-Aufnahme Events
 * Erkennt wenn Spieler neue Items bekommen
 */
public class ItemPickupListener implements Listener {

    private final ItemHunter plugin;
    
    public ItemPickupListener(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!plugin.getTimerManager().isRunning()) return;
        
        Player player = (Player) event.getEntity();
        Material material = event.getItem().getItemStack().getType();
        
        plugin.getItemManager().foundItem(player, material);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!plugin.getTimerManager().isRunning()) return;
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        
        if (result != null && result.getType() != Material.AIR) {
            plugin.getItemManager().foundItem(player, result.getType());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!plugin.getTimerManager().isRunning()) return;
        
        // GUI Klicks ignorieren
        String title = event.getView().getTitle();
        if (title.contains("Item Hunt")) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Item das in das Spieler-Inventar kommt prüfen
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        // Shift-Click in Spieler-Inventar
        if (event.isShiftClick() && current != null && current.getType() != Material.AIR) {
            // Prüfen ob vom Container ins Spieler-Inventar
            if (event.getClickedInventory() != player.getInventory()) {
                plugin.getItemManager().foundItem(player, current.getType());
            }
        }
        
        // Normaler Click - Cursor Item ins Inventar
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (event.getClickedInventory() == player.getInventory()) {
                plugin.getItemManager().foundItem(player, cursor.getType());
            }
        }
        
        // Swap mit Slot
        if (current != null && current.getType() != Material.AIR) {
            if (event.getClickedInventory() != player.getInventory()) {
                // Item aus Container aufgenommen
                plugin.getItemManager().foundItem(player, current.getType());
            }
        }
    }
}

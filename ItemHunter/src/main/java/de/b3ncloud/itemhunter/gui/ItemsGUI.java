package de.b3ncloud.itemhunter.gui;

import de.b3ncloud.itemhunter.ItemHunter;
import de.b3ncloud.itemhunter.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI für die Anzeige der offenen und gefundenen Items
 */
public class ItemsGUI implements Listener {

    private final ItemHunter plugin;
    
    private static final int ITEMS_PER_PAGE = 45;
    private static final String GUI_TITLE_PREFIX = "🎯 Item Hunt - ";
    
    public ItemsGUI(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    // ============ GUI ÖFFNEN ============
    
    public void openRemainingItems(Player player, int page) {
        ItemManager itemManager = plugin.getItemManager();
        List<Material> remaining = itemManager.getRemainingItemsSorted();
        
        int totalPages = (int) Math.ceil((double) remaining.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        String title = GUI_TITLE_PREFIX + "Offen (" + (page + 1) + "/" + totalPages + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Items anzeigen
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, remaining.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Material material = remaining.get(i);
            ItemStack item = createItemDisplay(material, false, null);
            gui.setItem(i - startIndex, item);
        }
        
        // Navigation Bar
        addNavigationBar(gui, page, totalPages, "remaining");
        
        // Info Item
        gui.setItem(49, createInfoItem(remaining.size(), itemManager.getTotalItems(), false));
        
        player.openInventory(gui);
    }
    
    public void openFoundItems(Player player, int page) {
        ItemManager itemManager = plugin.getItemManager();
        List<Material> found = new ArrayList<>(itemManager.getFoundItems());
        
        int totalPages = (int) Math.ceil((double) found.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        String title = GUI_TITLE_PREFIX + "Gefunden (" + (page + 1) + "/" + totalPages + ")";
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Items anzeigen
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, found.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Material material = found.get(i);
            String finder = itemManager.getItemFinder(material);
            ItemStack item = createItemDisplay(material, true, finder);
            gui.setItem(i - startIndex, item);
        }
        
        // Navigation Bar
        addNavigationBar(gui, page, totalPages, "found");
        
        // Info Item
        gui.setItem(49, createInfoItem(found.size(), itemManager.getTotalItems(), true));
        
        player.openInventory(gui);
    }
    
    public void openOverview(Player player) {
        ItemManager itemManager = plugin.getItemManager();
        
        String title = GUI_TITLE_PREFIX + "Übersicht";
        Inventory gui = Bukkit.createInventory(null, 27, title);
        
        int found = itemManager.getFoundCount();
        int total = itemManager.getTotalItems();
        int remaining = itemManager.getRemainingCount();
        double progress = total > 0 ? (double) found / total * 100 : 0;
        
        // Statistik Item
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.GOLD + "📊 Statistiken");
        List<String> statsLore = new ArrayList<>();
        statsLore.add("");
        statsLore.add(ChatColor.GRAY + "Fortschritt: " + ChatColor.GREEN + String.format("%.1f", progress) + "%");
        statsLore.add(ChatColor.GRAY + "Gefunden: " + ChatColor.GREEN + found);
        statsLore.add(ChatColor.GRAY + "Offen: " + ChatColor.RED + remaining);
        statsLore.add(ChatColor.GRAY + "Gesamt: " + ChatColor.WHITE + total);
        statsLore.add("");
        statsLore.add(ChatColor.GRAY + "Zeit: " + ChatColor.AQUA + plugin.getTimerManager().getFormattedTime());
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        gui.setItem(13, statsItem);
        
        // Offene Items Button
        ItemStack remainingItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta remainingMeta = remainingItem.getItemMeta();
        remainingMeta.setDisplayName(ChatColor.RED + "❌ Offene Items");
        List<String> remainingLore = new ArrayList<>();
        remainingLore.add("");
        remainingLore.add(ChatColor.GRAY + "Noch " + ChatColor.WHITE + remaining + ChatColor.GRAY + " Items zu finden!");
        remainingLore.add("");
        remainingLore.add(ChatColor.YELLOW + "Klicken zum Anzeigen");
        remainingMeta.setLore(remainingLore);
        remainingItem.setItemMeta(remainingMeta);
        gui.setItem(11, remainingItem);
        
        // Gefundene Items Button
        ItemStack foundItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta foundMeta = foundItem.getItemMeta();
        foundMeta.setDisplayName(ChatColor.GREEN + "✓ Gefundene Items");
        List<String> foundLore = new ArrayList<>();
        foundLore.add("");
        foundLore.add(ChatColor.GRAY + "Bereits " + ChatColor.WHITE + found + ChatColor.GRAY + " Items gefunden!");
        foundLore.add("");
        foundLore.add(ChatColor.YELLOW + "Klicken zum Anzeigen");
        foundMeta.setLore(foundLore);
        foundItem.setItemMeta(foundMeta);
        gui.setItem(15, foundItem);
        
        player.openInventory(gui);
    }
    
    // ============ GUI ITEMS ERSTELLEN ============
    
    private ItemStack createItemDisplay(Material material, boolean found, String finder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = ItemManager.formatItemName(material);
        meta.setDisplayName((found ? ChatColor.GREEN + "✓ " : ChatColor.RED + "❌ ") + 
                ChatColor.WHITE + displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Material: " + ChatColor.WHITE + material.name());
        
        if (found && finder != null) {
            lore.add("");
            if (finder.equals("SKIP")) {
                lore.add(ChatColor.YELLOW + "Übersprungen");
            } else {
                lore.add(ChatColor.GRAY + "Gefunden von: " + ChatColor.GREEN + finder);
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationBar(Inventory gui, int currentPage, int totalPages, String type) {
        // Vorherige Seite
        if (currentPage > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "◀ Vorherige Seite");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Seite " + currentPage + "/" + totalPages);
            prevMeta.setLore(lore);
            prevItem.setItemMeta(prevMeta);
            gui.setItem(45, prevItem);
        }
        
        // Nächste Seite
        if (currentPage < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Nächste Seite ▶");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Seite " + (currentPage + 2) + "/" + totalPages);
            nextMeta.setLore(lore);
            nextItem.setItemMeta(nextMeta);
            gui.setItem(53, nextItem);
        }
        
        // Zurück Button
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "✕ Zurück zur Übersicht");
        backItem.setItemMeta(backMeta);
        gui.setItem(49, backItem);
        
        // Toggle Button (zwischen Offen/Gefunden wechseln)
        Material toggleMat = type.equals("remaining") ? Material.LIME_DYE : Material.RED_DYE;
        String toggleName = type.equals("remaining") ? 
                ChatColor.GREEN + "→ Gefundene Items anzeigen" : 
                ChatColor.RED + "→ Offene Items anzeigen";
        
        ItemStack toggleItem = new ItemStack(toggleMat);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        toggleMeta.setDisplayName(toggleName);
        toggleItem.setItemMeta(toggleMeta);
        gui.setItem(51, toggleItem);
    }
    
    private ItemStack createInfoItem(int count, int total, boolean isFound) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "📋 Info");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (isFound) {
            lore.add(ChatColor.GREEN + "Gefundene Items: " + ChatColor.WHITE + count);
        } else {
            lore.add(ChatColor.RED + "Offene Items: " + ChatColor.WHITE + count);
        }
        lore.add(ChatColor.GRAY + "Gesamt: " + ChatColor.WHITE + total);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Klicke auf Barrier für Übersicht");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    // ============ EVENT HANDLER ============
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        
        String displayName = meta.getDisplayName();
        
        // Übersicht GUI
        if (title.contains("Übersicht")) {
            if (displayName.contains("Offene Items")) {
                openRemainingItems(player, 0);
            } else if (displayName.contains("Gefundene Items")) {
                openFoundItems(player, 0);
            }
            return;
        }
        
        // Navigation in Item-Listen
        int currentPage = extractPageFromTitle(title);
        
        if (displayName.contains("Vorherige Seite")) {
            if (title.contains("Offen")) {
                openRemainingItems(player, currentPage - 1);
            } else {
                openFoundItems(player, currentPage - 1);
            }
        } else if (displayName.contains("Nächste Seite")) {
            if (title.contains("Offen")) {
                openRemainingItems(player, currentPage + 1);
            } else {
                openFoundItems(player, currentPage + 1);
            }
        } else if (displayName.contains("Zurück zur Übersicht")) {
            openOverview(player);
        } else if (displayName.contains("Gefundene Items anzeigen")) {
            openFoundItems(player, 0);
        } else if (displayName.contains("Offene Items anzeigen")) {
            openRemainingItems(player, 0);
        }
    }
    
    private int extractPageFromTitle(String title) {
        try {
            // Format: "🎯 Item Hunt - Offen (1/5)"
            int start = title.lastIndexOf("(") + 1;
            int end = title.indexOf("/");
            if (start > 0 && end > start) {
                return Integer.parseInt(title.substring(start, end)) - 1;
            }
        } catch (Exception e) {
            // Ignorieren
        }
        return 0;
    }
}

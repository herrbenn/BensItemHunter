package de.b3ncloud.itemhunter.gui;

import de.b3ncloud.itemhunter.ItemHunter;
import de.b3ncloud.itemhunter.managers.AchievementManager;
import de.b3ncloud.itemhunter.managers.ItemManager;
import de.b3ncloud.itemhunter.managers.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Erweiterte GUI mit Tabs für Items, Mobs und Achievements
 */
public class ChallengeGUI implements Listener {

    private final ItemHunter plugin;
    
    private static final int ITEMS_PER_PAGE = 36; // 4 Reihen für Content
    private static final String GUI_TITLE = "§6§l✦ Challenge Progress ✦";
    
    // Speichert die aktuelle Seite und den Tab pro Spieler
    private Map<Player, Integer> playerPages = new HashMap<>();
    private Map<Player, TabType> playerTabs = new HashMap<>();
    
    public enum TabType {
        OVERVIEW,
        ITEMS,
        MOBS,
        ACHIEVEMENTS
    }
    
    public ChallengeGUI(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    // ============ HAUPTMENÜ ÖFFNEN ============
    
    public void openGUI(Player player) {
        openOverview(player);
    }
    
    public void openOverview(Player player) {
        playerTabs.put(player, TabType.OVERVIEW);
        
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Hintergrund
        fillBackground(gui, Material.BLACK_STAINED_GLASS_PANE);
        
        // Header
        gui.setItem(4, createHeader());
        
        // ============ STATISTIK BEREICH ============
        
        // Items Karte
        int foundItems = plugin.getItemManager().getFoundCount();
        int totalItems = plugin.getItemManager().getTotalItems();
        double itemProgress = totalItems > 0 ? (double) foundItems / totalItems * 100 : 0;
        gui.setItem(20, createCategoryCard(
                Material.CHEST,
                ChatColor.GREEN + "§l📦 Items",
                foundItems, totalItems, itemProgress,
                ChatColor.GREEN,
                "Sammle alle Items im Spiel!"
        ));
        
        // Mobs Karte
        int killedMobs = plugin.getMobManager().getKilledCount();
        int totalMobs = plugin.getMobManager().getTotalMobs();
        double mobProgress = totalMobs > 0 ? (double) killedMobs / totalMobs * 100 : 0;
        gui.setItem(22, createCategoryCard(
                Material.DIAMOND_SWORD,
                ChatColor.RED + "§l☠ Mobs",
                killedMobs, totalMobs, mobProgress,
                ChatColor.RED,
                "Töte jeden Mob mindestens einmal!"
        ));
        
        // Achievements Karte
        int completedAdv = plugin.getAchievementManager().getCompletedCount();
        int totalAdv = plugin.getAchievementManager().getTotalAdvancements();
        double advProgress = totalAdv > 0 ? (double) completedAdv / totalAdv * 100 : 0;
        gui.setItem(24, createCategoryCard(
                Material.GOLDEN_APPLE,
                ChatColor.LIGHT_PURPLE + "§l★ Achievements",
                completedAdv, totalAdv, advProgress,
                ChatColor.LIGHT_PURPLE,
                "Erreiche alle Advancements!"
        ));
        
        // ============ GESAMTFORTSCHRITT ============
        
        int totalFound = foundItems + killedMobs + completedAdv;
        int totalRequired = totalItems + totalMobs + totalAdv;
        double totalProgress = totalRequired > 0 ? (double) totalFound / totalRequired * 100 : 0;
        
        gui.setItem(40, createTotalProgress(totalFound, totalRequired, totalProgress));
        
        // ============ TIMER ============
        
        gui.setItem(49, createTimerDisplay());
        
        player.openInventory(gui);
    }
    
    // ============ ITEMS TAB ============
    
    public void openItemsTab(Player player, int page) {
        playerTabs.put(player, TabType.ITEMS);
        playerPages.put(player, page);
        
        List<Material> remaining = plugin.getItemManager().getRemainingItemsSorted();
        int totalPages = Math.max(1, (int) Math.ceil((double) remaining.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Hintergrund für Navigation
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, createGlassPane(Material.GREEN_STAINED_GLASS_PANE, " "));
        }
        
        // Tab-Leiste (Zeile 0)
        addTabBar(gui, TabType.ITEMS);
        
        // Items anzeigen (Zeile 1-4, Slots 9-44)
        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < remaining.size(); i++) {
            Material material = remaining.get(startIndex + i);
            gui.setItem(9 + i, createItemDisplay(material));
        }
        
        // Navigation
        addNavigation(gui, page, totalPages);
        
        // Info
        int found = plugin.getItemManager().getFoundCount();
        int total = plugin.getItemManager().getTotalItems();
        gui.setItem(49, createInfoDisplay(
                ChatColor.GREEN + "Items: " + found + "/" + total,
                remaining.size() + " noch zu finden"
        ));
        
        player.openInventory(gui);
    }
    
    // ============ MOBS TAB ============
    
    public void openMobsTab(Player player, int page) {
        playerTabs.put(player, TabType.MOBS);
        playerPages.put(player, page);
        
        List<EntityType> remaining = plugin.getMobManager().getRemainingMobsSorted();
        int totalPages = Math.max(1, (int) Math.ceil((double) remaining.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Hintergrund für Navigation
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, createGlassPane(Material.RED_STAINED_GLASS_PANE, " "));
        }
        
        // Tab-Leiste
        addTabBar(gui, TabType.MOBS);
        
        // Mobs anzeigen
        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < remaining.size(); i++) {
            EntityType type = remaining.get(startIndex + i);
            gui.setItem(9 + i, createMobDisplay(type));
        }
        
        // Navigation
        addNavigation(gui, page, totalPages);
        
        // Info
        int killed = plugin.getMobManager().getKilledCount();
        int total = plugin.getMobManager().getTotalMobs();
        gui.setItem(49, createInfoDisplay(
                ChatColor.RED + "Mobs: " + killed + "/" + total,
                remaining.size() + " noch zu töten"
        ));
        
        player.openInventory(gui);
    }
    
    // ============ ACHIEVEMENTS TAB ============
    
    public void openAchievementsTab(Player player, int page) {
        playerTabs.put(player, TabType.ACHIEVEMENTS);
        playerPages.put(player, page);
        
        List<NamespacedKey> remaining = plugin.getAchievementManager().getRemainingAdvancementsSorted();
        int totalPages = Math.max(1, (int) Math.ceil((double) remaining.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        // Hintergrund für Navigation
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, createGlassPane(Material.MAGENTA_STAINED_GLASS_PANE, " "));
        }
        
        // Tab-Leiste
        addTabBar(gui, TabType.ACHIEVEMENTS);
        
        // Achievements anzeigen
        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < remaining.size(); i++) {
            NamespacedKey key = remaining.get(startIndex + i);
            gui.setItem(9 + i, createAchievementDisplay(key));
        }
        
        // Navigation
        addNavigation(gui, page, totalPages);
        
        // Info
        int completed = plugin.getAchievementManager().getCompletedCount();
        int total = plugin.getAchievementManager().getTotalAdvancements();
        gui.setItem(49, createInfoDisplay(
                ChatColor.LIGHT_PURPLE + "Achievements: " + completed + "/" + total,
                remaining.size() + " noch zu erreichen"
        ));
        
        player.openInventory(gui);
    }
    
    // ============ GUI ELEMENTE ============
    
    private void addTabBar(Inventory gui, TabType activeTab) {
        // Übersicht
        gui.setItem(0, createTabButton(
                Material.COMPASS,
                ChatColor.GOLD + "§l⌂ Übersicht",
                activeTab == TabType.OVERVIEW
        ));
        
        // Items Tab
        int itemsRemaining = plugin.getItemManager().getRemainingCount();
        gui.setItem(2, createTabButton(
                Material.CHEST,
                ChatColor.GREEN + "§l📦 Items " + ChatColor.GRAY + "(" + itemsRemaining + ")",
                activeTab == TabType.ITEMS
        ));
        
        // Mobs Tab
        int mobsRemaining = plugin.getMobManager().getRemainingCount();
        gui.setItem(4, createTabButton(
                Material.DIAMOND_SWORD,
                ChatColor.RED + "§l☠ Mobs " + ChatColor.GRAY + "(" + mobsRemaining + ")",
                activeTab == TabType.MOBS
        ));
        
        // Achievements Tab
        int advRemaining = plugin.getAchievementManager().getRemainingCount();
        gui.setItem(6, createTabButton(
                Material.GOLDEN_APPLE,
                ChatColor.LIGHT_PURPLE + "§l★ Achievements " + ChatColor.GRAY + "(" + advRemaining + ")",
                activeTab == TabType.ACHIEVEMENTS
        ));
        
        // Schließen
        gui.setItem(8, createCloseButton());
    }
    
    private void addNavigation(Inventory gui, int currentPage, int totalPages) {
        // Vorherige Seite
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "◀ Seite " + currentPage);
            prev.setItemMeta(meta);
            gui.setItem(45, prev);
        }
        
        // Seitenanzeige
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName(ChatColor.WHITE + "Seite " + (currentPage + 1) + "/" + totalPages);
        pageInfo.setItemMeta(pageMeta);
        gui.setItem(49, pageInfo);
        
        // Nächste Seite
        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Seite " + (currentPage + 2) + " ▶");
            next.setItemMeta(meta);
            gui.setItem(53, next);
        }
    }
    
    private ItemStack createHeader() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "§l✦ Challenge Progress ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Sammle alle Items, töte alle Mobs");
        lore.add(ChatColor.GRAY + "und erreiche alle Achievements!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCategoryCard(Material material, String name, int current, int total, double progress, ChatColor color, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(color + "" + current + ChatColor.GRAY + "/" + ChatColor.WHITE + total);
        lore.add(getProgressBar(progress, 20, color));
        lore.add(ChatColor.YELLOW + String.format("%.1f", progress) + "%");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "» Klicken für Details");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTotalProgress(int current, int total, double progress) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "§l⚡ Gesamtfortschritt");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.WHITE + "" + current + ChatColor.GRAY + "/" + ChatColor.WHITE + total + ChatColor.GRAY + " abgeschlossen");
        lore.add("");
        lore.add(getProgressBar(progress, 25, ChatColor.GOLD));
        lore.add(ChatColor.GOLD + "§l" + String.format("%.2f", progress) + "%");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTimerDisplay() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        boolean active = plugin.getTimerManager().isActive();
        boolean paused = plugin.getTimerManager().isPaused();
        String time = plugin.getTimerManager().getFormattedTime();
        
        if (active) {
            meta.setDisplayName(ChatColor.AQUA + "⏱ " + time + (paused ? ChatColor.RED + " (PAUSIERT)" : ""));
        } else {
            meta.setDisplayName(ChatColor.GRAY + "⏱ Challenge nicht gestartet");
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (active) {
            lore.add(ChatColor.GRAY + "Challenge läuft seit");
            lore.add(ChatColor.WHITE + time);
            if (paused) {
                lore.add("");
                lore.add(ChatColor.RED + "Timer ist pausiert!");
            }
        } else {
            lore.add(ChatColor.GRAY + "Starte mit " + ChatColor.WHITE + "/itemhunt start");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTabButton(Material material, String name, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((active ? ChatColor.WHITE + "▶ " : "") + name);
        
        if (active) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Aktueller Tab");
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "✕ Schließen");
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createItemDisplay(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = ItemManager.formatItemName(material);
        meta.setDisplayName(ChatColor.RED + "❌ " + ChatColor.WHITE + name);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "ID: " + ChatColor.DARK_GRAY + material.name());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Noch nicht gefunden!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createMobDisplay(EntityType type) {
        // Versuche passendes Material für den Mob zu finden
        Material material = getMobMaterial(type);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = MobManager.formatMobName(type);
        meta.setDisplayName(ChatColor.RED + "☠ " + ChatColor.WHITE + name);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Typ: " + ChatColor.DARK_GRAY + type.name());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Noch nicht getötet!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAchievementDisplay(NamespacedKey key) {
        // Material basierend auf Kategorie
        Material material = getAchievementMaterial(key);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = AchievementManager.formatAdvancementName(key);
        String category = AchievementManager.getCategory(key);
        
        meta.setDisplayName(ChatColor.RED + "★ " + ChatColor.WHITE + name);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Kategorie: " + ChatColor.LIGHT_PURPLE + category);
        lore.add(ChatColor.DARK_GRAY + key.toString());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Noch nicht erreicht!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createInfoDisplay(String title, String subtitle) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + subtitle);
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillBackground(Inventory gui, Material material) {
        ItemStack filler = createGlassPane(material, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }
    }
    
    private String getProgressBar(double percentage, int length, ChatColor color) {
        int filled = (int) (percentage / 100 * length);
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.DARK_GRAY).append("[");
        bar.append(color);
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append(ChatColor.GRAY);
        for (int i = 0; i < empty; i++) bar.append("░");
        bar.append(ChatColor.DARK_GRAY).append("]");
        
        return bar.toString();
    }
    
    private Material getMobMaterial(EntityType type) {
        // Spawn Egg für den Mob zurückgeben
        // Spezialfälle ohne Spawn Egg
        switch (type) {
            case ENDER_DRAGON:
                return Material.DRAGON_EGG;
            case WITHER:
                return Material.NETHER_STAR;
            case IRON_GOLEM:
                return Material.IRON_GOLEM_SPAWN_EGG;
            case SNOW_GOLEM:
                return Material.SNOW_GOLEM_SPAWN_EGG;
            case GIANT:
                return Material.ZOMBIE_SPAWN_EGG; // Giant hat kein Spawn Egg
            case ILLUSIONER:
                return Material.PILLAGER_SPAWN_EGG; // Illusioner hat kein Spawn Egg
            default:
                // Versuche automatisch das Spawn Egg zu finden
                try {
                    String eggName = type.name() + "_SPAWN_EGG";
                    return Material.valueOf(eggName);
                } catch (IllegalArgumentException e) {
                    // Fallback wenn kein Spawn Egg existiert
                    return Material.CREEPER_SPAWN_EGG;
                }
        }
    }
    
    private Material getAchievementMaterial(NamespacedKey key) {
        String path = key.getKey();
        if (path.startsWith("story/")) return Material.GRASS_BLOCK;
        if (path.startsWith("nether/")) return Material.NETHERRACK;
        if (path.startsWith("end/")) return Material.END_STONE;
        if (path.startsWith("adventure/")) return Material.MAP;
        if (path.startsWith("husbandry/")) return Material.WHEAT;
        return Material.BOOK;
    }
    
    // ============ EVENT HANDLER ============
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        String name = meta.getDisplayName();
        int slot = event.getSlot();
        
        TabType currentTab = playerTabs.getOrDefault(player, TabType.OVERVIEW);
        int currentPage = playerPages.getOrDefault(player, 0);
        
        // Tab-Leiste (Slots 0-8)
        if (slot <= 8) {
            if (slot == 0 || name.contains("Übersicht")) {
                openOverview(player);
            } else if (slot == 2 || name.contains("Items")) {
                openItemsTab(player, 0);
            } else if (slot == 4 || name.contains("Mobs")) {
                openMobsTab(player, 0);
            } else if (slot == 6 || name.contains("Achievements")) {
                openAchievementsTab(player, 0);
            } else if (slot == 8 || name.contains("Schließen")) {
                player.closeInventory();
            }
            return;
        }
        
        // Übersicht - Klick auf Kategorie-Karten
        if (currentTab == TabType.OVERVIEW) {
            if (slot == 20) { // Items
                openItemsTab(player, 0);
            } else if (slot == 22) { // Mobs
                openMobsTab(player, 0);
            } else if (slot == 24) { // Achievements
                openAchievementsTab(player, 0);
            }
            return;
        }
        
        // Navigation (Slots 45-53)
        if (slot >= 45) {
            if (name.contains("◀")) {
                // Vorherige Seite
                switch (currentTab) {
                    case ITEMS: openItemsTab(player, currentPage - 1); break;
                    case MOBS: openMobsTab(player, currentPage - 1); break;
                    case ACHIEVEMENTS: openAchievementsTab(player, currentPage - 1); break;
                    default: break;
                }
            } else if (name.contains("▶")) {
                // Nächste Seite
                switch (currentTab) {
                    case ITEMS: openItemsTab(player, currentPage + 1); break;
                    case MOBS: openMobsTab(player, currentPage + 1); break;
                    case ACHIEVEMENTS: openAchievementsTab(player, currentPage + 1); break;
                    default: break;
                }
            }
        }
    }
}

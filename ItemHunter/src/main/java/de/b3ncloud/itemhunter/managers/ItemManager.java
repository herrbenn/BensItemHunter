package de.b3ncloud.itemhunter.managers;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verwaltet alle Items und den Fortschritt der Challenge
 */
public class ItemManager {

    private final ItemHunter plugin;
    
    // Alle Items die gefunden werden müssen
    private Set<Material> requiredItems = new LinkedHashSet<>();
    
    // Bereits gefundene Items
    private Set<Material> foundItems = new LinkedHashSet<>();
    
    // Ausgeschlossene Items aus Config
    private Set<Material> excludedItems = new HashSet<>();
    
    // Wer hat welches Item gefunden?
    private Map<Material, String> itemFinder = new HashMap<>();
    
    private File progressFile;
    private FileConfiguration progressConfig;
    
    public ItemManager(ItemHunter plugin) {
        this.plugin = plugin;
        this.progressFile = new File(plugin.getDataFolder(), "progress.yml");
        
        loadExcludedItems();
        initializeRequiredItems();
    }
    
    private void loadExcludedItems() {
        List<String> excluded = plugin.getConfig().getStringList("excluded-items");
        for (String itemName : excluded) {
            try {
                Material mat = Material.valueOf(itemName.toUpperCase());
                excludedItems.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unbekanntes Item in Ausschlussliste: " + itemName);
            }
        }
    }
    
    /**
     * Öffentliche Methode zum Initialisieren/Neuinitialisieren der Items
     */
    public void initializeItems() {
        foundItems.clear();
        itemFinder.clear();
        initializeRequiredItems();
    }
    
    private void initializeRequiredItems() {
        requiredItems.clear();
        
        for (Material material : Material.values()) {
            // Nur tatsächliche Items (keine Luft, keine Legacy, keine unobtainables)
            if (isValidItem(material)) {
                requiredItems.add(material);
            }
        }
        
        plugin.getLogger().info("Initialisiert mit " + requiredItems.size() + " Items");
    }
    
    private boolean isValidItem(Material material) {
        // Ausgeschlossene Items aus Config
        if (excludedItems.contains(material)) return false;
        
        // Keine Legacy Items
        if (material.isLegacy()) return false;
        
        // Muss ein Item sein (nicht nur Block)
        if (!material.isItem()) return false;
        
        // Keine Luft
        if (material.isAir()) return false;
        
        String name = material.name();
        
        // ============ SPAWN EGGS - Alle ausschließen ============
        if (name.endsWith("_SPAWN_EGG")) return false;
        
        // ============ UNOBTAINABLE / CREATIVE-ONLY ITEMS ============
        
        // Wand-Varianten (droppen als normale Items)
        if (name.contains("WALL_") && !name.equals("WALL_TORCH") && !name.equals("SOUL_WALL_TORCH") && !name.equals("REDSTONE_WALL_TORCH")) return false;
        if (name.startsWith("WALL_")) return false;
        
        // Topf-Varianten (droppen Topf + Pflanze separat)
        if (name.contains("POTTED_")) return false;
        
        // Kuchen mit Kerzen (unobtainable)
        if (name.contains("CANDLE_CAKE")) return false;
        
        // Attached Blocks
        if (name.startsWith("ATTACHED_")) return false;
        
        // ============ TECHNISCHE BLÖCKE ============
        
        // Luft-Varianten
        if (name.equals("AIR") || name.equals("CAVE_AIR") || name.equals("VOID_AIR")) return false;
        
        // Portale (nicht als Items erhältlich)
        if (name.equals("NETHER_PORTAL") || name.equals("END_PORTAL") || name.equals("END_GATEWAY")) return false;
        
        // Piston-Teile
        if (name.equals("MOVING_PISTON") || name.equals("PISTON_HEAD")) return false;
        
        // Feuer (nur Fire Charge ist ein Item)
        if (name.equals("FIRE") || name.equals("SOUL_FIRE")) return false;
        
        // Wasser/Lava
        if (name.equals("WATER") || name.equals("LAVA")) return false;
        if (name.equals("BUBBLE_COLUMN")) return false;
        
        // Eis-Varianten
        if (name.equals("FROSTED_ICE")) return false;
        
        // Redstone-Komponenten (technische Formen)
        if (name.equals("REDSTONE_WIRE")) return false;
        if (name.equals("TRIPWIRE")) return false;
        
        // ============ PFLANZENSTUFEN / CROPS ============
        
        // Crops die nur als Seeds droppen
        if (name.equals("WHEAT") && !name.equals("WHEAT_SEEDS")) return false; // Der Block, nicht das Item
        if (name.equals("CARROTS")) return false; // Block-Form
        if (name.equals("POTATOES")) return false; // Block-Form
        if (name.equals("BEETROOTS")) return false; // Block-Form
        if (name.equals("SWEET_BERRY_BUSH")) return false;
        if (name.equals("MELON_STEM") || name.equals("PUMPKIN_STEM")) return false;
        if (name.equals("ATTACHED_MELON_STEM") || name.equals("ATTACHED_PUMPKIN_STEM")) return false;
        if (name.equals("COCOA")) return false;
        if (name.equals("BAMBOO_SAPLING")) return false;
        if (name.equals("TORCHFLOWER_CROP") || name.equals("PITCHER_CROP")) return false;
        
        // Pflanzen-Stängel
        if (name.equals("KELP_PLANT")) return false;
        if (name.equals("TWISTING_VINES_PLANT") || name.equals("WEEPING_VINES_PLANT")) return false;
        if (name.equals("CAVE_VINES") || name.equals("CAVE_VINES_PLANT")) return false;
        if (name.equals("BIG_DRIPLEAF_STEM")) return false;
        
        // Tall Seagrass (droppt Seagrass)
        if (name.equals("TALL_SEAGRASS")) return false;
        
        // ============ CAULDRONS (nur leerer Cauldron ist Item) ============
        if (name.equals("WATER_CAULDRON") || name.equals("LAVA_CAULDRON") || name.equals("POWDER_SNOW_CAULDRON")) return false;
        
        // ============ HEADS (nur Mob-Heads, nicht Player) ============
        if (name.equals("PLAYER_HEAD") || name.equals("PLAYER_WALL_HEAD")) return false;
        
        // ============ BANNER-PATTERNS (nur craftbare) ============
        // Die meisten Banner Patterns sind OK, aber manche sind event-only
        
        // ============ WEITERE UNOBTAINABLE ITEMS ============
        
        // Powder Snow (nur mit Eimer erhältlich, Block selbst nicht)
        if (name.equals("POWDER_SNOW")) return false;
        
        // Licht-Block
        if (name.equals("LIGHT")) return false;
        
        // Petrified Oak Slab (Legacy, nicht mehr erhältlich)
        if (name.equals("PETRIFIED_OAK_SLAB")) return false;
        
        // Knowledge Book (nur via Commands)
        if (name.equals("KNOWLEDGE_BOOK")) return false;
        
        // Debug Stick
        if (name.equals("DEBUG_STICK")) return false;
        
        // Barrier
        if (name.equals("BARRIER")) return false;
        
        // Command Blocks
        if (name.contains("COMMAND_BLOCK")) return false;
        
        // Structure Blocks
        if (name.equals("STRUCTURE_BLOCK") || name.equals("STRUCTURE_VOID") || name.equals("JIGSAW")) return false;
        
        // Spawner & Vault
        if (name.equals("SPAWNER") || name.equals("TRIAL_SPAWNER") || name.equals("VAULT")) return false;
        
        // Bedrock & Reinforced Deepslate
        if (name.equals("BEDROCK") || name.equals("REINFORCED_DEEPSLATE")) return false;
        
        // End Portal Frame
        if (name.equals("END_PORTAL_FRAME")) return false;
        
        // Budding Amethyst (bricht ohne Silk Touch)
        if (name.equals("BUDDING_AMETHYST")) return false;
        
        // Infested Blocks (droppen Silverfish, nicht den Block)
        if (name.startsWith("INFESTED_")) return false;
        
        // Frogspawn (unobtainable as item)
        if (name.equals("FROGSPAWN")) return false;
        
        // Bundle (noch nicht vollständig im Spiel)
        if (name.equals("BUNDLE")) return false;
        
        // Written Book (benötigt Spieler-Input)
        if (name.equals("WRITTEN_BOOK")) return false;
        
        // Suspicious Blocks (droppen Loot, nicht sich selbst)
        if (name.equals("SUSPICIOUS_SAND") || name.equals("SUSPICIOUS_GRAVEL")) return false;
        
        // ============ 1.21+ ITEMS ============
        
        // Ominous Items (nur via Trials/Events)
        if (name.equals("OMINOUS_TRIAL_KEY") || name.equals("OMINOUS_BOTTLE")) return false;
        
        // Trial Chambers spezifische Items
        if (name.equals("TRIAL_KEY")) return false; // Optional - kann man finden
        
        // ============ TEST / DEBUG BLOCKS ============
        if (name.equals("TEST_BLOCK") || name.equals("TEST_INSTANCE_BLOCK")) return false;
        
        // ============ DIRT PATH ============
        // Dirt Path kann nicht mit Silk Touch abgebaut werden, wird zu Dirt
        if (name.equals("DIRT_PATH")) return false;
        
        return true;
    }
    
    // ============ ITEM FINDEN ============
    
    public boolean foundItem(Player player, Material material) {
        if (!requiredItems.contains(material)) return false;
        if (foundItems.contains(material)) return false;
        
        foundItems.add(material);
        itemFinder.put(material, player.getName());
        
        // Fortschritt berechnen
        int found = foundItems.size();
        int total = requiredItems.size();
        double progress = (double) found / total * 100;
        
        // Benachrichtigung
        String itemName = formatItemName(material);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GREEN + "✓ " + ChatColor.WHITE + player.getName() + 
                ChatColor.GRAY + " hat " + ChatColor.GOLD + itemName + ChatColor.GRAY + " gefunden!");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GRAY + "Fortschritt: " + 
                ChatColor.GREEN + found + ChatColor.GRAY + "/" + ChatColor.WHITE + total + 
                ChatColor.YELLOW + " (" + String.format("%.1f", progress) + "%)");
        Bukkit.broadcastMessage("");
        
        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
        
        // Challenge komplett?
        if (found >= total) {
            challengeComplete();
        }
        
        // BossBar aktualisieren
        plugin.updateBossBar();
        
        return true;
    }
    
    private void challengeComplete() {
        String time = plugin.getTimerManager().getFormattedTime();
        
        // Timer stoppen
        plugin.getTimerManager().stop();
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        Bukkit.broadcastMessage(ChatColor.GREEN + "    🎉 " + ChatColor.BOLD + "CHALLENGE GESCHAFFT!" + ChatColor.GREEN + " 🎉");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.WHITE + "    Alle " + ChatColor.GOLD + getTotalItems() + ChatColor.WHITE + " Items wurden gefunden!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "    Zeit: " + ChatColor.AQUA + time);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        Bukkit.broadcastMessage("");
        
        // Feuerwerk & Sound
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Titel anzeigen
            player.sendTitle(
                    ChatColor.GREEN + "🎉 GESCHAFFT! 🎉",
                    ChatColor.WHITE + "Alle Items in " + ChatColor.AQUA + time,
                    10, 100, 20
            );
        }
    }
    
    // ============ ITEM INFOS ============
    
    public int getTotalItems() {
        return requiredItems.size();
    }
    
    public int getFoundCount() {
        return foundItems.size();
    }
    
    public int getRemainingCount() {
        return requiredItems.size() - foundItems.size();
    }
    
    public Set<Material> getFoundItems() {
        return new LinkedHashSet<>(foundItems);
    }
    
    public Set<Material> getRemainingItems() {
        Set<Material> remaining = new LinkedHashSet<>(requiredItems);
        remaining.removeAll(foundItems);
        return remaining;
    }
    
    public List<Material> getRemainingItemsSorted() {
        return getRemainingItems().stream()
                .sorted(Comparator.comparing(Material::name))
                .collect(Collectors.toList());
    }
    
    public String getItemFinder(Material material) {
        return itemFinder.getOrDefault(material, "Unbekannt");
    }
    
    public boolean isRequired(Material material) {
        return requiredItems.contains(material);
    }
    
    public boolean isFound(Material material) {
        return foundItems.contains(material);
    }
    
    // ============ RESET ============
    
    public void reset() {
        foundItems.clear();
        itemFinder.clear();
        
        // Progress-Datei löschen
        if (progressFile.exists()) {
            progressFile.delete();
        }
        
        plugin.getLogger().info("Item-Fortschritt zurückgesetzt");
    }
    
    public void skipItem(Material material) {
        if (!requiredItems.contains(material)) return;
        if (foundItems.contains(material)) return;
        
        foundItems.add(material);
        itemFinder.put(material, "SKIP");
        
        plugin.updateBossBar();
    }
    
    // ============ SPEICHERN & LADEN ============
    
    public void saveProgress() {
        progressConfig = new YamlConfiguration();
        
        // Gefundene Items speichern
        List<String> foundList = foundItems.stream()
                .map(Material::name)
                .collect(Collectors.toList());
        progressConfig.set("found-items", foundList);
        
        // Wer hat was gefunden
        for (Map.Entry<Material, String> entry : itemFinder.entrySet()) {
            progressConfig.set("finders." + entry.getKey().name(), entry.getValue());
        }
        
        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte Fortschritt nicht speichern!");
            e.printStackTrace();
        }
    }
    
    public void loadProgress() {
        if (!progressFile.exists()) return;
        
        progressConfig = YamlConfiguration.loadConfiguration(progressFile);
        
        // Gefundene Items laden
        List<String> foundList = progressConfig.getStringList("found-items");
        for (String itemName : foundList) {
            try {
                Material mat = Material.valueOf(itemName);
                if (requiredItems.contains(mat)) {
                    foundItems.add(mat);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unbekanntes Item in Fortschritt: " + itemName);
            }
        }
        
        // Finder laden
        if (progressConfig.contains("finders")) {
            for (String key : progressConfig.getConfigurationSection("finders").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key);
                    String finder = progressConfig.getString("finders." + key);
                    itemFinder.put(mat, finder);
                } catch (IllegalArgumentException e) {
                    // Ignorieren
                }
            }
        }
        
        plugin.getLogger().info("Fortschritt geladen: " + foundItems.size() + "/" + requiredItems.size() + " Items");
    }
    
    // ============ HILFSMETHODEN ============
    
    public static String formatItemName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        
        // Erster Buchstabe groß
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}

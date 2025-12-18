package de.b3ncloud.itemhunter.managers;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verwaltet alle Mobs die getötet werden müssen
 */
public class MobManager {

    private final ItemHunter plugin;
    
    // Alle Mobs die getötet werden müssen
    private Set<EntityType> requiredMobs = new LinkedHashSet<>();
    
    // Bereits getötete Mobs
    private Set<EntityType> killedMobs = new LinkedHashSet<>();
    
    // Wer hat welchen Mob getötet?
    private Map<EntityType, String> mobKiller = new HashMap<>();
    
    private File progressFile;
    private FileConfiguration progressConfig;
    
    public MobManager(ItemHunter plugin) {
        this.plugin = plugin;
        this.progressFile = new File(plugin.getDataFolder(), "mobs.yml");
        
        initializeRequiredMobs();
    }
    
    public void initializeMobs() {
        killedMobs.clear();
        mobKiller.clear();
        initializeRequiredMobs();
    }
    
    private void initializeRequiredMobs() {
        requiredMobs.clear();
        
        for (EntityType type : EntityType.values()) {
            if (isValidMob(type)) {
                requiredMobs.add(type);
            }
        }
        
        plugin.getLogger().info("Mobs initialisiert: " + requiredMobs.size() + " Mobs");
    }
    
    private boolean isValidMob(EntityType type) {
        // Muss ein lebendiges Wesen sein
        if (!type.isAlive()) return false;
        
        // Muss spawnable sein
        if (!type.isSpawnable()) return false;
        
        String name = type.name();
        
        // ============ SPIELER & MARKER ============
        if (type == EntityType.PLAYER) return false;
        if (name.equals("MARKER") || name.equals("INTERACTION")) return false;
        
        // ============ TECHNISCHE ENTITIES ============
        if (name.equals("ARMOR_STAND")) return false;
        if (name.equals("GIANT")) return false; // Nicht natürlich spawnbar
        if (name.equals("ILLUSIONER")) return false; // Nicht natürlich spawnbar
        if (name.equals("ZOMBIE_HORSE")) return false; // Nicht natürlich spawnbar
        
        // ============ NPC-ARTIGE ============
        // Villager und Wandering Trader sind OK - man kann sie töten
        
        // ============ BOSS-MOBS ============
        // Ender Dragon und Wither sind OK - gehören zur Challenge
        
        return true;
    }
    
    // ============ MOB TÖTEN ============
    
    public boolean killedMob(Player player, EntityType type) {
        if (!requiredMobs.contains(type)) return false;
        if (killedMobs.contains(type)) return false;
        
        killedMobs.add(type);
        mobKiller.put(type, player.getName());
        
        // Fortschritt
        int killed = killedMobs.size();
        int total = requiredMobs.size();
        double progress = (double) killed / total * 100;
        
        String mobName = formatMobName(type);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.RED + "☠ " + ChatColor.WHITE + player.getName() + 
                ChatColor.GRAY + " hat " + ChatColor.GOLD + mobName + ChatColor.GRAY + " getötet!");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GRAY + "Mobs: " + 
                ChatColor.RED + killed + ChatColor.GRAY + "/" + ChatColor.WHITE + total + 
                ChatColor.YELLOW + " (" + String.format("%.1f", progress) + "%)");
        Bukkit.broadcastMessage("");
        
        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        }
        
        // Challenge prüfen
        plugin.checkChallengeComplete();
        plugin.updateBossBar();
        
        return true;
    }
    
    // ============ GETTER ============
    
    public int getTotalMobs() {
        return requiredMobs.size();
    }
    
    public int getKilledCount() {
        return killedMobs.size();
    }
    
    public int getRemainingCount() {
        return requiredMobs.size() - killedMobs.size();
    }
    
    public Set<EntityType> getKilledMobs() {
        return new LinkedHashSet<>(killedMobs);
    }
    
    public Set<EntityType> getRemainingMobs() {
        Set<EntityType> remaining = new LinkedHashSet<>(requiredMobs);
        remaining.removeAll(killedMobs);
        return remaining;
    }
    
    public List<EntityType> getRemainingMobsSorted() {
        return getRemainingMobs().stream()
                .sorted(Comparator.comparing(EntityType::name))
                .collect(Collectors.toList());
    }
    
    public String getMobKiller(EntityType type) {
        return mobKiller.getOrDefault(type, "Unbekannt");
    }
    
    public boolean isRequired(EntityType type) {
        return requiredMobs.contains(type);
    }
    
    public boolean isKilled(EntityType type) {
        return killedMobs.contains(type);
    }
    
    // ============ RESET ============
    
    public void reset() {
        killedMobs.clear();
        mobKiller.clear();
        
        if (progressFile.exists()) {
            progressFile.delete();
        }
    }
    
    // ============ SPEICHERN & LADEN ============
    
    public void saveProgress() {
        progressConfig = new YamlConfiguration();
        
        List<String> killedList = killedMobs.stream()
                .map(EntityType::name)
                .collect(Collectors.toList());
        progressConfig.set("killed-mobs", killedList);
        
        for (Map.Entry<EntityType, String> entry : mobKiller.entrySet()) {
            progressConfig.set("killers." + entry.getKey().name(), entry.getValue());
        }
        
        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte Mob-Fortschritt nicht speichern!");
        }
    }
    
    public void loadProgress() {
        if (!progressFile.exists()) return;
        
        progressConfig = YamlConfiguration.loadConfiguration(progressFile);
        
        List<String> killedList = progressConfig.getStringList("killed-mobs");
        for (String mobName : killedList) {
            try {
                EntityType type = EntityType.valueOf(mobName);
                if (requiredMobs.contains(type)) {
                    killedMobs.add(type);
                }
            } catch (IllegalArgumentException e) {
                // Ignorieren
            }
        }
        
        if (progressConfig.contains("killers")) {
            for (String key : progressConfig.getConfigurationSection("killers").getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key);
                    String killer = progressConfig.getString("killers." + key);
                    mobKiller.put(type, killer);
                } catch (IllegalArgumentException e) {
                    // Ignorieren
                }
            }
        }
        
        plugin.getLogger().info("Mob-Fortschritt geladen: " + killedMobs.size() + "/" + requiredMobs.size());
    }
    
    // ============ HILFSMETHODEN ============
    
    public static String formatMobName(EntityType type) {
        String name = type.name().toLowerCase().replace("_", " ");
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

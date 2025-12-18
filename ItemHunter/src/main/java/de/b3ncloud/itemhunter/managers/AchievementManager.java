package de.b3ncloud.itemhunter.managers;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verwaltet alle Achievements/Advancements die erreicht werden müssen
 */
public class AchievementManager {

    private final ItemHunter plugin;
    
    // Alle Advancements die erreicht werden müssen
    private Set<NamespacedKey> requiredAdvancements = new LinkedHashSet<>();
    
    // Bereits erreichte Advancements
    private Set<NamespacedKey> completedAdvancements = new LinkedHashSet<>();
    
    // Wer hat welches Achievement erreicht?
    private Map<NamespacedKey, String> achievementCompleter = new HashMap<>();
    
    private File progressFile;
    private FileConfiguration progressConfig;
    
    public AchievementManager(ItemHunter plugin) {
        this.plugin = plugin;
        this.progressFile = new File(plugin.getDataFolder(), "achievements.yml");
        
        initializeRequiredAdvancements();
    }
    
    public void initializeAdvancements() {
        completedAdvancements.clear();
        achievementCompleter.clear();
        initializeRequiredAdvancements();
    }
    
    private void initializeRequiredAdvancements() {
        requiredAdvancements.clear();
        
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            if (isValidAdvancement(advancement)) {
                requiredAdvancements.add(advancement.getKey());
            }
        }
        
        plugin.getLogger().info("Achievements initialisiert: " + requiredAdvancements.size() + " Advancements");
    }
    
    private boolean isValidAdvancement(Advancement advancement) {
        String key = advancement.getKey().toString();
        
        // Keine Root-Advancements (die "Tab"-Einträge)
        if (key.endsWith("/root")) return false;
        
        // Keine Rezept-Advancements
        if (key.contains("recipes/")) return false;
        
        // Keine technischen Advancements
        if (key.startsWith("minecraft:technical/")) return false;
        
        // Muss einen Display haben (sichtbar im Advancement-Screen)
        // Leider kann man das in der Bukkit API nicht direkt prüfen
        // Wir filtern nach bekannten Kategorien
        if (!key.startsWith("minecraft:story/") &&
            !key.startsWith("minecraft:nether/") &&
            !key.startsWith("minecraft:end/") &&
            !key.startsWith("minecraft:adventure/") &&
            !key.startsWith("minecraft:husbandry/")) {
            return false;
        }
        
        return true;
    }
    
    // ============ ACHIEVEMENT ERREICHEN ============
    
    public boolean completeAdvancement(Player player, Advancement advancement) {
        NamespacedKey key = advancement.getKey();
        
        if (!requiredAdvancements.contains(key)) return false;
        if (completedAdvancements.contains(key)) return false;
        
        completedAdvancements.add(key);
        achievementCompleter.put(key, player.getName());
        
        // Fortschritt
        int completed = completedAdvancements.size();
        int total = requiredAdvancements.size();
        double progress = (double) completed / total * 100;
        
        String advName = formatAdvancementName(key);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.LIGHT_PURPLE + "★ " + ChatColor.WHITE + player.getName() + 
                ChatColor.GRAY + " hat " + ChatColor.GOLD + advName + ChatColor.GRAY + " erreicht!");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GRAY + "Achievements: " + 
                ChatColor.LIGHT_PURPLE + completed + ChatColor.GRAY + "/" + ChatColor.WHITE + total + 
                ChatColor.YELLOW + " (" + String.format("%.1f", progress) + "%)");
        Bukkit.broadcastMessage("");
        
        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.3f, 1.5f);
        }
        
        // Challenge prüfen
        plugin.checkChallengeComplete();
        plugin.updateBossBar();
        
        return true;
    }
    
    // ============ GETTER ============
    
    public int getTotalAdvancements() {
        return requiredAdvancements.size();
    }
    
    public int getCompletedCount() {
        return completedAdvancements.size();
    }
    
    public int getRemainingCount() {
        return requiredAdvancements.size() - completedAdvancements.size();
    }
    
    public Set<NamespacedKey> getCompletedAdvancements() {
        return new LinkedHashSet<>(completedAdvancements);
    }
    
    public Set<NamespacedKey> getRemainingAdvancements() {
        Set<NamespacedKey> remaining = new LinkedHashSet<>(requiredAdvancements);
        remaining.removeAll(completedAdvancements);
        return remaining;
    }
    
    public List<NamespacedKey> getRemainingAdvancementsSorted() {
        return getRemainingAdvancements().stream()
                .sorted(Comparator.comparing(NamespacedKey::toString))
                .collect(Collectors.toList());
    }
    
    public String getAdvancementCompleter(NamespacedKey key) {
        return achievementCompleter.getOrDefault(key, "Unbekannt");
    }
    
    public boolean isRequired(NamespacedKey key) {
        return requiredAdvancements.contains(key);
    }
    
    public boolean isCompleted(NamespacedKey key) {
        return completedAdvancements.contains(key);
    }
    
    // ============ RESET ============
    
    public void reset() {
        completedAdvancements.clear();
        achievementCompleter.clear();
        
        if (progressFile.exists()) {
            progressFile.delete();
        }
    }
    
    // ============ SPEICHERN & LADEN ============
    
    public void saveProgress() {
        progressConfig = new YamlConfiguration();
        
        List<String> completedList = completedAdvancements.stream()
                .map(NamespacedKey::toString)
                .collect(Collectors.toList());
        progressConfig.set("completed-advancements", completedList);
        
        for (Map.Entry<NamespacedKey, String> entry : achievementCompleter.entrySet()) {
            String safeKey = entry.getKey().toString().replace(":", "_");
            progressConfig.set("completers." + safeKey, entry.getValue());
        }
        
        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte Achievement-Fortschritt nicht speichern!");
        }
    }
    
    public void loadProgress() {
        if (!progressFile.exists()) return;
        
        progressConfig = YamlConfiguration.loadConfiguration(progressFile);
        
        List<String> completedList = progressConfig.getStringList("completed-advancements");
        for (String keyStr : completedList) {
            try {
                String[] parts = keyStr.split(":");
                if (parts.length == 2) {
                    NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                    if (requiredAdvancements.contains(key)) {
                        completedAdvancements.add(key);
                    }
                }
            } catch (Exception e) {
                // Ignorieren
            }
        }
        
        if (progressConfig.contains("completers")) {
            for (String safeKey : progressConfig.getConfigurationSection("completers").getKeys(false)) {
                try {
                    String keyStr = safeKey.replace("_", ":");
                    String[] parts = keyStr.split(":");
                    if (parts.length == 2) {
                        NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                        String completer = progressConfig.getString("completers." + safeKey);
                        achievementCompleter.put(key, completer);
                    }
                } catch (Exception e) {
                    // Ignorieren
                }
            }
        }
        
        plugin.getLogger().info("Achievement-Fortschritt geladen: " + completedAdvancements.size() + "/" + requiredAdvancements.size());
    }
    
    // ============ HILFSMETHODEN ============
    
    public static String formatAdvancementName(NamespacedKey key) {
        // minecraft:story/mine_stone -> Mine Stone
        String path = key.getKey();
        if (path.contains("/")) {
            path = path.substring(path.lastIndexOf("/") + 1);
        }
        
        String name = path.replace("_", " ");
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
    
    /**
     * Gibt die Kategorie eines Advancements zurück
     */
    public static String getCategory(NamespacedKey key) {
        String path = key.getKey();
        if (path.startsWith("story/")) return "Story";
        if (path.startsWith("nether/")) return "Nether";
        if (path.startsWith("end/")) return "End";
        if (path.startsWith("adventure/")) return "Adventure";
        if (path.startsWith("husbandry/")) return "Husbandry";
        return "Other";
    }
}

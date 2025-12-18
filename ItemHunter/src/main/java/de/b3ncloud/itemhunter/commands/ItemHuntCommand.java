package de.b3ncloud.itemhunter.commands;

import de.b3ncloud.itemhunter.ItemHunter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command Handler für /itemhunt
 */
public class ItemHuntCommand implements CommandExecutor, TabCompleter {

    private final ItemHunter plugin;
    private final List<String> subCommands = Arrays.asList(
            "start", "stop", "pause", "resume", "items", "progress", "reset", "help"
    );
    
    public ItemHuntCommand(ItemHunter plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                handleStart(sender);
                break;
            case "stop":
                handleStop(sender);
                break;
            case "pause":
                handlePause(sender);
                break;
            case "resume":
                handleResume(sender);
                break;
            case "items":
                handleItems(sender);
                break;
            case "progress":
                handleProgress(sender);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            case "help":
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleStart(CommandSender sender) {
        if (!hasPermission(sender, "itemhunter.admin")) {
            return;
        }
        
        if (plugin.getTimerManager().isActive()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Es läuft bereits eine Challenge!");
            sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Benutze " + 
                    ChatColor.WHITE + "/itemhunt stop" + ChatColor.GRAY + " um sie zu beenden.");
            return;
        }
        
        // Alle Manager initialisieren
        plugin.getItemManager().initializeItems();
        plugin.getMobManager().initializeMobs();
        plugin.getAchievementManager().initializeAdvancements();
        
        int totalItems = plugin.getItemManager().getTotalItems();
        int totalMobs = plugin.getMobManager().getTotalMobs();
        int totalAchievements = plugin.getAchievementManager().getTotalAdvancements();
        int totalAll = totalItems + totalMobs + totalAchievements;
        
        if (totalAll == 0) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Keine Ziele gefunden!");
            return;
        }
        
        // Timer starten
        plugin.getTimerManager().start();
        
        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GREEN + "⚡ " + ChatColor.BOLD + "CHALLENGE GESTARTET!");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GRAY + "  📦 Items: " + ChatColor.WHITE + totalItems);
        Bukkit.broadcastMessage(ChatColor.GRAY + "  ☠ Mobs: " + ChatColor.WHITE + totalMobs);
        Bukkit.broadcastMessage(ChatColor.GRAY + "  ★ Achievements: " + ChatColor.WHITE + totalAchievements);
        Bukkit.broadcastMessage("");
        
        // Alle Spieler zur BossBar hinzufügen
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getProgressBar().addPlayer(player);
        }
    }
    
    private void handleStop(CommandSender sender) {
        if (!hasPermission(sender, "itemhunter.admin")) {
            return;
        }
        
        if (!plugin.getTimerManager().isActive()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Es läuft keine Challenge!");
            return;
        }
        
        String time = plugin.getTimerManager().getFormattedTime();
        int found = plugin.getItemManager().getFoundCount();
        int total = plugin.getItemManager().getTotalItems();
        
        // Timer stoppen
        plugin.getTimerManager().stop();
        
        // BossBar entfernen
        plugin.getProgressBar().removeAll();
        
        // Broadcast
        String message = plugin.getMessage("challenge-stopped");
        message = message.replace("{time}", time)
                .replace("{found}", String.valueOf(found))
                .replace("{total}", String.valueOf(total));
        plugin.getServer().broadcastMessage(message);
    }
    
    private void handlePause(CommandSender sender) {
        if (!hasPermission(sender, "itemhunter.admin")) {
            return;
        }
        
        if (!plugin.getTimerManager().isActive()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Es läuft keine Challenge!");
            return;
        }
        
        if (plugin.getTimerManager().isPaused()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Die Challenge ist bereits pausiert!");
            return;
        }
        
        plugin.getTimerManager().pause();
        plugin.getServer().broadcastMessage(plugin.getMessage("challenge-paused"));
    }
    
    private void handleResume(CommandSender sender) {
        if (!hasPermission(sender, "itemhunter.admin")) {
            return;
        }
        
        if (!plugin.getTimerManager().isActive()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Es läuft keine Challenge!");
            return;
        }
        
        if (!plugin.getTimerManager().isPaused()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Die Challenge läuft bereits!");
            return;
        }
        
        plugin.getTimerManager().resume();
        plugin.getServer().broadcastMessage(plugin.getMessage("challenge-resumed"));
    }
    
    private void handleItems(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Dieser Befehl ist nur für Spieler!");
            return;
        }
        
        if (!plugin.getTimerManager().isActive()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Es läuft keine Challenge!");
            return;
        }
        
        // Neue Challenge GUI öffnen
        plugin.getChallengeGUI().openGUI((Player) sender);
    }
    
    private void handleProgress(CommandSender sender) {
        if (!plugin.getTimerManager().isActive()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Es läuft keine Challenge!");
            return;
        }
        
        // Items
        int foundItems = plugin.getItemManager().getFoundCount();
        int totalItems = plugin.getItemManager().getTotalItems();
        double itemPercent = totalItems > 0 ? (double) foundItems / totalItems * 100 : 0;
        
        // Mobs
        int killedMobs = plugin.getMobManager().getKilledCount();
        int totalMobs = plugin.getMobManager().getTotalMobs();
        double mobPercent = totalMobs > 0 ? (double) killedMobs / totalMobs * 100 : 0;
        
        // Achievements
        int completedAdv = plugin.getAchievementManager().getCompletedCount();
        int totalAdv = plugin.getAchievementManager().getTotalAdvancements();
        double advPercent = totalAdv > 0 ? (double) completedAdv / totalAdv * 100 : 0;
        
        // Gesamt
        int totalFound = foundItems + killedMobs + completedAdv;
        int totalRequired = totalItems + totalMobs + totalAdv;
        double totalPercent = totalRequired > 0 ? (double) totalFound / totalRequired * 100 : 0;
        
        String time = plugin.getTimerManager().getFormattedTime();
        boolean paused = plugin.getTimerManager().isPaused();
        
        sender.sendMessage("");
        sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "═══ Challenge Fortschritt ═══");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "  ⏱ Zeit: " + ChatColor.AQUA + time + 
                (paused ? ChatColor.RED + " (PAUSIERT)" : ""));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "  📦 Items: " + ChatColor.WHITE + foundItems + "/" + totalItems + 
                ChatColor.GRAY + " (" + String.format("%.1f", itemPercent) + "%)");
        sender.sendMessage(ChatColor.RED + "  ☠ Mobs: " + ChatColor.WHITE + killedMobs + "/" + totalMobs + 
                ChatColor.GRAY + " (" + String.format("%.1f", mobPercent) + "%)");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "  ★ Achievements: " + ChatColor.WHITE + completedAdv + "/" + totalAdv + 
                ChatColor.GRAY + " (" + String.format("%.1f", advPercent) + "%)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "  ⚡ Gesamt: " + ChatColor.WHITE + totalFound + "/" + totalRequired);
        sender.sendMessage("  " + buildProgressBar(totalPercent));
        sender.sendMessage(ChatColor.YELLOW + "  " + String.format("%.2f", totalPercent) + "% abgeschlossen");
        sender.sendMessage("");
    }
    
    private String buildProgressBar(double percentage) {
        int filled = (int) (percentage / 5);
        int empty = 20 - filled;
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.DARK_GRAY).append("[");
        
        for (int i = 0; i < filled; i++) {
            bar.append(ChatColor.GREEN).append("█");
        }
        for (int i = 0; i < empty; i++) {
            bar.append(ChatColor.GRAY).append("░");
        }
        
        bar.append(ChatColor.DARK_GRAY).append("]");
        return bar.toString();
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "itemhunter.admin")) {
            return;
        }
        
        // Sicherheitsabfrage
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + 
                    "⚠ Bist du sicher? Dies löscht alle Challenge-Daten!");
            sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + 
                    "Benutze " + ChatColor.WHITE + "/itemhunt reset confirm" + 
                    ChatColor.GRAY + " zum Bestätigen.");
            return;
        }
        
        // Stoppen falls aktiv
        if (plugin.getTimerManager().isActive()) {
            plugin.getTimerManager().stop();
            plugin.getProgressBar().removeAll();
        }
        
        // Daten löschen
        plugin.getItemManager().reset();
        plugin.getMobManager().reset();
        plugin.getAchievementManager().reset();
        plugin.getTimerManager().reset();
        
        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Challenge-Daten wurden zurückgesetzt!");
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== ItemHunter Befehle ===");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt start" + ChatColor.GRAY + " - Startet eine neue Challenge");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt stop" + ChatColor.GRAY + " - Beendet die Challenge");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt pause" + ChatColor.GRAY + " - Pausiert den Timer");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt resume" + ChatColor.GRAY + " - Setzt den Timer fort");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt items" + ChatColor.GRAY + " - Zeigt fehlende Items");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt progress" + ChatColor.GRAY + " - Zeigt den Fortschritt");
        sender.sendMessage(ChatColor.WHITE + "/itemhunt reset" + ChatColor.GRAY + " - Setzt alle Daten zurück");
        sender.sendMessage("");
    }
    
    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Keine Berechtigung!");
            return false;
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            if ("confirm".startsWith(args[1].toLowerCase())) {
                return Arrays.asList("confirm");
            }
        }
        
        return new ArrayList<>();
    }
}

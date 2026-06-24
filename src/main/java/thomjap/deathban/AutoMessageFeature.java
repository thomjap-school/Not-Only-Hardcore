package thomjap.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AutoMessageFeature {

    private final DeathBan plugin;
    private File messageFile;
    private FileConfiguration messageConfig;

    private BukkitTask task;
    private boolean enabled;
    private int intervalMinutes;
    private int currentIndex = 0;

    public AutoMessageFeature(DeathBan plugin) {
        this.plugin = plugin;
        setupMessageFile();
        this.enabled = messageConfig.getBoolean("enabled", true);
        this.intervalMinutes = messageConfig.getInt("interval-minutes", 5);
        if (enabled) startTask();
    }

    // ===================== FICHIER YML =====================

    private void setupMessageFile() {
        messageFile = new File(plugin.getDataFolder(), "automessage.yml");
        if (!messageFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                messageFile.createNewFile();
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(messageFile);
                defaults.set("enabled", true);
                defaults.set("interval-minutes", 5);
                defaults.set("messages", java.util.Arrays.asList(
                        "&6[Serveur] &fBienvenue sur le serveur !"
                ));
                defaults.save(messageFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[AutoMessage] Impossible de créer automessage.yml : " + e.getMessage());
            }
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    private void save() {
        messageConfig.set("enabled", enabled);
        messageConfig.set("interval-minutes", intervalMinutes);
        try {
            messageConfig.save(messageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[AutoMessage] Impossible de sauvegarder automessage.yml : " + e.getMessage());
        }
    }

    // ===================== TASK =====================

    private void startTask() {
        if (task != null) {
            task.cancel();
        }
        long intervalTicks = (long) intervalMinutes * 60 * 20;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcast, intervalTicks, intervalTicks);
        plugin.getLogger().info("[AutoMessage] Démarré, intervalle : " + intervalMinutes + " min.");
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin.getLogger().info("[AutoMessage] Arrêté.");
    }

    private void broadcast() {
        List<String> messages = messageConfig.getStringList("messages");
        if (messages == null || messages.isEmpty()) return;

        // Rotation circulaire
        if (currentIndex >= messages.size()) currentIndex = 0;
        String raw = messages.get(currentIndex);
        currentIndex = (currentIndex + 1) % messages.size();

        // Supporte les \n pour les messages multi-lignes
        for (String line : raw.split("\\\\n")) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    // ===================== API =====================

    public void reload() {
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        boolean wasEnabled = enabled;
        enabled = messageConfig.getBoolean("enabled", true);
        intervalMinutes = messageConfig.getInt("interval-minutes", 5);
        currentIndex = 0;

        if (wasEnabled) stopTask();
        if (enabled) startTask();

        plugin.getLogger().info("[AutoMessage] Configuration rechargée.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
        if (enabled) {
            startTask();
        } else {
            stopTask();
        }
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int minutes) {
        this.intervalMinutes = minutes;
        save();
        if (enabled) startTask();
    }
}

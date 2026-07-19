package thomjap.deathban.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ensemble d'UUID persisté dans un fichier YAML sous la clé "pending".
 * Sert aux sanctions différées (forfait de duel, déco combat...) qui doivent
 * survivre à un crash/redémarrage du serveur entre la sanction et son application
 * effective à la reconnexion du joueur.
 */
public class PendingUuidSet {

    private final JavaPlugin plugin;
    private final String fileName;
    private final Set<UUID> values = new HashSet<>();
    private FileConfiguration config;

    public PendingUuidSet(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.config = YamlFiles.loadOrCreate(plugin, fileName);
        load();
    }

    private void load() {
        values.clear();
        for (String s : config.getStringList("pending")) {
            try {
                values.add(UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[PendingUuidSet] Entrée UUID corrompue dans " + fileName + ", ignorée : " + s);
            }
        }
    }

    /**
     * Recharge depuis le disque (utilisé par les commandes /db reload).
     */
    public void reload() {
        config = YamlFiles.loadOrCreate(plugin, fileName);
        load();
    }

    public boolean contains(UUID uuid) {
        return values.contains(uuid);
    }

    public void add(UUID uuid) {
        values.add(uuid);
        save();
    }

    public void remove(UUID uuid) {
        values.remove(uuid);
        save();
    }

    public int size() {
        return values.size();
    }

    private void save() {
        List<String> uuids = new ArrayList<>();
        for (UUID uuid : values) {
            uuids.add(uuid.toString());
        }
        config.set("pending", uuids);
        YamlFiles.save(plugin, config, fileName);
    }
}

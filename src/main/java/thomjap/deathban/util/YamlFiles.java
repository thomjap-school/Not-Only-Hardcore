package thomjap.deathban.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Charge/crée les fichiers YAML stockés dans le dossier de données du plugin.
 * Centralise le boilerplate (mkdirs, createNewFile, gestion des IOException)
 * répété dans chaque feature qui persiste son propre fichier.
 */
public final class YamlFiles {

    private YamlFiles() {
    }

    public static File resolve(JavaPlugin plugin, String fileName) {
        return new File(plugin.getDataFolder(), fileName);
    }

    public static FileConfiguration loadOrCreate(JavaPlugin plugin, String fileName) {
        return loadOrCreate(plugin, fileName, null);
    }

    /**
     * Charge le fichier, en le créant au préalable s'il n'existe pas.
     * Si {@code onCreate} est fourni, il n'est appelé qu'à la création (pas à chaque chargement),
     * pour y placer des valeurs par défaut avant la sauvegarde initiale.
     */
    public static FileConfiguration loadOrCreate(JavaPlugin plugin, String fileName, Consumer<FileConfiguration> onCreate) {
        File file = resolve(plugin, fileName);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
                if (onCreate != null) {
                    FileConfiguration defaults = YamlConfiguration.loadConfiguration(file);
                    onCreate.accept(defaults);
                    defaults.save(file);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer " + fileName + " : " + e.getMessage());
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void save(JavaPlugin plugin, FileConfiguration config, String fileName) {
        try {
            config.save(resolve(plugin, fileName));
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder " + fileName + " : " + e.getMessage());
        }
    }
}

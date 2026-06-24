package thomjap.deathban;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KitFeature {

    private final DeathBan plugin;
    private File kitFile;
    private FileConfiguration kitConfig;

    // Joueurs ayant déjà récupéré le kit depuis leur dernière mort/libération
    private final Set<UUID> kitClaimed = new HashSet<>();

    public KitFeature(DeathBan plugin) {
        this.plugin = plugin;
        setupKitFile();
    }

    // ===================== FICHIER YML =====================

    private void setupKitFile() {
        kitFile = new File(plugin.getDataFolder(), "kit.yml");
        if (!kitFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                kitFile.createNewFile();
                FileConfiguration defaults = YamlConfiguration.loadConfiguration(kitFile);

                // Kit par défaut : quelques items de base
                defaults.set("items.0.material", "STONE_SWORD");
                defaults.set("items.0.amount", 1);
                defaults.set("items.1.material", "BREAD");
                defaults.set("items.1.amount", 16);
                defaults.set("items.2.material", "OAK_LOG");
                defaults.set("items.2.amount", 32);

                defaults.save(kitFile);
                plugin.getLogger().info("[Kit] kit.yml créé avec des items par défaut.");
            } catch (IOException e) {
                plugin.getLogger().severe("[Kit] Impossible de créer kit.yml : " + e.getMessage());
            }
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitFile);
    }

    public void reload() {
        kitConfig = YamlConfiguration.loadConfiguration(kitFile);
        plugin.getLogger().info("[Kit] Configuration rechargée depuis kit.yml.");
    }

    // ===================== LOGIQUE =====================

    /**
     * Donne le kit au joueur.
     * Les items sont ajoutés à l'inventaire ; si plein, ils droppent au sol.
     */
    public void giveKit(Player player) {
        if (kitClaimed.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vous avez déjà récupéré votre kit. Il sera disponible après votre prochaine mort.");
            return;
        }

        if (!kitConfig.contains("items")) {
            player.sendMessage(ChatColor.RED + "Aucun item configuré dans kit.yml.");
            return;
        }

        kitClaimed.add(player.getUniqueId());

        int count = 0;
        for (String key : kitConfig.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + key + ".";
            String materialName = kitConfig.getString(path + "material", "AIR").toUpperCase();
            int amount = kitConfig.getInt(path + "amount", 1);

            Material material = Material.getMaterial(materialName);
            if (material == null || material == Material.AIR) {
                plugin.getLogger().warning("[Kit] Matériau inconnu : " + materialName + " (ignoré)");
                continue;
            }

            ItemStack item = new ItemStack(material, amount);
            // Donne l'item, le surplus drop au sol
            player.getInventory().addItem(item).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            count++;
        }

        if (count > 0) {
            player.sendMessage(ChatColor.GREEN + "Kit récupéré ! (" + count + " type(s) d'item)");
        } else {
            player.sendMessage(ChatColor.RED + "Le kit est vide ou mal configuré.");
        }
    }

    /**
     * Réinitialise le droit au kit pour un joueur (appelé à chaque mort ou libération).
     */
    public void resetKit(UUID uuid) {
        kitClaimed.remove(uuid);
    }

    /**
     * Retourne true si le joueur peut encore réclamer son kit.
     */
    public boolean canClaim(UUID uuid) {
        return !kitClaimed.contains(uuid);
    }
}

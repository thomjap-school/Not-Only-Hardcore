package thomjap.deathban;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AlexBanniereFeature implements Listener {

    // ==========================================================
    // === CONFIGURATION ===
    // ==========================================================

    private static final String TARGET_PLAYER = "AlexJanOne";

    // ==========================================================

    private final DeathBan plugin;
    private boolean enabled;

    public AlexBanniereFeature(DeathBan plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Tous les types de bannières du jeu
    private static final Set<Material> BANNER_MATERIALS = Arrays.stream(Material.values())
            .filter(m -> m.name().endsWith("_BANNER"))
            .collect(Collectors.toSet());

    private boolean isBanner(ItemStack item) {
        return item != null && BANNER_MATERIALS.contains(item.getType());
    }

    private boolean isTarget(Player player) {
        return player.getName().equalsIgnoreCase(TARGET_PLAYER);
    }

    /**
     * Clear toutes les bannières dans l'inventaire complet du joueur
     * (contenu principal + casque + plastron + jambières + bottes + offhand).
     * Retourne le nombre de slots vidés.
     */
    private int clearBanners(Player player) {
        PlayerInventory inv = player.getInventory();
        int count = 0;

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isBanner(contents[i])) {
                contents[i] = null;
                count++;
            }
        }
        inv.setContents(contents);

        if (isBanner(inv.getHelmet())) {
            inv.setHelmet(null);
            count++;
        }
        if (isBanner(inv.getChestplate())) {
            inv.setChestplate(null);
            count++;
        }
        if (isBanner(inv.getLeggings())) {
            inv.setLeggings(null);
            count++;
        }
        if (isBanner(inv.getBoots())) {
            inv.setBoots(null);
            count++;
        }
        if (isBanner(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
            count++;
        }

        return count;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        if (!isTarget(player)) return;

        int removed = clearBanners(player);
        if (removed > 0) {
            player.sendMessage(ChatColor.RED + "Vos bannières ont été retirées de votre inventaire.");
            plugin.getLogger().info("[AlexBanniere] " + removed + " bannière(s) retirée(s) de l'inventaire de " + player.getName() + " à la connexion.");
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!enabled) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (!isTarget(player)) return;

        if (!isBanner(event.getItem().getItemStack())) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Vous ne pouvez pas ramasser de bannières.");
        plugin.getLogger().info("[AlexBanniere] " + player.getName() + " a tenté de ramasser une bannière — annulé.");
    }
}

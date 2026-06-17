package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathBan extends JavaPlugin implements Listener {

    // ==========================================================
    // === CONFIGURATION : modifie ces valeurs selon tes besoins ===
    // ==========================================================

    // Son joué sur tout le serveur quand un joueur meurt.
    private static final Sound DEATH_SOUND = Sound.ENTITY_WITHER_SPAWN;

    // Volume et hauteur du son (1.0F = normal)
    private static final float SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 1.0F;

    // ==========================================================

    private PrisonFeature prisonFeature;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new HeadFeature(this), this);

        prisonFeature = new PrisonFeature(this);
        Bukkit.getPluginManager().registerEvents(prisonFeature, this);

        Bukkit.getPluginManager().registerEvents(
                new DeathListener(this, prisonFeature, DEATH_SOUND, SOUND_VOLUME, SOUND_PITCH),
                this
        );

        getLogger().info("DeathBan activé.");
    }

    @EventHandler
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();

        if (isInventoryEmpty(victim)) {
            event.setCancelled(true);
        }
    }

    private boolean isInventoryEmpty(Player player) {
        PlayerInventory inv = player.getInventory();

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }

        if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) return false;
        if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) return false;
        if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) return false;
        if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) return false;
        if (inv.getItemInOffHand().getType() != Material.AIR) return false;

        return true;
    }
}

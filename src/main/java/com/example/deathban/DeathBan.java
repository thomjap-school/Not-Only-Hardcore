package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Date;

public class DeathBan extends JavaPlugin implements Listener {

    // ==========================================================
    // === CONFIGURATION : modifie ces valeurs selon tes besoins ===
    // ==========================================================

    // Durée du ban en heures. Change cette valeur pour ajuster la durée.
    private static final long BAN_DURATION_HOURS = 48;

    // Son joué sur tout le serveur quand un joueur meurt.
    private static final Sound DEATH_SOUND = Sound.ENTITY_WITHER_SPAWN;

    // Volume et hauteur du son (1.0F = normal)
    private static final float SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 1.0F;

    // ==========================================================

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new HeadFeature(this), this);
        getLogger().info("DeathBan activé - durée de ban : " + BAN_DURATION_HOURS + "h");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
    
        // Récupère le message de mort vanilla (ex: "Steve a été tué par Zombie")
        Component deathComponent = event.deathMessage();
        String deathMessage = deathComponent != null
                ? PlainTextComponentSerializer.plainText().serialize(deathComponent)
                : "Cause inconnue";
    
        // Calcul de la date d'expiration du ban
        long durationMillis = BAN_DURATION_HOURS * 60 * 60 * 1000;
        Date expiration = new Date(System.currentTimeMillis() + durationMillis);
    
        // Message de ban affiché au joueur (en rouge)
        String banMessage = ChatColor.RED + "Vous êtes mort !\n"
                + ChatColor.RED + "Cause : " + deathMessage + "\n"
                + ChatColor.RED + "Vous êtes banni pour " + BAN_DURATION_HOURS + " heures.\n"
                + ChatColor.RED + "Revenez plus tard.";
    
        // Application du ban (par nom de joueur)
        BanList<org.bukkit.profile.PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
        banList.addBan(player.getPlayerProfile(), banMessage, expiration, "DeathBan plugin");
    
        // Joue le son de mort à tous les joueurs connectés
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), DEATH_SOUND, SOUND_VOLUME, SOUND_PITCH);
        }
    
        // Kick le joueur avec le message de ban (en rouge)
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.kickPlayer(banMessage);
            }
        });
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

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }

        if (inv.getHelmet() != null) return false;
        if (inv.getChestplate() != null) return false;
        if (inv.getLeggings() != null) return false;
        if (inv.getBoots() != null) return false;
        if (inv.getItemInOffHand().getType() != Material.AIR) return false;

        return true;
    }
}

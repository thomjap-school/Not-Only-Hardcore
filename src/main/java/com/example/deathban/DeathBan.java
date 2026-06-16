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

import java.util.Date;

public class DeathBan extends JavaPlugin implements Listener {

    // ==========================================================
    // === CONFIGURATION : modifie ces valeurs selon tes besoins ===
    // ==========================================================

    // Durée du ban en heures. Change cette valeur pour ajuster la durée.
    private static final long BAN_DURATION_HOURS = 48;

    // Son joué sur tout le serveur quand un joueur meurt.
    // Remplace XXXXXXXXXXXXXXXXXXXXXX par le Sound correspondant
    // (ex: Sound.ENTITY_WITHER_DEATH)
    private static final Sound DEATH_SOUND = Sound.ENTITY_WITHER_SPAWN;

    // Volume et hauteur du son (1.0F = normal)
    private static final float SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 1.0F;

    // ==========================================================

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DeathBan activé - durée de ban : " + BAN_DURATION_HOURS + "h");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Calcul de la date d'expiration du ban
        long durationMillis = BAN_DURATION_HOURS * 60 * 60 * 1000;
        Date expiration = new Date(System.currentTimeMillis() + durationMillis);

        // Message de ban affiché au joueur (en rouge)
        String banMessage = ChatColor.RED + "Vous êtes mort !\n"
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
}

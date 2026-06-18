package com.example.deathban;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NewPlayerProtection implements Listener {

    // ==========================================================
    // === CONFIGURATION ===
    // ==========================================================

    // Durée de la protection en minutes
    private static final int PROTECTION_DURATION_MINUTES = 30;

    // Amplificateur de résistance (4 = Résistance V, rend invincible)
    private static final int RESISTANCE_AMPLIFIER = 4;

    // ==========================================================

    private final DeathBan plugin;
    private final NamespacedKey hasJoinedKey;

    public NewPlayerProtection(DeathBan plugin) {
        this.plugin = plugin;
        this.hasJoinedKey = new NamespacedKey(plugin, "has_joined_before");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Vérifie si le joueur a déjà rejoint via les données persistantes
        boolean hasJoinedBefore = player.getPersistentDataContainer()
                .has(hasJoinedKey, PersistentDataType.BYTE);

        if (hasJoinedBefore) return;

        // Marque le joueur comme ayant déjà rejoint
        player.getPersistentDataContainer()
                .set(hasJoinedKey, PersistentDataType.BYTE, (byte) 1);

        // Applique Résistance V pendant X minutes
        int durationTicks = PROTECTION_DURATION_MINUTES * 60 * 20;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                durationTicks,
                RESISTANCE_AMPLIFIER,
                false,
                true  // affiche les particules pour que ce soit visible
        ));

        player.sendMessage(ChatColor.GREEN + "✔ Bienvenue ! Vous êtes protégé pendant "
                + PROTECTION_DURATION_MINUTES + " minutes.");

        plugin.getLogger().info("[Protection] " + player.getName()
                + " est un nouveau joueur, protection activée pour "
                + PROTECTION_DURATION_MINUTES + " min.");
    }
}
package thomjap.deathban.listeners;

import thomjap.deathban.ConfigManager;
import thomjap.deathban.DeathBan;

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

    private final DeathBan plugin;
    private final ConfigManager configManager;
    private final NamespacedKey hasJoinedKey;

    public NewPlayerProtection(DeathBan plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
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

        int durationMinutes = configManager.getNewPlayerProtectionDurationMinutes();
        int amplifier = configManager.getNewPlayerProtectionAmplifier();
        int durationTicks = durationMinutes * 60 * 20;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                durationTicks,
                amplifier,
                false,
                true  // affiche les particules pour que ce soit visible
        ));

        player.sendMessage(ChatColor.GREEN + "✔ Bienvenue ! Vous êtes protégé pendant "
                + durationMinutes + " minutes.");

        plugin.getLogger().info("[Protection] " + player.getName()
                + " est un nouveau joueur, protection activée pour "
                + durationMinutes + " min.");
    }
}

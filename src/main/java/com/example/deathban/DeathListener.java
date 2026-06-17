package com.example.deathban;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathListener implements Listener {

    private final JavaPlugin plugin;
    private final PrisonFeature prisonFeature;
    private final Sound deathSound;
    private final float soundVolume;
    private final float soundPitch;

    public DeathListener(JavaPlugin plugin, PrisonFeature prisonFeature, Sound deathSound, float soundVolume, float soundPitch) {
        this.plugin = plugin;
        this.prisonFeature = prisonFeature;
        this.deathSound = deathSound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Component deathComponent = event.deathMessage();
        String deathMessage = deathComponent != null
                ? PlainTextComponentSerializer.plainText().serialize(deathComponent)
                : "Cause inconnue";

        plugin.getLogger().info("[DeathBan] " + player.getName() + " est mort. Cause : " + deathMessage);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), deathSound, soundVolume, soundPitch);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            prisonFeature.imprison(player);
        });
    }
}

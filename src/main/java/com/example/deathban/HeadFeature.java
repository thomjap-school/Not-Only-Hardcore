package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class HeadFeature implements Listener {

    private final DeathBan plugin;
    private final ConfigManager configManager;
    private final NamespacedKey ownerKey;

    public HeadFeature(DeathBan plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.ownerKey = new NamespacedKey(plugin, "head_owner_uuid");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (Math.random() * 100 > configManager.getHeadDropChancePercent()) {
            return;
        }

        Player victim = event.getEntity();
        ItemStack head = createHeadFor(victim);
        event.getDrops().add(head);
    }

    private ItemStack createHeadFor(Player owner) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(owner);
        meta.setDisplayName(ChatColor.RED + "Tête de " + owner.getName());
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Clic droit pour empoisonner",
                ChatColor.GRAY + owner.getName() + " s'il est en ligne."
        ));

        // Stocke l'UUID du propriétaire dans les données persistantes de l'item
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());

        head.setItemMeta(meta);
        return head;
    }

    @EventHandler
    public void onHeadUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return;

        String ownerUuidString = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerUuidString == null) return;

        UUID ownerUuid = UUID.fromString(ownerUuidString);
        Player owner = Bukkit.getPlayer(ownerUuid);
        Player user = event.getPlayer();

        // Le propriétaire doit être en ligne pour que l'effet fonctionne
        if (owner == null || !owner.isOnline()) {
            user.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne.");
            return;
        }

        event.setCancelled(true);

        // Applique l'effet configuré
        PotionEffectType effectType = configManager.getHeadEffect();
        owner.addPotionEffect(new PotionEffect(
                effectType,
                configManager.getHeadDurationSeconds() * 20, // ticks (20 ticks = 1 seconde)
                configManager.getHeadAmplifier()
        ));

        // Message uniquement aux deux joueurs concernés
        owner.sendMessage(ChatColor.RED + "Vous avez été affecté via votre propre tête !");
        user.sendMessage(ChatColor.RED + "Vous avez affecté " + owner.getName() + " avec sa tête.");

        // Consomme la tête (retire 1 unité)
        ItemStack hand = user.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            user.getInventory().setItemInMainHand(null);
        }
    }
}

package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelFeature implements Listener {

    private static final long REQUEST_EXPIRY_SECONDS = 60;
    private static final long DISCONNECT_GRACE_SECONDS = 5 * 60;

    private final DeathBan plugin;
    private final ConfigManager configManager;
    private PrisonFeature prisonFeature;

    // Requêtes en attente : cible -> requête
    private final Map<UUID, DuelRequest> pendingRequests = new HashMap<>();

    // Duels actifs : joueur -> état du duel (les deux participants pointent vers le même objet)
    private final Map<UUID, ActiveDuel> activeDuels = new HashMap<>();

    // Joueurs déconnectés pendant un duel, en attente de reconnexion
    private final Map<UUID, BukkitTask> disconnectGraceTasks = new HashMap<>();

    // Snapshot (position + inventaire) capturé à la déconnexion, utilisé en cas de forfait
    private final Map<UUID, DisconnectSnapshot> disconnectSnapshots = new HashMap<>();

    // Joueurs ayant perdu par forfait, à punir (drop déjà fait, emprisonnement) dès leur prochaine connexion
    private final java.util.Set<UUID> pendingForfeitPunishment = new java.util.HashSet<>();

    public DuelFeature(DeathBan plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Injection tardive de PrisonFeature car DuelFeature est créé avant PrisonFeature
     * dans DeathBan#onEnable(). À appeler juste après la création de PrisonFeature.
     */
    public void setPrisonFeature(PrisonFeature prisonFeature) {
        this.prisonFeature = prisonFeature;
    }

    // ===================== REQUÊTES =====================

    public void sendRequest(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "Vous ne pouvez pas vous défier vous-même.");
            return;
        }
        if (isInDuel(requester) || isInDuel(target)) {
            requester.sendMessage(ChatColor.RED + "Un des deux joueurs est déjà en duel.");
            return;
        }

        DuelRequest request = new DuelRequest(requester.getUniqueId(), System.currentTimeMillis());
        pendingRequests.put(target.getUniqueId(), request);

        requester.sendMessage(ChatColor.YELLOW + "Demande de duel envoyée à " + target.getName() + ".");

        target.sendMessage(ChatColor.GOLD + requester.getName() + " vous défie en duel !");
        target.sendMessage(
                ChatColor.GREEN + "[Accepter]" + ChatColor.GRAY + " - " +
                ChatColor.RED + "[Refuser]" + ChatColor.GRAY +
                " (ou /db duel accept|deny " + requester.getName() + ")"
        );

        // Expiration automatique après 60s
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest current = pendingRequests.get(target.getUniqueId());
            if (current == request) {
                pendingRequests.remove(target.getUniqueId());
                if (requester.isOnline()) {
                    requester.sendMessage(ChatColor.RED + "Votre demande de duel envers " + target.getName() + " a expiré.");
                }
            }
        }, REQUEST_EXPIRY_SECONDS * 20);
    }

    public void acceptRequest(Player target, String requesterName) {
        DuelRequest request = pendingRequests.get(target.getUniqueId());
        Player requester = Bukkit.getPlayer(requesterName);

        if (request == null || requester == null || !request.requesterUuid.equals(requester.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "Aucune demande de duel en attente de la part de " + requesterName + ".");
            return;
        }

        pendingRequests.remove(target.getUniqueId());

        if (isInDuel(requester) || isInDuel(target)) {
            target.sendMessage(ChatColor.RED + "Un des deux joueurs est déjà en duel.");
            return;
        }

        startDuel(requester, target);
    }

    public void denyRequest(Player target, String requesterName) {
        DuelRequest request = pendingRequests.get(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ChatColor.RED + "Aucune demande de duel en attente de la part de " + requesterName + ".");
            return;
        }

        pendingRequests.remove(target.getUniqueId());
        target.sendMessage(ChatColor.YELLOW + "Vous avez refusé le duel.");

        Player requester = Bukkit.getPlayer(request.requesterUuid);
        if (requester != null) {
            requester.sendMessage(ChatColor.RED + target.getName() + " a refusé votre duel.");
        }
    }

    // ===================== DÉROULEMENT DU DUEL =====================

    private void startDuel(Player requester, Player target) {
        int zone = pickFreeZone();
        if (zone == -1) {
            requester.sendMessage(ChatColor.RED + "Aucune zone de duel disponible actuellement.");
            target.sendMessage(ChatColor.RED + "Aucune zone de duel disponible actuellement.");
            return;
        }

        World duelWorld = Bukkit.getWorld(configManager.getDuelWorldName());
        if (duelWorld == null) {
            requester.sendMessage(ChatColor.RED + "Le monde de duel n'existe pas. Préviens un admin.");
            target.sendMessage(ChatColor.RED + "Le monde de duel n'existe pas. Préviens un admin.");
            plugin.getLogger().severe("[Duel] Le monde '" + configManager.getDuelWorldName() + "' n'existe pas !");
            return;
        }

        ActiveDuel duel = new ActiveDuel(
                requester.getUniqueId(), target.getUniqueId(),
                requester.getLocation(), target.getLocation(),
                zone
        );

        activeDuels.put(requester.getUniqueId(), duel);
        activeDuels.put(target.getUniqueId(), duel);

        Location locA = new Location(duelWorld,
                configManager.getDuelX(zone, "a"), configManager.getDuelY(zone, "a"), configManager.getDuelZ(zone, "a"),
                configManager.getDuelYaw(zone, "a"), configManager.getDuelPitch(zone, "a"));
        Location locB = new Location(duelWorld,
                configManager.getDuelX(zone, "b"), configManager.getDuelY(zone, "b"), configManager.getDuelZ(zone, "b"),
                configManager.getDuelYaw(zone, "b"), configManager.getDuelPitch(zone, "b"));

        requester.teleport(locA);
        target.teleport(locB);

        requester.sendMessage(ChatColor.GOLD + "Le duel contre " + target.getName() + " commence !");
        target.sendMessage(ChatColor.GOLD + "Le duel contre " + requester.getName() + " commence !");
    }

    private int pickFreeZone() {
        boolean[] used = new boolean[4]; // index 1..3
        for (ActiveDuel duel : new java.util.HashSet<>(activeDuels.values())) {
            if (duel.zone >= 1 && duel.zone <= 3) used[duel.zone] = true;
        }
        for (int z = 1; z <= 3; z++) {
            if (!used[z]) return z;
        }
        return -1;
    }

    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    /**
     * Appelé par DeathListener / DeathBan AVANT le traitement normal de la mort
     * pour savoir si le HeadFeature doit être désactivé pour ce mort.
     */
    public boolean isDuelDeath(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        ActiveDuel duel = activeDuels.get(loser.getUniqueId());
        if (duel == null) return;

        UUID winnerUuid = duel.getOpponent(loser.getUniqueId());
        Player winner = Bukkit.getPlayer(winnerUuid);

        // On retire les deux du duel actif maintenant, le perdant suit le flow normal
        // (prison via DeathListener), seul le gagnant est géré ici.
        activeDuels.remove(loser.getUniqueId());
        activeDuels.remove(winnerUuid);

        if (winner == null || !winner.isOnline()) {
            return; // le gagnant n'est plus là, rien à faire de plus
        }

        Location returnLocation = duel.getReturnLocation(winnerUuid);
        int delaySeconds = configManager.getDuelReturnDelaySeconds();

        winner.sendMessage(ChatColor.GREEN + "Vous avez gagné le duel ! Retour dans " + delaySeconds + "s.");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (winner.isOnline()) {
                winner.teleport(returnLocation);
                winner.sendMessage(ChatColor.GREEN + "Vous avez été ramené à votre position d'avant duel.");
            }
        }, delaySeconds * 20L);
    }

    // ===================== DÉCONNEXION =====================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = activeDuels.get(player.getUniqueId());
        if (duel == null) return;

        UUID disconnectedUuid = player.getUniqueId();

        scheduleForfeitTask(disconnectedUuid, captureSnapshot(player));
    }

    /**
     * Capture la position et le contenu de l'inventaire du joueur au moment de sa déconnexion,
     * pour pouvoir faire dropper ses items au bon endroit si le forfait se confirme.
     */
    private DisconnectSnapshot captureSnapshot(Player player) {
        PlayerInventory inv = player.getInventory();

        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                items.add(item.clone());
            }
        }
        if (inv.getHelmet() != null && inv.getHelmet().getType() != org.bukkit.Material.AIR) items.add(inv.getHelmet().clone());
        if (inv.getChestplate() != null && inv.getChestplate().getType() != org.bukkit.Material.AIR) items.add(inv.getChestplate().clone());
        if (inv.getLeggings() != null && inv.getLeggings().getType() != org.bukkit.Material.AIR) items.add(inv.getLeggings().clone());
        if (inv.getBoots() != null && inv.getBoots().getType() != org.bukkit.Material.AIR) items.add(inv.getBoots().clone());
        if (inv.getItemInOffHand().getType() != org.bukkit.Material.AIR) items.add(inv.getItemInOffHand().clone());

        return new DisconnectSnapshot(player.getLocation().clone(), items);
    }

    /**
     * Programme (ou reprogramme) la tâche de forfait à 5 minutes pour un joueur déconnecté en duel.
     * Le snapshot fourni sera utilisé pour dropper ses items si le forfait se confirme.
     */
    private void scheduleForfeitTask(UUID disconnectedUuid, DisconnectSnapshot snapshot) {
        disconnectSnapshots.put(disconnectedUuid, snapshot);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Toujours en duel et toujours hors ligne après le délai -> forfait
            ActiveDuel stillActive = activeDuels.get(disconnectedUuid);
            if (stillActive == null) return;

            Player stillOffline = Bukkit.getPlayer(disconnectedUuid);
            if (stillOffline != null && stillOffline.isOnline()) return; // reconnecté entre temps (sécurité, normalement géré par onPlayerJoin)

            UUID winnerUuid = stillActive.getOpponent(disconnectedUuid);
            Player winner = Bukkit.getPlayer(winnerUuid);

            activeDuels.remove(disconnectedUuid);
            activeDuels.remove(winnerUuid);
            disconnectGraceTasks.remove(disconnectedUuid);

            // Drop des items du perdant à l'endroit où il s'est déconnecté
            DisconnectSnapshot loserSnapshot = disconnectSnapshots.remove(disconnectedUuid);
            if (loserSnapshot != null) {
                Location dropLocation = loserSnapshot.location;
                World dropWorld = dropLocation.getWorld();
                if (dropWorld != null) {
                    for (ItemStack item : loserSnapshot.items) {
                        dropWorld.dropItemNaturally(dropLocation, item);
                    }
                }
            }
            // Le joueur sera vidé et emprisonné dès sa prochaine connexion
            pendingForfeitPunishment.add(disconnectedUuid);

            if (winner != null && winner.isOnline()) {
                Location returnLocation = stillActive.getReturnLocation(winnerUuid);
                int delaySeconds = configManager.getDuelReturnDelaySeconds();

                winner.sendMessage(ChatColor.GREEN + "Votre adversaire ne s'est pas reconnecté à temps. Vous gagnez le duel !");
                winner.sendMessage(ChatColor.GREEN + "Retour dans " + delaySeconds + "s.");

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (winner.isOnline()) {
                        winner.teleport(returnLocation);
                        winner.sendMessage(ChatColor.GREEN + "Vous avez été ramené à votre position d'avant duel.");
                    }
                }, delaySeconds * 20L);
            }

            plugin.getLogger().info("[Duel] " + disconnectedUuid + " ne s'est pas reconnecté à temps, duel perdu par forfait. Items droppés, emprisonnement prévu à la reconnexion.");
        }, DISCONNECT_GRACE_SECONDS * 20L);

        disconnectGraceTasks.put(disconnectedUuid, task);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Cas 1 : le joueur revient après que son forfait a déjà été acté (items déjà droppés)
        if (pendingForfeitPunishment.remove(uuid)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.getInventory().clear();
                player.getInventory().setHelmet(null);
                player.getInventory().setChestplate(null);
                player.getInventory().setLeggings(null);
                player.getInventory().setBoots(null);
                player.getInventory().setItemInOffHand(null);

                player.sendMessage(ChatColor.RED + "Vous n'êtes pas revenu à temps : vous avez perdu le duel par forfait.");
                player.sendMessage(ChatColor.RED + "Vos affaires ont été lâchées là où vous vous êtes déconnecté.");

                if (prisonFeature != null) {
                    prisonFeature.imprison(player);
                }
            });
            return;
        }

        // Cas 2 : le joueur revient à temps, avant l'expiration du délai de grâce
        BukkitTask task = disconnectGraceTasks.remove(uuid);
        if (task != null) {
            task.cancel();
            disconnectSnapshots.remove(uuid);
            ActiveDuel duel = activeDuels.get(uuid);
            if (duel != null) {
                player.sendMessage(ChatColor.YELLOW + "Vous vous êtes reconnecté à temps, le duel continue !");
            }
        }
    }

    // ===================== STRUCTURES INTERNES =====================

    private static class DuelRequest {
        final UUID requesterUuid;
        final long timestampMillis;

        DuelRequest(UUID requesterUuid, long timestampMillis) {
            this.requesterUuid = requesterUuid;
            this.timestampMillis = timestampMillis;
        }
    }

    private static class DisconnectSnapshot {
        final Location location;
        final java.util.List<ItemStack> items;

        DisconnectSnapshot(Location location, java.util.List<ItemStack> items) {
            this.location = location;
            this.items = items;
        }
    }

    private static class ActiveDuel {
        final UUID playerA;
        final UUID playerB;
        final Location returnLocationA;
        final Location returnLocationB;
        final int zone;

        ActiveDuel(UUID playerA, UUID playerB, Location returnLocationA, Location returnLocationB, int zone) {
            this.playerA = playerA;
            this.playerB = playerB;
            this.returnLocationA = returnLocationA;
            this.returnLocationB = returnLocationB;
            this.zone = zone;
        }

        UUID getOpponent(UUID uuid) {
            return uuid.equals(playerA) ? playerB : playerA;
        }

        Location getReturnLocation(UUID uuid) {
            return uuid.equals(playerA) ? returnLocationA : returnLocationB;
        }
    }
}

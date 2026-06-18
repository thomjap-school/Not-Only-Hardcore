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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelFeature implements Listener {

    private static final long REQUEST_EXPIRY_SECONDS = 60;
    private static final long DISCONNECT_GRACE_SECONDS = 5 * 60;

    private final DeathBan plugin;
    private final ConfigManager configManager;

    // Requêtes en attente : cible -> requête
    private final Map<UUID, DuelRequest> pendingRequests = new HashMap<>();

    // Duels actifs : joueur -> état du duel (les deux participants pointent vers le même objet)
    private final Map<UUID, ActiveDuel> activeDuels = new HashMap<>();

    // Joueurs déconnectés pendant un duel, en attente de reconnexion
    private final Map<UUID, BukkitTask> disconnectGraceTasks = new HashMap<>();

    public DuelFeature(DeathBan plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
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

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Toujours en duel et toujours hors ligne après le délai -> mort forcée
            ActiveDuel stillActive = activeDuels.get(disconnectedUuid);
            if (stillActive == null) return;

            Player stillOffline = Bukkit.getPlayer(disconnectedUuid);
            if (stillOffline != null && stillOffline.isOnline()) return; // reconnecté entre temps

            UUID winnerUuid = stillActive.getOpponent(disconnectedUuid);
            Player winner = Bukkit.getPlayer(winnerUuid);

            activeDuels.remove(disconnectedUuid);
            activeDuels.remove(winnerUuid);
            disconnectGraceTasks.remove(disconnectedUuid);

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

            plugin.getLogger().info("[Duel] " + disconnectedUuid + " ne s'est pas reconnecté à temps, duel perdu par forfait.");
        }, DISCONNECT_GRACE_SECONDS * 20L);

        disconnectGraceTasks.put(disconnectedUuid, task);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BukkitTask task = disconnectGraceTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            ActiveDuel duel = activeDuels.get(player.getUniqueId());
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

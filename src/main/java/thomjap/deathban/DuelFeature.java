package thomjap.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
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

    // Joueurs ayant perdu par forfait, à punir (drop déjà fait, emprisonnement) dès leur prochaine connexion.
    // Persisté sur disque pour survivre à un crash/redémarrage du serveur entre le forfait et la reconnexion.
    private final java.util.Set<UUID> pendingForfeitPunishment = new java.util.HashSet<>();
    private File forfeitFile;
    private FileConfiguration forfeitConfig;

    // Gagnants en attente de leur téléportation de retour (entre la fin du duel et le délai écoulé).
    // Sert à bloquer toute téléportation parasite (ex: perle d'ender lancée juste avant la fin du duel)
    // qui pourrait interférer avec le retour à la position d'avant duel.
    private final java.util.Set<UUID> pendingReturn = new java.util.HashSet<>();

    public DuelFeature(DeathBan plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setupForfeitFile();
        loadPendingForfeits();
    }

    private void setupForfeitFile() {
        forfeitFile = new File(plugin.getDataFolder(), "pending_forfeits.yml");
        if (!forfeitFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                forfeitFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[Duel] Impossible de créer pending_forfeits.yml : " + e.getMessage());
            }
        }
        forfeitConfig = YamlConfiguration.loadConfiguration(forfeitFile);
    }

    private void loadPendingForfeits() {
        java.util.List<String> uuids = forfeitConfig.getStringList("pending");
        for (String s : uuids) {
            try {
                pendingForfeitPunishment.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // entrée corrompue, on l'ignore
            }
        }
        if (!pendingForfeitPunishment.isEmpty()) {
            plugin.getLogger().info("[Duel] " + pendingForfeitPunishment.size()
                    + " forfait(s) en attente chargé(s) depuis le fichier.");
        }
    }

    private void saveForfeitFile() {
        java.util.List<String> uuids = new java.util.ArrayList<>();
        for (UUID uuid : pendingForfeitPunishment) {
            uuids.add(uuid.toString());
        }
        forfeitConfig.set("pending", uuids);
        try {
            forfeitConfig.save(forfeitFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Duel] Impossible de sauvegarder pending_forfeits.yml : " + e.getMessage());
        }
    }

    private void addPendingForfeit(UUID uuid) {
        pendingForfeitPunishment.add(uuid);
        saveForfeitFile();
    }

    private void removePendingForfeit(UUID uuid) {
        pendingForfeitPunishment.remove(uuid);
        saveForfeitFile();
    }

    /**
     * Injection tardive de PrisonFeature car DuelFeature est créé avant PrisonFeature
     * dans DeathBan#onEnable(). À appeler juste après la création de PrisonFeature.
     */
    public void setPrisonFeature(PrisonFeature prisonFeature) {
        this.prisonFeature = prisonFeature;
    }

    public void reload() {
        forfeitConfig = YamlConfiguration.loadConfiguration(forfeitFile);
        plugin.getLogger().info("[Duel] Configuration rechargée depuis pending_forfeits.yml.");
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

        // Supprime toute perle d'ender déjà en vol lancée par l'un des deux joueurs
        // avant le duel : sans ça, elle pourrait atterrir une fois le combat commencé
        // et re-téléporter le joueur hors de l'arène.
        removeInFlightEnderPearls(requester);
        removeInFlightEnderPearls(target);

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

    /**
     * Supprime les perles d'ender actuellement en vol (entité EnderPearl)
     * appartenant à ce joueur, dans le monde où il se trouve.
     */
    private void removeInFlightEnderPearls(Player player) {
        World world = player.getWorld();
        for (org.bukkit.entity.Entity entity : world.getEntitiesByClass(org.bukkit.entity.EnderPearl.class)) {
            org.bukkit.entity.EnderPearl pearl = (org.bukkit.entity.EnderPearl) entity;
            if (pearl.getShooter() instanceof Player
                    && ((Player) pearl.getShooter()).getUniqueId().equals(player.getUniqueId())) {
                pearl.remove();
            }
        }
    }

    private int pickFreeZone() {
        boolean[] used = new boolean[4]; // index 1..3
        for (ActiveDuel duel : new java.util.HashSet<>(activeDuels.values())) {
            if (duel.zone >= 1 && duel.zone <= 3) used[duel.zone] = true;
        }
        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int z = 1; z <= 3; z++) {
            if (!used[z]) available.add(z);
        }
        if (available.isEmpty()) return -1;
        return available.get(new java.util.Random().nextInt(available.size()));
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

    /**
     * Annule toute téléportation parasite par perle d'ender qui atterrirait
     * pendant qu'un gagnant attend sa téléportation de retour après duel.
     * Pendant le combat lui-même, les perles restent autorisées (mécanique
     * PvP normale) : seule la fenêtre d'attente post-victoire est protégée,
     * pour éviter qu'une perle lancée juste avant la fin du duel ne vienne
     * perturber le TP de retour du gagnant.
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingReturn.contains(uuid)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Votre perle d'ender a été annulée (retour de duel en cours).");
        }
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

        scheduleWinnerReturn(winner, returnLocation, delaySeconds);
    }

    /**
     * Planifie le retour du gagnant après le délai configuré.
     * 1 seconde avant la téléportation, nettoie les items au sol dans un rayon
     * de 100 blocs autour du gagnant, pour éviter que le loot du combat (ou
     * celui du perdant) ne traîne ou soit récupéré par quelqu'un d'autre.
     */
    private void scheduleWinnerReturn(Player winner, Location returnLocation, int delaySeconds) {
        long cleanupDelayTicks = Math.max(0, delaySeconds - 1) * 20L;
        long teleportDelayTicks = delaySeconds * 20L;

        pendingReturn.add(winner.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (winner.isOnline()) {
                clearNearbyItems(winner.getLocation(), 100.0);
            }
        }, cleanupDelayTicks);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingReturn.remove(winner.getUniqueId());
            if (winner.isOnline()) {
                // Supprime une éventuelle perle d'ender lancée par le gagnant juste avant
                // sa téléportation de retour : sinon elle pourrait atterrir une fois revenu
                // dans le monde d'origine et le re-téléporter ailleurs.
                removeInFlightEnderPearls(winner);
                winner.teleport(returnLocation);
                winner.sendMessage(ChatColor.GREEN + "Vous avez été ramené à votre position d'avant duel.");
            }
        }, teleportDelayTicks);
    }

    /**
     * Supprime tous les items au sol (entités Item) dans un rayon donné
     * autour d'une position.
     */
    private void clearNearbyItems(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof org.bukkit.entity.Item) {
                entity.remove();
            }
        }
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
            // Le joueur sera vidé et emprisonné dès sa prochaine connexion (persisté sur disque)
            addPendingForfeit(disconnectedUuid);

            if (winner != null && winner.isOnline()) {
                Location returnLocation = stillActive.getReturnLocation(winnerUuid);
                int delaySeconds = configManager.getDuelReturnDelaySeconds();

                winner.sendMessage(ChatColor.GREEN + "Votre adversaire ne s'est pas reconnecté à temps. Vous gagnez le duel !");
                winner.sendMessage(ChatColor.GREEN + "Retour dans " + delaySeconds + "s.");

                scheduleWinnerReturn(winner, returnLocation, delaySeconds);
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
        if (pendingForfeitPunishment.contains(uuid)) {
            removePendingForfeit(uuid);
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

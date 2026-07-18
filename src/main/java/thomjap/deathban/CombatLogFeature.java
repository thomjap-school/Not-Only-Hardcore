package thomjap.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CombatLogFeature implements Listener {

    // Un joueur est considéré "en combat" pendant ce délai après avoir donné ou reçu un coup
    private static final long COMBAT_TAG_MILLIS = 20 * 1000;

    // Délai laissé pour se reconnecter après une déconnexion en combat
    private static final long DISCONNECT_GRACE_SECONDS = 5 * 60;

    private final DeathBan plugin;
    private final DuelFeature duelFeature;

    // Dernier coup donné/reçu par joueur (timestamp en millis)
    private final Map<UUID, Long> lastCombatMillis = new HashMap<>();

    // Joueurs déconnectés en combat, en attente de reconnexion, avec la tâche de punition programmée
    private final Map<UUID, BukkitTask> graceTasks = new HashMap<>();

    // Snapshot (position + inventaire) capturé à la déconnexion, utilisé pour dropper les items au bon endroit
    private final Map<UUID, DisconnectSnapshot> disconnectSnapshots = new HashMap<>();

    // Joueurs à vider dès leur prochaine connexion (drop déjà fait). Persisté sur disque pour
    // survivre à un crash/redémarrage du serveur entre la punition et la reconnexion.
    private final Set<UUID> pendingClear = new HashSet<>();
    private File pendingFile;
    private FileConfiguration pendingConfig;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public CombatLogFeature(DeathBan plugin, DuelFeature duelFeature) {
        this.plugin = plugin;
        this.duelFeature = duelFeature;
        setupPendingFile();
        loadPendingClears();
    }

    private void setupPendingFile() {
        pendingFile = new File(plugin.getDataFolder(), "pending_combatlog.yml");
        if (!pendingFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                pendingFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[CombatLog] Impossible de créer pending_combatlog.yml : " + e.getMessage());
            }
        }
        pendingConfig = YamlConfiguration.loadConfiguration(pendingFile);
    }

    private void loadPendingClears() {
        List<String> uuids = pendingConfig.getStringList("pending");
        for (String s : uuids) {
            try {
                pendingClear.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // entrée corrompue, on l'ignore
            }
        }
        if (!pendingClear.isEmpty()) {
            plugin.getLogger().info("[CombatLog] " + pendingClear.size()
                    + " punition(s) en attente chargée(s) depuis le fichier.");
        }
    }

    private void savePendingFile() {
        List<String> uuids = new ArrayList<>();
        for (UUID uuid : pendingClear) {
            uuids.add(uuid.toString());
        }
        pendingConfig.set("pending", uuids);
        try {
            pendingConfig.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[CombatLog] Impossible de sauvegarder pending_combatlog.yml : " + e.getMessage());
        }
    }

    public void reload() {
        pendingConfig = YamlConfiguration.loadConfiguration(pendingFile);
        plugin.getLogger().info("[CombatLog] Configuration rechargée depuis pending_combatlog.yml.");
    }

    // ===================== TAG DE COMBAT =====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Le combat de duel a déjà sa propre gestion de déconnexion
        if (duelFeature.isInDuel(victim) || duelFeature.isInDuel(attacker)) return;

        long now = System.currentTimeMillis();
        lastCombatMillis.put(victim.getUniqueId(), now);
        lastCombatMillis.put(attacker.getUniqueId(), now);
    }

    public boolean isInCombat(Player player) {
        Long last = lastCombatMillis.get(player.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last < COMBAT_TAG_MILLIS;
    }

    // ===================== DÉCONNEXION =====================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (duelFeature.isInDuel(player)) return; // géré par DuelFeature
        if (!isInCombat(player)) return;

        UUID uuid = player.getUniqueId();
        lastCombatMillis.remove(uuid);
        disconnectSnapshots.put(uuid, captureSnapshot(player));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            graceTasks.remove(uuid);

            Player stillOnline = Bukkit.getPlayer(uuid);
            if (stillOnline != null && stillOnline.isOnline()) return; // reconnecté entre temps (sécurité)

            DisconnectSnapshot snapshot = disconnectSnapshots.remove(uuid);
            if (snapshot != null) {
                World world = snapshot.location.getWorld();
                if (world != null) {
                    for (ItemStack item : snapshot.items) {
                        world.dropItemNaturally(snapshot.location, item);
                    }
                }
            }

            pendingClear.add(uuid);
            savePendingFile();

            Bukkit.broadcastMessage(ChatColor.RED + player.getName()
                    + " n'est pas revenu à temps après une déconnexion en combat : son inventaire a été vidé.");
            plugin.getLogger().info("[CombatLog] " + player.getName()
                    + " ne s'est pas reconnecté à temps, déco combat sanctionnée. Items droppés, inventaire vidé à la reconnexion.");
        }, DISCONNECT_GRACE_SECONDS * 20L);

        graceTasks.put(uuid, task);

        String time = timeFormat.format(new Date());
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " a déconnecté à " + time
                + " et a 5 minutes pour se reconnecter avant de perdre son inventaire (déco combat).");
    }

    /**
     * Capture la position et le contenu de l'inventaire du joueur au moment de sa déconnexion,
     * pour pouvoir faire dropper ses items au bon endroit si la sanction se confirme.
     */
    private DisconnectSnapshot captureSnapshot(Player player) {
        PlayerInventory inv = player.getInventory();

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) items.add(inv.getHelmet().clone());
        if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) items.add(inv.getChestplate().clone());
        if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) items.add(inv.getLeggings().clone());
        if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) items.add(inv.getBoots().clone());
        if (inv.getItemInOffHand().getType() != Material.AIR) items.add(inv.getItemInOffHand().clone());

        return new DisconnectSnapshot(player.getLocation().clone(), items);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Cas 1 : le joueur revient après que sa sanction a déjà été actée (items déjà droppés)
        if (pendingClear.contains(uuid)) {
            pendingClear.remove(uuid);
            savePendingFile();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.getInventory().clear();
                player.getInventory().setHelmet(null);
                player.getInventory().setChestplate(null);
                player.getInventory().setLeggings(null);
                player.getInventory().setBoots(null);
                player.getInventory().setItemInOffHand(null);

                player.sendMessage(ChatColor.RED + "Vous n'êtes pas revenu à temps après une déconnexion en combat.");
                player.sendMessage(ChatColor.RED + "Vos affaires ont été lâchées là où vous vous êtes déconnecté.");

                // Inventaire déjà vidé ci-dessus : ce kill ne fera dropper aucun item en double
                player.setHealth(0.0);
            });
            return;
        }

        // Cas 2 : le joueur revient à temps, avant l'expiration du délai de grâce
        BukkitTask task = graceTasks.remove(uuid);
        if (task != null) {
            task.cancel();
            disconnectSnapshots.remove(uuid);
            player.sendMessage(ChatColor.GREEN + "Vous vous êtes reconnecté à temps, pas de sanction pour déco combat.");
        }
    }

    // ===================== STRUCTURES INTERNES =====================

    private static class DisconnectSnapshot {
        final Location location;
        final List<ItemStack> items;

        DisconnectSnapshot(Location location, List<ItemStack> items) {
            this.location = location;
            this.items = items;
        }
    }
}

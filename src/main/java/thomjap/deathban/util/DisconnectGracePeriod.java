package thomjap.deathban.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/**
 * Orchestration commune aux sanctions différées à la déconnexion (forfait de duel,
 * déco combat...) : capture l'inventaire au moment de la déconnexion, laisse un délai
 * de grâce pour se reconnecter, et déclenche un callback de sanction si le délai expire
 * sans reconnexion entre-temps.
 */
public final class DisconnectGracePeriod {

    private final JavaPlugin plugin;
    private final LongSupplier graceSecondsSupplier;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, InventorySnapshot> snapshots = new HashMap<>();

    public DisconnectGracePeriod(JavaPlugin plugin, LongSupplier graceSecondsSupplier) {
        this.plugin = plugin;
        this.graceSecondsSupplier = graceSecondsSupplier;
    }

    public void start(Player player, BiConsumer<Player, InventorySnapshot> onExpire) {
        UUID uuid = player.getUniqueId();
        snapshots.put(uuid, InventorySnapshot.capture(player));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(uuid);
            InventorySnapshot snapshot = snapshots.remove(uuid);

            if (player.isOnline()) return; // reconnecté entre-temps (sécurité)

            onExpire.accept(player, snapshot);
        }, graceSecondsSupplier.getAsLong() * 20L);

        tasks.put(uuid, task);
    }

    /**
     * Annule le délai de grâce en attente pour ce joueur, s'il y en a un.
     * Retourne true si un délai était bien en attente (reconnexion à temps).
     */
    public boolean cancelIfPending(UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        snapshots.remove(uuid);
        if (task == null) return false;
        task.cancel();
        return true;
    }
}

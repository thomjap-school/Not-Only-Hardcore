package thomjap.deathban;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NickFeature implements Listener {

    // Nick en mémoire uniquement (pas de persistance)
    private final Map<UUID, String> nicks = new HashMap<>();

    public void setNick(Player player, String nick) {
        String colored = ChatColor.translateAlternateColorCodes('&', nick);
        nicks.put(player.getUniqueId(), colored);

        // Nom au-dessus de la tête + tab
        player.setCustomName(colored);
        player.setCustomNameVisible(true);
        player.setDisplayName(colored);
        player.setPlayerListName(colored);

        player.sendMessage(ChatColor.GREEN + "Votre nick est maintenant : " + colored);
    }

    public void resetNick(Player player) {
        nicks.remove(player.getUniqueId());

        player.setCustomName(null);
        player.setCustomNameVisible(false);
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());

        player.sendMessage(ChatColor.GREEN + "Votre nick a été réinitialisé.");
    }

    public boolean hasNick(UUID uuid) {
        return nicks.containsKey(uuid);
    }

    // Nettoie à la déconnexion (pas de persistance)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        nicks.remove(event.getPlayer().getUniqueId());
    }

    // Restaure le nick si le joueur se reconnecte dans la même session (edge case)
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nick = nicks.get(player.getUniqueId());
        if (nick != null) {
            player.setCustomName(nick);
            player.setCustomNameVisible(true);
            player.setDisplayName(nick);
            player.setPlayerListName(nick);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String nick = nicks.get(player.getUniqueId());
        if (nick != null) {
            event.setFormat(nick + " &r» %2$s");
            // Applique les couleurs dans le format
            event.setFormat(ChatColor.translateAlternateColorCodes('&', event.getFormat()));
        }
    }
}

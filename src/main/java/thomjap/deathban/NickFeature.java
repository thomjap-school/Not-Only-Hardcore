package thomjap.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NickFeature implements Listener {

    private final Map<UUID, String> nicks = new ConcurrentHashMap<>();

    public void setNick(Player player, String nick) {
        String colored = ChatColor.translateAlternateColorCodes('&', nick);
        nicks.put(player.getUniqueId(), colored);

        applyNickToScoreboard(player, colored);
        player.setDisplayName(colored);
        player.setPlayerListName(colored);

        player.sendMessage(ChatColor.GREEN + "Votre nick est maintenant : " + colored);
    }

    public void resetNick(Player player) {
        nicks.remove(player.getUniqueId());

        removeFromNickTeam(player);
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());

        player.sendMessage(ChatColor.GREEN + "Votre nick a été réinitialisé.");
    }

    public boolean hasNick(UUID uuid) {
        return nicks.containsKey(uuid);
    }

    private void applyNickToScoreboard(Player player, String colored) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "nick_" + player.getUniqueId().toString().substring(0, 16);

        removeFromNickTeam(player);

        Team team = board.registerNewTeam(teamName);
        // Prefix = nick, le nom vanilla est "écrasé" en le mettant dans le suffix vide
        // Minecraft affiche : [prefix][nom joueur][suffix]
        // On met le nick en prefix et on cache le nom avec un caractère obfusqué invisible
        team.setPrefix(colored + ChatColor.RESET);
        team.setSuffix("");
        // L'astuce : on override le nom affiché via setColor qui pousse le nom à droite
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.addEntry(player.getName());

        // On affiche notre propre custom name à la place
        player.setCustomName(colored);
        player.setCustomNameVisible(true);
    }

    private void removeFromNickTeam(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "nick_" + player.getUniqueId().toString().substring(0, 16);
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            team.unregister();
        }
        player.setCustomName(null);
        player.setCustomNameVisible(false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeFromNickTeam(player);
        nicks.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nick = nicks.get(player.getUniqueId());
        if (nick != null) {
            applyNickToScoreboard(player, nick);
            player.setDisplayName(nick);
            player.setPlayerListName(nick);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String nick = nicks.get(player.getUniqueId());
        if (nick != null) {
            event.setFormat(nick + ChatColor.RESET + " » %2$s");
        }
    }
}

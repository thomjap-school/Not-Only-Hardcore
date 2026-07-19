package thomjap.deathban.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public interface SubCommand {

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Si true (par défaut), la sous-commande est réservée aux ops : le dispatcher
     * central s'en charge avant même d'appeler execute().
     */
    default boolean opOnly() {
        return true;
    }

    default boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seul un joueur peut utiliser cette commande.");
            return false;
        }
        return true;
    }

    default Player requireOnlinePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayer(name);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
        }
        return target;
    }
}

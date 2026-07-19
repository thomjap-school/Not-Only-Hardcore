package thomjap.deathban.commands;

import thomjap.deathban.NickFeature;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NickSubCommand implements SubCommand {

    private final NickFeature nickFeature;

    public NickSubCommand(NickFeature nickFeature) {
        this.nickFeature = nickFeature;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage : /db nick <pseudo> | /db nick reset [joueur]");
            return true;
        }

        // /db nick reset [joueur]
        if (args[1].equalsIgnoreCase("reset")) {
            Player target;
            if (args.length >= 3) {
                target = requireOnlinePlayer(sender, args[2]);
                if (target == null) return true;
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Précisez un joueur : /db nick reset <joueur>");
                    return true;
                }
                target = (Player) sender;
            }
            nickFeature.resetNick(target);
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.GREEN + "Nick de " + target.getName() + " réinitialisé.");
            }
            return true;
        }

        // /db nick <pseudo> — s'applique à soi-même ou à un joueur cible
        // Support : /db nick <pseudo> ou /db nick <joueur> <pseudo>
        Player target;
        String nick;

        if (args.length >= 3 && Bukkit.getPlayer(args[1]) != null) {
            // /db nick <joueur> <pseudo>
            target = Bukkit.getPlayer(args[1]);
            nick = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            // /db nick <pseudo>
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Usage depuis la console : /db nick <joueur> <pseudo>");
                return true;
            }
            target = (Player) sender;
            nick = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        if (nick.length() > 32) {
            sender.sendMessage(ChatColor.RED + "Le nick ne peut pas dépasser 32 caractères.");
            return true;
        }

        nickFeature.setNick(target, nick);
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Nick de " + target.getName() + " changé en : "
                    + ChatColor.translateAlternateColorCodes('&', nick));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            completions.add("reset");
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            return completions;
        }
        return SubCommand.super.tabComplete(sender, args);
    }
}

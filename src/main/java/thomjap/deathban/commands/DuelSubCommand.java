package thomjap.deathban.commands;

import thomjap.deathban.DuelFeature;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DuelSubCommand implements SubCommand {

    private final DuelFeature duelFeature;

    public DuelSubCommand(DuelFeature duelFeature) {
        this.duelFeature = duelFeature;
    }

    @Override
    public boolean opOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage : /db duel <joueur> | /db duel accept|deny <joueur>");
            return true;
        }

        String second = args[1].toLowerCase();

        if (second.equals("accept") || second.equals("deny")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage : /db duel " + second + " <joueur>");
                return true;
            }
            String requesterName = args[2];
            if (second.equals("accept")) {
                duelFeature.acceptRequest(player, requesterName);
            } else {
                duelFeature.denyRequest(player, requesterName);
            }
            return true;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return true;

        duelFeature.sendRequest(player, target);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(Arrays.asList("accept", "deny"));
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length == 3 && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }
        return completions;
    }
}

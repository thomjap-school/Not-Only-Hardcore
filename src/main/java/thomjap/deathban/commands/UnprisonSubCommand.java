package thomjap.deathban.commands;

import thomjap.deathban.PrisonFeature;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class UnprisonSubCommand implements SubCommand {

    private final PrisonFeature prisonFeature;

    public UnprisonSubCommand(PrisonFeature prisonFeature) {
        this.prisonFeature = prisonFeature;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage : /db unprison <joueur>");
            return true;
        }

        Player target = requireOnlinePlayer(sender, args[1]);
        if (target == null) return true;

        prisonFeature.release(target);
        sender.sendMessage(ChatColor.GREEN + target.getName() + " a été libéré de prison.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return SubCommand.super.tabComplete(sender, args);
    }
}

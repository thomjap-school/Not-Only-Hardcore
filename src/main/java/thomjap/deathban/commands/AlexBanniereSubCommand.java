package thomjap.deathban.commands;

import thomjap.deathban.AlexBanniereFeature;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class AlexBanniereSubCommand implements SubCommand {

    private final AlexBanniereFeature alexBanniereFeature;

    public AlexBanniereSubCommand(AlexBanniereFeature alexBanniereFeature) {
        this.alexBanniereFeature = alexBanniereFeature;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String status = alexBanniereFeature.isEnabled() ? ChatColor.GREEN + "activé" : ChatColor.RED + "désactivé";
            sender.sendMessage(ChatColor.YELLOW + "AlexBanniere est actuellement " + status + ChatColor.YELLOW + ".");
            sender.sendMessage(ChatColor.YELLOW + "Usage : /db alexbanniere on|off");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "on":
                alexBanniereFeature.setEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "AlexBanniere activé : " + alexBanniereFeature.getTargetPlayerName() + " ne peut plus avoir de bannières.");
                break;
            case "off":
                alexBanniereFeature.setEnabled(false);
                sender.sendMessage(ChatColor.GREEN + "AlexBanniere désactivé.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Usage : /db alexbanniere on|off");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("on", "off");
        }
        return SubCommand.super.tabComplete(sender, args);
    }
}

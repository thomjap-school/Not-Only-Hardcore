package thomjap.deathban.commands;

import thomjap.deathban.AlexBanniereFeature;
import thomjap.deathban.AutoMessageFeature;
import thomjap.deathban.CombatLogFeature;
import thomjap.deathban.ConfigManager;
import thomjap.deathban.DuelFeature;
import thomjap.deathban.KitFeature;
import thomjap.deathban.NickFeature;
import thomjap.deathban.PrisonFeature;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeathBanCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public DeathBanCommand(ConfigManager configManager, PrisonFeature prisonFeature, DuelFeature duelFeature,
                            AlexBanniereFeature alexBanniereFeature, AutoMessageFeature autoMessageFeature,
                            KitFeature kitFeature, NickFeature nickFeature, CombatLogFeature combatLogFeature) {
        subCommands.put("unprison", new UnprisonSubCommand(prisonFeature));
        subCommands.put("set", new SetSubCommand(configManager, autoMessageFeature));
        subCommands.put("reload", new ReloadSubCommand(configManager, prisonFeature, duelFeature, autoMessageFeature, kitFeature, combatLogFeature));
        subCommands.put("duel", new DuelSubCommand(duelFeature));
        subCommands.put("alexbanniere", new AlexBanniereSubCommand(alexBanniereFeature));
        subCommands.put("kit", new KitSubCommand(kitFeature));
        subCommands.put("nick", new NickSubCommand(nickFeature));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        if (subCommand.opOnly() && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        return subCommand.execute(sender, args);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== DeathBan ===");
        sender.sendMessage(ChatColor.YELLOW + "/db unprison <joueur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set prison <x|y|z|world|yaw|pitch|duration> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set head <effect|duration|amplifier|drop> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set deathsound <sound|volume|pitch> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set protection <duration|amplifier> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set releaseprotection <duration|amplifier> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel world <nom>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel delay <secondes>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel request-expiry <secondes>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel <1|2|3> <a|b> <x|y|z|yaw|pitch> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db nick <pseudo>");
        sender.sendMessage(ChatColor.YELLOW + "/db nick reset [joueur]");
        sender.sendMessage(ChatColor.YELLOW + "/db kit");
        sender.sendMessage(ChatColor.YELLOW + "/db duel <joueur>");
        sender.sendMessage(ChatColor.YELLOW + "/db duel accept|deny <joueur>");
        sender.sendMessage(ChatColor.YELLOW + "/db alexbanniere on|off");
        sender.sendMessage(ChatColor.YELLOW + "/db set automessage on|off");
        sender.sendMessage(ChatColor.YELLOW + "/db set automessage interval <minutes>");
        sender.sendMessage(ChatColor.YELLOW + "/db reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(subCommands.keySet());
        } else {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                completions.addAll(subCommand.tabComplete(sender, args));
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }
}

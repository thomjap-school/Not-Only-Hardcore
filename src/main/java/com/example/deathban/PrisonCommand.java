package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrisonCommand implements CommandExecutor {

    private final PrisonFeature prisonFeature;

    public PrisonCommand(PrisonFeature prisonFeature) {
        this.prisonFeature = prisonFeature;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("unprison")) {
            sender.sendMessage(ChatColor.RED + "Usage : /db unprison <joueur>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
            return true;
        }

        prisonFeature.release(target);
        sender.sendMessage(ChatColor.GREEN + target.getName() + " a été libéré de prison.");
        return true;
    }
}

package thomjap.deathban.commands;

import thomjap.deathban.KitFeature;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitSubCommand implements SubCommand {

    private final KitFeature kitFeature;

    public KitSubCommand(KitFeature kitFeature) {
        this.kitFeature = kitFeature;
    }

    @Override
    public boolean opOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return true;
        kitFeature.giveKit((Player) sender);
        return true;
    }
}

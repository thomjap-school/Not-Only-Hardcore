package thomjap.deathban.commands;

import thomjap.deathban.AutoMessageFeature;
import thomjap.deathban.CombatLogFeature;
import thomjap.deathban.ConfigManager;
import thomjap.deathban.DuelFeature;
import thomjap.deathban.KitFeature;
import thomjap.deathban.PrisonFeature;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand implements SubCommand {

    private final ConfigManager configManager;
    private final PrisonFeature prisonFeature;
    private final DuelFeature duelFeature;
    private final AutoMessageFeature autoMessageFeature;
    private final KitFeature kitFeature;
    private final CombatLogFeature combatLogFeature;

    public ReloadSubCommand(ConfigManager configManager, PrisonFeature prisonFeature, DuelFeature duelFeature,
                             AutoMessageFeature autoMessageFeature, KitFeature kitFeature, CombatLogFeature combatLogFeature) {
        this.configManager = configManager;
        this.prisonFeature = prisonFeature;
        this.duelFeature = duelFeature;
        this.autoMessageFeature = autoMessageFeature;
        this.kitFeature = kitFeature;
        this.combatLogFeature = combatLogFeature;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        configManager.reload();
        prisonFeature.reload();
        duelFeature.reload();
        autoMessageFeature.reload();
        kitFeature.reload();
        combatLogFeature.reload();
        sender.sendMessage(ChatColor.GREEN + "Configuration rechargée (config.yml, prisoners.yml, pending_forfeits.yml, automessage.yml, kit.yml, pending_combatlog.yml).");
        return true;
    }
}

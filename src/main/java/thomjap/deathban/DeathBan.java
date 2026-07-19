package thomjap.deathban;

import thomjap.deathban.commands.DeathBanCommand;
import thomjap.deathban.listeners.DeathListener;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathBan extends JavaPlugin {

    private PrisonFeature prisonFeature;
    private ConfigManager configManager;
    private DuelFeature duelFeature;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        duelFeature = new DuelFeature(this, configManager);
        Bukkit.getPluginManager().registerEvents(duelFeature, this);

        Bukkit.getPluginManager().registerEvents(new EmptyInventoryProtectionFeature(duelFeature), this);

        Bukkit.getPluginManager().registerEvents(new HeadFeature(this, configManager, duelFeature), this);
        Bukkit.getPluginManager().registerEvents(new NewPlayerProtection(this, configManager), this);

        CombatLogFeature combatLogFeature = new CombatLogFeature(this, configManager, duelFeature);
        Bukkit.getPluginManager().registerEvents(combatLogFeature, this);

        prisonFeature = new PrisonFeature(this, configManager);
        Bukkit.getPluginManager().registerEvents(prisonFeature, this);
        duelFeature.setPrisonFeature(prisonFeature);

        AlexBanniereFeature alexBanniereFeature = new AlexBanniereFeature(this, configManager);
        Bukkit.getPluginManager().registerEvents(alexBanniereFeature, this);

        AutoMessageFeature autoMessageFeature = new AutoMessageFeature(this);
        KitFeature kitFeature = new KitFeature(this);
        prisonFeature.setKitFeature(kitFeature);

        NickFeature nickFeature = new NickFeature();
        Bukkit.getPluginManager().registerEvents(nickFeature, this);

        DeathBanCommand deathBanCommand = new DeathBanCommand(configManager, prisonFeature, duelFeature, alexBanniereFeature, autoMessageFeature, kitFeature, nickFeature, combatLogFeature);
        PluginCommand dbCommand = getCommand("db");
        if (dbCommand == null) {
            getLogger().severe("La commande 'db' est introuvable — vérifie plugin.yml.");
        } else {
            dbCommand.setExecutor(deathBanCommand);
            dbCommand.setTabCompleter(deathBanCommand);
        }

        Bukkit.getPluginManager().registerEvents(
                new DeathListener(this, prisonFeature, configManager),
                this
        );

        getLogger().info("DeathBan activé.");
    }
}

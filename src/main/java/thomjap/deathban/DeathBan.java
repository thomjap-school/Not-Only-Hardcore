package thomjap.deathban;

import thomjap.deathban.commands.DeathBanCommand;
import thomjap.deathban.listeners.DeathListener;
import thomjap.deathban.listeners.NewPlayerProtection;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathBan extends JavaPlugin implements Listener {

    private PrisonFeature prisonFeature;
    private ConfigManager configManager;
    private DuelFeature duelFeature;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        duelFeature = new DuelFeature(this, configManager);
        Bukkit.getPluginManager().registerEvents(duelFeature, this);

        Bukkit.getPluginManager().registerEvents(new HeadFeature(this, configManager, duelFeature), this);
        Bukkit.getPluginManager().registerEvents(new NewPlayerProtection(this, configManager), this);

        CombatLogFeature combatLogFeature = new CombatLogFeature(this, duelFeature);
        Bukkit.getPluginManager().registerEvents(combatLogFeature, this);

        prisonFeature = new PrisonFeature(this, configManager);
        Bukkit.getPluginManager().registerEvents(prisonFeature, this);
        duelFeature.setPrisonFeature(prisonFeature);

        AlexBanniereFeature alexBanniereFeature = new AlexBanniereFeature(this, true);
        Bukkit.getPluginManager().registerEvents(alexBanniereFeature, this);

        AutoMessageFeature autoMessageFeature = new AutoMessageFeature(this);
        KitFeature kitFeature = new KitFeature(this);
        prisonFeature.setKitFeature(kitFeature);

        NickFeature nickFeature = new NickFeature();
        Bukkit.getPluginManager().registerEvents(nickFeature, this);

        DeathBanCommand deathBanCommand = new DeathBanCommand(configManager, prisonFeature, duelFeature, alexBanniereFeature, autoMessageFeature, kitFeature, nickFeature, combatLogFeature);
        getCommand("db").setExecutor(deathBanCommand);
        getCommand("db").setTabCompleter(deathBanCommand);

        Bukkit.getPluginManager().registerEvents(
                new DeathListener(this, prisonFeature, configManager),
                this
        );

        getLogger().info("DeathBan activé.");
    }

    @EventHandler
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Pendant un duel, le combat doit toujours pouvoir se terminer
        if (duelFeature.isInDuel(victim) && duelFeature.isInDuel(attacker)) {
            return;
        }

        if (isInventoryEmpty(victim)) {
            event.setCancelled(true);
        }
    }

    private boolean isInventoryEmpty(Player player) {
        PlayerInventory inv = player.getInventory();

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }

        if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) return false;
        if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) return false;
        if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) return false;
        if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) return false;
        if (inv.getItemInOffHand().getType() != Material.AIR) return false;

        return true;
    }
}

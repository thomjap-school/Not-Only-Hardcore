package thomjap.deathban;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EmptyInventoryProtectionFeature implements Listener {

    private final DuelFeature duelFeature;

    public EmptyInventoryProtectionFeature(DuelFeature duelFeature) {
        this.duelFeature = duelFeature;
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

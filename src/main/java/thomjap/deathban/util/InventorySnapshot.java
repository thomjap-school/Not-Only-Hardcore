package thomjap.deathban.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Capture de la position et de l'inventaire complet (contenu + armure + offhand)
 * d'un joueur à un instant T, pour pouvoir dropper ses items au bon endroit plus
 * tard même s'il s'est déconnecté entre-temps (le vrai inventaire du joueur reste
 * inchangé : c'est une copie).
 */
public final class InventorySnapshot {

    private final Location location;
    private final List<ItemStack> items;

    private InventorySnapshot(Location location, List<ItemStack> items) {
        this.location = location;
        this.items = items;
    }

    public static InventorySnapshot capture(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>();

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        if (inv.getHelmet() != null && inv.getHelmet().getType() != Material.AIR) items.add(inv.getHelmet().clone());
        if (inv.getChestplate() != null && inv.getChestplate().getType() != Material.AIR) items.add(inv.getChestplate().clone());
        if (inv.getLeggings() != null && inv.getLeggings().getType() != Material.AIR) items.add(inv.getLeggings().clone());
        if (inv.getBoots() != null && inv.getBoots().getType() != Material.AIR) items.add(inv.getBoots().clone());
        if (inv.getItemInOffHand().getType() != Material.AIR) items.add(inv.getItemInOffHand().clone());

        return new InventorySnapshot(player.getLocation().clone(), items);
    }

    public Location getLocation() {
        return location;
    }

    public void dropItems() {
        World world = location.getWorld();
        if (world == null) return;

        for (ItemStack item : items) {
            world.dropItemNaturally(location, item);
        }
    }
}

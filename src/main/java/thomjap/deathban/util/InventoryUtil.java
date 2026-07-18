package thomjap.deathban.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Vide entièrement l'inventaire d'un joueur : contenu principal + armure + offhand.
     */
    public static void clearFully(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
        inv.setItemInOffHand(null);
    }
}

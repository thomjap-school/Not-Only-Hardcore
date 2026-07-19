package thomjap.deathban.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PotionEffects {

    private PotionEffects() {
    }

    public static void applyTimedResistance(Player player, int durationMinutes, int amplifier) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                durationMinutes * 60 * 20,
                amplifier,
                false,
                true
        ));
    }
}

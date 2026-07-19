package thomjap.deathban;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public class ConfigManager {

    private final DeathBan plugin;
    private FileConfiguration config;

    public ConfigManager(DeathBan plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        applyDefaults();
    }

    /**
     * Remplit le config.yml avec des valeurs par défaut si elles sont absentes,
     * sans écraser celles déjà définies.
     */
    private void applyDefaults() {
        config.addDefault("prison.world", "Jail");
        config.addDefault("prison.x", -32.0);
        config.addDefault("prison.y", 28.0);
        config.addDefault("prison.z", -74.0);
        config.addDefault("prison.yaw", 0.0);
        config.addDefault("prison.pitch", 0.0);
        config.addDefault("prison.duration-minutes", 48 * 60);

        config.addDefault("head.drop-chance-percent", 100.0);
        config.addDefault("head.effect", "POISON");
        config.addDefault("head.duration-seconds", 30);
        config.addDefault("head.amplifier", 0);

        config.addDefault("death.sound", "ENTITY_WITHER_SPAWN");
        config.addDefault("death.sound-volume", 1.0);
        config.addDefault("death.sound-pitch", 1.0);

        config.addDefault("new-player-protection.duration-minutes", 30);
        config.addDefault("new-player-protection.resistance-amplifier", 4);

        config.addDefault("release-protection.duration-minutes", 5);
        config.addDefault("release-protection.resistance-amplifier", 4);

        config.addDefault("duel.world", "duel");
        config.addDefault("duel.return-delay-seconds", 30);
        config.addDefault("duel.request-expiry-seconds", 60);
        config.addDefault("duel.zone-count", 3);
        config.addDefault("duel.item-cleanup-radius-blocks", 100.0);
        int zoneCount = config.getInt("duel.zone-count", 3);
        for (int zone = 1; zone <= zoneCount; zone++) {
            for (String slot : new String[]{"a", "b"}) {
                String base = "duel.zones." + zone + "." + slot + ".";
                config.addDefault(base + "x", 0.0);
                config.addDefault(base + "y", 100.0);
                config.addDefault(base + "z", 0.0);
                config.addDefault(base + "yaw", 0.0);
                config.addDefault(base + "pitch", 0.0);
            }
        }

        config.addDefault("combat.disconnect-grace-seconds", 5 * 60);
        config.addDefault("combat.tag-seconds", 20);

        config.addDefault("alexbanniere.enabled", true);
        config.addDefault("alexbanniere.target-player", "AlexJanOne");

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        applyDefaults();
    }

    public void save() {
        plugin.saveConfig();
    }

    // ===================== PRISON =====================

    public String getPrisonWorldName() {
        return config.getString("prison.world", "Jail");
    }

    public void setPrisonWorldName(String world) {
        config.set("prison.world", world);
        save();
    }

    public double getPrisonX() {
        return config.getDouble("prison.x");
    }

    public void setPrisonX(double x) {
        config.set("prison.x", x);
        save();
    }

    public double getPrisonY() {
        return config.getDouble("prison.y");
    }

    public void setPrisonY(double y) {
        config.set("prison.y", y);
        save();
    }

    public double getPrisonZ() {
        return config.getDouble("prison.z");
    }

    public void setPrisonZ(double z) {
        config.set("prison.z", z);
        save();
    }

    public float getPrisonYaw() {
        return (float) config.getDouble("prison.yaw");
    }

    public void setPrisonYaw(float yaw) {
        config.set("prison.yaw", (double) yaw);
        save();
    }

    public float getPrisonPitch() {
        return (float) config.getDouble("prison.pitch");
    }

    public void setPrisonPitch(float pitch) {
        config.set("prison.pitch", (double) pitch);
        save();
    }

    public long getPrisonDurationMinutes() {
        return config.getLong("prison.duration-minutes");
    }

    public void setPrisonDurationMinutes(long minutes) {
        config.set("prison.duration-minutes", minutes);
        save();
    }

    // ===================== HEAD =====================

    public double getHeadDropChancePercent() {
        return config.getDouble("head.drop-chance-percent");
    }

    public void setHeadDropChancePercent(double percent) {
        config.set("head.drop-chance-percent", percent);
        save();
    }

    public PotionEffectType getHeadEffect() {
        String name = config.getString("head.effect", "POISON");
        PotionEffectType type = PotionEffectType.getByName(name);
        return type != null ? type : PotionEffectType.POISON;
    }

    public void setHeadEffect(String effectName) {
        config.set("head.effect", effectName.toUpperCase());
        save();
    }

    public int getHeadDurationSeconds() {
        return config.getInt("head.duration-seconds");
    }

    public void setHeadDurationSeconds(int seconds) {
        config.set("head.duration-seconds", seconds);
        save();
    }

    public int getHeadAmplifier() {
        return config.getInt("head.amplifier");
    }

    public void setHeadAmplifier(int amplifier) {
        config.set("head.amplifier", amplifier);
        save();
    }

    // ===================== DEATH SOUND =====================

    public Sound getDeathSound() {
        String name = config.getString("death.sound", "entity.wither_spawn");
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase()));
        return sound != null ? sound : Sound.ENTITY_WITHER_SPAWN;
    }
    
    public void setDeathSound(String soundName) {
        config.set("death.sound", soundName.toLowerCase());
        save();
    }

    public float getDeathSoundVolume() {
        return (float) config.getDouble("death.sound-volume");
    }

    public void setDeathSoundVolume(float volume) {
        config.set("death.sound-volume", (double) volume);
        save();
    }

    public float getDeathSoundPitch() {
        return (float) config.getDouble("death.sound-pitch");
    }

    public void setDeathSoundPitch(float pitch) {
        config.set("death.sound-pitch", (double) pitch);
        save();
    }

    // ===================== NEW PLAYER PROTECTION =====================

    public int getNewPlayerProtectionDurationMinutes() {
        return config.getInt("new-player-protection.duration-minutes");
    }

    public void setNewPlayerProtectionDurationMinutes(int minutes) {
        config.set("new-player-protection.duration-minutes", minutes);
        save();
    }

    public int getNewPlayerProtectionAmplifier() {
        return config.getInt("new-player-protection.resistance-amplifier");
    }

    public void setNewPlayerProtectionAmplifier(int amplifier) {
        config.set("new-player-protection.resistance-amplifier", amplifier);
        save();
    }

    // ===================== RELEASE PROTECTION =====================

    public int getReleaseProtectionDurationMinutes() {
        return config.getInt("release-protection.duration-minutes");
    }

    public void setReleaseProtectionDurationMinutes(int minutes) {
        config.set("release-protection.duration-minutes", minutes);
        save();
    }

    public int getReleaseProtectionAmplifier() {
        return config.getInt("release-protection.resistance-amplifier");
    }

    public void setReleaseProtectionAmplifier(int amplifier) {
        config.set("release-protection.resistance-amplifier", amplifier);
        save();
    }

    // ===================== DUEL =====================

    public String getDuelWorldName() {
        return config.getString("duel.world", "duel");
    }

    public void setDuelWorldName(String world) {
        config.set("duel.world", world);
        save();
    }

    public int getDuelReturnDelaySeconds() {
        return config.getInt("duel.return-delay-seconds", 30);
    }

    public void setDuelReturnDelaySeconds(int seconds) {
        config.set("duel.return-delay-seconds", seconds);
        save();
    }

    public int getDuelRequestExpirySeconds() {
        return config.getInt("duel.request-expiry-seconds", 60);
    }

    public void setDuelRequestExpirySeconds(int seconds) {
        config.set("duel.request-expiry-seconds", seconds);
        save();
    }

    public int getDuelZoneCount() {
        return config.getInt("duel.zone-count", 3);
    }

    public double getDuelItemCleanupRadiusBlocks() {
        return config.getDouble("duel.item-cleanup-radius-blocks", 100.0);
    }

    public void setDuelItemCleanupRadiusBlocks(double radiusBlocks) {
        config.set("duel.item-cleanup-radius-blocks", radiusBlocks);
        save();
    }

    public boolean isValidZone(int zone) {
        return zone >= 1 && zone <= getDuelZoneCount();
    }

    public static boolean isValidSlot(String slot) {
        return slot.equalsIgnoreCase("a") || slot.equalsIgnoreCase("b");
    }

    private String duelBase(int zone, String slot) {
        return "duel.zones." + zone + "." + slot.toLowerCase() + ".";
    }

    public double getDuelX(int zone, String slot) {
        return config.getDouble(duelBase(zone, slot) + "x");
    }

    public double getDuelY(int zone, String slot) {
        return config.getDouble(duelBase(zone, slot) + "y");
    }

    public double getDuelZ(int zone, String slot) {
        return config.getDouble(duelBase(zone, slot) + "z");
    }

    public float getDuelYaw(int zone, String slot) {
        return (float) config.getDouble(duelBase(zone, slot) + "yaw");
    }

    public float getDuelPitch(int zone, String slot) {
        return (float) config.getDouble(duelBase(zone, slot) + "pitch");
    }

    public void setDuelCoordinate(int zone, String slot, String axis, double value) {
        config.set(duelBase(zone, slot) + axis.toLowerCase(), value);
        save();
    }

    // ===================== COMBAT =====================

    public long getCombatDisconnectGraceSeconds() {
        return config.getLong("combat.disconnect-grace-seconds", 5 * 60);
    }

    public void setCombatDisconnectGraceSeconds(long seconds) {
        config.set("combat.disconnect-grace-seconds", seconds);
        save();
    }

    public int getCombatTagSeconds() {
        return config.getInt("combat.tag-seconds", 20);
    }

    public void setCombatTagSeconds(int seconds) {
        config.set("combat.tag-seconds", seconds);
        save();
    }

    // ===================== ALEXBANNIERE =====================

    public boolean isAlexBanniereEnabled() {
        return config.getBoolean("alexbanniere.enabled", true);
    }

    public void setAlexBanniereEnabled(boolean enabled) {
        config.set("alexbanniere.enabled", enabled);
        save();
    }

    public String getAlexBanniereTargetPlayer() {
        return config.getString("alexbanniere.target-player", "AlexJanOne");
    }

    public void setAlexBanniereTargetPlayer(String name) {
        config.set("alexbanniere.target-player", name);
        save();
    }
}

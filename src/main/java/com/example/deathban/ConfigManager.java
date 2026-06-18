package com.example.deathban;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

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

    public String getHeadEffectName() {
        return config.getString("head.effect", "POISON");
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
        String name = config.getString("death.sound", "ENTITY_WITHER_SPAWN");
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_WITHER_SPAWN;
        }
    }

    public void setDeathSound(String soundName) {
        config.set("death.sound", soundName.toUpperCase());
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
}
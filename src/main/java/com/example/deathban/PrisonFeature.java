package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrisonFeature implements Listener {

    private static final long PRISON_DURATION_MINUTES = (48*60);

    private static final String PRISON_WORLD_NAME = "Jail";

    private static final double PRISON_X = -32;
    private static final double PRISON_Y = 28;
    private static final double PRISON_Z = -74;
    private static final float PRISON_YAW = 0.0f;
    private static final float PRISON_PITCH = 0.0f;

    private final DeathBan plugin;
    private final Map<UUID, PrisonData> prisoners = new HashMap<>();
    private final java.util.Set<UUID> releasing = new java.util.HashSet<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public PrisonFeature(DeathBan plugin) {
        this.plugin = plugin;
        setupDataFile();
        loadPrisoners();
        startReleaseChecker();
    }

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "prisoners.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer prisoners.yml : " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder prisoners.yml : " + e.getMessage());
        }
    }

    private void loadPrisoners() {
        if (dataConfig.contains("prisoners")) {
            for (String key : dataConfig.getConfigurationSection("prisoners").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String path = "prisoners." + key + ".";

                long releaseTimeMillis = dataConfig.getLong(path + "releaseTime");
                String worldName = dataConfig.getString(path + "world");
                double x = dataConfig.getDouble(path + "x");
                double y = dataConfig.getDouble(path + "y");
                double z = dataConfig.getDouble(path + "z");
                float yaw = (float) dataConfig.getDouble(path + "yaw");
                float pitch = (float) dataConfig.getDouble(path + "pitch");

                PrisonData data = new PrisonData(releaseTimeMillis, worldName, x, y, z, yaw, pitch);
                prisoners.put(uuid, data);
            }
            plugin.getLogger().info("[Prison] " + prisoners.size() + " prisonnier(s) chargé(s) depuis le fichier.");
        }
    }

    private void savePrisoner(UUID uuid, PrisonData data) {
        String path = "prisoners." + uuid.toString() + ".";
        dataConfig.set(path + "ign", Bukkit.getOfflinePlayer(uuid).getName());
        dataConfig.set(path + "releaseTime", data.releaseTimeMillis);
        dataConfig.set(path + "world", data.returnWorldName);
        dataConfig.set(path + "x", data.returnX);
        dataConfig.set(path + "y", data.returnY);
        dataConfig.set(path + "z", data.returnZ);
        dataConfig.set(path + "yaw", data.returnYaw);
        dataConfig.set(path + "pitch", data.returnPitch);
        saveData();
    }

    private void removePrisoner(UUID uuid) {
        dataConfig.set("prisoners." + uuid.toString(), null);
        saveData();
        prisoners.remove(uuid);
    }

    public void imprison(Player player) {
        UUID uuid = player.getUniqueId();
        if (prisoners.containsKey(uuid)) return;
        Location currentLocation = player.getLocation();

        long releaseTimeMillis = System.currentTimeMillis() + (PRISON_DURATION_MINUTES * 60 * 1000);

        PrisonData data = new PrisonData(
                releaseTimeMillis,
                currentLocation.getWorld().getName(),
                currentLocation.getX(),
                currentLocation.getY(),
                currentLocation.getZ(),
                currentLocation.getYaw(),
                currentLocation.getPitch()
        );

        prisoners.put(uuid, data);
        savePrisoner(uuid, data);

        teleportToPrison(player);
    }

    private void teleportToPrison(Player player) {
        World prisonWorld = Bukkit.getWorld(PRISON_WORLD_NAME);
        if (prisonWorld == null) {
            plugin.getLogger().severe("[Prison] Le monde '" + PRISON_WORLD_NAME + "' n'existe pas ! Vérifie la configuration.");
            return;
        }

        Location prisonLocation = new Location(prisonWorld, PRISON_X, PRISON_Y, PRISON_Z, PRISON_YAW, PRISON_PITCH);
        player.teleport(prisonLocation);
        long remainingMillis = prisoners.get(player.getUniqueId()).releaseTimeMillis - System.currentTimeMillis();
        long remainingMinutes = remainingMillis / 1000 / 60;
        long remainingSeconds = (remainingMillis / 1000) % 60;
    
        player.sendMessage(ChatColor.RED + "Vous êtes emprisonné. Temps restant : " + remainingMinutes + "m " + remainingSeconds + "s");
    }

    private void release(Player player) {
        UUID uuid = player.getUniqueId();
        if (prisoners.get(uuid) == null) return;

        Location releaseLocation = getReleaseLocation(player);

        player.getInventory().clear();
        releasing.add(uuid);
        player.teleport(releaseLocation);
        releasing.remove(uuid);

        player.sendMessage(ChatColor.GREEN + "Vous êtes libéré !");
        removePrisoner(uuid);
    }

    /**
     * Retourne la destination de libération du joueur :
     *  1. Son spawn de lit/respawn anchor s'il en a un valide.
     *  2. Sinon le spawn du monde principal ("world").
     */
    private Location getReleaseLocation(Player player) {
        // 1. Spawn de lit (ou respawn anchor en 1.16+)
        Location bedSpawn = player.getBedSpawnLocation();
        if (bedSpawn != null) {
            plugin.getLogger().info("[Prison] " + player.getName() + " libéré à son spawn de lit.");
            return bedSpawn;
        }

        // 2. Spawn du monde principal
        World mainWorld = Bukkit.getWorld("world");
        if (mainWorld == null) {
            // Fallback ultime : premier monde chargé
            mainWorld = Bukkit.getWorlds().get(0);
        }
        plugin.getLogger().info("[Prison] " + player.getName() + " libéré au spawn du monde principal.");
        return mainWorld.getSpawnLocation();
    }

    private void startReleaseChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Map.Entry<UUID, PrisonData> entry : new HashMap<>(prisoners).entrySet()) {
                    UUID uuid = entry.getKey();
                    PrisonData data = entry.getValue();

                    if (now >= data.releaseTimeMillis) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            release(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 30);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!prisoners.containsKey(uuid)) return;

        PrisonData data = prisoners.get(uuid);
        long now = System.currentTimeMillis();

        if (now >= data.releaseTimeMillis) {
            release(player);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> teleportToPrison(player));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!prisoners.containsKey(uuid)) return;

        World prisonWorld = Bukkit.getWorld(PRISON_WORLD_NAME);
        if (prisonWorld == null) return;

        event.setRespawnLocation(new Location(prisonWorld, PRISON_X, PRISON_Y, PRISON_Z, PRISON_YAW, PRISON_PITCH));
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!prisoners.containsKey(uuid)) return;
        if (releasing.contains(uuid)) return; // TP de libération, on laisse passer
        if (event.getTo() == null) return;

        if (!event.getTo().getWorld().getName().equals(PRISON_WORLD_NAME)) {
            event.setCancelled(true);
        }
    }

    private static class PrisonData {
        final long releaseTimeMillis;
        final String returnWorldName;
        final double returnX, returnY, returnZ;
        final float returnYaw, returnPitch;

        PrisonData(long releaseTimeMillis, String returnWorldName, double returnX, double returnY, double returnZ, float returnYaw, float returnPitch) {
            this.releaseTimeMillis = releaseTimeMillis;
            this.returnWorldName = returnWorldName;
            this.returnX = returnX;
            this.returnY = returnY;
            this.returnZ = returnZ;
            this.returnYaw = returnYaw;
            this.returnPitch = returnPitch;
        }
    }
}

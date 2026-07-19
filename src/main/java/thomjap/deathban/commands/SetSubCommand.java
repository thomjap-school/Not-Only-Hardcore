package thomjap.deathban.commands;

import thomjap.deathban.AutoMessageFeature;
import thomjap.deathban.ConfigManager;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SetSubCommand implements SubCommand {

    private static final List<String> CATEGORIES = Arrays.asList(
            "prison", "head", "deathsound", "protection", "releaseprotection", "duel", "automessage");

    private static final List<String> DUEL_AXES = Arrays.asList("x", "y", "z", "yaw", "pitch");
    private static final List<String> DUEL_SLOTS = Arrays.asList("a", "b");
    private static final List<String> DUEL_GLOBAL_KEYS = Arrays.asList("world", "delay", "request-expiry");
    private static final List<String> AUTOMESSAGE_KEYS = Arrays.asList("on", "off", "interval");

    private final ConfigManager configManager;
    private final AutoMessageFeature autoMessageFeature;

    private final Map<String, Function<String, String>> prisonSetters = new LinkedHashMap<>();
    private final Map<String, Function<String, String>> headSetters = new LinkedHashMap<>();
    private final Map<String, Function<String, String>> deathSoundSetters = new LinkedHashMap<>();
    private final Map<String, Function<String, String>> protectionSetters = new LinkedHashMap<>();
    private final Map<String, Function<String, String>> releaseProtectionSetters = new LinkedHashMap<>();

    public SetSubCommand(ConfigManager configManager, AutoMessageFeature autoMessageFeature) {
        this.configManager = configManager;
        this.autoMessageFeature = autoMessageFeature;
        registerSetters();
    }

    // Chaque setter retourne null si la valeur a été appliquée, ou un message d'erreur à afficher sinon.
    // NumberFormatException est laissée remonter : elle est interceptée une seule fois dans execute().
    private void registerSetters() {
        prisonSetters.put("x", v -> { configManager.setPrisonX(Double.parseDouble(v)); return null; });
        prisonSetters.put("y", v -> { configManager.setPrisonY(Double.parseDouble(v)); return null; });
        prisonSetters.put("z", v -> { configManager.setPrisonZ(Double.parseDouble(v)); return null; });
        prisonSetters.put("world", v -> { configManager.setPrisonWorldName(v); return null; });
        prisonSetters.put("yaw", v -> { configManager.setPrisonYaw(Float.parseFloat(v)); return null; });
        prisonSetters.put("pitch", v -> { configManager.setPrisonPitch(Float.parseFloat(v)); return null; });
        prisonSetters.put("duration", v -> { configManager.setPrisonDurationMinutes(Long.parseLong(v)); return null; });

        headSetters.put("effect", v -> {
            PotionEffectType type = PotionEffectType.getByName(v.toUpperCase());
            if (type == null) return "Effet de potion inconnu : " + v;
            configManager.setHeadEffect(v.toUpperCase());
            return null;
        });
        headSetters.put("duration", v -> { configManager.setHeadDurationSeconds(Integer.parseInt(v)); return null; });
        headSetters.put("amplifier", v -> { configManager.setHeadAmplifier(Integer.parseInt(v)); return null; });
        headSetters.put("drop", v -> {
            double percent = Double.parseDouble(v);
            if (percent < 0 || percent > 100) return "Le pourcentage doit être entre 0 et 100.";
            configManager.setHeadDropChancePercent(percent);
            return null;
        });

        deathSoundSetters.put("sound", v -> {
            try {
                Sound.valueOf(v.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Son inconnu : " + v;
            }
            configManager.setDeathSound(v.toUpperCase());
            return null;
        });
        deathSoundSetters.put("volume", v -> { configManager.setDeathSoundVolume(Float.parseFloat(v)); return null; });
        deathSoundSetters.put("pitch", v -> { configManager.setDeathSoundPitch(Float.parseFloat(v)); return null; });

        protectionSetters.put("duration", v -> { configManager.setNewPlayerProtectionDurationMinutes(Integer.parseInt(v)); return null; });
        protectionSetters.put("amplifier", v -> { configManager.setNewPlayerProtectionAmplifier(Integer.parseInt(v)); return null; });

        releaseProtectionSetters.put("duration", v -> { configManager.setReleaseProtectionDurationMinutes(Integer.parseInt(v)); return null; });
        releaseProtectionSetters.put("amplifier", v -> { configManager.setReleaseProtectionAmplifier(Integer.parseInt(v)); return null; });
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage : /db set <prison|head|deathsound|protection|duel> <clé> [valeur]");
            return true;
        }

        String category = args[1].toLowerCase();

        try {
            if (category.equals("duel")) {
                return handleDuel(sender, args);
            }

            if (category.equals("automessage")) {
                return handleAutoMessage(sender, args);
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage : /db set " + category + " <clé> <valeur>");
                return true;
            }

            String key = args[2].toLowerCase();
            String value = args[3];

            switch (category) {
                case "prison":
                    return dispatch(sender, "prison", prisonSetters, key, value);
                case "head":
                    return dispatch(sender, "head", headSetters, key, value);
                case "deathsound":
                    return dispatch(sender, "deathsound", deathSoundSetters, key, value);
                case "protection":
                    return dispatch(sender, "protection", protectionSetters, key, value);
                case "releaseprotection":
                    return dispatch(sender, "releaseprotection", releaseProtectionSetters, key, value);
                default:
                    sender.sendMessage(ChatColor.RED + "Catégorie inconnue. Utilise : prison, head, deathsound, protection, duel, automessage");
                    return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Valeur invalide : ce n'est pas un nombre.");
            return true;
        }
    }

    private boolean dispatch(CommandSender sender, String category, Map<String, Function<String, String>> setters, String key, String value) {
        Function<String, String> setter = setters.get(key);
        if (setter == null) {
            sender.sendMessage(ChatColor.RED + "Clé inconnue pour '" + category + "'. Utilise : " + String.join(", ", setters.keySet()));
            return true;
        }
        String error = setter.apply(value);
        if (error != null) {
            sender.sendMessage(ChatColor.RED + error);
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + category + "." + key + " = " + value);
        return true;
    }

    private boolean handleDuel(CommandSender sender, String[] args) {
        // /db set duel world <nom>
        // /db set duel delay <secondes>
        // /db set duel request-expiry <secondes>
        // /db set duel <zone> <slot:a|b> <axe:x|y|z|yaw|pitch> <valeur>
        String key = args[2].toLowerCase();

        if (key.equals("world")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage : /db set duel world <nom>");
                return true;
            }
            configManager.setDuelWorldName(args[3]);
            sender.sendMessage(ChatColor.GREEN + "duel.world = " + args[3]);
            return true;
        }

        if (key.equals("delay")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage : /db set duel delay <secondes>");
                return true;
            }
            configManager.setDuelReturnDelaySeconds(Integer.parseInt(args[3]));
            sender.sendMessage(ChatColor.GREEN + "duel.delay = " + args[3] + "s");
            return true;
        }

        if (key.equals("request-expiry")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage : /db set duel request-expiry <secondes>");
                return true;
            }
            configManager.setDuelRequestExpirySeconds(Integer.parseInt(args[3]));
            sender.sendMessage(ChatColor.GREEN + "duel.request-expiry-seconds = " + args[3] + "s");
            return true;
        }

        // Format zone : /db set duel <1..N> <a|b> <x|y|z|yaw|pitch> <valeur>
        int zoneCount = configManager.getDuelZoneCount();
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage : /db set duel <1-" + zoneCount + "> <a|b> <x|y|z|yaw|pitch> <valeur>");
            return true;
        }

        int zone;
        try {
            zone = Integer.parseInt(key);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'duel'. Utilise : world, delay, request-expiry, ou un numéro de zone (1-" + zoneCount + ").");
            return true;
        }

        if (!configManager.isValidZone(zone)) {
            sender.sendMessage(ChatColor.RED + "Zone invalide. Utilise 1 à " + zoneCount + ".");
            return true;
        }

        String slot = args[3].toLowerCase();
        if (!ConfigManager.isValidSlot(slot)) {
            sender.sendMessage(ChatColor.RED + "Slot invalide. Utilise 'a' ou 'b'.");
            return true;
        }

        String axis = args[4].toLowerCase();
        if (!DUEL_AXES.contains(axis)) {
            sender.sendMessage(ChatColor.RED + "Axe invalide. Utilise : x, y, z, yaw, pitch.");
            return true;
        }

        double value = Double.parseDouble(args[5]);
        configManager.setDuelCoordinate(zone, slot, axis, value);
        sender.sendMessage(ChatColor.GREEN + "duel.zones." + zone + "." + slot + "." + axis + " = " + value);
        return true;
    }

    private boolean handleAutoMessage(CommandSender sender, String[] args) {
        // args : [0]=set [1]=automessage [2]=clé [3]=valeur(optionnel)
        if (args.length < 3) {
            String status = autoMessageFeature.isEnabled()
                    ? ChatColor.GREEN + "activé"
                    : ChatColor.RED + "désactivé";
            sender.sendMessage(ChatColor.YELLOW + "AutoMessage est " + status
                    + ChatColor.YELLOW + ", intervalle : "
                    + autoMessageFeature.getIntervalMinutes() + " min.");
            sender.sendMessage(ChatColor.YELLOW + "Usage : /db set automessage on|off|interval <minutes>");
            return true;
        }

        String key = args[2].toLowerCase();

        switch (key) {
            case "on":
                autoMessageFeature.setEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "AutoMessage activé.");
                break;
            case "off":
                autoMessageFeature.setEnabled(false);
                sender.sendMessage(ChatColor.GREEN + "AutoMessage désactivé.");
                break;
            case "interval":
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage : /db set automessage interval <minutes>");
                    return true;
                }
                try {
                    int minutes = Integer.parseInt(args[3]);
                    if (minutes < 1) {
                        sender.sendMessage(ChatColor.RED + "L'intervalle doit être d'au moins 1 minute.");
                        return true;
                    }
                    autoMessageFeature.setIntervalMinutes(minutes);
                    sender.sendMessage(ChatColor.GREEN + "Intervalle automessage = " + minutes + " min.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Valeur invalide : ce n'est pas un nombre.");
                }
                break;
            default:
                // Raccourci : /db set automessage <nombre>
                try {
                    int minutes = Integer.parseInt(key);
                    if (minutes < 1) {
                        sender.sendMessage(ChatColor.RED + "L'intervalle doit être d'au moins 1 minute.");
                        return true;
                    }
                    autoMessageFeature.setIntervalMinutes(minutes);
                    sender.sendMessage(ChatColor.GREEN + "Intervalle automessage = " + minutes + " min.");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Clé inconnue. Utilise : on, off, interval <minutes>");
                }
                break;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            completions.addAll(CATEGORIES);
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "prison":
                    completions.addAll(prisonSetters.keySet());
                    break;
                case "head":
                    completions.addAll(headSetters.keySet());
                    break;
                case "deathsound":
                    completions.addAll(deathSoundSetters.keySet());
                    break;
                case "protection":
                    completions.addAll(protectionSetters.keySet());
                    break;
                case "releaseprotection":
                    completions.addAll(releaseProtectionSetters.keySet());
                    break;
                case "duel":
                    completions.addAll(DUEL_GLOBAL_KEYS);
                    completions.addAll(zoneNumbers());
                    break;
                case "automessage":
                    completions.addAll(AUTOMESSAGE_KEYS);
                    break;
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("automessage") && args[2].equalsIgnoreCase("interval")) {
                completions.add("<minutes>");
            } else if (args[1].equalsIgnoreCase("head") && args[2].equalsIgnoreCase("effect")) {
                completions.addAll(Arrays.stream(PotionEffectType.values())
                        .map(PotionEffectType::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList()));
            } else if (args[1].equalsIgnoreCase("deathsound") && args[2].equalsIgnoreCase("sound")) {
                completions.addAll(Arrays.stream(Sound.values())
                        .map(Sound::name)
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .limit(50)
                        .collect(Collectors.toList()));
            } else if (args[1].equalsIgnoreCase("duel") && zoneNumbers().contains(args[2])) {
                completions.addAll(DUEL_SLOTS);
            }
        } else if (args.length == 5 && args[1].equalsIgnoreCase("duel") && zoneNumbers().contains(args[2])) {
            completions.addAll(DUEL_AXES);
        }

        return completions;
    }

    private List<String> zoneNumbers() {
        return IntStream.rangeClosed(1, configManager.getDuelZoneCount())
                .mapToObj(String::valueOf)
                .collect(Collectors.toList());
    }
}

package com.example.deathban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeathBanCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager configManager;
    private final PrisonFeature prisonFeature;

    private static final List<String> SUBCOMMANDS = Arrays.asList("unprison", "set", "reload");
    private static final List<String> SET_CATEGORIES = Arrays.asList("prison", "head", "deathsound", "protection");

    private static final List<String> PRISON_KEYS = Arrays.asList("x", "y", "z", "world", "yaw", "pitch", "duration");
    private static final List<String> HEAD_KEYS = Arrays.asList("effect", "duration", "amplifier", "drop");
    private static final List<String> DEATHSOUND_KEYS = Arrays.asList("sound", "volume", "pitch");
    private static final List<String> PROTECTION_KEYS = Arrays.asList("duration", "amplifier");

    public DeathBanCommand(ConfigManager configManager, PrisonFeature prisonFeature) {
        this.configManager = configManager;
        this.prisonFeature = prisonFeature;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unprison":
                return handleUnprison(sender, args);
            case "set":
                return handleSet(sender, args);
            case "reload":
                configManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Configuration rechargée.");
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== DeathBan ===");
        sender.sendMessage(ChatColor.YELLOW + "/db unprison <joueur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set prison <x|y|z|world|yaw|pitch|duration> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set head <effect|duration|amplifier|drop> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set deathsound <sound|volume|pitch> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set protection <duration|amplifier> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db reload");
    }

    private boolean handleUnprison(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage : /db unprison <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
            return true;
        }

        prisonFeature.release(target);
        sender.sendMessage(ChatColor.GREEN + target.getName() + " a été libéré de prison.");
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage : /db set <prison|head|deathsound> <clé> <valeur>");
            return true;
        }

        String category = args[1].toLowerCase();
        String key = args[2].toLowerCase();
        String value = args[3];

        try {
            switch (category) {
                case "prison":
                    return handleSetPrison(sender, key, value, args);
                case "head":
                    return handleSetHead(sender, key, value);
                case "deathsound":
                    return handleSetDeathSound(sender, key, value);
                case "protection":
                    return handleSetProtection(sender, key, value);
                default:
                    sender.sendMessage(ChatColor.RED + "Catégorie inconnue. Utilise : prison, head, deathsound, protection");
                    return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Valeur invalide : '" + value + "' n'est pas un nombre.");
            return true;
        }
    }

    private boolean handleSetPrison(CommandSender sender, String key, String value, String[] args) {
        switch (key) {
            case "x":
                configManager.setPrisonX(Double.parseDouble(value));
                break;
            case "y":
                configManager.setPrisonY(Double.parseDouble(value));
                break;
            case "z":
                configManager.setPrisonZ(Double.parseDouble(value));
                break;
            case "world":
                configManager.setPrisonWorldName(value);
                break;
            case "yaw":
                configManager.setPrisonYaw(Float.parseFloat(value));
                break;
            case "pitch":
                configManager.setPrisonPitch(Float.parseFloat(value));
                break;
            case "duration":
                configManager.setPrisonDurationMinutes(Long.parseLong(value));
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'prison'. Utilise : x, y, z, world, yaw, pitch, duration");
                return true;
        }
        sender.sendMessage(ChatColor.GREEN + "prison." + key + " = " + value);
        return true;
    }

    private boolean handleSetHead(CommandSender sender, String key, String value) {
        switch (key) {
            case "effect":
                PotionEffectType type = PotionEffectType.getByName(value.toUpperCase());
                if (type == null) {
                    sender.sendMessage(ChatColor.RED + "Effet de potion inconnu : " + value);
                    return true;
                }
                configManager.setHeadEffect(value.toUpperCase());
                break;
            case "duration":
                configManager.setHeadDurationSeconds(Integer.parseInt(value));
                break;
            case "amplifier":
                configManager.setHeadAmplifier(Integer.parseInt(value));
                break;
            case "drop":
                double percent = Double.parseDouble(value);
                if (percent < 0 || percent > 100) {
                    sender.sendMessage(ChatColor.RED + "Le pourcentage doit être entre 0 et 100.");
                    return true;
                }
                configManager.setHeadDropChancePercent(percent);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'head'. Utilise : effect, duration, amplifier, drop");
                return true;
        }
        sender.sendMessage(ChatColor.GREEN + "head." + key + " = " + value);
        return true;
    }

    private boolean handleSetDeathSound(CommandSender sender, String key, String value) {
        switch (key) {
            case "sound":
                try {
                    org.bukkit.Sound.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Son inconnu : " + value);
                    return true;
                }
                configManager.setDeathSound(value.toUpperCase());
                break;
            case "volume":
                configManager.setDeathSoundVolume(Float.parseFloat(value));
                break;
            case "pitch":
                configManager.setDeathSoundPitch(Float.parseFloat(value));
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'deathsound'. Utilise : sound, volume, pitch");
                return true;
        }
        sender.sendMessage(ChatColor.GREEN + "deathsound." + key + " = " + value);
        return true;
    }

    private boolean handleSetProtection(CommandSender sender, String key, String value) {
        switch (key) {
            case "duration":
                configManager.setNewPlayerProtectionDurationMinutes(Integer.parseInt(value));
                break;
            case "amplifier":
                configManager.setNewPlayerProtectionAmplifier(Integer.parseInt(value));
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'protection'. Utilise : duration, amplifier");
                return true;
        }
        sender.sendMessage(ChatColor.GREEN + "protection." + key + " = " + value);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(SUBCOMMANDS);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("unprison")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args[0].equalsIgnoreCase("set")) {
                completions.addAll(SET_CATEGORIES);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            switch (args[1].toLowerCase()) {
                case "prison":
                    completions.addAll(PRISON_KEYS);
                    break;
                case "head":
                    completions.addAll(HEAD_KEYS);
                    break;
                case "deathsound":
                    completions.addAll(DEATHSOUND_KEYS);
                    break;
                case "protection":
                    completions.addAll(PROTECTION_KEYS);
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            // Suggestions de valeurs pour certaines clés
            if (args[1].equalsIgnoreCase("head") && args[2].equalsIgnoreCase("effect")) {
                return Arrays.stream(PotionEffectType.values())
                        .map(PotionEffectType::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[1].equalsIgnoreCase("deathsound") && args[2].equalsIgnoreCase("sound")) {
                return Arrays.stream(org.bukkit.Sound.values())
                        .map(org.bukkit.Sound::name)
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .limit(50)
                        .collect(Collectors.toList());
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }
}

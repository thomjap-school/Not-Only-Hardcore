package thomjap.deathban;

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
    private final DuelFeature duelFeature;
    private final AlexBanniereFeature alexBanniereFeature;
    private final AutoMessageFeature autoMessageFeature;
    private final KitFeature kitFeature;
    private final NickFeature nickFeature;

    private static final List<String> SUBCOMMANDS = Arrays.asList("unprison", "set", "reload", "duel", "alexbanniere", "kit", "nick");
    private static final List<String> SET_CATEGORIES = Arrays.asList("prison", "head", "deathsound", "protection", "duel", "automessage", "releaseprotection");

    private static final List<String> PRISON_KEYS = Arrays.asList("x", "y", "z", "world", "yaw", "pitch", "duration");
    private static final List<String> HEAD_KEYS = Arrays.asList("effect", "duration", "amplifier", "drop");
    private static final List<String> DEATHSOUND_KEYS = Arrays.asList("sound", "volume", "pitch");
    private static final List<String> PROTECTION_KEYS = Arrays.asList("duration", "amplifier");
    private static final List<String> RELEASE_PROTECTION_KEYS = Arrays.asList("duration", "amplifier");
    private static final List<String> DUEL_AXES = Arrays.asList("x", "y", "z", "yaw", "pitch");
    private static final List<String> DUEL_ZONES = Arrays.asList("1", "2", "3");
    private static final List<String> DUEL_SLOTS = Arrays.asList("a", "b");
    private static final List<String> DUEL_GLOBAL_KEYS = Arrays.asList("world", "delay");
    private static final List<String> AUTOMESSAGE_KEYS = Arrays.asList("on", "off", "interval");

    public DeathBanCommand(ConfigManager configManager, PrisonFeature prisonFeature, DuelFeature duelFeature, AlexBanniereFeature alexBanniereFeature, AutoMessageFeature autoMessageFeature, KitFeature kitFeature, NickFeature nickFeature) {
        this.configManager = configManager;
        this.prisonFeature = prisonFeature;
        this.duelFeature = duelFeature;
        this.alexBanniereFeature = alexBanniereFeature;
        this.autoMessageFeature = autoMessageFeature;
        this.kitFeature = kitFeature;
        this.nickFeature = nickFeature;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("duel")) {
            return handleDuel(sender, args);
        }

        if (sub.equals("kit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Seul un joueur peut utiliser cette commande.");
                return true;
            }
            kitFeature.giveKit((Player) sender);
            return true;
        }

        if (sub.equals("nick")) {
            return handleNick(sender, args);
        }

        // Les autres sous-commandes restent réservées aux ops
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        switch (sub) {
            case "unprison":
                return handleUnprison(sender, args);
            case "set":
                return handleSet(sender, args);
            case "reload":
                configManager.reload();
                prisonFeature.reload();
                duelFeature.reload();
                autoMessageFeature.reload();
                kitFeature.reload();
                sender.sendMessage(ChatColor.GREEN + "Configuration rechargée (config.yml, prisoners.yml, pending_forfeits.yml, automessage.yml, kit.yml).");
                return true;
            case "alexbanniere":
                return handleAlexBanniere(sender, args);
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
        sender.sendMessage(ChatColor.YELLOW + "/db set releaseprotection <duration|amplifier> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel world <nom>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel delay <secondes>");
        sender.sendMessage(ChatColor.YELLOW + "/db set duel <1|2|3> <a|b> <x|y|z|yaw|pitch> <valeur>");
        sender.sendMessage(ChatColor.YELLOW + "/db nick <pseudo>");
        sender.sendMessage(ChatColor.YELLOW + "/db nick reset [joueur]");
        sender.sendMessage(ChatColor.YELLOW + "/db kit");
        sender.sendMessage(ChatColor.YELLOW + "/db duel <joueur>");
        sender.sendMessage(ChatColor.YELLOW + "/db duel accept|deny <joueur>");
        sender.sendMessage(ChatColor.YELLOW + "/db alexbanniere on|off");
        sender.sendMessage(ChatColor.YELLOW + "/db set automessage on|off");
        sender.sendMessage(ChatColor.YELLOW + "/db set automessage interval <minutes>");
        sender.sendMessage(ChatColor.YELLOW + "/db reload");
    }

    private boolean handleAlexBanniere(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String status = alexBanniereFeature.isEnabled() ? ChatColor.GREEN + "activé" : ChatColor.RED + "désactivé";
            sender.sendMessage(ChatColor.YELLOW + "AlexBanniere est actuellement " + status + ChatColor.YELLOW + ".");
            sender.sendMessage(ChatColor.YELLOW + "Usage : /db alexbanniere on|off");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "on":
                alexBanniereFeature.setEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "AlexBanniere activé : AlexJanOne ne peut plus avoir de bannières.");
                break;
            case "off":
                alexBanniereFeature.setEnabled(false);
                sender.sendMessage(ChatColor.GREEN + "AlexBanniere désactivé.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Usage : /db alexbanniere on|off");
        }
        return true;
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

    private boolean handleDuel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seul un joueur peut utiliser cette commande.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage : /db duel <joueur> | /db duel accept|deny <joueur>");
            return true;
        }

        String second = args[1].toLowerCase();

        if (second.equals("accept") || second.equals("deny")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage : /db duel " + second + " <joueur>");
                return true;
            }
            String requesterName = args[2];
            if (second.equals("accept")) {
                duelFeature.acceptRequest(player, requesterName);
            } else {
                duelFeature.denyRequest(player, requesterName);
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
            return true;
        }

        duelFeature.sendRequest(player, target);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage : /db set <prison|head|deathsound|protection|duel> <clé> [valeur]");
            return true;
        }

        String category = args[1].toLowerCase();

        try {
            if (category.equals("duel")) {
                return handleSetDuel(sender, args);
            }

            if (category.equals("automessage")) {
                return handleSetAutoMessage(sender, args);
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage : /db set " + category + " <clé> <valeur>");
                return true;
            }

            String key = args[2].toLowerCase();
            String value = args[3];

            switch (category) {
                case "prison":
                    return handleSetPrison(sender, key, value, args);
                case "head":
                    return handleSetHead(sender, key, value);
                case "deathsound":
                    return handleSetDeathSound(sender, key, value);
                case "protection":
                    return handleSetProtection(sender, key, value);
                case "releaseprotection":
                    return handleSetReleaseProtection(sender, key, value);
                default:
                    sender.sendMessage(ChatColor.RED + "Catégorie inconnue. Utilise : prison, head, deathsound, protection, duel, automessage");
                    return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Valeur invalide : ce n'est pas un nombre.");
            return true;
        }
    }

    private boolean handleSetDuel(CommandSender sender, String[] args) {
        // /db set duel world <nom>
        // /db set duel delay <secondes>
        // /db set duel <zone:1-3> <slot:a|b> <axe:x|y|z|yaw|pitch> <valeur>
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

        // Format zone : /db set duel <1|2|3> <a|b> <x|y|z|yaw|pitch> <valeur>
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage : /db set duel <1|2|3> <a|b> <x|y|z|yaw|pitch> <valeur>");
            return true;
        }

        int zone;
        try {
            zone = Integer.parseInt(key);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'duel'. Utilise : world, delay, ou un numéro de zone (1-3).");
            return true;
        }

        if (!ConfigManager.isValidZone(zone)) {
            sender.sendMessage(ChatColor.RED + "Zone invalide. Utilise 1, 2 ou 3.");
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

    private boolean handleSetReleaseProtection(CommandSender sender, String key, String value) {
        switch (key) {
            case "duration":
                configManager.setReleaseProtectionDurationMinutes(Integer.parseInt(value));
                break;
            case "amplifier":
                configManager.setReleaseProtectionAmplifier(Integer.parseInt(value));
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Clé inconnue pour 'releaseprotection'. Utilise : duration, amplifier");
                return true;
        }
        sender.sendMessage(ChatColor.GREEN + "releaseprotection." + key + " = " + value);
        return true;
    }

    private boolean handleNick(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage : /db nick <pseudo> | /db nick reset [joueur]");
            return true;
        }

        // /db nick reset [joueur]
        if (args[1].equalsIgnoreCase("reset")) {
            Player target;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Joueur introuvable ou hors ligne.");
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Précisez un joueur : /db nick reset <joueur>");
                    return true;
                }
                target = (Player) sender;
            }
            nickFeature.resetNick(target);
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.GREEN + "Nick de " + target.getName() + " réinitialisé.");
            }
            return true;
        }

        // /db nick <pseudo> — s'applique à soi-même ou à un joueur cible
        // Support : /db nick <pseudo> ou /db nick <joueur> <pseudo>
        Player target;
        String nick;

        if (args.length >= 3 && Bukkit.getPlayer(args[1]) != null) {
            // /db nick <joueur> <pseudo>
            target = Bukkit.getPlayer(args[1]);
            nick = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        } else {
            // /db nick <pseudo>
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Usage depuis la console : /db nick <joueur> <pseudo>");
                return true;
            }
            target = (Player) sender;
            nick = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        }

        if (nick.length() > 32) {
            sender.sendMessage(ChatColor.RED + "Le nick ne peut pas dépasser 32 caractères.");
            return true;
        }

        nickFeature.setNick(target, nick);
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Nick de " + target.getName() + " changé en : "
                    + ChatColor.translateAlternateColorCodes('&', nick));
        }
        return true;
    }

    private boolean handleSetAutoMessage(CommandSender sender, String[] args) {
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
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(SUBCOMMANDS);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("unprison")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args[0].equalsIgnoreCase("set")) {
                completions.addAll(SET_CATEGORIES);
            } else if (args[0].equalsIgnoreCase("duel")) {
                completions.addAll(Arrays.asList("accept", "deny"));
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args[0].equalsIgnoreCase("nick")) {
                completions.add("reset");
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args[0].equalsIgnoreCase("alexbanniere")) {
                completions.addAll(Arrays.asList("on", "off"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("duel")
                && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
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
                case "releaseprotection":
                    completions.addAll(RELEASE_PROTECTION_KEYS);
                    break;
                case "duel":
                    completions.addAll(DUEL_GLOBAL_KEYS);
                    completions.addAll(DUEL_ZONES);
                    break;
                case "automessage":
                    completions.addAll(AUTOMESSAGE_KEYS);
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")
                && args[1].equalsIgnoreCase("automessage")
                && args[2].equalsIgnoreCase("interval")) {
            completions.add("<minutes>");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
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
            if (args[1].equalsIgnoreCase("duel") && DUEL_ZONES.contains(args[2])) {
                completions.addAll(DUEL_SLOTS);
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("set")
                && args[1].equalsIgnoreCase("duel") && DUEL_ZONES.contains(args[2])) {
            completions.addAll(DUEL_AXES);
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }
}

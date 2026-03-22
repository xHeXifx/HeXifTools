package org.hexif.hexiftools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final HeXifTools plugin;
    private final HelpCommand helpCommand;
    private final ConfigCommand configCommand;

    private List<String> partial(String token, List<String> candidates) {
      List<String> matches = new ArrayList<>();
      StringUtil.copyPartialMatches(token, candidates, matches);
      return matches;
    }

    private List<String> getAllPlayerNames() {
        Set<String> names = new HashSet<>();

        Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
        Arrays.stream(Bukkit.getOfflinePlayers())
                .map(p -> p.getName())
                .filter(n -> n != null && !n.isBlank())
                .forEach(names::add);

        var section = plugin.getConfig().getConfigurationSection("placeBlocker");
        if (section != null) {
            names.addAll(section.getKeys(false));
        }

        return new ArrayList<>(names);
    }

    private List<String> getAllBlockIds() {
        return Arrays.stream(Material.values())
                .filter(Material::isBlock)
                .map(m -> m.getKey().toString())
                .toList();
    }
  
    public CommandHandler(HeXifTools plugin) {
        this.plugin = plugin;
        this.helpCommand = new HelpCommand();
        this.configCommand = new ConfigCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            helpCommand.sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload" -> {
                if (!sender.hasPermission("hexiftools.reload")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }

                if (args.length > 1 && args[1].equalsIgnoreCase("full")) {
                    sender.sendMessage("§ePerforming full plugin reload...");
                    plugin.fullReload();
                    sender.sendMessage("§aHeXifTools fully reloaded successfully!");
                } else {
                    plugin.reloadPluginConfig();
                    sender.sendMessage("§aHeXifTools config reloaded successfully!");
                }
                return true;
            }
            case "viewcontainers" -> {
                if (!sender.hasPermission("hexiftools.viewcontainers")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }

                plugin.openContainerView(player);
                return true;
            }
            case "help" -> {
                helpCommand.sendHelp(sender, label);
                return true;
            }
            case "credits" -> {
                String version = Bukkit.getPluginManager().getPlugin("HeXifTools").getPluginMeta().getVersion();
                sender.sendMessage("§b§lHeXifTools §7- §fVersion: §a" + version);
                sender.sendMessage("§7Created by §bHeXif");
                sender.sendMessage("§e[§9Github§e] §7» §b§nhttps://github.com/xHeXifx");
                sender.sendMessage("§e[§aWebsite§e] §7» §a§nhttps://hexif.vercel.app");
                return true;
            }
            case "config" -> {
                if (!sender.hasPermission("hexiftools.config")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cMissing config action!\nUsage: /ht config <argument>");
                    return true;
                }

                String action = args[1].toLowerCase();

                switch (action) {
                    case "toggle" -> {
                        if (args.length < 3) {
                            sender.sendMessage("§cMissing feature to toggle!\nUsage: /ht config toggle <feature>");
                            return true;
                        }
                        configCommand.toggle(sender, args[2]);
                        return true;
                    }
                    case "list" -> {
                        configCommand.listConfig(sender, label);
                        return true;
                    }
                    case "placeblocker" -> {
                        if (args.length < 4) {
                            sender.sendMessage("§cUsage: /ht config placeblocker <players|blocks> <add|remove|list> ...");
                            return true;
                        }

                        String target = args[2].toLowerCase();
                        String operation = args[3].toLowerCase();

                        if (target.equals("players")) {
                            if (!(operation.equals("add") || operation.equals("remove")) || args.length < 5) {
                                sender.sendMessage("§cUsage: /ht config placeblocker players <add|remove> <username>");
                                return true;
                            }

                            configCommand.managePlacementBlockedPlayers(sender, operation, args[4]);
                            return true;
                        }

                        if (target.equals("blocks")) {
                            if (operation.equals("list")) {
                                if (args.length < 5) {
                                    sender.sendMessage("§cUsage: /ht config placeblocker blocks list <username>");
                                    return true;
                                }
                                configCommand.listPlacementBlockedBlocks(sender, args[4]);
                                return true;
                            }

                            if ((operation.equals("add") || operation.equals("remove")) && args.length >= 6) {
                                configCommand.managePlacementBlockedBlocks(sender, label, operation, args[4], args[5]);
                                return true;
                            }

                            sender.sendMessage("§cUsage: /ht config placeblocker blocks <add|remove|list> <username> [block]");
                            return true;
                        }

                        sender.sendMessage("§cUsage: /ht config placeblocker <players|blocks> <add|remove|list> ...");
                        return true;
                    }
                    case "trackedcontainers" -> {
                        if (args.length < 3) {
                            sender.sendMessage("§cUsage: /ht config trackedcontainers <add|remove> <block>");
                            return true;
                        }

                        if (!(args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                            sender.sendMessage("§cUsage: /ht config trackedcontainers <add|remove> <block>");
                            return true;
                        }

                        configCommand.manageContainersTrackedBlocks(sender, args[2], args[3]);
                        return true;
                    }
                    case "recipes" -> {
                        if (args.length < 4) {
                            sender.sendMessage("§cUsage: /ht config recipes add|remove <recipeID>");
                            return true;
                        }

                        String operation = args[2].toLowerCase(Locale.ROOT);
                        String recipeId = args[3];

                        if (operation.equals("add")) {
                            if (!(sender instanceof Player player)) {
                                sender.sendMessage("§cOnly players can run the recipe editor.");
                                return true;
                            }
                            configCommand.openRecipeEditor(player, recipeId);
                            return true;
                        }

                        if (operation.equals("remove")) {
                            configCommand.removeRecipe(sender, recipeId);
                            return true;
                        }

                        sender.sendMessage("§cUsage: /ht config recipes <add|remove> <recipeId>");
                        return true;
                    }
                }

                sender.sendMessage("§cUnknown config action: §f" + args[1]);
                sender.sendMessage("§eUsage: /ht config <argument>");
                return true;
            }
            case "todo" -> {
                if (plugin.getToDoCommand() == null) {
                    sender.sendMessage("§cTodos are not avalible right now.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /" + label + " todo <create|view|complete> ...");
                    return true;
                }
                String todoAction = args[1].toLowerCase(Locale.ROOT);
                if (todoAction.equals("create")) {
                    if (args.length < 4) {
                        sender.sendMessage("§cUsage: /" + label + " todo create <title> <target>");
                        sender.sendMessage("§7Target: player name or " + ToDoCommand.TARGET_EVERYONE);
                        return true;
                    }
                    String target = args[args.length - 1];
                    String title = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1));
                    plugin.getToDoCommand().createToDo(sender, title, target);
                    return true;
                }
                if (todoAction.equals("view")) {
                    plugin.getToDoCommand().viewToDos(sender);
                    return true;
                }
                if (todoAction.equals("complete")) {
                    if (args.length < 3) {
                        sender.sendMessage("§cUsage: /" + label + " todo complete <title>");
                        return true;
                    }
                    String title = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    plugin.getToDoCommand().completeToDo(sender, title);
                    return true;
                }
                sender.sendMessage("§cUnknown todo action. Use §fcreate§c, §fview§c, or §fcomplete§c.");
                return true;
            }
            default -> {
                sender.sendMessage("§cUnknown subcommand: §f" + args[0]);
                helpCommand.sendHelp(sender, label);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("hexiftools.reload")) candidates.add("reload");
            if (sender.hasPermission("hexiftools.viewcontainers")) candidates.add("viewcontainers");
            if (sender.hasPermission("hexiftools.help")) candidates.add("help");
            if (sender.hasPermission("hexiftools.config")) candidates.add("config");
            candidates.add("credits");
            candidates.add("todo");
            return partial(args[0], candidates);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("hexiftools.reload")) {
            return partial(args[1], List.of("full"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("config") && sender.hasPermission("hexiftools.config")) {
            return partial(args[1], List.of("toggle", "list", "placeblocker", "trackedcontainers", "recipes"));
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("config")
                && args[1].equalsIgnoreCase("toggle")
                && sender.hasPermission("hexiftools.config")) {
            var section = plugin.getConfig().getConfigurationSection("features");
            if (section != null) {
                return partial(args[2], configCommand.getToggleCandidates());
            }
          }

        if (args.length == 3 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("trackedcontainers")) {
            return partial(args[2], List.of("add", "remove"));
        }

        if (args.length == 4
                && args[0].equalsIgnoreCase("config")
                && args[1].equalsIgnoreCase("trackedcontainers")
                && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
            return partial(args[3], getAllBlockIds());
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("config")
                && args[1].equalsIgnoreCase("recipes")) {
            return partial(args[2], List.of("add", "remove"));
        }

        if (args.length == 4
                && args[0].equalsIgnoreCase("config")
                && args[1].equalsIgnoreCase("recipes")
                && args[2].equalsIgnoreCase("remove")) {
            return partial(args[3], configCommand.getCustomRecipeIds());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("placeblocker")) {
            return partial(args[2], List.of("players", "blocks"));
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("placeblocker")) {
            if (args[2].equalsIgnoreCase("players")) return partial(args[3], List.of("add", "remove"));
            if (args[2].equalsIgnoreCase("blocks")) return partial(args[3], List.of("add", "remove", "list"));
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("placeblocker")) {
            return partial(args[4], getAllPlayerNames());
        }

        if (args.length == 6 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("placeblocker")
                && args[2].equalsIgnoreCase("blocks")
                && (args[3].equalsIgnoreCase("add") || args[3].equalsIgnoreCase("remove"))) {
            return partial(args[5], getAllBlockIds());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("todo")) {
            List<String> sub = new ArrayList<>();
            sub.add("create");
            sub.add("view");
            sub.add("complete");
            return partial(args[1], sub);
        }

        if (args.length >= 3
            && args[0].equalsIgnoreCase("todo")
            && args[1].equalsIgnoreCase("complete")
            && sender instanceof Player p
            && plugin.getToDoCommand() != null) {
                String[] titleParts = Arrays.copyOfRange(args, 2, args.length);
                return plugin.getToDoCommand().tabCompleteAssignableTitles(p, titleParts);
            }

        if (args.length >= 4
            && args[0].equalsIgnoreCase("todo")
            && args[1].equalsIgnoreCase("create")
        ) {
            List<String> last = new ArrayList<>(getAllPlayerNames());
            last.add(ToDoCommand.TARGET_EVERYONE);
            return partial(args[args.length - 1], last);
        }
        

        return List.of();
    }
}
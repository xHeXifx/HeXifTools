package org.hexif.hexiftools;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class ConfigCommand {
  
  public Map<String, Boolean> getAllFeatures() {
    ConfigurationSection section = plugin.getConfig().getConfigurationSection("features");
    Map<String, Boolean> features = new LinkedHashMap<>();

    if (section == null) {
      return features;
    }

    for (String key : section.getKeys(false)) {
      features.put(key, section.getBoolean(key));
    }

    return features;
  }

  public List<String> getPlayersBlockedBlocks(String playerName) {
      return plugin.getConfig().getStringList("placeBlocker." + playerName);
  }

private String normalizeBlockId(String block) {
    String value = block.toLowerCase(Locale.ROOT);
    return value.startsWith("minecraft:") ? value : "minecraft:" + value;
}

  private final HeXifTools plugin;

  private static final Map<String, List<String>> WEBHOOK_VALUES = Map.of(
      "webhooks", List.of(
          "features.sendPlayerDeath",
          "features.sendEntityDeath",
          "features.sendPlayerJoinLeave"
      )
  );

  public List<String> getToggleCandidates() {
      Set<String> candidates = new LinkedHashSet<>(getAllFeatures().keySet());
      candidates.addAll(WEBHOOK_VALUES.keySet());
      return new ArrayList<>(candidates);
  }

    public ConfigCommand(HeXifTools plugin) {
        this.plugin = plugin;
    }

    public void openRecipeEditor(CommandSender sender, String recipeId) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage("§cOnly players can use recipe editor.");
        return;
      }
      if (recipeId == null || recipeId.isBlank()) {
        sender.sendMessage("§cRecipe id cannot be empty.");
        return;
      }

      plugin.getRecipeEditorGUI().open(player, recipeId.toLowerCase(Locale.ROOT));
    }

    public void removeRecipe(CommandSender sender, String recipeId) {
      String targetId = recipeId == null ? "" : recipeId.toLowerCase(Locale.ROOT);
      if (targetId.isBlank()) {
        sender.sendMessage("§cRecipe id cannot be empty");
        return;
      }

      List<Map<?, ?>> existing = plugin.getConfig().getMapList("custom-recipes");
      List<Map<String, Object>> updated = new ArrayList<>();
      boolean removed = false;

      for (Map<?, ?> entry : existing) {
        String id = String.valueOf(entry.get("id")).toLowerCase(Locale.ROOT);
        if (id.equals(targetId)) {
          removed = true;
          continue;
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> kv : entry.entrySet()) {
          copy.put(String.valueOf(kv.getKey()), kv.getValue());
        }
        updated.add(copy);
      }
      if (!removed) {
        sender.sendMessage("§cRecipe not found: §f" + recipeId);
        return;
      }

      plugin.getConfig().set("custom-recipes", updated);
      plugin.saveConfig();
      plugin.reloadPluginConfig();
      sender.sendMessage("§aRemoved recipe: §f" + recipeId);
    }

    public List<String> getCustomRecipeIds() {
      List<String> ids = new ArrayList<>();
      for (Map<?, ?> entry : plugin.getConfig().getMapList("custom-recipes")) {
        Object id = entry.get("id");
        if (id != null) ids.add(String.valueOf(id));
      }
      return ids;
    }

    public void saveOrUpdateRecipe(
      String recipeId,
      ItemStack result,
      List<String> shape,
      Map<Character, Material> ingrediants
    ) {
      String id = recipeId.toLowerCase(Locale.ROOT);

      Map<String, Object> recipe = new LinkedHashMap<>();
      recipe.put("id", id);
      recipe.put("result", result.getType().getKey().toString());
      recipe.put("amount", Math.max(1, result.getAmount()));
      recipe.put("shape", shape);

      Map<String, String> ing = new LinkedHashMap<>();
      for (Map.Entry<Character, Material> e : ingrediants.entrySet()) {
        ing.put(String.valueOf(e.getKey()), e.getValue().getKey().toString());
      }
      recipe.put("ingredients", ing);

      List<Map<?, ?>> existing = plugin.getConfig().getMapList("custom-recipes");
      List<Map<String, Object>> updated = new ArrayList<>();
      boolean replaced = false;

      for (Map<?, ?> entry: existing) {
        String existingId = String.valueOf(entry.get("id")).toLowerCase(Locale.ROOT);
        if (existingId.equals(id)) {
          updated.add(recipe);
          replaced = true;
        } else {
          Map<String, Object> copy = new LinkedHashMap<>();
          for (Map.Entry<?, ?> kv : entry.entrySet()) {
            copy.put(String.valueOf(kv.getKey()), kv.getValue());
          }
          updated.add(copy);
        }
      }

      if (!replaced) updated.add(recipe);

      plugin.getConfig().set("custom-recipes", updated);
      plugin.saveConfig();
      plugin.reloadPluginConfig();
    }

  public void toggle(CommandSender sender, String feature) {
      String aliasKey = feature.toLowerCase(Locale.ROOT);
      List<String> aliasPaths = WEBHOOK_VALUES.get(aliasKey);

      if (aliasPaths != null) {
          boolean current = plugin.getConfig().getBoolean(aliasPaths.get(0));
          boolean newValue = !current;
          for (String path : aliasPaths) {
              plugin.getConfig().set(path, newValue);
          }
          plugin.saveConfig();
          sender.sendMessage("§eGroup §f" + feature + " §eset to " + newValue);
          return;
      }

      String path = "features." + feature;
      if (!plugin.getConfig().contains(path)) {
          sender.sendMessage("§cFeature §f" + feature + "§c does not exist!");
          return;
      }

      boolean currentValue = plugin.getConfig().getBoolean(path);
      plugin.getConfig().set(path, !currentValue);
      plugin.saveConfig();
      sender.sendMessage("§eFeature §f" + feature + " §eset to " + !currentValue);
  }



  // List blocks a player is blocked from using
  public void listPlacementBlockedBlocks(CommandSender sender, String player) {
      List<String> blockedBlocks = getPlayersBlockedBlocks(player);

      if (blockedBlocks.isEmpty()) {
          sender.sendMessage("§cPlayer is not blocked from placing anything");
          return;
      }

      sender.sendMessage("§6§lPlayer " + player + " is blocked from placing:");
      blockedBlocks.forEach(block -> sender.sendMessage("§e- " + block));
  }



  // Manage the PLAYERS that are in placementblocker
  public void managePlacementBlockedPlayers(CommandSender sender, String operation, String player) {
    String playerPath = "placeBlocker." + player;

    if (operation.equalsIgnoreCase("add")) {
      if (plugin.getConfig().isSet(playerPath)) {
        sender.sendMessage("§ePlayer §f" + player + "§e is already in placeBlocker.");
        return;
      }

      plugin.getConfig().set(playerPath, new ArrayList<String>());
      plugin.saveConfig();
      plugin.reloadPluginConfig();
      plugin.getLogger().info("Reloaded config due to placeBlocker config change");
      sender.sendMessage("§aAdded §f" + player + "§e to placeBlocker.");
      return;
    }

    if (operation.equalsIgnoreCase("remove")) {
      if (!plugin.getConfig().isSet(playerPath)) {
        sender.sendMessage("§cPlayer §f" + player + "§c is not in placeBlocker.");
        return;
      }

      plugin.getConfig().set(playerPath, null);
      plugin.saveConfig();
      plugin.reloadPluginConfig();
      plugin.getLogger().info("Reloaded config due to placeBlocker config change");
      sender.sendMessage("§aRemoved §f" + player + " §a from placeBlocker.");
      return;
    }

    sender.sendMessage("§cUnknown operation: " + operation);
  }



  // Manage individual blocks a player is blocked from using
  public void managePlacementBlockedBlocks(CommandSender sender, String label, String operation, String player, String block) {
    String playerPath = "placeBlocker." + player;

    if (!plugin.getConfig().isSet(playerPath)) { 
      sender.sendMessage("§cPlayer §f" + player + "§c is not in placeBlocker. Add them first.");
      return;
    }

    List<String> blocked = new ArrayList<>(plugin.getConfig().getStringList(playerPath));
    String normBlock = normalizeBlockId(block);

    if (operation.equalsIgnoreCase("add")) { 
      if (blocked.contains(normBlock)) {
        sender.sendMessage("§e" + normBlock + " is already blocked for §f" + player);
        return;
      }

      blocked.add(normBlock);
      plugin.getConfig().set(playerPath, blocked);
      plugin.saveConfig();
      plugin.reloadPluginConfig();
      plugin.getLogger().info("Reloaded config due to placeBlocker config change");
      sender.sendMessage("§aAdded §f" + normBlock + " §afor §f " + player);
      return;
    }
    if (operation.equalsIgnoreCase("remove")) { 
      if (!blocked.remove(normBlock)) {
        sender.sendMessage("§c" + normBlock + " is not blocked for §f" + player);
        return;
      }

      plugin.getConfig().set(playerPath, blocked);
      plugin.saveConfig();
      plugin.reloadPluginConfig();
      plugin.getLogger().info("Reloaded config due to placeBlocker config change");
      sender.sendMessage("§aRemoved §f" + normBlock + " §afor §f" + player);
      return;
    }
    sender.sendMessage("Unknown operation: " + operation);
  }

  public void manageContainersTrackedBlocks(CommandSender sender, String operation, String block) {
      List<String> trackedBlocks = new ArrayList<>(plugin.getConfig().getStringList("tracked-blocks"));

      if (operation.equalsIgnoreCase("add")) {
          if (trackedBlocks.contains(block)) {
              sender.sendMessage("§c" + block + "is already in tracked containers");
              return;
          }

          trackedBlocks.add(block);
          plugin.getConfig().set("tracked-blocks", trackedBlocks);
          plugin.saveConfig();
          plugin.reloadPluginConfig();
          plugin.getLogger().info("Reloaded config due to viewContainers config change");
          sender.sendMessage(block + "§a is now being tracked.");
      }
      else if (operation.equalsIgnoreCase("remove")) {
          if (!trackedBlocks.contains(block)) {
              sender.sendMessage(block + " §cis not in tracked containers");
              return;
          }

          trackedBlocks.remove(block);
          plugin.getConfig().set("tracked-blocks", trackedBlocks);
          plugin.saveConfig();
          plugin.reloadPluginConfig();
          plugin.getLogger().info("Reloaded config due to viewContainers config change");
          sender.sendMessage(block + " §ais no longer being tracked.");
      }
  }

  public void listConfig(CommandSender sender, String label) {
    Map<String, Boolean> features = getAllFeatures();

    if (features.isEmpty()) {
      sender.sendMessage("§cNo features found in the conifg.");
      return;
    }

    sender.sendMessage("§6§lCurrent Config:");
    features.forEach((name, enabled) ->
        sender.sendMessage("§e- " + name + ": " + (enabled ? "§aenabled" : "§cdisabled"))
    );
  }
}

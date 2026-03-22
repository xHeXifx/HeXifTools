package org.hexif.hexiftools;



import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ToDoCommand {
  public static final String TARGET_EVERYONE = "*everyone";

  private final File dataFile;
  private final Gson gson;
  private final Logger logger;
  
  public ToDoCommand(Logger logger, File dataFolder) {
    this.dataFile = new File(dataFolder, "todos.json");
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.logger = logger;

    if (!dataFile.exists()) {
        try {
            dataFile.getParentFile().mkdirs();
            dataFile.createNewFile();
            try (FileWriter writer = new FileWriter(dataFile)) {
                writer.write("[]");
            }
        } catch (IOException e) {
            logger.severe("Failed to create todos.json: " + e.getMessage());
        }
    }
  }

  public void createToDo(CommandSender sender, String title, String targetRaw) {
    String titleTrim = title == null ? "" : title.trim();
    if (titleTrim.isEmpty()) {
        sender.sendMessage("§cTodo title cannot be empty");
        return;
    }

    String target = normaliseTarget(sender, targetRaw);
    if (target == null) {
        return;
    }

    JsonObject entry = new JsonObject();
    entry.addProperty("title", titleTrim);
    entry.addProperty("target", target);

    saveToJson(entry);
    sender.sendMessage("§aTodo created");
  }

  private String normaliseTarget(CommandSender sender, String targetRaw) {
    if (targetRaw == null || targetRaw.isBlank()) {
        sender.sendMessage("§cTarget cannot be empty");
        return null;
    }
    String t = targetRaw.trim();
    if (t.equalsIgnoreCase(TARGET_EVERYONE)) {
        return TARGET_EVERYONE;
    }
    OfflinePlayer op = Bukkit.getOfflinePlayer(t);
    if (!op.hasPlayedBefore() && !op.isOnline()) {
        sender.sendMessage("§cUnknown player: §f" + t);
        return null;
    }
    String name = op.getName();
    if (name == null || name.isBlank()) {
        sender.sendMessage("§cCould not resolve player name for: §f" +t);
        return null;
    }
    return name;
  }

  public void viewToDos(CommandSender sender) {
    JsonArray array = loadArray();
    if (array == null || array.size() == 0) {
        sender.sendMessage("§7No todos.");
        return;
    }

    List<JsonObject> matches = new ArrayList<>();
    String viewerName = null;
    if (sender instanceof Player p) {
        viewerName = p.getName();
    }

    for (JsonElement el : array) {
        if (!el.isJsonObject()) {
            continue;
        }
        JsonObject o = el.getAsJsonObject();
        if (!o.has("target") || !o.get("target").isJsonPrimitive()) {
            continue;
        }
        String target = o.get("target").getAsString();
        if (TARGET_EVERYONE.equals(target)) {
            matches.add(o);
            continue;
        }
        if (viewerName != null && target.equalsIgnoreCase(viewerName)) {
            matches.add(o);
        }
    }

    if (matches.isEmpty()) {
        sender.sendMessage("§7No todos for you");
        return;
    }

    sender.sendMessage("§6§lTodos §7(" + matches.size() + ")");
    int i = 1;
    for (JsonObject o : matches) {
        String title = o.has("target") && o.get("target").isJsonPrimitive()
            ? o .get("title").getAsString()
            : "?";
        String target = o.has("target") && o.get("target").isJsonPrimitive()
            ? o.get("target").getAsString()
            : "?";
        sender.sendMessage("§e" + i + ". §f" + title + "§7→ §b" + target);
        i++;
    }
  }

  private JsonArray loadArray() {
    if (!dataFile.exists() || dataFile.length() == 0) {
        return new JsonArray();
    }
    try (FileReader reader = new FileReader(dataFile)) {
        JsonArray array = gson.fromJson(reader, JsonArray.class);
        return array != null ? array : new JsonArray();
    } catch (Exception e) {
        logger.warning("Failed to read todos.json: " + e.getMessage());
        return new JsonArray();
    }
  }

  private void saveToJson(JsonObject newEntry) {
    try {
      JsonArray array;
      if (dataFile.length() > 0) {
        try (FileReader reader = new FileReader(dataFile)) {
          array = gson.fromJson(reader, JsonArray.class);
          if (array == null) {
            array = new JsonArray();
          }
        } catch (Exception e) {
          logger.warning("Failed to read existing todos, creating new array: " + e.getMessage());
          array = new JsonArray();
        }
      } else {
        array = new JsonArray();
      }
      array.add(newEntry);
      try (FileWriter writer = new FileWriter(dataFile)) {
        gson.toJson(array, writer);
      }
    } catch (IOException e) {
      logger.severe("Failed to save todos: " + e.getMessage());
    }
  }

  public List<String> getAssignableTitles(Player player) {
    List<String> out = new ArrayList<>();
    String name = player.getName();
    JsonArray array = loadArray();
    for (JsonElement el : array) {
      if (!el.isJsonObject()) {
        continue;
      }
      JsonObject o = el.getAsJsonObject();
      if (!o.has("target") || !o.get("target").isJsonPrimitive()) {
        continue;
      }
      if (!o.has("title") || !o.get("title").isJsonPrimitive()) {
        continue;
      }
      String target = o.get("target").getAsString();
      if (TARGET_EVERYONE.equals(target) || name.equalsIgnoreCase(target)) {
        out.add(o.get("title").getAsString());
      }
    }
    return out;
  }

  public List<String> tabCompleteAssignableTitles(Player player, String[] titleParts) {
    List<String> titles = getAssignableTitles(player);
    if (titleParts.length == 0) {
        return new ArrayList<>(titles);
    }
    if (titleParts.length == 1) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(titleParts[0], titles, matches);
        return matches;
    }
    String prefixWords = String.join(" ", Arrays.copyOfRange(titleParts, 0, titleParts.length - 1));
    String lastToken = titleParts[titleParts.length - 1];
    Set<String> nextWords = new LinkedHashSet<>();
    
    for (String title : titles) {
        if (!title.toLowerCase().startsWith(prefixWords.toLowerCase() + " ")) {
            continue;
        }
        String remainder = title.substring(prefixWords.length() + 1).trim();
        if (remainder.isEmpty()) {
            continue;
        }
        String nextWord = remainder.split(" ", 2)[0];
        if (nextWord.toLowerCase().startsWith(lastToken.toLowerCase())) {
            nextWords.add(nextWord);
        }
    }
    List<String> matches = new ArrayList<>();
    StringUtil.copyPartialMatches(lastToken, new ArrayList<>(nextWords), matches);
    return matches;
  }

  public void completeToDo(CommandSender sender, String title) {
    if (!(sender instanceof Player player)) {
        sender.sendMessage("§cOnly players can complete todos.");
        return;
    }
    
    String trimmed = title == null ? "" : title.trim();
    if (trimmed.isEmpty()) {
        sender.sendMessage("§cUsage: /ht todo complete <title>");
        return;
    }

    String playerName = player.getName();
    JsonArray array = loadArray();
    if (array == null || array.size() == 0) {
        sender.sendMessage("§cNo matching todos found");
        return;
    }

    for (int i = 0; i < array.size(); i++) {
        JsonElement el = array.get(i);
        if (!el.isJsonObject()) {
            continue;
        }
        
        JsonObject o = el.getAsJsonObject();
        if (!o.has("target") || !o.get("target").isJsonPrimitive()) {
          continue;
        }
        String tgt = o.get("target").getAsString();
        boolean canComplete = TARGET_EVERYONE.equals(tgt) || playerName.equalsIgnoreCase(tgt);
        if (!canComplete) {
          continue;
        }
        if (!o.has("title") || !o.get("title").isJsonPrimitive()) {
          continue;
        }
        
        if (o.get("title").getAsString().equals(trimmed)) {
            array.remove(i);
            writeArray(array);
            sender.sendMessage("§aTodo completed");
            return;
        }
    }
    sender.sendMessage("§cNo matching todo found");
  }

  private void writeArray(JsonArray array) {
    try (FileWriter writer = new FileWriter(dataFile)) {
        gson.toJson(array, writer);
    } catch (IOException e) {
        logger.severe("Failed to save todos: " + e.getMessage());
    }
  }
}

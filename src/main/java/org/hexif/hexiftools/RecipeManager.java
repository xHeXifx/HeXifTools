package org.hexif.hexiftools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecipeManager {
  private final HeXifTools plugin;
  private final Set<NamespacedKey> registeredKeys = new HashSet<>();
  
  public RecipeManager(HeXifTools plugin) {
    this.plugin = plugin;
  }

  public void reloadCustomRecipes() {
      clearRegisteredRecipes();

      List<Map<?, ?>> recipes = plugin.getConfig().getMapList("custom-recipes");
      Set<String> seenIds = new HashSet<>();

      int loaded = 0;
      for (Map<?, ?> raw : recipes) {
          String id = asString(raw.get("id"));
          if (id == null || id.isBlank()) {
              plugin.getLogger().warning("Skipping recipe: missing id");
              continue;
          }

          String normalizedId = id.toLowerCase(Locale.ROOT);
          if (!seenIds.add(normalizedId)) {
              plugin.getLogger().warning("Skipping duplicate recipe id in config: " + id);
              continue;
          }

          if (registeredShapeRecipe(raw)) loaded++;
      }
      registerBuiltInRecipes();

      plugin.getLogger().info("Loaded " + loaded + " custom recipes");
  }

  public void registerBuiltInRecipes() {
    registerBigExpBottleRecipe();
  }

  private void registerBigExpBottleRecipe() {
    NamespacedKey key = new NamespacedKey(plugin, "big_exp_bottle_recipe");

    ItemStack result = plugin.getCustomItems().createBigXPBottle();
    ShapelessRecipe recipe = new ShapelessRecipe(key, result);
    recipe.addIngredient(4, Material.EXPERIENCE_BOTTLE);

    Bukkit.removeRecipe(key);
    Bukkit.addRecipe(recipe);
    registeredKeys.add(key);

    plugin.getLogger().info("Registered built in recipe: big_experiance_bottle_recipe");
  }

  private void clearRegisteredRecipes() {
    for (NamespacedKey key : registeredKeys) {
      Bukkit.removeRecipe(key);
    }
    registeredKeys.clear();
  }


  private boolean registeredShapeRecipe(Map<?, ?> raw) {
    String id = asString(raw.get("id"));
    String resultID = asString(raw.get("result"));
    Material resultMaterial = parseMaterial(resultID);
    if (resultMaterial == null) { 
      plugin.getLogger().warning("Skipping recipe " + id + ": invalid result " + resultID);
      return false;
    }
    int amount = asInt(raw.get("amount"), 1);

    if (id == null || id.isBlank()) {
      plugin.getLogger().warning("Skipping recipe: missing id");
      return false;
    }

    List<String> shape = parseShape(raw.get("shape"));
    if (shape.isEmpty()) {
      plugin.getLogger().warning("Skipping recipe" + id + ": invalid shape.");
      return false;
    }

    Map<Character, Material> ingrediantMap = parseIngrediants(raw.get("ingredients"));
    if (ingrediantMap.isEmpty()) {
      plugin.getLogger().warning("Skipped recipe " + id + ": no valid ingrediants");
      return false;
    }

    Set<Character> usedSymbols = new HashSet<>();
    for (String row : shape) {
      for (char c : row.toCharArray()) {
        if (c != ' ') usedSymbols.add(c);
      }
    }

    for (char used : usedSymbols) {
      if (!ingrediantMap.containsKey(used)) {
        plugin.getLogger().warning("Skipping recipe '" + id + "': missing ingredient for symbol '" + used);
        return false;
      }
    }

    NamespacedKey key = new NamespacedKey(plugin, id.toLowerCase(Locale.ROOT));
    ItemStack resultStack = new ItemStack(resultMaterial, Math.max(1, amount));

    ShapedRecipe recipe = new ShapedRecipe(key, resultStack);
    recipe.shape(shape.toArray(new String[0]));

    for (Map.Entry<Character, Material> entry : ingrediantMap.entrySet()) { 
      recipe.setIngredient(entry.getKey(), entry.getValue());
    }

    Bukkit.addRecipe(recipe);
    registeredKeys.add(key);
    return true;
  }

  private Map<Character, Material> parseIngrediants(Object rawIngrediants) {
    Map<Character, Material> out = new HashMap<>();
    if (!(rawIngrediants instanceof Map<?, ?> map)) return out;

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String symbolStr = asString(entry.getKey());
      String materialStr = asString(entry.getValue());

      if (symbolStr == null || symbolStr.length() != 1) continue;
      char symbol = symbolStr.charAt(0);
      if (symbol == ' ') continue;

      Material material = parseMaterial(materialStr);
      if (material == null) continue;

      out.put(symbol, material);
    }
    return out;
  }

  private List<String> parseShape(Object rawShape) {
    List<String> out = new ArrayList<>();
    if (!(rawShape instanceof List<?> list)) return out;

    for (Object rowObj : list) {
      String row = asString(rowObj);
      if (row == null) return List.of();
      if (row.length() < 1 | row.length() > 3) return List.of();
      out.add(row);
    }

    if (out.size() < 1 || out.size() > 3) return List.of();
    return out;
  }

  private Material parseMaterial(String raw) {
    if (raw == null || raw.isBlank()) return null;

    String normalised = raw.toLowerCase(Locale.ROOT);
    if (normalised.startsWith("minecraft:")) {
      normalised = normalised.substring("minecraft:".length());
    }
    return Material.matchMaterial(normalised);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private int asInt(Object value, int fallback) {
    if (value instanceof Number n) return n.intValue();
    try {
      return value == null ? fallback : Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }
}

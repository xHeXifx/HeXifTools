package org.hexif.hexiftools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RecipeEditorGUI implements Listener {
  private static final int SIZE = 54;
  private static final int RESULT_SLOT = 24;
  private static final int CANCEL_SLOT = 45;
  private static final int CONFIRM_SLOT = 53;
  private static final int[] PATTERN_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
  private static final Set<Integer> EDITABLE_SLOTS = Set.of(
          10, 11, 12, 19, 20, 21, 28, 29, 30, RESULT_SLOT
  );
  

  private final ConfigCommand configCommand;

  public RecipeEditorGUI(HeXifTools plugin) {
    this.configCommand = new ConfigCommand(plugin);
  }

  public void open(Player player, String recipeId) { 
    Inventory gui = Bukkit.createInventory(
      new RecipeEditorHolder(recipeId, player.getUniqueId()),
      SIZE,
      Component.text("Recipe Editor: " + recipeId, NamedTextColor.GOLD)
    );

    for (int slot = 0; slot < SIZE; slot ++) {
      if (EDITABLE_SLOTS.contains(slot) || slot == CANCEL_SLOT || slot == CONFIRM_SLOT) continue;
      gui.setItem(slot, filler());
    }

    gui.setItem(CANCEL_SLOT, control(Material.RED_STAINED_GLASS_PANE, "Cancel", NamedTextColor.RED));
    gui.setItem(CONFIRM_SLOT, control(Material.GREEN_STAINED_GLASS_PANE, "Confirm", NamedTextColor.GREEN));

    player.openInventory(gui);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) { 
    if (!(event.getInventory().getHolder() instanceof RecipeEditorHolder holder)) return;

    int raw = event.getRawSlot();
    if (raw < 0 || raw >= SIZE) return;

    Player player = (Player) event.getWhoClicked();

    if (raw == CANCEL_SLOT) {
      event.setCancelled(true);
      player.closeInventory();
      return;
    }

    if (raw == CONFIRM_SLOT) {
      event.setCancelled(true);
      boolean saved = saveFromGui(player, holder.recipeId(), event.getView().getTopInventory());
      if (saved) {
        holder.confirmed = true;
        player.closeInventory();
      }
      return;
    }
    if (event.isShiftClick() || event.getHotbarButton() != -1) {
      event.setCancelled(true);
      return;
    }

    if (raw < SIZE) {
      event.setCancelled(!EDITABLE_SLOTS.contains(raw));
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (!(event.getInventory().getHolder() instanceof RecipeEditorHolder)) return;
    for (int rawSlot : event.getRawSlots()) {
      if (rawSlot < SIZE && !EDITABLE_SLOTS.contains(rawSlot)) {
        event.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof RecipeEditorHolder holder)) return;
    if (holder.confirmed) return;

    Player player = (Player) event.getPlayer();
    Inventory top = event.getInventory();

    for (int slot : PATTERN_SLOTS) {
      returnItem(player, top.getItem(slot));
      top.setItem(slot, null);
    }
    returnItem(player, top.getItem(RESULT_SLOT));
    top.setItem(RESULT_SLOT, null);
  }

  private boolean saveFromGui(Player player, String recipeId, Inventory inv) {
    ItemStack result = inv.getItem(RESULT_SLOT);
    if (result == null || result.getType() == Material.AIR) {
      player.sendMessage("§cSet a result item first.");
      return false;
    }

    Material[][] grid = new Material[3][3];
    int nonAir = 0;

    for (int i = 0; i < PATTERN_SLOTS.length; i++) {
      ItemStack stack = inv.getItem(PATTERN_SLOTS[i]);
      Material material = (stack == null) ? Material.AIR : stack.getType();
      int row = i / 3;
      int col = i % 3;
      grid[row][col] = material;
      if (material != Material.AIR) nonAir++;
    }

    if (nonAir == 0) {
      player.sendMessage("§cAdd at least one ingrediant in the 3x3 pattern.");
      return false;
    }

    int minRow = 2, maxRow = 0, minCol = 2, maxCol = 0;
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        if (grid[r][c] != Material.AIR) {
          if (r < minRow) minRow = r;
          if (r > maxRow) maxRow = r;
          if (c < minCol) minCol = c;
          if (c > maxCol) maxCol = c;
        }
      }
    }

    Map<Material, Character> materialToSymbol = new LinkedHashMap<>();
    Map<Character, Material> ingredients = new LinkedHashMap<>();
    List<String> shape = new ArrayList<>();
    String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
    int symbolIndex = 0;

    for (int r = minRow; r <= maxRow; r++) {
      StringBuilder rowBuilder = new StringBuilder();
      for (int c = minCol; c <= maxCol; c++) {
        Material material = grid[r][c];
        if (material == Material.AIR) {
          rowBuilder.append(' ');
          continue;
        }

        Character symbol = materialToSymbol.get(material);
        if (symbol == null) {
          if (symbolIndex >= symbols.length()) {
            player.sendMessage("§cToo many unique ingredients");
            return false;
          }
          symbol = symbols.charAt(symbolIndex++);
          materialToSymbol.put(material, symbol);
          ingredients.put(symbol, material);
        }
        rowBuilder.append(symbol);
      }
      shape.add(rowBuilder.toString());
    }

    configCommand.saveOrUpdateRecipe(
      recipeId,
      new ItemStack(result.getType(), Math.max(1, result.getAmount())),
      shape,
      ingredients
    );

    player.sendMessage("§aSaved recipe §f" + recipeId);
    return true;
  }

  private void returnItem(Player player, ItemStack stack) {
    if (stack == null || stack.getType() == Material.AIR) return;
    Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
    leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
  }

  private ItemStack filler() {
    ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta meta = pane.getItemMeta();
    meta.displayName(Component.text(" "));
    pane.setItemMeta(meta);
    return pane;
  }

  private ItemStack control(Material type, String name, NamedTextColor color) {
    ItemStack pane = new ItemStack(type);
    ItemMeta meta = pane.getItemMeta();
    meta.displayName(Component.text(name, color));
    pane.setItemMeta(meta);
    return pane;
  }

  private static class RecipeEditorHolder implements InventoryHolder {
    private final String recipeId;
    private boolean confirmed = false;

    private RecipeEditorHolder(String recipeId, UUID owner) {
      this.recipeId = recipeId;
    }

    @Override
    public Inventory getInventory() {
      return null;
    }
    public String recipeId() {
      return recipeId;
    }
  }
}

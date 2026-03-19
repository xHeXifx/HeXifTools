package org.hexif.hexiftools;


import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

public class CustomItems implements Listener {
  private final HeXifTools plugin;
  private final NamespacedKey key;
  private static final int BURST_SIZE = 4;
  private static final double SPREAD = 0.08;

  public CustomItems(HeXifTools plugin) {
    this.plugin = plugin;
    this.key = new NamespacedKey(plugin, "big_exp_bottle");
  }

  public ItemStack createBigXPBottle() {
      ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
      ItemMeta meta = item.getItemMeta();
      plugin.getLogger().info("Created Big Experiance Bottle");

      meta.displayName(Component.text("Big Experiance Bottle", NamedTextColor.AQUA));

      meta.getPersistentDataContainer().set(
          key,
          PersistentDataType.BYTE,
          (byte) 1
      );

      item.setItemMeta(meta);
      return item;
  }

  @EventHandler
  public void onThrow(ProjectileLaunchEvent event) {
      if (!(event.getEntity() instanceof ThrownExpBottle bottle)) return;
      if (!(bottle.getShooter() instanceof Player player)) return;

      ItemStack hand = player.getInventory().getItemInMainHand();
      if (!isBigBottle(hand)) return;

      bottle.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

      if (!player.isSneaking()) return;

      int extrasToLaunch = Math.min(BURST_SIZE - 1, hand.getAmount());
      hand.setAmount(hand.getAmount() - extrasToLaunch);

      for (int i = 0; i < extrasToLaunch; i++) {
        ThrownExpBottle extra = player.getWorld().spawn(player.getEyeLocation(), ThrownExpBottle.class);
        extra.setShooter(player);
        extra.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        Vector base = bottle.getVelocity().clone();
        Vector jitter = new Vector(
          (Math.random() - 0.5) * SPREAD,
          (Math.random() - 0.5) * SPREAD,
          (Math.random() - 0.5) * SPREAD
        );
        extra.setVelocity(base.add(jitter));
      }
  }

  private boolean isBigBottle(ItemStack item) {
    return item != null
      && item.getType() == Material.EXPERIENCE_BOTTLE
      && item.hasItemMeta()
      && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
  }

  @EventHandler
  public void onXP(ExpBottleEvent event) {
      ThrownExpBottle bottle = event.getEntity();

      if (!bottle.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

      int original = event.getExperience();
      event.setExperience(original * 5);
  }
}

package org.hexif.hexiftools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ViewContainers implements Listener {

    private static final Component GUI_TITLE =
            Component.text("Container Viewer", NamedTextColor.GOLD);

    public void open(Player player, List<ContainerInfo> containers) {

        int size = 54;
        Inventory gui = Bukkit.createInventory(new ContainerHolder(), size, GUI_TITLE);

        int slot = 0;
        for (ContainerInfo info : containers) {

            if (slot >= 45) break;

            gui.setItem(slot, createContainerItem(info));
            slot++;
        }

        for (int i = 45; i < 54; i++) {
            gui.setItem(i, createFiller());
        }

        player.openInventory(gui);
    }

    private ItemStack createContainerItem(ContainerInfo info) {

        ItemStack item = new ItemStack(info.material());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(formatMaterial(info.material()), NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Placed by: ", NamedTextColor.GRAY)
                .append(Component.text(info.placedByName(), NamedTextColor.WHITE)));
        lore.add(Component.text("Position: ", NamedTextColor.GRAY)
                .append(Component.text(formatLocation(info.location()), NamedTextColor.WHITE)));
        lore.add(Component.text("World: ", NamedTextColor.GRAY)
                .append(Component.text(info.location().getWorld().getName(), NamedTextColor.WHITE)));
        
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);
        return glass;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ContainerHolder) {
            event.setCancelled(true);
        }
    }

    private String formatMaterial(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }

    private String formatLocation(Location loc) {
        return "x: " + loc.getBlockX() + " y: " +
                loc.getBlockY() + " z: " +
                loc.getBlockZ();
    }

    private static class ContainerHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record ContainerInfo(
            Material material,
            UUID placedBy,
            String placedByName,
            Location location
    ) {}
}
package org.hexif.hexiftools;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

public class AnvilRenameListener implements Listener {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    
    private final JavaPlugin plugin;

    public AnvilRenameListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilRename(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("features.anvilFormatting", true)) {
            return;
        }

        if (!(event.getView() instanceof AnvilView anvilView)) {
            return;
        }

        String renameText = anvilView.getRenameText();

        if (renameText == null) {
            return;
        }

        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }

        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.displayName(AMPERSAND_SERIALIZER.deserialize(renameText));
        result.setItemMeta(meta);
        event.setResult(result);
    }
}
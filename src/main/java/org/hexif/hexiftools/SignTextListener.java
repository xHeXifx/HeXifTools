package org.hexif.hexiftools;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SignTextListener implements Listener {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    
    private final JavaPlugin plugin;

    public SignTextListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!plugin.getConfig().getBoolean("features.signFormatting", true)) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            net.kyori.adventure.text.Component component = event.line(i);
            if (component == null) continue;
            String plain = AMPERSAND_SERIALIZER.serialize(component);
            if (!plain.isEmpty()) {
                event.line(i, AMPERSAND_SERIALIZER.deserialize(plain));
            }
        }
    }
}
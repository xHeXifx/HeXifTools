package org.hexif.hexiftools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PlaceBlocker {
    private final Logger logger;
    private final Map<String, List<String>> blockedPlacements;

    public PlaceBlocker(Map<String, List<String>> blockedPlacements) {
        this.logger = Logger.getLogger(PlaceBlocker.class.getName());
        this.blockedPlacements = blockedPlacements;
    }

    public boolean checkAndBlock(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String blockType = "minecraft:" + event.getBlock().getType().name().toLowerCase();
        
        if (!blockedPlacements.containsKey(playerName)) {
            return false;
        }
        
        List<String> blockedBlocks = blockedPlacements.get(playerName);
        
        if (blockedBlocks.contains(blockType)) {
            event.setCancelled(true);
            
            Component actionBar = Component.text("Block Placement Denied: You are not permitted to place this block")
                                            .color(NamedTextColor.RED);
            
            try {
                player.sendActionBar(actionBar);
            } catch (Exception e) {
                logger.warning("Failed to send actionbar: " + e);
                player.sendMessage(Component.text("You are not permitted to place this block")
                    .color(NamedTextColor.RED));
            }

            
            return true;
        }
        
        return false;
    }
}

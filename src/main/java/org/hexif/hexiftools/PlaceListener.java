package org.hexif.hexiftools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class PlaceListener implements Listener {
    private final List<String> trackedBlocks;
    private final File dataFile;
    private final Logger logger;
    private final Gson gson;
    private final PlaceBlocker placeBlocker;
    private final JavaPlugin plugin;

    public PlaceListener(List<String> trackedBlocks, File dataFolder, Logger logger, PlaceBlocker placeBlocker, JavaPlugin plugin) {
        this.trackedBlocks = trackedBlocks;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(dataFolder, "placed_blocks.json");
        this.placeBlocker = placeBlocker;
        this.plugin = plugin;

        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                try (FileWriter writer = new FileWriter(dataFile)) {
                    writer.write("[]");
                }
            } catch (IOException e) {
                logger.severe("Failed to create placed_blocks.json: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (placeBlocker != null && placeBlocker.checkAndBlock(event)) {
            return;
        }

        if (!plugin.getConfig().getBoolean("features.trackContainers")) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String blockType = "minecraft:" + block.getType().name().toLowerCase();
        
        if (!trackedBlocks.contains(blockType)) {
            return;
        }
        
        logger.info("Tracked block placed: " + blockType + " by " + player.getName());
        
        JsonObject entry = new JsonObject();
        entry.addProperty("playername", player.getName());
        entry.addProperty("block", blockType);
        
        Location loc = block.getLocation();
        JsonObject position = new JsonObject();
        position.addProperty("x", loc.getBlockX());
        position.addProperty("y", loc.getBlockY());
        position.addProperty("z", loc.getBlockZ());
        position.addProperty("world", loc.getWorld().getName());
        entry.add("position", position);
        
        entry.addProperty("timestamp", System.currentTimeMillis());
        
        saveToJson(entry);
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
                    logger.warning("Failed to read existing data, creating new array: " + e.getMessage());
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
            logger.severe("Failed to save block placement data: " + e.getMessage());
        }
    }
}

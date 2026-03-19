package org.hexif.hexiftools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class ContainerBreakListener implements Listener {
    private final File dataFile;
    private final Logger logger;
    private final Gson gson;

    public ContainerBreakListener(File dataFolder, Logger logger) {
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(dataFolder, "placed_blocks.json");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        removeFromJson(loc);
    }

    private void removeFromJson(Location location) {
        if (!dataFile.exists()) {
            return;
        }

        try {
            JsonArray array;

            try (FileReader reader = new FileReader(dataFile)) {
                array = gson.fromJson(reader, JsonArray.class);
                if (array == null) {
                    return;
                }
            } catch (Exception e) {
                logger.warning("Failed to read placed blocks data: " + e.getMessage());
                return;
            }

            JsonArray newArray = new JsonArray();
            boolean found = false;

            for (JsonElement element : array) {
                JsonObject entry = element.getAsJsonObject();
                JsonObject position = entry.getAsJsonObject("position");

                int x = position.get("x").getAsInt();
                int y = position.get("y").getAsInt();
                int z = position.get("z").getAsInt();
                String worldName = position.get("world").getAsString();

                // Check if this entry matches the broken block location
                if (x == location.getBlockX() && 
                    y == location.getBlockY() && 
                    z == location.getBlockZ() && 
                    worldName.equals(location.getWorld().getName())) {
                    found = true;
                    logger.info("Removed broken container from tracking at " + 
                               x + ", " + y + ", " + z + " in " + worldName);
                } else {
                    newArray.add(entry);
                }
            }

            if (found) {
                try (FileWriter writer = new FileWriter(dataFile)) {
                    gson.toJson(newArray, writer);
                }
            }

        } catch (IOException e) {
            logger.severe("Failed to update placed blocks data: " + e.getMessage());
        }
    }
}

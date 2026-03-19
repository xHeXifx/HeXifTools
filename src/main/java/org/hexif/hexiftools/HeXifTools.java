package org.hexif.hexiftools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HeXifTools extends JavaPlugin {

    private DiscordWebhook webhook;
    private DeathListener deathListener;
    private PlaceListener placeListener;
    private ContainerBreakListener containerBreakListener;
    private AnvilRenameListener anvilRenameListener;
    private ViewContainers viewContainers;
    private RecipeManager recipeManager;
    private RecipeEditorGUI recipeEditorGUI;
    private CustomItems customItems;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        customItems = new CustomItems(this);
        recipeManager = new RecipeManager(this);
        loadConfig();
        
        viewContainers = new ViewContainers();
        Bukkit.getPluginManager().registerEvents(viewContainers, this);
        
        containerBreakListener = new ContainerBreakListener(getDataFolder(), getLogger());
        Bukkit.getPluginManager().registerEvents(containerBreakListener, this);

        anvilRenameListener = new AnvilRenameListener(this);
        Bukkit.getPluginManager().registerEvents(anvilRenameListener, this);

        customItems = new CustomItems(this);
        Bukkit.getPluginManager().registerEvents(customItems, this);
        
        recipeEditorGUI = new RecipeEditorGUI(this);
        Bukkit.getPluginManager().registerEvents(recipeEditorGUI, this);

        Bukkit.getPluginManager().registerEvents(new SignTextListener(this), this);
        
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("hexiftools").setExecutor(commandHandler);
        getCommand("hexiftools").setTabCompleter(commandHandler);
        
        getLogger().info("HeXifTools enabled");
    }
    
    @Override
    public void onDisable() {
        if (deathListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(deathListener);
        }
        if (placeListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(placeListener);
        }
        if (containerBreakListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(containerBreakListener);
        }
        if (anvilRenameListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(anvilRenameListener);
        }
        if (customItems != null) {
            org.bukkit.event.HandlerList.unregisterAll(customItems);
        }
        if (recipeEditorGUI != null) {
            org.bukkit.event.HandlerList.unregisterAll(recipeEditorGUI);
        }


        getLogger().info("HeXifTools disabled");
    }
    
    public CustomItems getCustomItems() {
        return customItems;
    }

    public RecipeEditorGUI getRecipeEditorGUI() {
        return recipeEditorGUI;
    }

    private void loadConfig() {
        String webhookUrl = getConfig().getString("webhook-url");
        
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("PUT_YOUR_DISCORD_WEBHOOK_HERE")) {
            getLogger().severe("Invalid webhook url in config.yml Please set a valid Discord webhook url.");
            getLogger().severe("Current value: " + webhookUrl);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        webhook = new DiscordWebhook(webhookUrl, getLogger(), this);
        
        java.util.List<String> excludedPlayers = getConfig().getStringList("excluded-players");
        java.util.List<String> bypassWhenOnline = getConfig().getStringList("bypasswhenonline");
        java.util.List<String> trackedBlocks = getConfig().getStringList("tracked-blocks");
        
        boolean hardcoreMode = getConfig().getBoolean("hardcoreMode", false);
        long roleID = getConfig().getLong("roleID", 0);
        
        java.util.Map<String, java.util.List<String>> placeBlockerMap = new java.util.HashMap<>();
        if (getConfig().contains("placeBlocker")) {
            org.bukkit.configuration.ConfigurationSection placeBlockerSection = getConfig().getConfigurationSection("placeBlocker");
            if (placeBlockerSection != null) {
                for (String playerName : placeBlockerSection.getKeys(false)) {
                    java.util.List<String> blockedBlocks = placeBlockerSection.getStringList(playerName);
                    placeBlockerMap.put(playerName, blockedBlocks);
                }
            }
        }
        PlaceBlocker placeBlocker = new PlaceBlocker(placeBlockerMap);
        
        if (deathListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(deathListener);
        }
        if (placeListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(placeListener);
        }

        if (recipeManager != null) { 
            recipeManager.reloadCustomRecipes();
        }

        
        deathListener = new DeathListener(webhook, excludedPlayers, bypassWhenOnline, hardcoreMode, roleID, this);
        Bukkit.getPluginManager().registerEvents(deathListener, this);
        
        placeListener = new PlaceListener(trackedBlocks, getDataFolder(), getLogger(), placeBlocker, this);
        Bukkit.getPluginManager().registerEvents(placeListener, this);
    }
    
    public void reloadPluginConfig() {
        reloadConfig();
        loadConfig();
        getLogger().info("Config reloaded");
    }
    
    public void fullReload() {
        getLogger().info("Performing full plugin reload...");
        
        org.bukkit.event.HandlerList.unregisterAll(this);
        
        onDisable();
        
        onEnable();
        
        getLogger().info("Full plugin reload completed");
    }
    
    public void openContainerView(Player player) {
        List<ViewContainers.ContainerInfo> containers = loadContainersFromJson();
        
        if (containers.isEmpty()) {
            player.sendMessage("§cNo tracked containers found.");
            return;
        }
        
        viewContainers.open(player, containers);
    }
    
    private List<ViewContainers.ContainerInfo> loadContainersFromJson() {
        List<ViewContainers.ContainerInfo> containers = new ArrayList<>();
        File dataFile = new File(getDataFolder(), "placed_blocks.json");
        
        if (!dataFile.exists()) {
            return containers;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Gson gson = new Gson();
            JsonArray array = gson.fromJson(reader, JsonArray.class);
            
            if (array == null) {
                return containers;
            }
            
            for (JsonElement element : array) {
                JsonObject entry = element.getAsJsonObject();
                
                String playerName = entry.get("playername").getAsString();
                String blockType = entry.get("block").getAsString();
                
                JsonObject position = entry.getAsJsonObject("position");
                int x = position.get("x").getAsInt();
                int y = position.get("y").getAsInt();
                int z = position.get("z").getAsInt();
                String worldName = position.get("world").getAsString();
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }
                
                Location location = new Location(world, x, y, z);
                
                String materialName = blockType.replace("minecraft:", "").toUpperCase();
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Unknown material: " + materialName);
                    continue;
                }
                
                // no player id so we make a fake one
                UUID playerId = UUID.nameUUIDFromBytes(playerName.getBytes());
                
                containers.add(new ViewContainers.ContainerInfo(
                    material,
                    playerId,
                    playerName,
                    location
                ));
            }
            
        } catch (Exception e) {
            getLogger().severe("Failed to load container data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return containers;
    }
}

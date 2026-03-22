package org.hexif.hexiftools;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathListener implements Listener {

    private final DiscordWebhook webhook;
    private final List<String> excludedPlayers;
    private final List<String> bypassWhenOnline;
    private final boolean hardcoreMode;
    private final long roleID;
    private final JavaPlugin plugin;
    private final ToDoCommand todos;

    public DeathListener(DiscordWebhook webhook, List<String> excludedPlayers, List<String> bypassWhenOnline, boolean hardcoreMode, long roleID, JavaPlugin plugin, ToDoCommand todos) {
        this.webhook = webhook;
        this.excludedPlayers = excludedPlayers;
        this.bypassWhenOnline = bypassWhenOnline;
        this.hardcoreMode = hardcoreMode;
        this.roleID = roleID;
        this.plugin = plugin;
        this.todos = todos;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!hardcoreMode) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("features.sendPlayerDeath")) {
            return;
        }

        Player player = event.getEntity();
        String playerName = player.getName();
        String deathCause = getPlayerDeathCause(player);
        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        String playTime = formatPlaytime(ticks);
        
        // Format: "{playername} died {deathcause}"
        String description = "**" + playerName + "** died " + deathCause + "\nPlayed for " + playTime;
        
        // Only ping role if its set
        String rolePing = roleID > 0 ? "<@&" + roleID + ">" : null;
        
        webhook.sendEmbedWithContent(
            "💀 Hardcore Death",
            description,
            0x8B0000,
            rolePing
        );
    }
    
    private String formatPlaytime(long ticks) {
        long seconds = ticks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        return hours + "h " + minutes + "m";
    }

    private String getPlayerDeathCause(Player player) {
        EntityDamageEvent damageEvent = player.getLastDamageCause();
        
        if (damageEvent == null) {
            return "from unknown causes";
        }
        
        if (damageEvent instanceof EntityDamageByEntityEvent damageByEntity) {
            Entity damager = damageByEntity.getDamager();
            
            if (damager instanceof Player killer) {
                return "to **" + killer.getName() + "**";
            }
            
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
                return "to **" + shooter.getName() + "** (projectile)";
            }
            
            String mobName = damager.getType().toString().toLowerCase().replace("_", " ");
            return "to " + mobName;
        }
        
        EntityDamageEvent.DamageCause cause = damageEvent.getCause();
        return switch (cause) {
            case FALL -> "from a fatal fall";
            case FIRE, FIRE_TICK, LAVA -> "in flames";
            case DROWNING -> "by drowning";
            case SUFFOCATION -> "from suffocation";
            case STARVATION -> "from starvation";
            case POISON -> "from poison";
            case MAGIC -> "by magic";
            case WITHER -> "to wither";
            case VOID -> "in the void";
            case LIGHTNING -> "from lightning";
            case FREEZE -> "from freezing";
            case THORNS -> "trying to hurt a thorny creature";
            case DRAGON_BREATH -> "from dragon's breath";
            case FLY_INTO_WALL -> "from kinetic energy";
            case HOT_FLOOR -> "discovering the floor was lava";
            case CRAMMING -> "from cramming";
            case DRYOUT -> "from drying out";
            case FALLING_BLOCK -> "from a falling block";
            case SONIC_BOOM -> "from a sonic boom";
            default -> "from " + cause.toString().toLowerCase().replace("_", " ");
        };
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (!plugin.getConfig().getBoolean("features.sendEntityDeath")) {
            return;
        }

        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            String petType = entity.getType().toString().toLowerCase().replace("_", " ");
            String owner = tameable.getOwner() != null ? tameable.getOwner().getName() : "Unknown";
            String cause = getCauseOfDeath(event.getEntity());
            
            webhook.sendEmbed(
                "🐾 Pet Death",
                "Tamed " + petType + " of **" + owner + "** has died by " + cause,
                0xE74C3C
            );
        }

        if (entity instanceof Villager villager) {
            String location = String.format("X: %d, Y: %d, Z: %d",
                villager.getLocation().getBlockX(),
                villager.getLocation().getBlockY(),
                villager.getLocation().getBlockZ());
            String cause = getCauseOfDeath(event.getEntity());
            
            webhook.sendEmbed(
                "Villager Death",
                "A villager died at " + location + " by " + cause,
                0xE67E22
            );
        }
    }
    
    private String getCauseOfDeath(LivingEntity entity) {
        Player killer = entity.getKiller();
        if (killer != null) {
            return "player **" + killer.getName() + "**";
        }
        
        EntityDamageEvent damageEvent = entity.getLastDamageCause();
        if (damageEvent instanceof EntityDamageByEntityEvent damageByEntity) {
            Entity damager = damageByEntity.getDamager();
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
                return "player **" + shooter.getName() + "** (projectile)";
            }
            return damager.getType().toString().toLowerCase().replace("_", " ");
        }
        
        if (damageEvent != null) {
            return damageEvent.getCause().toString().toLowerCase().replace("_", " ");
        }
        
        return "unknown reasons";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        
        if (excludedPlayers.contains(playerName)) {
            return;
        }
        
        if (isBypassPlayerOnline()) {
            return;
        }

        if (!plugin.getConfig().getBoolean("features.sendPlayerJoinLeave")) {
            return;
        }
        
        todos.viewToDos(event.getPlayer());

        webhook.sendEmbed(
            "Player Joined",
            "**" + playerName + "** joined the game",
            0x2ECC71
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        
        if (excludedPlayers.contains(playerName)) {
            return;
        }
        
        if (isBypassPlayerOnline(playerName)) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("features.sendPlayerJoinLeave")) {
            return;
        }

        webhook.sendEmbed(
            "Player Left",
            "**" + playerName + "** left the game",
            0x95A5A6
        );
    }
    
    private boolean isBypassPlayerOnline() {
        return isBypassPlayerOnline(null);
    }
    
    private boolean isBypassPlayerOnline(String excludePlayer) {
        return bypassWhenOnline.stream()
            .filter(name -> excludePlayer == null || !name.equals(excludePlayer))
            .anyMatch(name -> Bukkit.getPlayerExact(name) != null);
    }
}

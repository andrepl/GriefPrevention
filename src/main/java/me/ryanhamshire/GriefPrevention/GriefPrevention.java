/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import me.ryanhamshire.GriefPrevention.configuration.ConfigData;
import me.ryanhamshire.GriefPrevention.messages.MessageManager;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.commands.CommandHandler;
import me.ryanhamshire.GriefPrevention.data.*;
import me.ryanhamshire.GriefPrevention.listeners.BlockEventHandler;
import me.ryanhamshire.GriefPrevention.listeners.EntityEventHandler;
import me.ryanhamshire.GriefPrevention.listeners.PlayerEventHandler;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import me.ryanhamshire.GriefPrevention.tasks.CleanupUnusedClaimsTask;
import me.ryanhamshire.GriefPrevention.tasks.DeliverClaimBlocksTask;
import me.ryanhamshire.GriefPrevention.tasks.EntityCleanupTask;
import me.ryanhamshire.GriefPrevention.tasks.RestoreNatureProcessingTask;
import me.ryanhamshire.GriefPrevention.tasks.SendPlayerMessageTask;
import me.ryanhamshire.GriefPrevention.tasks.TreeCleanupTask;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GriefPrevention extends JavaPlugin {
    // for convenience, a reference to the instance of this plugin
    public static GriefPrevention instance;

    // for logging to the console and log file
    public ConfigData configuration = null;

    
    // this handles data storage, like player and region data
    public DataStore dataStore;

    // reference to the economy plugin, if economy integration is enabled
    public static Economy economy = null;

    // how far away to search from a tree trunk for its branch blocks
    public static final int TREE_RADIUS = 5;

    // how long to wait before deciding a player is staying online or staying offline, for notification messages
    public static final int NOTIFICATION_SECONDS = 20;

    private CommandHandler commandHandler;
    
    private static boolean eventsRegistered = false;

    public DeliverClaimBlocksTask claimTask = null;
    public CleanupUnusedClaimsTask cleanupTask = null;
    private MessageManager messageManager;
    private BlockEventHandler blockEventHandler;


    // adds a server log entry
    public static void addLogEntry(String entry) {
        instance.getLogger().info(entry);
    }

    /**
     * Retrieves a World Configuration given the World. if the World Configuration is not loaded,
     * it will be loaded from the plugins/GriefPreventionData/WorldConfigs folder. If a file is not present for the world,
     * the template file will be used. The template file is configured in config.yml, and defaults to _template.cfg in the given folder.
     * if no template is found, a default, empty configuration is created and returned.
     *
     * @param world World to retrieve configuration for.
     * @return WorldConfig representing the configuration of the given world.
     */
    public WorldConfig getWorldCfg(World world) {
        return configuration.getWorldConfig(world);
    }

    /**
     * Retrieves a World Configuration given the World Name. If the World Configuration is not loaded, it will be loaded
     * from the plugins/GriefPreventionData/WorldConfigs folder. If a file is not present, the template will be used and a new file will be created for
     * the given name.
     *
     * @param worldname Name of world to get configuration for.
     * @return WorldConfig representing the configuration of the given world.
     */
    public WorldConfig getWorldCfg(String worldname) {
        return configuration.getWorldConfig(worldname);
    }


    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    // initializes well...   everything
    public void onEnable() {
        instance = this;
        // load the config if it exists
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();
        configuration = new ConfigData(config, outConfig);
        // read configuration settings (note defaults)
        commandHandler = new CommandHandler(this);
        commandHandler.initialize();

        messageManager = new MessageManager(this);
        
        // load player groups.

        // optional database settings

        // when datastore initializes, it loads player and claim data, and posts some stats to the log
        if (configuration.getDatabaseUrl().length() > 0) {
            try {
                DatabaseDataStore databaseStore = new DatabaseDataStore(configuration.getDatabaseUrl(),
                        configuration.getDatabaseUserName(), configuration.getDatabasePassword());
                if (FlatFileDataStore.hasData()) {
                    GriefPrevention.addLogEntry("There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore();
                    flatFileStore.migrateData(databaseStore);
                    GriefPrevention.addLogEntry("Data migration process complete.  Reloading data from the database...");
                    databaseStore.close();
                    databaseStore = new DatabaseDataStore(configuration.getDatabaseUrl(), configuration.getDatabaseUserName(),
                            configuration.getDatabasePassword());
                }
                this.dataStore = databaseStore;
            } catch (Exception e) {
                GriefPrevention.addLogEntry("Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
                return;
            }
        }

        // if not using the database because it's not configured or because there was a problem, use the file system to store data
        // this is the preferred method, as it's simpler than the database scenario
        if (this.dataStore == null) {
            try {
                this.dataStore = new FlatFileDataStore();
            } catch (Exception e) {
                GriefPrevention.addLogEntry("Unable to initialize the file system data store.  Details:");
                GriefPrevention.addLogEntry(e.getMessage());
            }
        }
        // start recurring cleanup scan for unused claims belonging to inactive players
        // if the option is enabled.
        // look through all world configurations.
        boolean claimcleanupOn = false;
        boolean entitycleanupEnabled = false;
        if (entitycleanupEnabled) {
            EntityCleanupTask task = new EntityCleanupTask(0);
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 20L);
        }

        // register for events
        if (!eventsRegistered) {
            eventsRegistered = true;
            PluginManager pluginManager = this.getServer().getPluginManager();
            // player events
            PlayerEventHandler playerEventHandler = new PlayerEventHandler(this.dataStore, this);
            pluginManager.registerEvents(playerEventHandler, this);
            // block events
            blockEventHandler = new BlockEventHandler(this.dataStore, this);
            pluginManager.registerEvents(blockEventHandler, this);
            // entity events
            EntityEventHandler entityEventHandler = new EntityEventHandler(this.dataStore, this);
            pluginManager.registerEvents(entityEventHandler, this);
        }

        // if economy is enabled
        if (this.configuration.getClaimBlocksPurchaseCost() > 0 || this.configuration.getClaimBlocksSellValue() > 0) {
            // try to load Vault
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            GriefPrevention.addLogEntry("Vault loaded successfully!");
            // ask Vault to hook into an economy plugin
            if (economyProvider != null) {
                GriefPrevention.economy = economyProvider.getProvider();
            } else {
                GriefPrevention.addLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
            }
        }
        try {
            new File(DataStore.configFilePath).delete();
            outConfig.save(new File(DataStore.configFilePath).getAbsolutePath());
        } catch (IOException exx) {
            this.getLogger().log(Level.SEVERE, "Failed to save primary configuration file:" + DataStore.configFilePath);
        }
    }

    public void handleClaimClean(Claim c, MaterialInfo source, MaterialInfo target, Player player) {
        Location lesser = c.getLesserBoundaryCorner();
        Location upper = c.getGreaterBoundaryCorner();
        for (int x = lesser.getBlockX(); x <= upper.getBlockX(); x++) {
            for (int y = 0; y <= 255; y++) {
                for (int z = lesser.getBlockZ(); z <= upper.getBlockZ(); z++) {
                    Location createloc = new Location(lesser.getWorld(), x, y, z);
                    Block acquired = lesser.getWorld().getBlockAt(createloc);
                    if (acquired.getTypeId() == source.getTypeID() && acquired.getData() == source.getData()) {
                        acquired.setTypeIdAndData(target.getTypeID(), target.getData(), true);
                    }
                }
            }
        }
    }

    // handles slash commands
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        return commandHandler.onCommand(sender, cmd, commandLabel, args);
    }

    /**
     * transfers a number of claim blocks from a source player to a  target player.
     *
     * @param source player name.
     * @param target Player name.
     * @return number of claim blocks transferred.
     */
    public synchronized int transferClaimBlocks(String source, String target, int desiredAmount) {
        // transfer claim blocks from source to target, return number of claim blocks transferred.
        PlayerData playerData = this.dataStore.getPlayerData(source);
        PlayerData receiverData = this.dataStore.getPlayerData(target);
        if (playerData != null && receiverData != null) {
            int xferamount = Math.min(playerData.getAccruedClaimBlocks(), desiredAmount);
            playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - xferamount);
            receiverData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() + xferamount);
            return xferamount;
        }
        return 0;
  }


    /**
     * Creates a friendly Location string for the given Location.
     *
     * @param location Location to retrieve a string for.
     * @return a formatted String to be shown to a user or for a log file depicting the approximate given location.
     */
    public static String getfriendlyLocationString(Location location) {
        return location.getWorld().getName() + "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
    }

    // helper method to resolve a player by name
    public OfflinePlayer resolvePlayer(String name) {
        // try online players first
        Player player = this.getServer().getPlayer(name);
        if (player != null) return player;

        // then search offline players
        OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
        for (OfflinePlayer offlinePlayer : offlinePlayers) {
            if (offlinePlayer.getName().equalsIgnoreCase(name)) {
                return offlinePlayer;
            }
        }
        return null;
    }

    public void onDisable() {
        // save data for any online players
        Player[] players = this.getServer().getOnlinePlayers();
        for (Player player : players) {
            String playerName = player.getName();
            PlayerData playerData = this.dataStore.getPlayerData(playerName);
            this.dataStore.savePlayerData(playerName, playerData);
        }
        // cancel ALL pending tasks.
        Bukkit.getScheduler().cancelTasks(this);
        this.dataStore.close();
    }

    // called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        WorldConfig wc = getWorldCfg(player.getWorld());
        // if pvp is disabled, do nothing
        if (!player.getWorld().getPVP()) return;

        // if player is in creative mode, do nothing
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // if anti spawn camping feature is not enabled, do nothing
        if (!wc.getProtectFreshSpawns()) return;

        // if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission("griefprevention.nopvpimmunity")) return;

        // check inventory for well, anything
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorStacks = inventory.getArmorContents();

        // check armor slots, stop if any items are found
        for (ItemStack armorStack : armorStacks) {
            if (!(armorStack == null || armorStack.getType() == Material.AIR)) return;
        }

        // check other slots, stop if any items are found
        ItemStack[] generalStacks = inventory.getContents();
        for (ItemStack generalStack : generalStacks) {
            if (!(generalStack == null || generalStack.getType() == Material.AIR)) return;
        }

        // otherwise, apply immunity
        PlayerData playerData = this.dataStore.getPlayerData(player.getName());
        playerData.setPvpImmune(true);

        // inform the player
        GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.PvPImmunityStart);
    }

    // checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        return this.getWorldCfg(world).getClaimsEnabled();
    }

    // moves a player from the claim he's in to a nearby wilderness location
    public Location ejectPlayer(Player player) {
        // look for a suitable location
        Location candidateLocation = player.getLocation();
        while (true) {
            Claim claim = null;
            claim = this.dataStore.getClaimAt(candidateLocation, false, null);

            // if there's a claim here, keep looking
            if (claim != null) {
                candidateLocation = new Location(claim.getLesserBoundaryCorner().getWorld(), claim.getLesserBoundaryCorner().getBlockX() - 1, claim.getLesserBoundaryCorner().getBlockY(), claim.getLesserBoundaryCorner().getBlockZ() - 1);
                continue;
            }

            // otherwise find a safe place to teleport the player
            else {
                // find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
                Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
                player.teleport(destination);
                return destination;
            }
        }
    }

    private static String removeColors(String source) {
        for (ChatColor cc : ChatColor.values()) {
            source = source.replace(cc.toString(), "");
        }
        return source;
    }

    // ensures a piece of the managed world is loaded into server memory
    // (generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location location) {
        Chunk chunk = location.getChunk();
        while (!chunk.isLoaded() || !chunk.load(true)) ;
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSender player, TextMode color, Messages messageID, String... args) {
        sendMessage(player, color, messageID, 0, args);
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSender player, TextMode color, Messages messageID, long delayInTicks, String... args) {
        String message = instance.getMessageManager().getMessage(messageID, args);
        if (message == null || message.equals("")) return;
        sendMessage(player, color, message, delayInTicks);
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSender player, TextMode color, String message) {
        if (player == null) {
            GriefPrevention.addLogEntry(removeColors(message));
        } else {
            player.sendMessage(instance.configuration.getColor(color) + message);
        }
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSender player, ChatColor color, String message) {
        if (player == null) {
            GriefPrevention.addLogEntry(removeColors(message));
        } else {
            player.sendMessage(color + message);
        }
    }

    public static void sendMessage(CommandSender player, TextMode color, String message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, instance.configuration.getColor(color), message);
        if (delayInTicks > 0) {
            instance.getServer().getScheduler().runTaskLater(instance, task, delayInTicks);
        } else {
            task.run();
        }
    }

    // determines whether creative anti-grief rules apply at a location
    public boolean creativeRulesApply(Location location) {
        // return this.config_claims_enabledCreativeWorlds.contains(location.getWorld().getName());
        return configuration.getWorldConfig(location.getWorld()).getCreativeRules();
    }


    // restores nature in multiple chunks, as described by a claim instance
    // this restores all chunks which have ANY number of claim blocks from this claim in them
    // if the claim is still active (in the data store), then the claimed blocks will not be changed (only the area bordering the claim)
    public void restoreClaim(Claim claim, long delayInTicks) {
        // admin claims aren't automatically cleaned up when deleted or abandoned
        if (claim.isAdminClaim()) return;

        // it's too expensive to do this for huge claims
        if (claim.getArea() > 10000) return;

        Chunk lesserChunk = claim.getLesserBoundaryCorner().getChunk();
        Chunk greaterChunk = claim.getGreaterBoundaryCorner().getChunk();

        for (int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
            for (int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++) {
                Chunk chunk = lesserChunk.getWorld().getChunkAt(x, z);
                this.restoreChunk(chunk, getWorldCfg(chunk.getWorld()).getSeaLevelOverride() - 15, false, delayInTicks, null);
            }
    }

    public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization) {
        // build a snapshot of this chunk, including 1 block boundary outside of the chunk all the way around
        int maxHeight = chunk.getWorld().getMaxHeight();
        BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
        Block startBlock = chunk.getBlock(0, 0, 0);
        Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
        for (int x = 0; x < snapshots.length; x++) {
            for (int z = 0; z < snapshots[0][0].length; z++) {
                for (int y = 0; y < snapshots[0].length; y++) {
                    Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
                    snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getTypeId(), block.getData());
                }
            }
        }

        // create task to process those data in another thread
        Location lesserBoundaryCorner = chunk.getBlock(0, 0, 0).getLocation();
        Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();

        // create task
        // when done processing, this task will create a main thread task to actually update the world with processing results
        RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, getWorldCfg(chunk.getWorld()).getSeaLevelOverride(), aggressiveMode, this.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
        this.getServer().getScheduler().runTaskLaterAsynchronously(this, task, delayInTicks);
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public BlockEventHandler getBlockEventHandler() {
        return blockEventHandler;
    }
}

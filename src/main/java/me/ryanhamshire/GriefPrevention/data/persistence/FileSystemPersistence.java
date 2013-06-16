package me.ryanhamshire.GriefPrevention.data.persistence;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.SerializationUtil;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.data.PluginClaimMeta;
import me.ryanhamshire.GriefPrevention.exceptions.DatastoreException;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import javax.persistence.PersistenceException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileSystemPersistence implements IPersistence {

    private GriefPrevention plugin;
    private File dataFolder;
    private File playerFolder;
    private File claimFolder;
    private ConcurrentHashMap<String, Map<String, Object>> dirtyPlayers = new ConcurrentHashMap<String, Map<String, Object>>();
    private ConcurrentHashMap<UUID, Map<String, Object>> dirtyClaims = new ConcurrentHashMap<UUID, Map<String, Object>>();
    private BukkitTask saveTask;

    private Runnable saver = new Runnable() {
        @Override
        public void run() {
            Iterator<Map.Entry<UUID, Map<String, Object>>> dirtyClaimIter = dirtyClaims.entrySet().iterator();
            while (dirtyClaimIter.hasNext()) {
                Map.Entry<UUID, Map<String, Object>> entry = dirtyClaimIter.next();
                writeSerializedClaim(entry.getKey().toString(), entry.getValue());
                dirtyClaimIter.remove();
            }
            Iterator<Map.Entry<String, Map<String, Object>>> dirtyPlayerIter = dirtyPlayers.entrySet().iterator();
            while (dirtyPlayerIter.hasNext()) {
                Map.Entry<String, Map<String, Object>> entry = dirtyPlayerIter.next();
                writeSerializedPlayer(entry.getKey(), entry.getValue());
                dirtyPlayerIter.remove();
            }
        }
    };

    private void writeSerializedPlayer(String playerName, Map<String, Object> data) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Object> e: data.entrySet()) {
            cfg.set(e.getKey(), e.getValue());
        }
        try {
            File playerFile = getPlayerDataFile(playerName, true);
            cfg.save(playerFile);
        } catch (DatastoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSerializedClaim(String id, Map<String, Object> data) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Object> e: data.entrySet()) {
            cfg.set(e.getKey(), e.getValue());
        }
        try {
            File claimFile = getClaimDataFile(id, true);
            cfg.save(claimFile);
        } catch (DatastoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FilenameFilter claimFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            try {
                UUID.fromString(name);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    };


    public FileSystemPersistence(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() throws PersistenceException {
        // Make sure the directories are there.
        try {
            verifyDirectoryStructure();
        } catch (IOException ex) {
            throw new PersistenceException(ex);
        }
        saveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this.saver, 400, 400);
    }

    private void verifyDirectoryStructure() throws IOException {
        dataFolder = new File(plugin.getDataFolder(), "data");
        playerFolder = new File(dataFolder, "players");
        if (!playerFolder.isDirectory()) {
            if (!playerFolder.mkdirs()) {
                throw new IOException("Failed to create player data directory " + playerFolder);
            }
        }
        claimFolder = new File(dataFolder, "claims");
        if (!claimFolder.isDirectory()) {
            if (!claimFolder.mkdirs()) {
                throw new IOException("Failed to create claim data directory " + claimFolder);
            }
        }
    }

    @Override
    public void onDisable() {
        saveTask.cancel();
        saver.run();
    }

    @Override
    public Collection<Claim> loadClaimData() {
        Claim claim;
        YamlConfiguration cfg;
        HashMap<UUID, Claim> claims = new HashMap<UUID, Claim>();
        HashMap<UUID, Set<Claim>> orphans = new HashMap<UUID, Set<Claim>>();

        for (File file: claimFolder.listFiles(claimFileFilter)) {
            if (file.isDirectory()) continue;
            cfg = YamlConfiguration.loadConfiguration(file);
            UUID id = UUID.fromString(file.getName());
            Location min;
            Location max;
            try {
                max = SerializationUtil.locationFromString(cfg.getString("maximumPoint"));
                min = SerializationUtil.locationFromString(cfg.getString("minimumPoint"));
            } catch (WorldNotFoundException e) {
                continue;
            }
            String ownerName = cfg.getString("ownerName", "");
            String[] builders = cfg.getStringList("builders").toArray(new String[0]);
            String[] containers = cfg.getStringList("containers").toArray(new String[0]);
            String[] accessors = cfg.getStringList("accessors").toArray(new String[0]);
            String[] managers = cfg.getStringList("managers").toArray(new String[0]);
            boolean neverDelete = cfg.getBoolean("neverDelete", false);
            claim = new Claim(plugin, min, max, ownerName, builders, containers, accessors, managers, id, neverDelete);
            claim.setModifiedDate(cfg.getLong("modifiedDate", System.currentTimeMillis()));
            if (cfg.contains("flags")) {
                ConfigurationSection flagSec = cfg.getConfigurationSection("flags");
                claim.loadFlags(flagSec.getValues(false));
            }
            if (cfg.contains("meta")) {
                claim.loadClaimMeta(cfg.getConfigurationSection("meta"));
            }
            if (cfg.contains("parentId")) {
                UUID parentId = UUID.fromString(cfg.getString("parentId"));
                if (claims.containsKey(parentId)) {
                    claim.setParent(claims.get(parentId));
                    claims.get(parentId).getChildren().add(claim);
                } else {
                    if (!orphans.containsKey(parentId)) {
                        orphans.put(parentId, new HashSet<Claim>());
                    }
                    orphans.get(parentId).add(claim);
                }
            } else {
                if (orphans.containsKey(claim.getId())) {
                    for (Claim orphan: orphans.get(claim.getId())) {
                        claim.getChildren().add(orphan);
                        orphan.setParent(claim);
                    }
                    orphans.remove(claim.getId());
                }
            }
            claims.put(claim.getId(), claim);
        }
        return claims.values();
    }

    /**
     * Returns a collection of PlayerData to be loaded when the plugin starts up.
     *
     * @return all playerdata instances with logins in the last week
     */
    @Override
    public Collection<PlayerData> loadPlayerData() {
        PlayerData playerData;
        YamlConfiguration cfg;
        List<PlayerData> results = new LinkedList<PlayerData>();
        Date now = new Date();
        for (File file: playerFolder.listFiles()) {
            if (file.isDirectory()) continue;
            cfg = YamlConfiguration.loadConfiguration(file);
            playerData = new PlayerData(plugin);
            playerData.setAccruedClaimBlocks(cfg.getInt("accruedClaimBlocks"));
            playerData.setBonusClaimBlocks(cfg.getInt("bonusClaimBlocks"));
            playerData.setLastLogin(new Date(cfg.getLong("lastLogin")));
            playerData.setPlayerName(cfg.getString("playerName"));
            // TODO Make this configurable.
            // Only load players into memory if they've logged in this week.
            if (now.getTime() - playerData.getLastLogin().getTime() < 1000*60*60*24*7) {
                results.add(playerData);
            }
        }
        return results;
    }

    /**
     * Attempt to retrieve a players data, creating it
     * if no data is found.
     *
     * @param playerName the players name
     * @return a PlayerData
     */
    @Override
    public PlayerData loadOrCreatePlayerData(String playerName) {
        plugin.debug("loadOrCreatePlayerData: " + playerName);
        PlayerData playerData;
        YamlConfiguration cfg;
        File playerFile = null;
        try {
            playerFile = getPlayerDataFile(playerName, false);
        } catch (DatastoreException e) {
            e.printStackTrace();
        }
        playerData = new PlayerData(plugin);
        if (!playerFile.exists()) {
            playerData.setPlayerName(playerName);
        } else {
            cfg = YamlConfiguration.loadConfiguration(playerFile);
            playerData.setAccruedClaimBlocks(cfg.getInt("accruedClaimBlocks"));
            playerData.setBonusClaimBlocks(cfg.getInt("bonusClaimBlocks"));
            playerData.setLastLogin(new Date(cfg.getLong("lastLogin")));
            playerData.setPlayerName(cfg.getString("playerName"));
        }
        return playerData;
    }


    /**
     * Persist a collection of players
     *
     * @param players
     */

    @Override
    public void writePlayerData(PlayerData... players) {
        for (PlayerData p: players) {
            this.dirtyPlayers.put(p.getPlayerName(), p.serialize());
        }
    }

    /**
     * Persist a collection of claims.
     *
     * @param claims
     */
    @Override
    public void writeClaimData(Claim... claims) {
        for (Claim claim: claims) {
            this.dirtyClaims.put(claim.getId(), claim.serialize());
        }
    }

    /**
     * Persist a collection of players
     *
     * @param players an array of player data to be saved
     */
    @Override
    public void writePlayerDataSync(PlayerData... players) {
        File playerFile;
        for (PlayerData pd: players) {
            plugin.debug("Saving player data: " + pd.getPlayerName());
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<String, Object> e: pd.serialize().entrySet()) {
                cfg.set(e.getKey(), e.getValue());
            }
            try {
                playerFile = getPlayerDataFile(pd.getPlayerName(), true);
                cfg.save(playerFile);
            } catch (DatastoreException ex) {
                ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Persist a collection of claims
     *
     * @param claims an array of claims to be saved
     */
    @Override
    public void writeClaimDataSync(Claim... claims) {
        File claimFile;
        for (Claim c: claims) {
            plugin.debug("Saving Claim: " + c);
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<String, Object> e: c.serialize().entrySet()) {
                cfg.set(e.getKey(), e.getValue());
            }
            try {
                claimFile = getClaimDataFile(c.getId().toString(), true);
                cfg.save(claimFile);
            } catch (DatastoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteClaim(Claim claim) {
        try {
            File claimFile = getClaimDataFile(claim.getId().toString(), false);
            claimFile.delete();
        } catch (DatastoreException e) {}
    }


    /**
     * get a File pointing to the location on disk where player/group data should be stored.
     *
     * @param name the filename without extension ('playername' or '$groupname')
     * @param create whether or not to create the file if it doesn't exist
     * @return a File where player data should be written.
     * @throws DatastoreException if any IO errors occur
     */
    File getPlayerDataFile(String name, boolean create) throws DatastoreException {
        if (!playerFolder.isDirectory()) {
            if (!playerFolder.mkdir()) {
                throw new DatastoreException("The player data folder disappeared.");
            }
        }
        File playerFile = new File(playerFolder, name + ".yml");
        if (create && !playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException ex) {
                throw new DatastoreException("Failed to create player file: " + playerFile);
            }
        }
        return playerFile;
    }

    /**
     * get a File pointing to the location on disk where claim data should be stored.
     *
     * @param name the filename without extension (should always be the claim ID)
     * @param create whether or not to create the file if it doesn't exist
     * @return a File where claim data should be written.
     * @throws DatastoreException if any IO errors occur
     */
    File getClaimDataFile(String name, boolean create) throws DatastoreException {
        if (!claimFolder.isDirectory()) {
            if (!claimFolder.mkdir()) {
                throw new DatastoreException("The claim data folder disappeared.");
            }
        }
        File claimFile = new File(claimFolder, name);
        if (create && !claimFile.exists()) {
            try {
                claimFile.createNewFile();
            } catch (IOException ex) {
                throw new DatastoreException("Failed to create claim file: " + claimFile);
            }
        }
        return claimFile;
    }
}
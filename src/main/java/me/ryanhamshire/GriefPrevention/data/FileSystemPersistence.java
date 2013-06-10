package me.ryanhamshire.GriefPrevention.data;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.SerializationUtil;
import me.ryanhamshire.GriefPrevention.exceptions.DatastoreException;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.persistence.PersistenceException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class FileSystemPersistence implements IPersistence {

    private DataStore datastore;
    private File dataFolder;
    private File playerFolder;
    private File claimFolder;

    private FilenameFilter claimFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            try {
                UUID uuid = UUID.fromString(name);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    };

    public FileSystemPersistence(DataStore datastore) {
        this.datastore = datastore;
    }


    @Override
    public void onEnable() throws PersistenceException {
        // Make sure the directories are there.
        try {
            verifyDirectoryStructure();
        } catch (IOException ex) {
            throw new PersistenceException(ex);
        }

    }

    private void verifyDirectoryStructure() throws IOException {
        dataFolder = new File(datastore.plugin.getDataFolder(), "data");
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
    public void onDisable() {}

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
            claim = new Claim(min, max, ownerName, builders, containers, accessors, managers, id, neverDelete);
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

    @Override
    public Collection<PlayerData> loadPlayerData() {
        PlayerData playerData;
        YamlConfiguration cfg;
        List<PlayerData> results = new LinkedList<PlayerData>();
        for (File file: playerFolder.listFiles()) {
            if (file.isDirectory()) continue;
            cfg = YamlConfiguration.loadConfiguration(file);
            playerData = new PlayerData();
            playerData.setAccruedClaimBlocks(cfg.getInt("accruedClaimBlocks"));
            playerData.setBonusClaimBlocks(cfg.getInt("bonusClaimBlocks"));
            playerData.setLastLogin(new Date(cfg.getLong("lastLogin")));
            playerData.setPlayerName(cfg.getString("playerName"));
            results.add(playerData);
        }
        return results;
    }

    @Override
    public void writePlayerData(PlayerData... players) {
        // TODO Don't be Lazy
        writePlayerDataSync(players);
    }

    @Override
    public void writeClaimData(Claim... claims) {
        // TODO Don't Be Lazy
        writeClaimDataSync(claims);
    }

    @Override
    public void writePlayerDataSync(PlayerData... players) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeClaimDataSync(Claim... claims) {
        // To change body of implemented methods use File | Settings | File Templates.
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
        File playerFolder = new File(datastore.plugin.getDataFolder(), "data");
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
        File claimFolder = new File(datastore.plugin.getDataFolder(), "data");
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

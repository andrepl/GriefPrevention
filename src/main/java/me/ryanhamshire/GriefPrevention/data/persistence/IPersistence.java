package me.ryanhamshire.GriefPrevention.data.persistence;

import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;

import java.util.Collection;

public interface IPersistence {

    // Called during plugin enable and disable
    // do not save or load data in these methods.
    // use onEnable to initialize the storage system
    // and onDisable to do any cleanup before the server
    // shuts down, don't be async there.
    public void onEnable();
    public void onDisable();

    /**
     * Load all claims from secondary storage
     *
     * @return a collection of {@link Claim} objects
     */
    public Collection<Claim> loadClaimData();

    /**
     * Load initial players from disk
     *
     * This method doesn't need to load all, or even any player data, but should load
     * recently used players and return them.
     * @return
     */
    public Collection<PlayerData> loadPlayerData();

    /**
     * load a single player from persistence if it exists
     * otherwise create it
     */
    public PlayerData loadOrCreatePlayerData(String playerName);

    // These methods are called periodically with only unsaved data
    // If possible, a persistence engine should do as little
    // on the main thread and schedule actual disk writes async

    /**
     * write a collection of players to persistent storage
     *
     * implementors should do as much work asynchronously as possible.
     * @param players {@link PlayerData} objects to be saved
     */
    public void writePlayerData(PlayerData... players);

    /**
     * write a collection of claims to persistent storage
     *
     * implementors should do as much work asynchronously as possible.
     * @param claims {@link Claim} objects to be saved
     */
    public void writeClaimData(Claim... claims);

    /**
     * Write a collection of players to persistent storage
     *
     * Implementors must save the data immedaitely without
     * using the bukkit scheduler or any other threads
     * @param players {@link PlayerData} to be saved
     */
    public void writePlayerDataSync(PlayerData... players);

    /**
     * Write a collection of claims to persistent storage
     *
     * Implementors must save the data immedaitely without
     * using the bukkit scheduler or any other threads
     * @param claims claims to be saved
     */
    public void writeClaimDataSync(Claim... claims);

    public void deleteClaim(Claim claim);
}

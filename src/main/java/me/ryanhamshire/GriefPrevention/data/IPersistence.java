package me.ryanhamshire.GriefPrevention.data;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 6/9/13
 * Time: 8:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IPersistence {

    // Called during plugin enable and disable
    // do not save or load data in these methods.
    // use onEnable to initialize the storage system
    // and onDisable to do any cleanup before the server
    // shuts down, don't be async there.
    public void onEnable();
    public void onDisable();

    // load all your data here.
    public Collection<Claim> loadClaimData();
    public Collection<PlayerData> loadPlayerData();

    // These methods are called periodically with only unsaved data
    // If possible, a persistence engine should do as little
    // on the main thread and schedule actual disk writes async
    public void writePlayerData(PlayerData... players);
    public void writeClaimData(Claim... claims);

    // Sync methods always called on plugin disable
    // lazy persistence engines could call the from the above
    // 'write' methods as well.
    public void writePlayerDataSync(PlayerData... players);
    public void writeClaimDataSync(Claim... claims);
}

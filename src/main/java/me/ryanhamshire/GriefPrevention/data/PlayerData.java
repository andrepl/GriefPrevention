/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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

package me.ryanhamshire.GriefPrevention.data;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

// holds all of GriefPrevention's player-tied data
@SuppressWarnings("unused")
public class PlayerData {

    GriefPrevention plugin;

    // the player's name
    private String playerName;

    // the player's claims
    private Vector<Claim> claims = new Vector<Claim>();

    public Vector<Claim> getWorldClaims(World p) {
        Vector<Claim> makeresult = new Vector<Claim>();
        for (Claim cc : claims) {
            if (cc.getLesserBoundaryCorner().getWorld().equals(p)) {
                makeresult.add(cc);
            }
        }
        return makeresult;
    }

    // how many claim blocks the player has earned via play time
    private int accruedClaimBlocks = plugin.configuration.getInitialBlocks();

    // where this player was the last time we checked on him for earning claim blocks
    private Location lastAfkCheckLocation = null;

    // how many claim blocks the player has been gifted by admins, or purchased via economy integration 
    private int bonusClaimBlocks = 0;

    // what "mode" the shovel is in determines what it will do when it's used
    private ShovelMode shovelMode = ShovelMode.BASIC;

    // radius for restore nature fill mode
    private int fillRadius = 0;

    // last place the player used the shovel, useful in creating and resizing claims, 
    // because the player must use the shovel twice in those instances
    private Location lastShovelLocation = null;

    // the claim this player is currently resizing
    private Claim claimResizing = null;

    // the claim this player is currently subdividing
    private Claim claimSubdividing = null;

    // the timestamp for the last time the player used /trapped
    private Date lastTrappedUsage;

    // whether or not the player has a pending /trapped rescue
    private boolean pendingTrapped = false;

    // last place the player damaged a chest
    private Location lastChestDamageLocation = null;

    // number of blocks placed outside claims before next warning
    int unclaimedBlockPlacementsUntilWarning = 1;

    // timestamp of last death, for use in preventing death message spam
    long lastDeathTimeStamp = 0;

    private boolean ignoreClaimMessage = false; // using "claim" in a chat will usually send a message
    // to that player about claiming. This is used to implement a cooldown- after a configurable period the player will not
    // receive another message about Claims as a result of using the trigger words.

    private boolean ignoreStuckMessage = false; // save semantics as above, but for Stuck Messages.

    private Date lastLogin;                            // when the player last logged into the server
    private String lastMessage = "";                    // the player's last chat message, or slash command complete with parameters
    private Date lastMessageTimestamp = new Date();  // last time the player sent a chat message or used a monitored slash command

    // visualization
    private Visualization currentVisualization = null;

    // anti-camping pvp protection
    private boolean pvpImmune = false;
    private long lastSpawn = 0;

    // ignore claims mode
    private boolean ignoreClaims = false;

    // the last claim this player was in, that we know of
    private Claim lastClaim = null;

    // pvp
    private long lastPvpTimestamp = 0;
    private String lastPvpPlayer = "";

    // safety confirmation for deleting multi-subdivision claims
    private boolean warnedAboutMajorDeletion = false;

    private InetAddress ipAddress;

    public PlayerData(GriefPrevention plugin) {
        // default last login date value to a year ago to ensure a brand new player can log in
        // see login cooldown feature, PlayerEventHandler.onPlayerLogin()
        // if the player successfully logs in, this value will be overwritten with the current date and time 
        this.plugin = plugin;
        Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -1);
        this.lastLogin = lastYear.getTime();
        this.lastTrappedUsage = lastYear.getTime();
    }

    private WorldConfig wc() {
        Player p = Bukkit.getPlayer(playerName);
        return plugin.getWorldCfg(p.getWorld());
    }

    // whether or not this player is "in" pvp combat
    public boolean inPvpCombat() {
        if (this.lastPvpTimestamp == 0) return false;

        WorldConfig wc = wc();
        long now = Calendar.getInstance().getTimeInMillis();
        long elapsed = now - this.lastPvpTimestamp;
        if (elapsed > wc.getPvPCombatTimeoutSeconds() * 1000) {
            this.lastPvpTimestamp = 0;
            return false;
        }
        return true;
    }

    // the number of claim blocks a player has available for claiming land
    public int getRemainingClaimBlocks() {
        int remainingBlocks = this.accruedClaimBlocks + this.bonusClaimBlocks;
        for (Claim claim : this.claims) {
            remainingBlocks -= claim.getArea();
        }
        // add any blocks this player might have based on group membership (permissions)
        // remainingBlocks += GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerName);
        return remainingBlocks;
    }

    public int getFillRadius() {
        return fillRadius;
    }

    public void setFillRadius(int fillRadius) {
        this.fillRadius = fillRadius;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Vector<Claim> getClaims() {
        return claims;
    }

    public int getAccruedClaimBlocks() {
        return accruedClaimBlocks;
    }

    public void setAccruedClaimBlocks(int accruedClaimBlocks) {
        this.accruedClaimBlocks = accruedClaimBlocks;
    }

    public Location getLastAfkCheckLocation() {
        return lastAfkCheckLocation;
    }

    public void setLastAfkCheckLocation(Location lastAfkCheckLocation) {
        this.lastAfkCheckLocation = lastAfkCheckLocation;
    }

    public int getBonusClaimBlocks() {
        return bonusClaimBlocks;
    }

    public void setBonusClaimBlocks(int bonusClaimBlocks) {
        this.bonusClaimBlocks = bonusClaimBlocks;
    }

    public ShovelMode getShovelMode() {
        return shovelMode;
    }

    public void setShovelMode(ShovelMode shovelMode) {
        this.shovelMode = shovelMode;
    }

    public Location getLastShovelLocation() {
        return lastShovelLocation;
    }

    public void setLastShovelLocation(Location lastShovelLocation) {
        this.lastShovelLocation = lastShovelLocation;
    }

    public Claim getClaimResizing() {
        return claimResizing;
    }

    public void setClaimResizing(Claim claimResizing) {
        this.claimResizing = claimResizing;
    }

    public Claim getClaimSubdividing() {
        return claimSubdividing;
    }

    public void setClaimSubdividing(Claim claimSubdividing) {
        this.claimSubdividing = claimSubdividing;
    }

    public Date getLastTrappedUsage() {
        return lastTrappedUsage;
    }

    public void setLastTrappedUsage(Date lastTrappedUsage) {
        this.lastTrappedUsage = lastTrappedUsage;
    }

    public boolean isPendingTrapped() {
        return pendingTrapped;
    }

    public void setPendingTrapped(boolean pendingTrapped) {
        this.pendingTrapped = pendingTrapped;
    }

    public Location getLastChestDamageLocation() {
        return lastChestDamageLocation;
    }

    public void setLastChestDamageLocation(Location lastChestDamageLocation) {
        this.lastChestDamageLocation = lastChestDamageLocation;
    }

    public int getUnclaimedBlockPlacementsUntilWarning() {
        return unclaimedBlockPlacementsUntilWarning;
    }

    public void setUnclaimedBlockPlacementsUntilWarning(int unclaimedBlockPlacementsUntilWarning) {
        this.unclaimedBlockPlacementsUntilWarning = unclaimedBlockPlacementsUntilWarning;
    }

    public long getLastDeathTimeStamp() {
        return lastDeathTimeStamp;
    }

    public void setLastDeathTimeStamp(long lastDeathTimeStamp) {
        this.lastDeathTimeStamp = lastDeathTimeStamp;
    }

    public boolean isIgnoreClaimMessage() {
        return ignoreClaimMessage;
    }

    public void setIgnoreClaimMessage(boolean value) {
        ignoreClaimMessage = value;
    }

    public boolean isIgnoreStuckMessage() {
        return ignoreStuckMessage;
    }

    public void setIgnoreStuckMessage(boolean value) {
        ignoreStuckMessage = value;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public Visualization getCurrentVisualization() {
        return currentVisualization;
    }

    public void setCurrentVisualization(Visualization currentVisualization) {
        this.currentVisualization = currentVisualization;
    }

    public boolean isPvpImmune() {
        return pvpImmune;
    }

    public void setPvpImmune(boolean pvpImmune) {
        this.pvpImmune = pvpImmune;
    }

    public long getLastSpawn() {
        return lastSpawn;
    }

    public void setLastSpawn(long lastSpawn) {
        this.lastSpawn = lastSpawn;
    }

    public boolean isIgnoreClaims() {
        return ignoreClaims;
    }

    public void setIgnoreClaims(boolean ignoreClaims) {
        this.ignoreClaims = ignoreClaims;
    }

    public Claim getLastClaim() {
        return lastClaim;
    }

    public void setLastClaim(Claim lastClaim) {
        this.lastClaim = lastClaim;
    }

    public long getLastPvpTimestamp() {
        return lastPvpTimestamp;
    }

    public void setLastPvpTimestamp(long lastPvpTimestamp) {
        this.lastPvpTimestamp = lastPvpTimestamp;
    }

    public String getLastPvpPlayer() {
        return lastPvpPlayer;
    }

    public void setLastPvpPlayer(String lastPvpPlayer) {
        this.lastPvpPlayer = lastPvpPlayer;
    }

    public boolean isWarnedAboutMajorDeletion() {
        return warnedAboutMajorDeletion;
    }

    public void setWarnedAboutMajorDeletion(boolean warnedAboutMajorDeletion) {
        this.warnedAboutMajorDeletion = warnedAboutMajorDeletion;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }
}
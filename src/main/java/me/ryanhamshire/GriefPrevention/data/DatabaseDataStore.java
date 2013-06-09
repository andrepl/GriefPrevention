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

package me.ryanhamshire.GriefPrevention.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.exceptions.DatastoreInitializationException;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;

import org.bukkit.*;

//manages data stored in the file system
public class DatabaseDataStore extends DataStore {
    private Connection databaseConnection = null;

    private String databaseUrl;
    private String userName;
    private String password;

    public DatabaseDataStore(String url, String userName, String password) throws DatastoreInitializationException {
        this.databaseUrl = url;
        this.userName = userName;
        this.password = password;
        this.initialize();
    }

    @Override
    void initialize() throws DatastoreInitializationException {
        try {
            //load the java driver for mySQL
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            GriefPrevention.addLogEntry("ERROR: Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
            throw new DatastoreInitializationException(ex);
        }

        try {
            this.refreshDataConnection();
        } catch (Exception e2) {
            GriefPrevention.addLogEntry("ERROR: Unable to connect to database.  Check your config file settings.");
            throw new DatastoreInitializationException(e2);
        }

        try {
            //ensure the data tables exist
            Statement statement = databaseConnection.createStatement();

            if (this.databaseUrl.startsWith("jdbc:postgresql")) {
                statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_nextclaimid (nextid INTEGER);");
                statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_claimdata (id INTEGER, owner VARCHAR(50), lessercorner VARCHAR(100), greatercorner VARCHAR(100), builders TEXT, containers TEXT, accessors TEXT, managers TEXT, parentid INTEGER, neverdelete BOOLEAN NOT NULL DEFAULT false);");
                statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_playerdata (name VARCHAR(50), lastlogin TIMESTAMP WITH TIME ZONE, accruedblocks INTEGER, bonusblocks INTEGER);");
            } else {
                statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_nextclaimid (nextid INT(15));");
                statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_claimdata (id INT(15), owner VARCHAR(50), lessercorner VARCHAR(100), greatercorner VARCHAR(100), builders VARCHAR(1000), containers VARCHAR(1000), accessors VARCHAR(1000), managers VARCHAR(1000), parentid INT(15), neverdelete BOOLEAN NOT NULL DEFAULT 0);");
                statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_playerdata (name VARCHAR(50), lastlogin DATETIME, accruedblocks INT(15), bonusblocks INT(15));");
                ResultSet tempresult = statement.executeQuery("SHOW COLUMNS FROM griefprevention_claimdata LIKE 'neverdelete';");
                if (!tempresult.next()) {
                    statement.execute("ALTER TABLE griefprevention_claimdata ADD neverdelete BOOLEAN NOT NULL DEFAULT 0;");
                }
            }
        } catch (Exception e3) {
            GriefPrevention.addLogEntry("ERROR: Unable to create the necessary database table.  Details:");
            GriefPrevention.addLogEntry(e3.getMessage());
            throw new DatastoreInitializationException(e3);
        }

        //load group data into memory
        Statement statement = null;
        ResultSet results = null;
        try {
            statement = databaseConnection.createStatement();
            results = statement.executeQuery("SELECT * FROM griefprevention_playerdata;");
            while (results.next()) {
                String name = results.getString("name");

                //ignore non-groups.  all group names start with a dollar sign.
                if (!name.startsWith("$")) continue;

                String groupName = name.substring(1);
                if (groupName == null || groupName.isEmpty()) continue;  //defensive coding, avoid unlikely cases

                int groupBonusBlocks = results.getInt("bonusblocks");
                this.permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
            }
        } catch (SQLException e) {
            throw new DatastoreInitializationException(e);
        }

        //load next claim number into memory
        try {
            results = statement.executeQuery("SELECT * FROM griefprevention_nextclaimid;");
            //if there's nothing yet, add it
            if (!results.next()) {
                statement.execute("INSERT INTO griefprevention_nextclaimid VALUES(0);");
                this.nextClaimID = (long) 0;
            } else {  //otherwise load it
                this.nextClaimID = results.getLong("nextid");
            }
        } catch (SQLException e) {
            throw new DatastoreInitializationException(e);
        }

        //load claims data into memory

        ArrayList<Claim> claimsToRemove = new ArrayList<Claim>();

        try {
            results = statement.executeQuery("SELECT * FROM griefprevention_claimdata;");
        } catch (SQLException e) {
            throw new DatastoreInitializationException(e);
        }


        for (int i = 0; i < claimsToRemove.size(); i++) {
            this.deleteClaimFromSecondaryStorage(claimsToRemove.get(i));
        }
        super.initialize();
    }

    @Override
    synchronized void writeClaimToStorage(Claim claim)  //see datastore.cs.  this will ALWAYS be a top level claim
    {
        try {
            this.refreshDataConnection();

            //wipe out any existing data about this claim
            this.deleteClaimFromSecondaryStorage(claim);

            //write top level claim data to the database
            this.writeClaimData(claim);

            //for each subdivision
            for (int i = 0; i < claim.getChildren().size(); i++) {
                //write the subdivision's data to the database
                this.writeClaimData(claim.getChildren().get(i));
            }
        } catch (SQLException e) {
            GriefPrevention.addLogEntry("Unable to save data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
            GriefPrevention.addLogEntry(e.getMessage());
        }
    }

    //actually writes claim data to the database
    synchronized private void writeClaimData(Claim claim) throws SQLException {
        String lesserCornerString = this.locationToString(claim.getLesserBoundaryCorner());
        String greaterCornerString = this.locationToString(claim.getGreaterBoundaryCorner());
        String owner = claim.getOwnerName();

        ArrayList<String> builders = new ArrayList<String>();
        ArrayList<String> containers = new ArrayList<String>();
        ArrayList<String> accessors = new ArrayList<String>();
        ArrayList<String> managers = new ArrayList<String>();

        claim.getPermissions(builders, containers, accessors, managers);

        String buildersString = "";
        for (int i = 0; i < builders.size(); i++) {
            buildersString += builders.get(i) + ";";
        }

        String containersString = "";
        for (int i = 0; i < containers.size(); i++) {
            containersString += containers.get(i) + ";";
        }

        String accessorsString = "";
        for (int i = 0; i < accessors.size(); i++) {
            accessorsString += accessors.get(i) + ";";
        }

        String managersString = "";
        for (int i = 0; i < managers.size(); i++) {
            managersString += managers.get(i) + ";";
        }

        long parentId;
        long id;
        if (claim.getParent() == null) {
            parentId = -1;
        } else {
            parentId = claim.getParent().getID();

            id = claim.getSubClaimID() != null ? claim.getSubClaimID() : claim.getParent().getChildren().indexOf(claim);
        }

        if (claim.getID() == null) {
            id = claim.getSubClaimID() != null ? claim.getSubClaimID() : -1;
        } else {
            id = claim.getID();
        }

        try {
            this.refreshDataConnection();

            Statement statement = databaseConnection.createStatement();
            statement.execute("INSERT INTO griefprevention_claimdata VALUES(" +
                    id + ", '" +
                    owner + "', '" +
                    lesserCornerString + "', '" +
                    greaterCornerString + "', '" +
                    buildersString + "', '" +
                    containersString + "', '" +
                    accessorsString + "', '" +
                    managersString + "', " +
                    parentId + ", " +
                    claim.isNeverdelete() +
                    ");");
        } catch (SQLException e) {
            GriefPrevention.addLogEntry("Unable to save data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
            GriefPrevention.addLogEntry(e.getMessage());
        }
    }

    //deletes a top level claim from the database
    @Override
    synchronized void deleteClaimFromSecondaryStorage(Claim claim) {
        try {
            this.refreshDataConnection();

            Statement statement = this.databaseConnection.createStatement();
            statement.execute("DELETE FROM griefprevention_claimdata WHERE id=" + claim.getID() + ";");
            statement.execute("DELETE FROM griefprevention_claimdata WHERE parentid=" + claim.getID() + ";");
        } catch (SQLException e) {
            GriefPrevention.addLogEntry("Unable to delete data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
            GriefPrevention.addLogEntry(e.getMessage());
        }
    }

    @Override
    synchronized PlayerData getPlayerDataFromStorage(String playerName) {
        PlayerData playerData = new PlayerData();
        playerData.setPlayerName(playerName);

        try {
            this.refreshDataConnection();

            Statement statement = this.databaseConnection.createStatement();
            ResultSet results = statement.executeQuery("SELECT * FROM griefprevention_playerdata WHERE name='" + playerName + "';");

            //if there's no data for this player, create it with defaults
            if (!results.next()) {
                this.savePlayerData(playerName, playerData);
            }

            //otherwise, just read from the database
            else {
                playerData.setLastLogin(results.getTimestamp("lastlogin"));
                playerData.setAccruedClaimBlocks(results.getInt("accruedblocks"));
                playerData.setBonusClaimBlocks(results.getInt("bonusblocks"));
            }
        } catch (SQLException e) {
            GriefPrevention.addLogEntry("Unable to retrieve data for player " + playerName + ".  Details:");
            GriefPrevention.addLogEntry(e.getMessage());
        }

        return playerData;
    }

    //saves changes to player data.  MUST be called after you're done making changes, otherwise a reload will lose them
    @Override
    synchronized public void savePlayerData(String playerName, PlayerData playerData) {
        //never save data for the "administrative" account.  an empty string for player name indicates administrative account
        if (playerName.length() == 0) return;

        try {
            this.refreshDataConnection();

            SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = sqlFormat.format(playerData.getLastLogin());

            Statement statement = databaseConnection.createStatement();
            statement.execute("DELETE FROM griefprevention_playerdata WHERE name='" + playerName + "';");
            statement.execute("INSERT INTO griefprevention_playerdata VALUES ('" + playerName + "', '" + dateString + "', " + playerData.getAccruedClaimBlocks() + ", " + playerData.getBonusClaimBlocks() + ");");
        } catch (SQLException e) {
            GriefPrevention.addLogEntry("Unable to save data for player " + playerName + ".  Details:");
            GriefPrevention.addLogEntry(e.getMessage());
        }
    }

    @Override
    synchronized void incrementNextClaimID() {
        this.setNextClaimID(this.nextClaimID + 1);
    }

    //sets the next claim ID.  used by incrementNextClaimID() above, and also while migrating data from a flat file data store
    synchronized void setNextClaimID(long nextID) {
        this.nextClaimID = nextID;
        try {
            this.refreshDataConnection();

            Statement statement = databaseConnection.createStatement();
            statement.execute("DELETE FROM griefprevention_nextclaimid;");
            statement.execute("INSERT INTO griefprevention_nextclaimid VALUES (" + nextID + ");");
        } catch (SQLException e) {
            GriefPrevention.addLogEntry("Unable to set next claim ID to " + nextID + ".  Details:");
            GriefPrevention.addLogEntry(e.getMessage());
        }
    }

    //updates the database with a group's bonus blocks
    @Override
    synchronized void saveGroupBonusBlocks(String groupName, int currentValue) {
        //group bonus blocks are stored in the player data table, with player name = $groupName
        String playerName = "$" + groupName;
        PlayerData playerData = new PlayerData();
        playerData.setBonusClaimBlocks(currentValue);

        this.savePlayerData(playerName, playerData);
    }

    @Override
    public synchronized void close() {
        if (this.databaseConnection != null) {
            try {
                if (!this.databaseConnection.isClosed()) {
                    this.databaseConnection.close();
                }
            } catch (SQLException e) {}
        }
        this.databaseConnection = null;
    }

    private void refreshDataConnection() throws SQLException {
        if (this.databaseConnection == null || this.databaseConnection.isClosed()) {
            //set username/pass properties
            Properties connectionProps = new Properties();
            connectionProps.put("user", this.userName);
            connectionProps.put("password", this.password);
            //establish connection
            this.databaseConnection = DriverManager.getConnection(this.databaseUrl, connectionProps);
        }
    }
}
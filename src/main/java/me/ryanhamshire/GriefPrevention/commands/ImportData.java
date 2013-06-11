package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.SerializationUtil;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ImportData extends BaseCommand {

    public ImportData(GriefPrevention plugin) {
        super(plugin, "gpimportdata");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        File dataDir;
        try {
            String pluginDir = new File(plugin.getDataFolder(), "../").getCanonicalPath();
            dataDir = new File(pluginDir, "GriefPreventionData");
        } catch (IOException e) {
            sender.sendMessage("No previous data was found.");
            return true;
        }
        if (!dataDir.isDirectory()) {
            sender.sendMessage("No previous data was found.");
            return true;
        }
        sender.sendMessage("Attempting to import data from previous Grief Prevention Installation at " + dataDir + ", please watch the server console for details.");
        File claimDir = new File(dataDir, "ClaimData");
        if (claimDir.exists()) {
            Claim topLevelClaim = null;
            plugin.getLogger().info("Importing Claims...");
            for (File f: claimDir.listFiles()) {
                if (f.getName().startsWith("_")) {
                    continue;
                }
                plugin.getLogger().info("Claim #" + f.getName());
                LinkedList<String> lines = getFileContents(f);
                String line = lines.pop();
                while (!lines.isEmpty()) {
                    //first line is lesser boundary corner location
                    Location lesserBoundaryCorner = null;
                    Location greaterBoundaryCorner = null;
                    try {
                        lesserBoundaryCorner = SerializationUtil.locationFromString(line);
                        //second line is greater boundary corner location
                        line = lines.pop();
                        greaterBoundaryCorner = SerializationUtil.locationFromString(line);
                    } catch (WorldNotFoundException e) {
                        e.printStackTrace();
                    }
                    //third line is owner name
                    line = lines.pop();
                    String ownerName = line;
                    //fourth line is list of builders
                    line = lines.pop();
                    String [] builderNames = line.split(";");
                    //fifth line is list of players who can access containers
                    line = lines.pop();
                    String [] containerNames = line.split(";");
                    //sixth line is list of players who can use buttons and switches
                    line = lines.pop();
                    String [] accessorNames = line.split(";");
                    //seventh line is list of players who can grant permissions
                    line = lines.pop();
                    if(line == null) line = "";
                    String [] managerNames = line.split(";");
                    //Eighth line either contains whether the claim can ever be deleted, or the divider for the subclaims
                    boolean neverdelete = false;
                    line = lines.pop();
                    if(line == null) line = "";
                    if(!line.contains("==========")) {
                        neverdelete = Boolean.parseBoolean(line);
                    }
                    //Sub claims below this line
                    while(line != null && !line.contains("==========")) {
                        line = lines.pop();
                    }
                    //build a claim instance from those data
                    //if this is the first claim loaded from this file, it's the top level claim
                    if (topLevelClaim == null) {
                        //instantiate
                        topLevelClaim = new Claim(plugin, lesserBoundaryCorner, greaterBoundaryCorner, ownerName, builderNames, containerNames, accessorNames, managerNames, UUID.randomUUID(), neverdelete);
                        //otherwise, add this claim to the claims collection
                        plugin.getDataStore().saveClaim(topLevelClaim);
                    } else {
                        Claim subdivision = new Claim(plugin, lesserBoundaryCorner, greaterBoundaryCorner, "--subdivision--", builderNames, containerNames, accessorNames, managerNames, UUID.randomUUID(), neverdelete);
                        subdivision.setParent(topLevelClaim);
                        topLevelClaim.getChildren().add(subdivision);
                        plugin.getDataStore().saveClaim(subdivision);
                    }
                    if (!lines.isEmpty()) {
                        line = lines.pop();
                    }
                }
            }
            plugin.getLogger().info("Import Finished.  restarting the server is suggested.");
            sender.sendMessage(plugin.configuration.getColor(TextMode.SUCCESS) + "Legacy data was sucessfully imported.");
            return true;
        }
        return true;
    }

    private LinkedList<String> getFileContents(File f) {
        LinkedList<String> lines = new LinkedList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            reader.close();
            return lines;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.LinkedList;
import java.util.List;


public class Help extends BaseCommand {

    private static final String[] HelpIndex = new String[] {
            ChatColor.AQUA + "-----=GriefPrevention Help Index=------",
            "use /gphelp [topic] to view each topic.",
            ChatColor.YELLOW + "Topics: Claims,Trust" };

    private static final String[] ClaimsHelp = new String[] {
            ChatColor.AQUA + "-----=GriefPrevention Claims=------",
            ChatColor.YELLOW + " GriefPrevention uses Claims to allow you to claim areas and prevent ",
            ChatColor.YELLOW + "other players from messing with your stuff without your permission.",
            ChatColor.YELLOW + "Claims are created by either placing your first Chest or by using the",
            ChatColor.YELLOW + "Claim creation tool, which is by default a Golden Shovel.",
            ChatColor.YELLOW + "You can resize your claims by using a Golden Shovel on a corner, or",
            ChatColor.YELLOW + "by defining a new claim that encompasses it. The original claim",
            ChatColor.YELLOW + "Will be resized. you can use trust commands to give other players abilities",
            ChatColor.YELLOW + "In your claim. See /gphelp trust for more information" };

    private static final String[] TrustHelp = new String[] {
            ChatColor.AQUA + "------=GriefPrevention Trust=------",
            ChatColor.YELLOW + "You can control who can do things in your claims by using the trust commands",
            ChatColor.YELLOW + "/accesstrust can be used to allow a player to interact with items in your claim",
            ChatColor.YELLOW + "/containertrust can be used to allow a player to interact with your chests.",
            ChatColor.YELLOW + "/trust allows players to build on your claim.",
            ChatColor.YELLOW + "Each trust builds upon the previous one in this list; containertrust includes accesstrust",
            ChatColor.YELLOW + "and build trust includes container trust and access trust." };

    public Help(GriefPrevention plugin) {
        super(plugin, "gphelp");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        String topic = "";
        String[] useLines;

        if (args.size() > 0) {
            topic = args.pop();
        }
        if (topic.equalsIgnoreCase("claims")) {
            useLines = ClaimsHelp;
        } else if (topic.equalsIgnoreCase("trust")) {
            useLines = TrustHelp;
        } else {
            useLines = HelpIndex;
        }
        sender.sendMessage(useLines);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (args.size() == 1) {
            List<String> results = new LinkedList<String>();
            String partial = args.peek().toLowerCase();
            if ("index".startsWith(partial)) results.add("index");
            if ("claims".startsWith(partial)) results.add("claims");
            if ("trust".startsWith(partial)) results.add("trust");
            return results;
        }
        return null;
    }
}

package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.exceptions.InvalidFlagValueException;
import me.ryanhamshire.GriefPrevention.flags.BaseFlag;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Flag extends BaseClaimCommand {

    public Flag(GriefPrevention plugin) {
        super(plugin, "flag", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        String err = claim.allowEdit(player);
        if (err != null) {
            plugin.sendMessage(player, TextMode.ERROR, err);
            return true;
        }
        ChatColor keyColor = plugin.configuration.getColor(TextMode.INSTR);
        ChatColor valueColor = plugin.configuration.getColor(TextMode.INFO);
        if (args.size() == 0) {
            // List Available Flags
            List<String> results = new LinkedList<String>();
            for (BaseFlag flag: plugin.getFlagManager().getAllFlags()) {
                if (player.hasPermission(flag.getRequiredPermission())) {
                    results.add(keyColor + flag.getDisplayName() + ": " + valueColor + claim.getFlag(flag));
                }
            }
            player.sendMessage(results.toArray(new String[results.size()]));
            return true;
        } else {
            BaseFlag flag = plugin.getFlagManager().getFlag(args.peek());
            if (flag == null) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.UnknownFlag, args.peek());
                return true;
            }
            args.pop();
            if (args.size() == 0) {
                // Flag details
                String[] lines = new String[3];
                lines[0] = keyColor + flag.getDisplayName() + valueColor + "(" + keyColor + flag.getKey() + valueColor + ")";
                lines[1] = valueColor + flag.getDescription();
                lines[2] = keyColor + "Set To: " + valueColor + claim.getFlag(flag);
                if (claim.getFlag(flag).equals(flag.getDefaultValue())) {
                    lines[2] += "(default)";
                } else {
                    lines[2] += "(default: " + flag.getDefaultValue() + ")";
                }
                player.sendMessage(lines);
                return true;
            } else if (args.size() == 1) {
                if (!player.hasPermission(flag.getRequiredPermission())) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.UnknownFlag);
                    return true;
                }
                String value = args.pop();
                try {
                    claim.setFlag(flag, value);
                } catch (InvalidFlagValueException ex) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.InvalidFlagValue, value);
                    return true;
                }
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.FlagSet, flag.getDisplayName(), claim.getFlag(flag));
                flag.onSet(player, claim, value);
                plugin.getDataStore().saveClaim(claim);
                return true;
            }
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        List<String> results = new ArrayList<String>();
        if (args.size() == 1) {
            for (BaseFlag flag: plugin.getFlagManager().getAllFlags()) {
                if (flag.getKey().toLowerCase().startsWith(args.peek().toLowerCase())) {
                    if (sender.hasPermission(flag.getRequiredPermission())) {
                        results.add(flag.getKey());
                    }
                }
            }
        } else if (args.size() == 2) {
            BaseFlag flag = plugin.getFlagManager().getFlag(args.peek());
            if (flag != null && sender.hasPermission(flag.getRequiredPermission())) {
                for (String vo: flag.getValidOptions()) {
                    if (vo.toLowerCase().startsWith(args.get(1).toLowerCase())) {
                        results.add(vo);
                    }
                }
            }
        }
        return results;
    }
}

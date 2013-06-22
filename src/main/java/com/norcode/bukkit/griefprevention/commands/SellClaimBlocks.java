package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class SellClaimBlocks extends BaseCommand {

    public SellClaimBlocks(GriefPreventionTNG plugin) {
        super(plugin, "sellclaimblocks");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }

        Player player = (Player) sender;

        //if economy is disabled, don't do anything
        if (!plugin.hasEconomy()) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.BuySellNotConfigured);
            return true;
        }

        if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.NoPermissionForCommand);
            return true;
        }

        //if disabled, error message
        if (plugin.configuration.getClaimBlocksSellValue() == 0) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.OnlyPurchaseBlocks);
            return true;
        }

        //load player data
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        int availableBlocks = playerData.getRemainingClaimBlocks();

        //if no amount provided, just tell player value per block sold, and how many he can sell
        if (args.size() != 1) {
            plugin.sendMessage(player, TextMode.INFO, Messages.BlockSaleValue, String.valueOf(plugin.configuration.getClaimBlocksSellValue()), String.valueOf(availableBlocks));
            return false;
        }

        //parse number of blocks
        int blockCount;
        try {
            blockCount = Integer.parseInt(args.peek());
        } catch (NumberFormatException numberFormatException) {
            return false;  //causes usage to be displayed
        }

        if (blockCount <= 0) {
            return false;
        }

        //if he doesn't have enough blocks, tell him so
        if (blockCount > availableBlocks) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.NotEnoughBlocksForSale);
        } else {
            //compute value and deposit it
            double totalValue = blockCount * plugin.configuration.getClaimBlocksSellValue();
            plugin.getEconomy().depositPlayer(player.getName(), totalValue);

            //subtract blocks
            playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks()-blockCount);
            plugin.getDataStore().savePlayerData(player.getName(), playerData);

            //inform player
            plugin.sendMessage(player, TextMode.SUCCESS, Messages.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

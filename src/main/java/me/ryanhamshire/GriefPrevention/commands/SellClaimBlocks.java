package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class SellClaimBlocks extends BaseCommand {

    public SellClaimBlocks(GriefPrevention plugin) {
        super(plugin, "sellclaimblocks");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.Err, Messages.CommandRequiresPlayer);
            return true;
        }

        Player player = (Player) sender;

        //if economy is disabled, don't do anything
        if (GriefPrevention.economy == null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
            return true;
        }

        if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
            return true;
        }

        //if disabled, error message
        if (plugin.config_economy_claimBlocksSellValue == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
            return true;
        }

        //load player data
        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
        int availableBlocks = playerData.getRemainingClaimBlocks();

        //if no amount provided, just tell player value per block sold, and how many he can sell
        if (args.size() != 1) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(plugin.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
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
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
        } else {
            //compute value and deposit it
            double totalValue = blockCount * plugin.config_economy_claimBlocksSellValue;
            plugin.economy.depositPlayer(player.getName(), totalValue);

            //subtract blocks
            playerData.accruedClaimBlocks -= blockCount;
            plugin.dataStore.savePlayerData(player.getName(), playerData);

            //inform player
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

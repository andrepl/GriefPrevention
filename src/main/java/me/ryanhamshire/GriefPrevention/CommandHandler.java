package me.ryanhamshire.GriefPrevention;

import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.tasks.PlayerRescueTask;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 6/8/13
 * Time: 2:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class CommandHandler implements TabExecutor {

    GriefPrevention plugin;

    public CommandHandler(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        Player player = null;
        WorldConfig wc = null;
        if (sender instanceof Player) {
            player = (Player) sender;
            wc = plugin.getWorldCfg(player.getWorld());

        }

        //abandonclaim
        if (cmd.getName().equalsIgnoreCase("gphelp") && player != null) {
            String topic = "index";
            if (args.length > 0) topic = args[0];
            handleHelp(player, topic);


        } else if (cmd.getName().equalsIgnoreCase("abandonclaim") && player != null) {
            return plugin.abandonClaimHandler(player, false);
        } else if (cmd.getName().equalsIgnoreCase("claiminfo") && player != null) {
            //show information about a claim.
            Claim claimatpos = null;
            if (args.length == 0)
                claimatpos = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            else {
                int claimid = Integer.parseInt(args[0]);
                claimatpos = plugin.dataStore.getClaim(claimid);
                if (claimatpos == null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, "Invalid Claim ID:" + claimid);
                    return true;
                }
            }
            if (claimatpos == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, "There is no Claim here!");
                GriefPrevention.sendMessage(player, TextMode.Err, "Make sure you are inside a claim.");

                return true;
            } else {
                //there is a claim, show all sorts of pointless info about it.
                //we do not show trust, since that can be shown with /trustlist.
                //first, get the upper and lower boundary.
                //see that it has Children.
                if (claimatpos.children.size() > 0) {

                }


                String lowerboundary = GriefPrevention.getfriendlyLocationString(claimatpos.getLesserBoundaryCorner());
                String upperboundary = GriefPrevention.getfriendlyLocationString(claimatpos.getGreaterBoundaryCorner());
                String SizeString = "(" + String.valueOf(claimatpos.getWidth()) + "," + String.valueOf(claimatpos.getHeight()) + ")";
                String ClaimOwner = claimatpos.getOwnerName();
                GriefPrevention.sendMessage(player, TextMode.Info, "ID:" + claimatpos.getID());
                GriefPrevention.sendMessage(player, TextMode.Info, "Position:" + lowerboundary + "-" + upperboundary);
                GriefPrevention.sendMessage(player, TextMode.Info, "Size:" + SizeString);
                GriefPrevention.sendMessage(player, TextMode.Info, "Owner:" + ClaimOwner);
                String parentid = claimatpos.parent == null ? "(no parent)" : String.valueOf(claimatpos.parent.getID());
                GriefPrevention.sendMessage(player, TextMode.Info, "Parent ID:" + parentid);
                String childinfo = "";
                //if no subclaims, set childinfo to indicate as such.
                if (claimatpos.children.size() == 0) {
                    childinfo = "No subclaims.";
                } else { //otherwise, we need to get the childclaim info by iterating through each child claim.
                    childinfo = claimatpos.children.size() + " (";

                    for (Claim childclaim : claimatpos.children) {
                        childinfo += String.valueOf(childclaim.getSubClaimID()) + ",";
                    }
                    //remove the last character since it is a comma we do not want.
                    childinfo = childinfo.substring(0, childinfo.length() - 1);

                    //tada
                }
                GriefPrevention.sendMessage(player, TextMode.Info, "Subclaims:" + childinfo);

                return true;
            }


        } else if (cmd.getName().equalsIgnoreCase("cleanclaim") && player != null) {
            //source is first arg; target is second arg.
            player.sendMessage("cleanclaim command..." + args.length);
            if (args.length == 0) {
                return true;
            }
            MaterialInfo source = MaterialInfo.fromString(args[0]);
            if (source == null) {
                Material attemptparse = Material.valueOf(args[0]);
                if (attemptparse != null) {
                    source = new MaterialInfo(attemptparse.getId(), (byte) 0, args[0]);
                } else {
                    player.sendMessage("Failed to parse Source Material," + args[0]);
                    return true;
                }

            }
            MaterialInfo target = new MaterialInfo(Material.AIR.getId(), (byte) 0, "Air");
            if (args.length > 1) {
                target = MaterialInfo.fromString(args[1]);
                if (target == null) {
                    Material attemptparse = Material.valueOf(args[1]);
                    if (attemptparse != null) {
                        target = new MaterialInfo(attemptparse.getId(), (byte) 0, args[1]);
                    } else {
                        player.sendMessage("Failed to parse Target Material," + args[1]);
                    }
                }

            }
            System.out.println(source.typeID + " " + target.typeID);
            PlayerData pd = plugin.dataStore.getPlayerData(player.getName());
            Claim retrieveclaim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (retrieveclaim != null) {
                if (pd.ignoreClaims || retrieveclaim.ownerName.equalsIgnoreCase(player.getName())) {
                    plugin.handleClaimClean(retrieveclaim, source, target, player);
                    return true;
                }
            }


        }
        if (cmd.getName().equalsIgnoreCase("setclaimblocks") && player != null) {


        }
        if (cmd.getName().equalsIgnoreCase("clearmanagers") && player != null) {
            Claim claimatpos = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            PlayerData pdata = plugin.dataStore.getPlayerData(player.getName());
            if (claimatpos != null) {
                if (claimatpos.isAdminClaim()) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotAdmin);
                    return true;
                }
                if (pdata.ignoreClaims || claimatpos.ownerName.equalsIgnoreCase(player.getName())) {
                    for (String currmanager : claimatpos.getManagerList()) {
                        claimatpos.removeManager(currmanager);
                    }
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersSuccess);
                } else {
                    //nope
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotOwned);
                }

            } else {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotFound);
            }
        }
        if (cmd.getName().equalsIgnoreCase("gpreload")) {
            if (player == null || player.hasPermission("griefprevention.reload")) {
                plugin.onDisable();
                plugin.onEnable();
            }
        }
        //abandontoplevelclaim
        if (cmd.getName().equalsIgnoreCase("abandontoplevelclaim") && player != null) {
            return plugin.abandonClaimHandler(player, true);
        }

        //ignoreclaims
        if (cmd.getName().equalsIgnoreCase("ignoreclaims") && player != null) {
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());

            playerData.ignoreClaims = !playerData.ignoreClaims;

            //toggle ignore claims mode on or off
            if (!playerData.ignoreClaims) {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
            } else {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
            }

            return true;
        } else if (cmd.getName().equalsIgnoreCase("giveclaimblocks") && player != null) {
            if (args.length < 2) {
                return false;
            }
            int desiredxfer = 0;
            try {
                desiredxfer = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                return false;
            }
            plugin.transferClaimBlocks(player.getName(), args[0], desiredxfer);
        } else if (cmd.getName().equalsIgnoreCase("transferclaimblocks") && player != null) {
            if (args.length < 3) {
                return false;
            }
            String sourcename = args[0];
            String targetname = args[1];
            int desiredxfer = 0;
            try {
                desiredxfer = Integer.parseInt(args[2]);
            } catch (NumberFormatException exx) {
                return false;
            }
            plugin.transferClaimBlocks(sourcename, targetname, desiredxfer);


        }
        //abandonallclaims
        else if (cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null) {
            if (args.length > 1) return false;
            boolean deletelocked = false;
            if (args.length > 0) {
                deletelocked = Boolean.parseBoolean(args[0]);
            }

            if (!wc.getAllowUnclaim()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
                return true;
            }

            //count claims
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            int originalClaimCount = playerData.claims.size();

            //check count
            if (originalClaimCount == 0) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
                return true;
            }

            //delete them
            plugin.dataStore.deleteClaimsForPlayer(player.getName(), false, deletelocked);

            //inform the player
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            if (deletelocked) {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandonIncludingLocked, String.valueOf(remainingBlocks));
            } else {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandonExcludingLocked, String.valueOf(remainingBlocks));
            }

            //revert any current visualization
            Visualization.Revert(player);

            return true;
        }

        //restore nature
        else if (cmd.getName().equalsIgnoreCase("restorenature") && player != null) {
            //change shovel mode
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            playerData.shovelMode = ShovelMode.RestoreNature;
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
            return true;
        }

        //restore nature aggressive mode
        else if (cmd.getName().equalsIgnoreCase("restorenatureaggressive") && player != null) {
            //change shovel mode
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
            GriefPrevention.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
            return true;
        }

        //restore nature fill mode
        else if (cmd.getName().equalsIgnoreCase("restorenaturefill") && player != null) {
            //change shovel mode
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            playerData.shovelMode = ShovelMode.RestoreNatureFill;

            //set radius based on arguments
            playerData.fillRadius = 2;
            if (args.length > 0) {
                try {
                    playerData.fillRadius = Integer.parseInt(args[0]);
                } catch (Exception exception) {
                }
            }

            if (playerData.fillRadius < 0) playerData.fillRadius = 2;

            GriefPrevention.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
            return true;
        }

        //trust <player>
        else if (cmd.getName().equalsIgnoreCase("trust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //most trust commands use this helper method, it keeps them consistent
            handleTrustCommand(player, ClaimPermission.Build, args[0]);

            return true;
        }

        //lockclaim
        else if (cmd.getName().equalsIgnoreCase("lockclaim") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 0) return false;

            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
            if ((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
                claim.neverdelete = true;
                plugin.dataStore.saveClaim(claim);
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimLocked);
            }

            return true;
        }

        //unlockclaim
        else if (cmd.getName().equalsIgnoreCase("unlockclaim") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 0) return false;

            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
            if ((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
                claim.neverdelete = false;
                plugin.dataStore.saveClaim(claim);
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimUnlocked);
            }

            return true;
        } else if (cmd.getName().equalsIgnoreCase("giveclaim") && player != null) {
            //gives a claim to another player. get the source player first.
            if (args.length == 0) return false;
            Player source = player;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                GriefPrevention.sendMessage(source, TextMode.Err, Messages.PlayerNotFound, args[0]);
                return true;
            }
            //if it's not null, make sure they have either have giveclaim permission or adminclaims permission.

            if (!(source.hasPermission("griefprevention.giveclaims") || source.hasPermission("griefprevention.adminclaims"))) {

                //find the claim at the players location.
                Claim claimtogive = plugin.dataStore.getClaimAt(source.getLocation(), true, null);
                //if the owner is not the source, they have to have adminclaims permission too.
                if (!claimtogive.getOwnerName().equalsIgnoreCase(source.getName())) {
                    //if they don't have adminclaims permission, deny it.
                    if (!source.hasPermission("griefprevention.adminclaims")) {
                        GriefPrevention.sendMessage(source, TextMode.Err, Messages.NoAdminClaimsPermission);
                        return true;
                    }
                }
                //transfer ownership.
                claimtogive.ownerName = target.getName();

                String originalOwner = claimtogive.getOwnerName();
                try {
                    plugin.dataStore.changeClaimOwner(claimtogive, target.getName());
                    //message both players.
                    GriefPrevention.sendMessage(source, TextMode.Success, Messages.GiveSuccessSender, originalOwner, target.getName());
                    if (target != null && target.isOnline()) {
                        GriefPrevention.sendMessage(target, TextMode.Success, Messages.GiveSuccessTarget, originalOwner);
                    }
                } catch (Exception exx) {
                    GriefPrevention.sendMessage(source, TextMode.Err, "Failed to transfer Claim.");
                }


            }


        }
        //transferclaim <player>
        else if (cmd.getName().equalsIgnoreCase("transferclaim") && player != null) {
            //can take two parameters. Source Player and target player.
            if (args.length == 0) return false;
            //one arg requires "GriefPrevention.transferclaims" or "GriefPrevention.adminclaims" permission.
            //two args requires the latter.
            if (args.length > 0)
                //check additional permission
                if (!player.hasPermission("griefprevention.adminclaims")) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
                    return true;
                }

            //which claim is the user in?
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
                return true;
            }

            OfflinePlayer targetPlayer = plugin.resolvePlayer(args[0]);
            if (targetPlayer == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                return true;
            }

            //change ownership
            try {
                plugin.dataStore.changeClaimOwner(claim, targetPlayer.getName());
            } catch (Exception e) {
                GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
                return true;
            }

            //confirm
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
            GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + targetPlayer.getName() + ".");

            return true;
        }

        //trustlist
        else if (cmd.getName().equalsIgnoreCase("trustlist") && player != null) {
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);

            //if no claim here, error message
            if (claim == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
                return true;
            }

            //if no permission to manage permissions, error message
            String errorMessage = claim.allowGrantPermission(player);
            if (errorMessage != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, errorMessage);
                return true;
            }

            //otherwise build a list of explicit permissions by permission level
            //and send that to the player
            ArrayList<String> builders = new ArrayList<String>();
            ArrayList<String> containers = new ArrayList<String>();
            ArrayList<String> accessors = new ArrayList<String>();
            ArrayList<String> managers = new ArrayList<String>();
            claim.getPermissions(builders, containers, accessors, managers);

            player.sendMessage("Explicit permissions here:");

            StringBuilder permissions = new StringBuilder();
            permissions.append(ChatColor.GOLD + "M: ");

            if (managers.size() > 0) {
                for (int i = 0; i < managers.size(); i++)
                    permissions.append(managers.get(i) + " ");
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.YELLOW + "B: ");

            if (builders.size() > 0) {
                for (int i = 0; i < builders.size(); i++)
                    permissions.append(builders.get(i) + " ");
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.GREEN + "C: ");

            if (containers.size() > 0) {
                for (int i = 0; i < containers.size(); i++)
                    permissions.append(containers.get(i) + " ");
            }

            player.sendMessage(permissions.toString());
            permissions = new StringBuilder();
            permissions.append(ChatColor.BLUE + "A :");

            if (accessors.size() > 0) {
                for (int i = 0; i < accessors.size(); i++)
                    permissions.append(accessors.get(i) + " ");
            }

            player.sendMessage(permissions.toString());

            player.sendMessage("(M-anager, B-builder, C-ontainers, A-ccess)");

            return true;
        }

        //untrust <player> or untrust [<group>]
        else if (cmd.getName().equalsIgnoreCase("untrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            //determine which claim the player is standing in
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);

            //bracket any permissions
            if (args[0].contains(".")) {
                args[0] = "[" + args[0] + "]";
            }

            //determine whether a single player or clearing permissions entirely
            boolean clearPermissions = false;
            OfflinePlayer otherPlayer = null;
            System.out.println("clearing perms for name:" + args[0]);
            if (args[0].equals("all")) {
                if (claim == null || claim.allowEdit(player) == null) {
                    clearPermissions = true;
                } else {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
                    return true;
                }
            } else if ((!args[0].startsWith("[") || !args[0].endsWith("]"))
                    && !args[0].toUpperCase().startsWith("G:") && !args[0].startsWith("!")) {
                otherPlayer = plugin.resolvePlayer(args[0]);
                if (!clearPermissions && otherPlayer == null && !args[0].equals("public")) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                    return true;
                }

                //correct to proper casing
                if (otherPlayer != null)
                    args[0] = otherPlayer.getName();
            } else if (args[0].startsWith("G:")) {
                //make sure the group exists, otherwise show the message.
                String groupname = args[0].substring(2);
                if (!plugin.config_player_groups.GroupExists(groupname)) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.GroupNotFound);
                    return true;
                }
            }


            //if no claim here, apply changes to all his claims
            if (claim == null) {
                PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
                for (int i = 0; i < playerData.claims.size(); i++) {
                    claim = playerData.claims.get(i);

                    //if untrusting "all" drop all permissions
                    if (clearPermissions) {
                        claim.clearPermissions();
                    }

                    //otherwise drop individual permissions
                    else {
                        claim.dropPermission(args[0]);
                        claim.removeManager(args[0]);
                        //claim.managers.remove(args[0]);
                    }

                    //save changes
                    plugin.dataStore.saveClaim(claim);
                }

                //beautify for output
                if (args[0].equals("public")) {
                    args[0] = "the public";
                }

                //confirmation message
                if (!clearPermissions) {
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, args[0]);
                } else {
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
                }
            }

            //otherwise, apply changes to only this claim
            else if (claim.allowGrantPermission(player) != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
            } else {
                //if clearing all
                if (clearPermissions) {
                    claim.clearPermissions();
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
                }

                //otherwise individual permission drop
                else {
                    claim.dropPermission(args[0]);
                    if (claim.allowEdit(player) == null) {
                        claim.removeManager(args[0]);

                        //beautify for output
                        if (args[0].equals("public")) {
                            args[0] = "the public";
                        }

                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, args[0]);
                    } else {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustOwnerOnly, claim.getOwnerName());
                    }
                }

                //save changes
                plugin.dataStore.saveClaim(claim);
            }

            return true;
        }

        //accesstrust <player>
        else if (cmd.getName().equalsIgnoreCase("accesstrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, ClaimPermission.Access, args[0]);

            return true;
        }

        //containertrust <player>
        else if (cmd.getName().equalsIgnoreCase("containertrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, ClaimPermission.Inventory, args[0]);

            return true;
        }

        //permissiontrust <player>
        else if (cmd.getName().equalsIgnoreCase("permissiontrust") && player != null) {
            //requires exactly one parameter, the other player's name
            if (args.length != 1) return false;

            handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method

            return true;
        }

        //buyclaimblocks
        else if (cmd.getName().equalsIgnoreCase("buyclaimblocks") && player != null) {
            //if economy is disabled, don't do anything
            if (GriefPrevention.economy == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
                return true;
            }

            if (!player.hasPermission("griefprevention.buysellclaimblocks")) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
                return true;
            }

            //if purchase disabled, send error message
            if (plugin.config_economy_claimBlocksPurchaseCost == 0) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
                return true;
            }

            //if no parameter, just tell player cost per block and balance
            if (args.length != 1) {
                GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost, String.valueOf(plugin.config_economy_claimBlocksPurchaseCost), String.valueOf(GriefPrevention.economy.getBalance(player.getName())));
                return false;
            } else {
                //determine max purchasable blocks
                PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
                int maxPurchasable = plugin.config_claims_maxAccruedBlocks - playerData.accruedClaimBlocks;

                //if the player is at his max, tell him so
                if (maxPurchasable <= 0) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimBlockLimit);
                    return true;
                }

                //try to parse number of blocks
                int blockCount;
                try {
                    blockCount = Integer.parseInt(args[0]);
                } catch (NumberFormatException numberFormatException) {
                    return false;  //causes usage to be displayed
                }

                if (blockCount <= 0) {
                    return false;
                }

                //correct block count to max allowed
                if (blockCount > maxPurchasable) {
                    blockCount = maxPurchasable;
                }

                //if the player can't afford his purchase, send error message
                double balance = plugin.economy.getBalance(player.getName());
                double totalCost = blockCount * plugin.config_economy_claimBlocksPurchaseCost;
                if (totalCost > balance) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost), String.valueOf(balance));
                }

                //otherwise carry out transaction
                else {
                    //withdraw cost
                    plugin.economy.withdrawPlayer(player.getName(), totalCost);

                    //add blocks
                    playerData.accruedClaimBlocks += blockCount;
                    plugin.dataStore.savePlayerData(player.getName(), playerData);

                    //inform player
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
                }

                return true;
            }
        }

        //sellclaimblocks <amount>
        else if (cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null) {
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
            if (args.length != 1) {
                GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(plugin.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
                return false;
            }

            //parse number of blocks
            int blockCount;
            try {
                blockCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException numberFormatException) {
                return false;  //causes usage to be displayed
            }

            if (blockCount <= 0) {
                return false;
            }

            //if he doesn't have enough blocks, tell him so
            if (blockCount > availableBlocks) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
            }

            //otherwise carry out the transaction
            else {
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

        //adminclaims
        else if (cmd.getName().equalsIgnoreCase("adminclaims") && player != null) {
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            playerData.shovelMode = ShovelMode.Admin;
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);

            return true;
        }

        //basicclaims
        else if (cmd.getName().equalsIgnoreCase("basicclaims") && player != null) {
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            playerData.shovelMode = ShovelMode.Basic;
            playerData.claimSubdividing = null;
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);

            return true;
        }

        //subdivideclaims
        else if (cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null) {
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            playerData.shovelMode = ShovelMode.Subdivide;
            playerData.claimSubdividing = null;
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionDemo);

            return true;
        }

        //deleteclaim
        else if (cmd.getName().equalsIgnoreCase("deleteclaim") && player != null) {
            //determine which claim the player is standing in
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);

            if (claim == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
            } else {
                //deleting an admin claim additionally requires the adminclaims permission
                if (!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims")) {
                    PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
                    if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                        GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
                        playerData.warnedAboutMajorDeletion = true;
                    } else if (claim.neverdelete && !playerData.warnedAboutMajorDeletion) {
                        GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeleteLockedClaimWarning);
                        playerData.warnedAboutMajorDeletion = true;
                    } else {
                        claim.removeSurfaceFluids(null);
                        plugin.dataStore.deleteClaim(claim);

                        //if in a creative mode world, /restorenature the claim
                        if (wc.getAutoRestoreUnclaimed() && plugin.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                            plugin.restoreClaim(claim, 0);
                        }

                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
                        GriefPrevention.AddLogEntry(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));

                        //revert any current visualization
                        Visualization.Revert(player);

                        playerData.warnedAboutMajorDeletion = false;
                    }
                } else {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
                }
            }

            return true;
        } else if (cmd.getName().equalsIgnoreCase("claimexplosions") && player != null) {
            //determine which claim the player is standing in
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);

            if (claim == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
            } else {
                String noBuildReason = claim.allowBuild(player);
                if (noBuildReason != null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
                    return true;
                }

                if (claim.areExplosivesAllowed) {
                    claim.areExplosivesAllowed = false;
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
                } else {
                    claim.areExplosivesAllowed = true;
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
                }
            }

            return true;
        }

        //deleteallclaims <player>
        else if (cmd.getName().equalsIgnoreCase("deleteallclaims")) {
            //requires one or two parameters, the other player's name and whether to delete locked claims.
            if (args.length < 1 && args.length > 2) return false;

            //try to find that player
            OfflinePlayer otherPlayer = plugin.resolvePlayer(args[0]);
            if (otherPlayer == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                return true;
            }

            boolean deletelocked = false;
            if (args.length == 2) {
                deletelocked = Boolean.parseBoolean(args[1]);
            }

            //delete all that player's claims
            plugin.dataStore.deleteClaimsForPlayer(otherPlayer.getName(), true, deletelocked);

            if (deletelocked) {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccessIncludingLocked, otherPlayer.getName());
            } else {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccessExcludingLocked, otherPlayer.getName());
            }
            if (player != null) {
                GriefPrevention.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");

                //revert any current visualization
                Visualization.Revert(player);
            }

            return true;
        }

        //claimslist or claimslist <player>
        else if (cmd.getName().equalsIgnoreCase("claimslist")) {
            //at most one parameter
            if (args.length > 1) return false;

            //player whose claims will be listed
            OfflinePlayer otherPlayer;

            //if another player isn't specified, assume current player
            if (args.length < 1) {
                if (player != null)
                    otherPlayer = player;
                else
                    return false;
            }

            //otherwise if no permission to delve into another player's claims data
            else if (player != null && !player.hasPermission("griefprevention.deleteclaims")) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsListNoPermission);
                return true;
            }

            //otherwise try to find the specified player
            else {
                otherPlayer = plugin.resolvePlayer(args[0]);
                if (otherPlayer == null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                    return true;
                }
            }

            //load the target player's data
            PlayerData playerData = plugin.dataStore.getPlayerData(otherPlayer.getName());
            GriefPrevention.sendMessage(player, TextMode.Instr, " " + playerData.accruedClaimBlocks + "(+" + (playerData.bonusClaimBlocks + plugin.dataStore.getGroupBonusBlocks(otherPlayer.getName())) + ")=" + (playerData.accruedClaimBlocks + playerData.bonusClaimBlocks + plugin.dataStore.getGroupBonusBlocks(otherPlayer.getName())));
            for (int i = 0; i < playerData.claims.size(); i++) {
                Claim claim = playerData.claims.get(i);
                GriefPrevention.sendMessage(player, TextMode.Instr, "  (-" + claim.getArea() + ") " + plugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
            }

            if (playerData.claims.size() > 0)
                GriefPrevention.sendMessage(player, TextMode.Instr, "   =" + playerData.getRemainingClaimBlocks());

            //drop the data we just loaded, if the player isn't online
            if (!otherPlayer.isOnline())
                plugin.dataStore.clearCachedPlayerData(otherPlayer.getName());

            return true;
        }

        //deathblow <player> [recipientPlayer]
        else if (cmd.getName().equalsIgnoreCase("deathblow")) {
            //requires at least one parameter, the target player's name
            if (args.length < 1) return false;

            //try to find that player
            Player targetPlayer = plugin.getServer().getPlayer(args[0]);
            if (targetPlayer == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                return true;
            }

            //try to find the recipient player, if specified
            Player recipientPlayer = null;
            if (args.length > 1) {
                recipientPlayer = plugin.getServer().getPlayer(args[1]);
                if (recipientPlayer == null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                    return true;
                }
            }

            //if giving inventory to another player, teleport the target player to that receiving player
            if (recipientPlayer != null) {
                targetPlayer.teleport(recipientPlayer);
            }

            //otherwise, plan to "pop" the player in place
            else {
                //if in a normal world, shoot him up to the sky first, so his items will fall on the surface.
                if (targetPlayer.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    Location location = targetPlayer.getLocation();
                    location.setY(location.getWorld().getMaxHeight());
                    targetPlayer.teleport(location);
                }
            }

            //kill target player
            targetPlayer.setHealth(0);

            //log entry
            if (player != null) {
                GriefPrevention.AddLogEntry(player.getName() + " used /DeathBlow to kill " + targetPlayer.getName() + ".");
            } else {
                GriefPrevention.AddLogEntry("Killed " + targetPlayer.getName() + ".");
            }

            return true;
        }

        //deletealladminclaims
        else if (cmd.getName().equalsIgnoreCase("deletealladminclaims")) {
            if (!player.hasPermission("griefprevention.deleteclaims")) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoDeletePermission);
                return true;
            }

            //delete all admin claims
            plugin.dataStore.deleteClaimsForPlayer("", true, true);  //empty string for owner name indicates an administrative claim

            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
            if (player != null) {
                GriefPrevention.AddLogEntry(player.getName() + " deleted all administrative claims.");

                //revert any current visualization
                Visualization.Revert(player);
            }

            return true;
        }

        //adjustbonusclaimblocks <player> <amount> or [<permission>] amount
        else if (cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks")) {
            //requires exactly two parameters, the other player or group's name and the adjustment
            if (args.length != 2) return false;

            //parse the adjustment amount
            int adjustment;
            try {
                adjustment = Integer.parseInt(args[1]);
            } catch (NumberFormatException numberFormatException) {
                return false;  //causes usage to be displayed
            }

            //if granting blocks to all players with a specific permission
            if (args[0].startsWith("[") && args[0].endsWith("]")) {
                String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
                int newTotal = plugin.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);

                if (player != null)
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
                if (player != null)
                    GriefPrevention.AddLogEntry(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");

                return true;
            }

            //otherwise, find the specified player
            OfflinePlayer targetPlayer = plugin.resolvePlayer(args[0]);
            if (targetPlayer == null) {
                if (player != null) GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                return true;
            }

            //give blocks to player
            PlayerData playerData = plugin.dataStore.getPlayerData(targetPlayer.getName());
            playerData.bonusClaimBlocks += adjustment;
            plugin.dataStore.savePlayerData(targetPlayer.getName(), playerData);

            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.bonusClaimBlocks));
            if (player != null)
                GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");

            return true;
        }

        //trapped
        else if (cmd.getName().equalsIgnoreCase("trapped") && player != null) {
            //FEATURE: empower players who get "stuck" in an area where they don't have permission to build to save themselves

            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

            //if another /trapped is pending, ignore this slash command
            if (playerData.pendingTrapped) {
                return true;
            }

            //if the player isn't in a claim or has permission to build, tell him to man up
            if (claim == null || claim.allowBuild(player) == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);
                return true;
            }

            //if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
                return true;
            }

            //if the player is in an administrative claim, he should contact an admin
            if (claim.isAdminClaim()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
                return true;
            }

            //check cooldown
            long lastTrappedUsage = playerData.lastTrappedUsage.getTime();
            long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * 60 * wc.getClaimsTrappedCooldownHours();
            long now = Calendar.getInstance().getTimeInMillis();
            if (now < nextTrappedUsage) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedOnCooldown, String.valueOf(wc.getClaimsTrappedCooldownHours()), String.valueOf((nextTrappedUsage - now) / (1000 * 60) + 1));
                return true;
            }

            //send instructions
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);

            //create a task to rescue this player in a little while
            PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 200L);  //20L ~ 1 second

            return true;
        }

        //siege
        else if (cmd.getName().equalsIgnoreCase("siege") && player != null) {
            //error message for when siege mode is disabled
            if (!plugin.siegeEnabledForWorld(player.getWorld())) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NonSiegeWorld);
                return true;
            }

            //requires one argument
            if (args.length > 1) {
                return false;
            }

            //can't start a siege when you're already involved in one
            Player attacker = player;
            PlayerData attackerData = plugin.dataStore.getPlayerData(attacker.getName());
            if (attackerData.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadySieging);
                return true;
            }

            //can't start a siege when you're protected from pvp combat
            if (attackerData.pvpImmune) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantFightWhileImmune);
                return true;
            }

            //if a player name was specified, use that
            Player defender = null;
            if (args.length >= 1) {
                defender = plugin.getServer().getPlayer(args[0]);
                if (defender == null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                    return true;
                }
            }

            //otherwise use the last player this player was in pvp combat with
            else if (attackerData.lastPvpPlayer.length() > 0) {
                defender = plugin.getServer().getPlayer(attackerData.lastPvpPlayer);
                if (defender == null) {
                    return false;
                }
            } else {
                return false;
            }

            //victim must not be under siege already
            PlayerData defenderData = plugin.dataStore.getPlayerData(defender.getName());
            if (defenderData.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegePlayer);
                return true;
            }

            //victim must not be pvp immune
            if (defenderData.pvpImmune) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeDefenseless);
                return true;
            }

            Claim defenderClaim = plugin.dataStore.getClaimAt(defender.getLocation(), false, null);

            //defender must have some level of permission there to be protected
            if (defenderClaim == null || defenderClaim.allowAccess(defender) != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotSiegableThere);
                return true;
            }

            //attacker must be close to the claim he wants to siege
            if (!defenderClaim.isNear(attacker.getLocation(), 25)) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeTooFarAway);
                return true;
            }

            //claim can't be under siege already
            if (defenderClaim.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegeArea);
                return true;
            }

            //can't siege admin claims
            if (defenderClaim.isAdminClaim()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeAdminClaim);
                return true;
            }

            //can't be on cooldown
            if (plugin.dataStore.onCooldown(attacker, defender, defenderClaim)) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeOnCooldown);
                return true;
            }

            //start the siege
            if (plugin.dataStore.startSiege(attacker, defender, defenderClaim)) {

                //confirmation message for attacker, warning message for defender
                GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());
            }
        }

        return false;
    }

    private static final String[] HelpIndex = new String[]{
            ChatColor.AQUA + "-----=GriefPrevention Help Index=------",
            "use /gphelp [topic] to view each topic.",
            ChatColor.YELLOW + "Topics: Claims,Trust"};


    private static final String[] ClaimsHelp = new String[]{
            ChatColor.AQUA + "-----=GriefPrevention Claims=------",
            ChatColor.YELLOW + " GriefPrevention uses Claims to allow you to claim areas and prevent ",
            ChatColor.YELLOW + "other players from messing with your stuff without your permission.",
            ChatColor.YELLOW + "Claims are created by either placing your first Chest or by using the",
            ChatColor.YELLOW + "Claim creation tool, which is by default a Golden Shovel.",
            ChatColor.YELLOW + "You can resize your claims by using a Golden Shovel on a corner, or",
            ChatColor.YELLOW + "by defining a new claim that encompasses it. The original claim",
            ChatColor.YELLOW + "Will be resized. you can use trust commands to give other players abilities",
            ChatColor.YELLOW + "In your claim. See /gphelp trust for more information"};

    private static final String[] TrustHelp = new String[]{
            ChatColor.AQUA + "------=GriefPrevention Trust=------",
            ChatColor.YELLOW + "You can control who can do things in your claims by using the trust commands",
            ChatColor.YELLOW + "/accesstrust can be used to allow a player to interact with items in your claim",
            ChatColor.YELLOW + "/containertrust can be used to allow a player to interact with your chests.",
            ChatColor.YELLOW + "/trust allows players to build on your claim.",
            ChatColor.YELLOW + "Each trust builds upon the previous one in this list; containertrust includes accesstrust",
            ChatColor.YELLOW + "and build trust includes container trust and access trust."};


    private void handleHelp(Player p, String Topic) {
        if (p == null) return;
        String[] uselines;
        if (Topic.equalsIgnoreCase("claims"))
            uselines = ClaimsHelp;
        else if (Topic.equalsIgnoreCase("trust"))
            uselines = TrustHelp;
        else
            uselines = HelpIndex;


        for (String iterate : uselines) {
            p.sendMessage(uselines);
        }


    }


    //helper method keeps the trust commands consistent and eliminates duplicate code
    void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) {
        //determine which claim the player is standing in
        Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);

        //validate player or group argument
        String permission = null;
        OfflinePlayer otherPlayer = null;
        boolean isforceddenial = false;
        //if it starts with "!", remove it and set the forced denial value.
        //we use this flag to indicate to add in a "!" again when we set the perm.
        //This will have the effect of causing the logic to explicitly deny permissions for players that do not match.
        if (recipientName.startsWith("!")) {
            isforceddenial = true;
            recipientName = recipientName.substring(1); //remove the exclamation for the rest of the parsing.
        }

        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
                return;
            }
        } else if (recipientName.contains(".")) {
            permission = recipientName;
        } else {
            otherPlayer = plugin.resolvePlayer(recipientName);
            //addition: if it starts with G:, it indicates a group name, rather than a player name.

            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") &&
                    !recipientName.toUpperCase().startsWith("G:"))

            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
                return;
            } else if (recipientName.toUpperCase().startsWith("G:")) {
                //keep it as is.
                //we will give trust to that group, that is...

            } else if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
            } else {
                recipientName = "public";
            }
        }

        //determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<Claim>();
        if (claim == null) {
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            for (int i = 0; i < playerData.claims.size(); i++) {
                targetClaims.add(playerData.claims.get(i));
            }
        } else {
            //check permission here
            if (claim.allowGrantPermission(player) != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
                return;
            }

            //see if the player has the level of permission he's trying to grant
            String errorMessage = null;

            //permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.allowEdit(player);
                if (errorMessage != null) {
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            //otherwise just use the ClaimPermission enum values
            else {
                switch (permissionLevel) {
                    case Access:
                        errorMessage = claim.allowAccess(player);
                        break;
                    case Inventory:
                        errorMessage = claim.allowContainers(player);
                        break;
                    default:
                        errorMessage = claim.allowBuild(player);
                }
            }

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
                return;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
            return;
        }
        //if forcedenial is true, we will add the exclamation back to the name for addition.
        if (isforceddenial) recipientName = "!" + recipientName;
        //apply changes
        for (int i = 0; i < targetClaims.size(); i++) {
            Claim currentClaim = targetClaims.get(i);
            if (permissionLevel == null) {
                if (!currentClaim.isManager(recipientName)) {
                    currentClaim.addManager(recipientName);
                }
            } else {
                currentClaim.setPermission(recipientName, permissionLevel);
            }
            plugin.dataStore.saveClaim(currentClaim);
        }

        //notify player
        if (recipientName.equals("public")) recipientName = plugin.dataStore.getMessage(Messages.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = plugin.dataStore.getMessage(Messages.PermissionsPermission);
        } else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = plugin.dataStore.getMessage(Messages.BuildPermission);
        } else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = plugin.dataStore.getMessage(Messages.AccessPermission);
        } else //ClaimPermission.Inventory
        {
            permissionDescription = plugin.dataStore.getMessage(Messages.ContainersPermission);
        }

        String location;
        if (claim == null) {

            location = plugin.dataStore.getMessage(Messages.LocationAllClaims);
        } else {
            location = plugin.dataStore.getMessage(Messages.LocationCurrentClaim);
        }
        String userecipientName = recipientName;
        if (userecipientName.toUpperCase().startsWith("G:")) {
            userecipientName = "Group " + userecipientName.substring(2);
        }
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

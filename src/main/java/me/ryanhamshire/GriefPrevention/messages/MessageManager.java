package me.ryanhamshire.GriefPrevention.messages;


import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.DataStore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MessageManager {

    private String[] messages;
    private GriefPrevention plugin;

    public MessageManager(GriefPrevention plugin) {
        this.plugin = plugin;
        this.loadMessages();
    }
    private void loadMessages() {
        Messages[] messageIDs = Messages.values();
        this.messages = new String[Messages.values().length];

        HashMap<String, CustomizableMessage> defaults = new HashMap<String, CustomizableMessage>();

        // initialize defaults
        this.addDefault(defaults, Messages.RespectingClaims, "Now respecting claims.", null);
        this.addDefault(defaults, Messages.IgnoringClaims, "Now ignoring claims.", null);
        this.addDefault(defaults, Messages.NoCreativeUnClaim, "You can't unclaim this land.  You can only make this claim larger or create additional claims.", null);
        this.addDefault(defaults, Messages.SuccessfulAbandonExcludingLocked, "All claims abandoned except for locked claims.  You now have {0} available claim blocks.", "0: remaining blocks");
        this.addDefault(defaults, Messages.SuccessfulAbandonIncludingLocked, "All claims abandoned including locked claims.  You now have {0} available claim blocks.", "0: remaining blocks");
        this.addDefault(defaults, Messages.RestoreNatureActivate, "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.", null);
        this.addDefault(defaults, Messages.RestoreNatureAggressiveActivate, "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and use /BasicClaims to stop.", null);
        this.addDefault(defaults, Messages.FillModeActive, "Fill mode activated with radius {0}.  Right click an area to fill.", "0: fill radius");
        this.addDefault(defaults, Messages.TransferClaimPermission, "That command requires the administrative claims permission.", null);
        this.addDefault(defaults, Messages.TransferClaimMissing, "There's no claim here.  Stand in the administrative claim you want to transfer.", null);
        this.addDefault(defaults, Messages.TransferClaimAdminOnly, "Only administrative claims may be transferred to a player.", null);
        this.addDefault(defaults, Messages.PlayerNotFound, "Player not found.", null);
        this.addDefault(defaults, Messages.TransferTopLevel, "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.", null);
        this.addDefault(defaults, Messages.TransferSuccess, "Claim transferred.", null);
        this.addDefault(defaults, Messages.TrustListNoClaim, "Stand inside the claim you're curious about.", null);
        this.addDefault(defaults, Messages.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.", null);
        this.addDefault(defaults, Messages.UntrustIndividualAllClaims, "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it.", "0: untrusted player");
        this.addDefault(defaults, Messages.UntrustEveryoneAllClaims, "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.", null);
        this.addDefault(defaults, Messages.NoPermissionTrust, "You don't have {0}'s permission to manage permissions here.", "0: claim owner's name");
        this.addDefault(defaults, Messages.ClearPermissionsOneClaim, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.", null);
        this.addDefault(defaults, Messages.UntrustIndividualSingleClaim, "Revoked {0}'s access to this claim.  To set permissions for a ALL your claims, stand outside them.", "0: untrusted player");
        this.addDefault(defaults, Messages.OnlySellBlocks, "Claim blocks may only be sold, not purchased.", null);
        this.addDefault(defaults, Messages.BlockPurchaseCost, "Each claim block costs {0}.  Your balance is {1}.", "0: cost of one block; 1: player's account balance");
        this.addDefault(defaults, Messages.ClaimBlockLimit, "You've reached your claim block limit.  You can't purchase more.", null);
        this.addDefault(defaults, Messages.InsufficientFunds, "You don't have enough money.  You need {0}, but you only have {1}.", "0: total cost; 1: player's account balance");
        this.addDefault(defaults, Messages.PurchaseConfirmation, "Withdrew {0} from your account.  You now have {1} available claim blocks.", "0: total cost; 1: remaining blocks");
        this.addDefault(defaults, Messages.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.", null);
        this.addDefault(defaults, Messages.BlockSaleValue, "Each claim block is worth {0}.  You have {1} available for sale.", "0: block value; 1: available blocks");
        this.addDefault(defaults, Messages.NotEnoughBlocksForSale, "You don't have that many claim blocks available for sale.", null);
        this.addDefault(defaults, Messages.BlockSaleConfirmation, "Deposited {0} in your account.  You now have {1} available claim blocks.", "0: amount deposited; 1: remaining blocks");
        this.addDefault(defaults, Messages.AdminClaimsMode, "Administrative claims mode active.  Any claims created will be free and editable by other administrators.", null);
        this.addDefault(defaults, Messages.BasicClaimsMode, "Returned to basic claim creation mode.", null);
        this.addDefault(defaults, Messages.SubdivisionMode, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.", null);
        this.addDefault(defaults, Messages.SubdivisionDemo, "Land Claim Help:  http://tinyurl.com/7urdtue", null);
        this.addDefault(defaults, Messages.ClaimMissing, "There's no claim here.", null);
        this.addDefault(defaults, Messages.DeletionSubdivisionWarning, "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.", null);
        this.addDefault(defaults, Messages.DeleteLockedClaimWarning, "This claim is locked.  If you're sure you want to delete it, use /DeleteClaim again.", null);
        this.addDefault(defaults, Messages.DeleteSuccess, "Claim deleted.", null);
        this.addDefault(defaults, Messages.CantDeleteAdminClaim, "You don't have permission to delete administrative claims.", null);
        this.addDefault(defaults, Messages.DeleteAllSuccessExcludingLocked, "Deleted all of {0}'s claims excluding locked claims.", "0: owner's name");
        this.addDefault(defaults, Messages.DeleteAllSuccessIncludingLocked, "Deleted all of {0}'s claims including locked claims.", "0: owner's name");
        this.addDefault(defaults, Messages.NoDeletePermission, "You don't have permission to delete claims.", null);
        this.addDefault(defaults, Messages.AllAdminDeleted, "Deleted all administrative claims.", null);
        this.addDefault(defaults, Messages.AdjustBlocksSuccess, "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.", "0: player; 1: adjustment; 2: new total");
        this.addDefault(defaults, Messages.NotTrappedHere, "You can build here.  Save yourself.", null);
        this.addDefault(defaults, Messages.TrappedOnCooldown, "You used /trapped within the last {0} hours.  You have to wait about {1} more minutes before using it again.", "0: default cooldown hours; 1: remaining minutes");
        this.addDefault(defaults, Messages.RescuePending, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.", null);
        this.addDefault(defaults, Messages.NotSiegableThere, "{0} isn't protected there.", "0: defending player");
        this.addDefault(defaults, Messages.AbandonClaimMissing, "Stand in the claim you want to delete, or consider /AbandonAllClaims.", null);
        this.addDefault(defaults, Messages.NotYourClaim, "This isn't your claim.", null);
        this.addDefault(defaults, Messages.DeleteTopLevelClaim, "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.", null);
        this.addDefault(defaults, Messages.AbandonSuccess, "Claim abandoned.  You now have {0} available claim blocks.", "0: remaining claim blocks");
        this.addDefault(defaults, Messages.CantGrantThatPermission, "You can't grant a permission you don't have yourself.", null);
        this.addDefault(defaults, Messages.GrantPermissionNoClaim, "Stand inside the claim where you want to grant permission.", null);
        this.addDefault(defaults, Messages.GrantPermissionConfirmation, "Granted {0} permission to {1} {2}.", "0: target player; 1: permission description; 2: scope (changed claims)");
        this.addDefault(defaults, Messages.ManageUniversalPermissionsInstruction, "To manage permissions for ALL your claims, stand outside them.", null);
        this.addDefault(defaults, Messages.ManageOneClaimPermissionsInstruction, "To manage permissions for a specific claim, stand inside it.", null);
        this.addDefault(defaults, Messages.CollectivePublic, "the public", "as in 'granted the public permission to...'");
        this.addDefault(defaults, Messages.BuildPermission, "build", null);
        this.addDefault(defaults, Messages.ContainersPermission, "access containers and animals", null);
        this.addDefault(defaults, Messages.AccessPermission, "use buttons and levers", null);
        this.addDefault(defaults, Messages.PermissionsPermission, "manage permissions", null);
        this.addDefault(defaults, Messages.LocationCurrentClaim, "in this claim", null);
        this.addDefault(defaults, Messages.LocationAllClaims, "in all your claims", null);
        this.addDefault(defaults, Messages.PvPImmunityStart, "You're protected from attack by other players as long as your inventory is empty.", null);
        this.addDefault(defaults, Messages.DonateItemsInstruction, "To give away the item(s) in your hand, left-click the chest again.", null);
        this.addDefault(defaults, Messages.ChestFull, "This chest is full.", null);
        this.addDefault(defaults, Messages.DonationSuccess, "Item(s) transferred to chest!", null);
        this.addDefault(defaults, Messages.PlayerTooCloseForFire, "You can't start a fire this close to {0}.", "0: other player's name");
        this.addDefault(defaults, Messages.TooDeepToClaim, "This chest can't be protected because it's too deep underground.  Consider moving it.", null);
        this.addDefault(defaults, Messages.ChestClaimConfirmation, "This chest is protected.", null);
        this.addDefault(defaults, Messages.AutomaticClaimNotification, "This chest and nearby blocks are protected from breakage and theft.  The temporary gold and glowstone blocks mark the protected area.  To toggle them on and off, right-click with a stick.", null);
        this.addDefault(defaults, Messages.TrustCommandAdvertisement, "Use the /trust command to grant other players access.", null);
        this.addDefault(defaults, Messages.GoldenShovelAdvertisement, "To claim more land, you need a golden shovel.  When you equip one, you'll get more information.", null);
        this.addDefault(defaults, Messages.UnprotectedChestWarning, "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.", null);
        this.addDefault(defaults, Messages.ThatPlayerPvPImmune, "You can't injure defenseless players.", null);
        this.addDefault(defaults, Messages.CantFightWhileImmune, "You can't fight someone while you're protected from PvP.", null);
        this.addDefault(defaults, Messages.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
        this.addDefault(defaults, Messages.ShovelBasicClaimMode, "Shovel returned to basic claims mode.", null);
        this.addDefault(defaults, Messages.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
        this.addDefault(defaults, Messages.CreativeBasicsDemoAdvertisement, "Land Claim Help:  http://tinyurl.com/c7bajb8", null);
        this.addDefault(defaults, Messages.SurvivalBasicsDemoAdvertisement, "Land Claim Help:  http://tinyurl.com/6nkwegj", null);
        this.addDefault(defaults, Messages.TrappedChatKeyword, "trapped", "When mentioned in chat, players get information about the /trapped command.");
        this.addDefault(defaults, Messages.TrappedInstructions, "Are you trapped in someone's land claim?  Try the /trapped command.", null);
        this.addDefault(defaults, Messages.PvPNoDrop, "You can't drop items while in PvP combat.", null);
        this.addDefault(defaults, Messages.PvPNoContainers, "You can't access containers during PvP combat.", null);
        this.addDefault(defaults, Messages.PvPImmunityEnd, "Now you can fight with other players.", null);
        this.addDefault(defaults, Messages.NoBedPermission, "{0} hasn't given you permission to sleep here.", "0: claim owner");
        this.addDefault(defaults, Messages.NoWildernessBuckets, "You may only dump buckets inside your claim(s) or underground.", null);
        this.addDefault(defaults, Messages.NoLavaNearOtherPlayer, "You can't place lava this close to {0}.", "0: nearby player");
        this.addDefault(defaults, Messages.TooFarAway, "That's too far away.", null);
        this.addDefault(defaults, Messages.BlockNotClaimed, "No one has claimed this block.", null);
        this.addDefault(defaults, Messages.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
        this.addDefault(defaults, Messages.RestoreNaturePlayerInChunk, "Unable to restore.  {0} is in that chunk.", "0: nearby player");
        this.addDefault(defaults, Messages.NoCreateClaimPermission, "You don't have permission to claim land.", null);
        this.addDefault(defaults, Messages.ResizeClaimTooSmall, "This new size would be too small.  Claims must be at least {0} x {0}.", "0: minimum claim size");
        this.addDefault(defaults, Messages.ResizeNeedMoreBlocks, "You don't have enough blocks for this size.  You need {0} more.", "0: how many needed");
        this.addDefault(defaults, Messages.ClaimResizeSuccess, "Claim resized.  You now have {0} available claim blocks.", "0: remaining blocks");
        this.addDefault(defaults, Messages.ResizeFailOverlap, "Can't resize here because it would overlap another nearby claim.", null);
        this.addDefault(defaults, Messages.ResizeStart, "Resizing claim.  Use your shovel again at the new location for this corner.", null);
        this.addDefault(defaults, Messages.ResizeFailOverlapSubdivision, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
        this.addDefault(defaults, Messages.SubdivisionStart, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.", null);
        this.addDefault(defaults, Messages.CreateSubdivisionOverlap, "Your selected area overlaps another subdivision.", null);
        this.addDefault(defaults, Messages.SubdivisionSuccess, "Subdivision created!  Use /trust to share it with friends.", null);
        this.addDefault(defaults, Messages.CreateClaimFailOverlap, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
        this.addDefault(defaults, Messages.CreateClaimFailOverlapOtherPlayer, "You can't create a claim here because it would overlap {0}'s claim.", "0: other claim owner");
        this.addDefault(defaults, Messages.ClaimsDisabledWorld, "Land claims are disabled in this world.", null);
        this.addDefault(defaults, Messages.ClaimStart, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.", null);
        this.addDefault(defaults, Messages.NewClaimTooSmall, "This claim would be too small.  Any claim must be at least {0} x {0}.", "0: minimum claim size");
        this.addDefault(defaults, Messages.CreateClaimInsufficientBlocks, "You don't have enough blocks to claim that entire area.  You need {0} more blocks.", "0: additional blocks needed");
        this.addDefault(defaults, Messages.AbandonClaimAdvertisement, "To delete another claim and free up some blocks, use /AbandonClaim.", null);
        this.addDefault(defaults, Messages.CreateClaimFailOverlapShort, "Your selected area overlaps an existing claim.", null);
        this.addDefault(defaults, Messages.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.", null);
        this.addDefault(defaults, Messages.RescueAbortedMoved, "You moved!  Rescue cancelled.", null);
        this.addDefault(defaults, Messages.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
        this.addDefault(defaults, Messages.NoBuildPvP, "You can't build in claims during PvP combat.", null);
        this.addDefault(defaults, Messages.NoBuildPermission, "You don't have {0}'s permission to build here.", "0: owner name");
        this.addDefault(defaults, Messages.NoAccessPermission, "You don't have {0}'s permission to use that.", "0: owner name.  access permission controls buttons, levers, and beds");
        this.addDefault(defaults, Messages.NoContainersPermission, "You don't have {0}'s permission to use that.", "0: owner's name.  containers also include crafting blocks");
        this.addDefault(defaults, Messages.OwnerNameForAdminClaims, "an administrator", "as in 'You don't have an administrator's permission to build here.'");
        this.addDefault(defaults, Messages.ClaimTooSmallForEntities, "This claim isn't big enough for that.  Try enlarging it.", null);
        this.addDefault(defaults, Messages.TooManyEntitiesInClaim, "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.", null);
        this.addDefault(defaults, Messages.YouHaveNoClaims, "You don't have any land claims.", null);
        this.addDefault(defaults, Messages.ConfirmFluidRemoval, "Abandoning this claim will remove all your lava and water.  If you're sure, use /AbandonClaim again.", null);
        this.addDefault(defaults, Messages.ConfirmAbandonLockedClaim, "This claim has been locked.  If you really want to abandon it, use /AbandonClaim again.", null);
        this.addDefault(defaults, Messages.AutoBanNotify, "Auto-banned {0}({1}).  See logs for details.", null);
        this.addDefault(defaults, Messages.AdjustGroupBlocksSuccess, "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.", "0: permission; 1: adjustment amount; 2: new total bonus");
        this.addDefault(defaults, Messages.InvalidPermissionID, "Please specify a player name, or a permission in [brackets].", null);
        this.addDefault(defaults, Messages.UntrustOwnerOnly, "Only {0} can revoke permissions here.", "0: claim owner's name");
        this.addDefault(defaults, Messages.HowToClaimRegex, "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how to claim land.");
        this.addDefault(defaults, Messages.NoBuildOutsideClaims, "You can't build here unless you claim some land first.", null);
        this.addDefault(defaults, Messages.PlayerOfflineTime, "  Last login: {0} days ago.", "0: number of full days since last login");
        this.addDefault(defaults, Messages.BuildingOutsideClaims, "Other players can undo your work here!  Consider using a golden shovel to claim this area so that your work will be protected.", null);
        this.addDefault(defaults, Messages.TrappedWontWorkHere, "Sorry, unable to find a safe location to teleport you to.  Contact an admin, or consider /kill if you don't want to wait.", null);
        this.addDefault(defaults, Messages.CommandBannedInPvP, "You can't use that command while in PvP combat.", null);
        this.addDefault(defaults, Messages.UnclaimCleanupWarning, "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.", null);
        this.addDefault(defaults, Messages.BuySellNotConfigured, "Sorry, buying anhd selling claim blocks is disabled.", null);
        this.addDefault(defaults, Messages.NoTeleportPvPCombat, "You can't teleport while fighting another player.", null);
        this.addDefault(defaults, Messages.NoTNTDamageAboveSeaLevel, "Warning: TNT will not destroy blocks above sea level.", null);
        this.addDefault(defaults, Messages.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.", null);
        this.addDefault(defaults, Messages.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.", null);
        this.addDefault(defaults, Messages.NoPermissionForCommand, "You don't have permission to do that.", null);
        this.addDefault(defaults, Messages.ClaimsListNoPermission, "You don't have permission to get information about another player's land claims.", null);
        this.addDefault(defaults, Messages.ExplosivesDisabled, "This claim is now protected from explosions.  Use /ClaimExplosions again to disable.", null);
        this.addDefault(defaults, Messages.ExplosivesEnabled, "This claim is now vulnerable to explosions.  Use /ClaimExplosions again to re-enable protections.", null);
        this.addDefault(defaults, Messages.ClaimExplosivesAdvertisement, "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.", null);
        this.addDefault(defaults, Messages.PlayerInPvPSafeZone, "That player is in a PvP safe zone.", null);
        this.addDefault(defaults, Messages.ClaimLocked, "This claim has been successfully locked against accidental/automatic deletion. Use /unlockclaim to unlock.", null);
        this.addDefault(defaults, Messages.ClaimUnlocked, "This claim has been successfully unlocked.", null);
        this.addDefault(defaults, Messages.AbandonClaimRestoreWarning, "Abandoning this claim will restore nature! If you still want to abandon it, use /abandonclaim again.", null);
        this.addDefault(defaults, Messages.AbandonCost, "you lose {0} Claim blocks from abandoning this claim.", "0:Number of claim blocks lost");
        this.addDefault(defaults, Messages.AbandonCostWarning, "You will lose {0} Claim blocks if you abandon this claim. enter /abandonclaim again to confirm.", "0:Number of claim blocks that will be lost");
        this.addDefault(defaults, Messages.NoVillagerTradeOutsideClaims, "You cannot trade with Villagers outside of Claims.", null);
        this.addDefault(defaults, Messages.PlayerClaimLimit, "You have reached the claim limit ({0}) and cannot create new claims.", "0:Configuration Claim Limit.");
        this.addDefault(defaults, Messages.ClearManagersNotFound, "You must be in a claim to clear it's Managers.", null);
        this.addDefault(defaults, Messages.ClearManagersSuccess, "Cleared all Managers in this Claim.", null);
        this.addDefault(defaults, Messages.ClearManagersNotOwned, "Only {0} can clear managers in their Claim.", "0:Claim Owner");
        this.addDefault(defaults, Messages.ClearManagersNotAdmin, "Only Administrators can change managers on an admin claim.", null);
        this.addDefault(defaults, Messages.GroupNotFound, "Group {0} Not found.", "0:Name of Group");
        this.addDefault(defaults, Messages.ConfigDisabled, "{0} has been disabled for this location.", "0:name of operation");
        this.addDefault(defaults, Messages.TamedDeathDefend, "A {0} has been killed in defense of your Claim!", "0:Type of tamed animal;1:Attacker");
        this.addDefault(defaults, Messages.NoGiveClaimsPermission, "You do not have Permission to Give your Claims to other players", null);
        this.addDefault(defaults, Messages.NoAdminClaimsPermission, "You need Administrator Claims Permission to do that.", null);
        this.addDefault(defaults, Messages.GiveSuccessSender, "Claim Owner changed from {0} to {1} Successfully.", "0:Sender;1:Recipient");
        this.addDefault(defaults, Messages.GiveSuccessTarget, "{0} has transferred a Claim to you.", "0:Sender");
        this.addDefault(defaults, Messages.CommandRequiresPlayer, "This command must be run by a player.", null);
        this.addDefault(defaults, Messages.UnknownMaterial, "Unknown Material: {0}", "0:Material");
        this.addDefault(defaults, Messages.PluginReloaded, "GriefPrevention has been reloaded.", null);
        this.addDefault(defaults, Messages.NotANumber, "Expecting a number, not {0}", "0:A non-number");
        this.addDefault(defaults, Messages.TransferBlocksLessThanOne, "You cannot transfer less than 1 block.", null);
        this.addDefault(defaults, Messages.TransferBlocksError, "There was a problem attempting to transfer claimblocks, Check the server log for errors.", null);
        this.addDefault(defaults, Messages.BooleanParseError, "Expected 'true' or 'false', not {0}", "0:not a bool");
        this.addDefault(defaults, Messages.UnknownCommand, "Unknown subcommand {0}", "0:subcommand");

        // load the config file
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        // for each message ID
        for (int i = 0; i < messageIDs.length; i++) {
            // get default for this message
            Messages messageID = messageIDs[i];
            CustomizableMessage messageData = defaults.get(messageID.name());

            // if default is missing, log an error and use some fake data for now so that the plugin can run
            if (messageData == null) {
                GriefPrevention.addLogEntry("Missing message for " + messageID.name() + ".  Please contact the developer.");
                messageData = new CustomizableMessage(messageID, "Missing message!  ID: " + messageID.name() + ".  Please contact a server admin.", null);
            }

            // read the message from the file, use default if necessary
            this.messages[messageID.ordinal()] = config.getString("Messages." + messageID.name() + ".Text", messageData.text);
            config.set("Messages." + messageID.name() + ".Text", this.messages[messageID.ordinal()]);

            if (messageData.notes != null) {
                messageData.notes = config.getString("Messages." + messageID.name() + ".Notes", messageData.notes);
                config.set("Messages." + messageID.name() + ".Notes", messageData.notes);
            }
        }

        // save any changes
        try {
            config.save(new File(plugin.getDataFolder(), "messages.yml"));
        } catch (IOException exception) {
            GriefPrevention.addLogEntry("Unable to write to the configuration file at \"" + new File(plugin.getDataFolder(), "messages.yml") + "\"");
        }

        defaults.clear();
        System.gc();
    }

    private void addDefault(HashMap<String, CustomizableMessage> defaults, Messages id, String text, String notes) {
        CustomizableMessage message = new CustomizableMessage(id, text, notes);
        defaults.put(id.name(), message);
    }

    synchronized public String getMessage(Messages messageID, String... args) {
        String message = messages[messageID.ordinal()];
        for (int i = 0; i < args.length; i++) {
            String param = args[i];
            message = message.replace("{" + i + "}", param);
        }
        return message;
    }

}

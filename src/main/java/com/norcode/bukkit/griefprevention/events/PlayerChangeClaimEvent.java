package com.norcode.bukkit.griefprevention.events;

import com.norcode.bukkit.griefprevention.data.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 6/9/13
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerChangeClaimEvent extends PlayerEvent implements Cancellable {

    Claim oldClaim;
    Claim newClaim;
    boolean canceled = false;
    private static final HandlerList handlers = new HandlerList();

    public PlayerChangeClaimEvent(Player p, Claim oldClaim, Claim newClaim) {
        super(p);
        this.oldClaim = oldClaim;
        this.newClaim = newClaim;
    }

    public Claim getOldClaim() {
        return oldClaim;
    }

    public Claim getNewClaim() {
        return newClaim;
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean iscancelled) {
        canceled = iscancelled;
    }
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }



}

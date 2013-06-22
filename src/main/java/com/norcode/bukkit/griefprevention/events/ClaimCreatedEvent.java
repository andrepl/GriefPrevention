package com.norcode.bukkit.griefprevention.events;

import com.norcode.bukkit.griefprevention.data.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is created this event is called.
 */
public class ClaimCreatedEvent extends Event implements Cancellable {

    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    Claim claim;
    Player p;

    /**
     * returns the Player creating this Claim. This could be null in some circumstances.
     *
     * @return
     */
    public Player getPlayer() {
        return p;
    }

    /**
     * constructs an event instance.
     *
     * @param claim
     * @param p
     */
    public ClaimCreatedEvent(Claim claim, Player p) {
        this.claim = claim;
        this.p = p;
    }

    /**
     * the claim being created.
     *
     * @return
     */
    public Claim getClaim() {
        return claim;
    }

    boolean canceled = false;

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public void setCancelled(boolean iscancelled) {
        canceled = iscancelled;
    }
}

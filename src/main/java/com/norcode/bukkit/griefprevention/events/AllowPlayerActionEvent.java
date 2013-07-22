package com.norcode.bukkit.griefprevention.events;

import com.norcode.bukkit.griefprevention.data.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;


public class AllowPlayerActionEvent extends PlayerEvent implements Cancellable {

    boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();
    Event originalEvent;
    Claim claim;
    String denyMessage;

    public AllowPlayerActionEvent(Player who, Claim claim, Event event, String denyMessage) {
        super(who);
        this.claim = claim;
        this.originalEvent = event;
        this.denyMessage = denyMessage;
    }

    public Claim getClaim() {
        return claim;
    }

    public Event getOriginalEvent() {
        return originalEvent;
    }

    public boolean isAllowed() {
        return denyMessage == null;
    }

    public void setDenyMessage(String msg) {
        this.denyMessage = msg;
    }

    public String getDenyMessage() {
        return denyMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }


    public static HandlerList getHandlerList() {
        return handlers;
    }
}

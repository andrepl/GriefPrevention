package com.norcode.bukkit.griefprevention.events;

import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.flags.BaseFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class FlagSetEvent extends PlayerEvent implements Cancellable {

    private Claim claim;
    private BaseFlag flag;
    private String value;
    private boolean cancelled;

    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();

    public FlagSetEvent(Player who, Claim claim, BaseFlag flag, String value) {
        super(who);
        this.claim = claim;
        this.flag = flag;
        this.value = value;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = true;
    }

    public Claim getClaim() {
        return claim;
    }

    public BaseFlag getFlag() {
        return flag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}

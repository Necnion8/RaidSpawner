package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class RaidSpawnsAllEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Collection<RaidSpawner> raids;

    public RaidSpawnsAllEndEvent(Collection<RaidSpawner> raids) {
        this.raids = raids;
    }

    public Collection<RaidSpawner> raids() {
        return raids;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RaidSpawnStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final RaidSpawner raid;

    public RaidSpawnStartEvent(RaidSpawner raid) {
        this.raid = raid;
    }

    public RaidSpawner getRaid() {
        return raid;
    }

    public Land getLand() {
        return raid.getLand();
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

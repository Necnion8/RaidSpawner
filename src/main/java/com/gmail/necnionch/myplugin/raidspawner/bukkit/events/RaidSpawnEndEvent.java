package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RaidSpawnEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final RaidSpawner raid;
    private final Result result;

    public RaidSpawnEndEvent(RaidSpawner raid, Result result) {
        this.raid = raid;
        this.result = result;
    }

    public RaidSpawner getRaid() {
        return raid;
    }

    public Land getLand() {
        return raid.getLand();
    }

    public Result getResult() {
        return result;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }


    public enum Result {
        CANCEL,
        WIN,
        LOSE,
    }

}

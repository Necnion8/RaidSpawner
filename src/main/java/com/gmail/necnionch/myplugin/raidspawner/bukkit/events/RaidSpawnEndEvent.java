package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndReason;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RaidSpawnEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final RaidSpawner raid;
    private final RaidEndResult result;
    private final RaidEndReason reason;

    public RaidSpawnEndEvent(RaidSpawner raid, RaidEndResult result, @Nullable RaidEndReason reason) {
        this.raid = raid;
        this.result = result;
        this.reason = reason;
    }

    public RaidSpawner getRaid() {
        return raid;
    }

    public Land getLand() {
        return raid.getLand();
    }

    public RaidEndResult getResult() {
        return result;
    }

    public @Nullable RaidEndReason getReason() {
        return reason;
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

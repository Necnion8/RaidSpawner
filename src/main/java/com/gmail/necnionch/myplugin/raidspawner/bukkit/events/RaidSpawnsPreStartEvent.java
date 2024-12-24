package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.Condition;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class RaidSpawnsPreStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Collection<RaidSpawner> raids;
    private final @Nullable Condition startBy;
    private boolean cancelled;

    public RaidSpawnsPreStartEvent(Collection<RaidSpawner> raids, @Nullable Condition startBy) {
        this.raids = raids;
        this.startBy = startBy;
    }

    public Collection<RaidSpawner> raids() {
        return raids;
    }

    public @Nullable Condition getByStartCondition() {
        return startBy;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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

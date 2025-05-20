package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RaidSpawnsPreStartNotifyEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final @Nullable ConditionWrapper condition;
    private boolean cancelled;

    public RaidSpawnsPreStartNotifyEvent(@Nullable ConditionWrapper condition) {
        this.condition = condition;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Nullable
    public ConditionWrapper getCondition() {
        return condition;
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

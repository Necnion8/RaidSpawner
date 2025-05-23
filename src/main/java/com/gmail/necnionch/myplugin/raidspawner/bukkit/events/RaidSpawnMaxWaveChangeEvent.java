package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RaidSpawnMaxWaveChangeEvent extends RaidSpawnerRaidEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int maxWaves;

    public RaidSpawnMaxWaveChangeEvent(RaidSpawner raid, int maxWaves) {
        super(raid);
        this.maxWaves = maxWaves;
    }

    public int getMaxWaves() {
        return maxWaves;
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

package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RaidSpawnWaveChangeEvent extends RaidSpawnerRaidEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int waves;

    public RaidSpawnWaveChangeEvent(RaidSpawner raid, int waves) {
        super(raid);
        this.waves = waves;
    }

    public int getWaves() {
        return waves;
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

package com.gmail.necnionch.myplugin.raidspawner.bukkit.events;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.event.Event;

public abstract class RaidSpawnerRaidEvent extends Event {

    private final RaidSpawner raid;

    public RaidSpawnerRaidEvent(RaidSpawner raid) {
        this.raid = raid;
    }

    public RaidSpawner getRaid() {
        return raid;
    }

}

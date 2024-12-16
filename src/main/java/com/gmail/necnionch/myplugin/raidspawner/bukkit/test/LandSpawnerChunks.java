package com.gmail.necnionch.myplugin.raidspawner.bukkit.test;

import me.angeschossen.lands.api.land.Land;
import org.bukkit.World;

import java.util.List;

public class LandSpawnerChunks {

    private final Land land;
    private final World world;
    private final List<Pos> chunks;

    public LandSpawnerChunks(Land land, World world, List<Pos> chunks) {
        this.land = land;
        this.world = world;
        this.chunks = chunks;
    }

    public Land getLand() {
        return land;
    }

    public World getWorld() {
        return world;
    }

    public List<Pos> getChunks() {
        return chunks;
    }

    public record Pos(Land land, int x, int z) {}

    public record LandWorld(Land land, World world) {}

}

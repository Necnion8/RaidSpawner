package com.gmail.necnionch.myplugin.raidspawner.bukkit.raid;

import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.World;

import java.util.List;

public record LandChunkFindResult(
        Land land,
        World world,
        List<ChunkCoordinate> landChunks,
        List<RaidSpawner.Chunk> raidChunks
) {
}

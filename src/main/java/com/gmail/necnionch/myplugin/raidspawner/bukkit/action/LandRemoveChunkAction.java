package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LandRemoveChunkAction implements LandAction {

    private final Provider provider;
    private final int chunkCount;

    public LandRemoveChunkAction(Provider provider, int chunkCount) {
        this.provider = provider;
        this.chunkCount = chunkCount;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Land land) {
        World world = spawner.getWorld();
        Container container = land.getContainer(world);
        if (container == null) {
            RaidSpawnerUtil.getLogger().warning("Unable to get land container: land.getContainer(w) is null");
            return false;
        }

        List<? extends ChunkCoordinate> chunks = new ArrayList<>(container.getChunks());
        if (chunks.size() <= this.chunkCount) {
            // remove all
            for (ChunkCoordinate chunk : chunks) {
                land.unclaimChunk(world, chunk.getX(), chunk.getZ(), null);
            }
        } else {
            // random
            Random random = new Random();
            for (int i = 0; i < this.chunkCount; i++) {
                ChunkCoordinate chunk = chunks.remove(random.nextInt(chunks.size()));
                land.unclaimChunk(world, chunk.getX(), chunk.getZ(), null);
            }
        }
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<LandRemoveChunkAction> {

        public Provider() {
            super("remove-chunk");
        }

        @Override
        public LandRemoveChunkAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            return new LandRemoveChunkAction(this, parseInt(value));
        }

    }

}

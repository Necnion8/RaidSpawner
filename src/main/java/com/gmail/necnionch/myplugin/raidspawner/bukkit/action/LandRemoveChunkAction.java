package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.player.LandPlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LandRemoveChunkAction implements LandAction {

    private final Provider provider;
    private final int chunkCount;
    private final boolean keepLand;

    public LandRemoveChunkAction(Provider provider, int chunkCount, boolean keepLand) {
        this.provider = provider;
        this.chunkCount = chunkCount;
        this.keepLand = keepLand;
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
        if (chunks.isEmpty() || chunkCount <= 0)
            return false;

        RaidSpawnerUtil.d(() -> "Removing " + Math.min(chunks.size(), chunkCount) + " land chunks: " + land.getName());
        Set<CompletableFuture<?>> tasks = new HashSet<>();
        if (chunks.size() <= chunkCount) {
            // remove all
            for (ChunkCoordinate chunk : chunks) {
                tasks.add(queueUnclaimChunk(land, world, chunk));
            }
        } else {
            // random
            Random random = new Random();
            for (int i = 0; i < chunkCount; i++) {
                ChunkCoordinate chunk = chunks.remove(random.nextInt(chunks.size()));
                tasks.add(queueUnclaimChunk(land, world, chunk));
            }
        }

        if (keepLand)
            return true;

        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).thenAccept(v -> {
            // remove land
            if (land.getContainers().stream().mapToInt(c -> c.getChunks().size()).sum() <= 0) {
                RaidSpawnerUtil.d(() -> "Deleting land: " + land.getName());
                land.delete((LandPlayer) null).whenComplete((result, error) -> {
                    if (result) {
                        RaidSpawnerUtil.getLogger().info("Deleted land (by unclaim chunks): " + land.getName());
                    } else if (error != null) {
                        RaidSpawnerUtil.getLogger().log(Level.WARNING, "Failed to delete land: " + land.getName(), error);
                    } else {
                        RaidSpawnerUtil.getLogger().warning("Failed to delete land: " + land.getName());
                    }
                });
            }
        });
        return true;
    }

    private CompletableFuture<Void> queueUnclaimChunk(Land land, World world, ChunkCoordinate chunk) {
        return land.unclaimChunk(world, chunk.getX(), chunk.getZ(), null).thenAccept(c -> {
            if (c != null) {
                RaidSpawnerUtil.getLogger().info("Unclaim chunk: land: " + land.getName() + ", world: " + world.getName() + ", chunk: " + chunk.getX() + "," + chunk.getZ());
            } else {
                RaidSpawnerUtil.d(() -> "Cancelled unclaim chunk: " + land.getName() + ", world: " + world.getName() + ", chunk: " + chunk.getX() + "," + chunk.getZ());
            }
        });
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
            if (config != null) {
                if (!config.contains("value"))
                    throw new ConfigurationError("Require 'value' key with int value");
                return new LandRemoveChunkAction(this, parseInt("value"), config.getBoolean("keep-land"));
            }
            return new LandRemoveChunkAction(this, parseInt(value), false);
        }

    }

}

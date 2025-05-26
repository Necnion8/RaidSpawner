package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.MobSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;

public abstract class Enemy {

    private final MobSetting.Enemy config;

    public Enemy(MobSetting.Enemy config) {
        this.config = config;
    }

    public MobSetting.Enemy getConfig() {
        return config;
    }

    public abstract @Nullable Entity spawn(RaidSpawner spawner, World world, Location location);

    public abstract boolean isAlive();

    public abstract boolean remove();

    public abstract @Nullable Entity getEntity();

    public @Nullable Location getEntityLocation() {
        return Optional.ofNullable(getEntity()).map(Entity::getLocation).orElse(null);
    }

    public void unload() {
        remove();
    }

    public abstract @NotNull EnemyProvider<?> getProvider();

    public @Nullable Location searchRandomSpawnLocationByChunk(RaidSpawner spawner, Chunk chunk, Random random, boolean force) {
        return spawner.selectRandomSpawnLocationByChunk(getConfig(), chunk, random, force);
    }

}

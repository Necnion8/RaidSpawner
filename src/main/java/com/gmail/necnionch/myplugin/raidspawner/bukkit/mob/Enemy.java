package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface Enemy {

    boolean spawn(RaidSpawner spawner, World world, Location location);

    boolean isAlive();

    boolean remove();

    @Nullable Entity getEntity();

    default @Nullable Location getEntityLocation() {
        return Optional.ofNullable(getEntity()).map(Entity::getLocation).orElse(null);
    }

    default void unload() {}

    @NotNull EnemyProvider<?> getProvider();

}

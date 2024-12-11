package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public interface Enemy {

    boolean spawn(RaidSpawner spawner, World world, Location location);

    boolean isAlive();

    boolean remove();

    default void unload() {}

    @NotNull EnemyProvider<?> getProvider();

}

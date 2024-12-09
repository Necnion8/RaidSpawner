package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import me.angeschossen.lands.api.land.Land;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public interface Enemy {

    boolean spawn(Land land, World world, Location location);

    boolean isAlive();

    boolean remove();

    default void unload() {}

    @NotNull EnemyProvider<?> getProvider();

}

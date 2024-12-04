package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class Actions {

    private final Item[] player;
    private final Item[] land;

    public Actions(Item[] player, Item[] land) {
        this.player = player;
        this.land = land;
    }

    public Item[] getPlayerActions() {
        return player;
    }

    public Item[] getLandActions() {
        return land;
    }

    public record Item(String type, Object value, @Nullable ConfigurationSection config) {
    }

}

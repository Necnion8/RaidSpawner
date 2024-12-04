package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import me.angeschossen.lands.api.land.Land;

import java.util.List;

public class RaidSpawner {

    private final Land land;
    private final List<ConditionWrapper> conditions;

    public RaidSpawner(Land land, List<ConditionWrapper> conditions) {
        this.land = land;
        this.conditions = conditions;
    }

    public Land getLand() {
        return land;
    }

    public List<ConditionWrapper> getConditions() {
        return conditions;
    }


    public void start() {
        conditions.forEach(ConditionWrapper::start);
    }

    public void clear() {
        conditions.forEach(ConditionWrapper::clear);
    }

}

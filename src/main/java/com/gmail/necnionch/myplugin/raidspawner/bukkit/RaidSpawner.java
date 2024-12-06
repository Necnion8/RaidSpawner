package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import me.angeschossen.lands.api.land.Land;

import java.util.List;

public class RaidSpawner {

    private final Land land;
    private final List<ConditionWrapper> conditions;
    private boolean running;
    private boolean lose;

    public RaidSpawner(Land land, List<ConditionWrapper> conditions) {
        this.land = land;
        this.conditions = conditions;
    }

    public Land getLand() {
        return land;
    }

    public List<ConditionWrapper> conditions() {
        return conditions;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isLose() {
        return lose;
    }

    public void start() {
        running = true;
        conditions.forEach(ConditionWrapper::start);
    }

    public void clear() {
        running = false;
        conditions.forEach(ConditionWrapper::clear);
    }

    public void clearSetLose() {
        lose = true;
        clear();
    }

    public void clearSetWin() {
        lose = false;
        clear();
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnStartEvent;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RaidSpawner {

    private final Land land;
    private final List<ConditionWrapper> conditions;
    private final RaidSetting setting;
    private boolean running;
    private boolean lose;
    private int waves;

    public RaidSpawner(Land land, List<ConditionWrapper> conditions, RaidSetting setting) {
        this.land = land;
        this.conditions = conditions;
        this.setting = setting;
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

    public int getMaxWaves() {
        return setting.maxWaves();
    }

    public int getWave() {
        return waves;
    }

    public void start() {
        running = true;
        conditions.forEach(ConditionWrapper::start);

        Bukkit.getPluginManager().callEvent(new RaidSpawnStartEvent(this));
        tryNextWave();
    }

    public void clear(@Nullable RaidSpawnEndEvent.Result result) {
        running = false;
        try {
            conditions.forEach(ConditionWrapper::clear);

        } finally {
            Bukkit.getPluginManager().callEvent(new RaidSpawnEndEvent(this, result));
        }

    }

    public void clear() {
        clear(RaidSpawnEndEvent.Result.CANCEL);
    }

    public void clearSetLose() {
        lose = true;
        clear(RaidSpawnEndEvent.Result.LOSE);
    }

    public void clearSetWin() {
        lose = false;
        clear(RaidSpawnEndEvent.Result.WIN);
    }


    public void tryNextWave() {
        if (!running)
            return;

        if (waves < setting.maxWaves()) {
            waves++;

        } else {
            clearSetWin();
        }

    }

}

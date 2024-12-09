package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.TestEnemy;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record RaidSetting(
        int eventTimeMinutes,
        int maxWaves,
        @Nullable String luckPermsGroup,
        List<MobSetting> mobs
) {

    public static final RaidSetting DEFAULTS = new RaidSetting(
            30,
            5,
            null,
            Collections.singletonList(new MobSetting(
                    s -> 3, 1, Collections.singletonList(new MobSetting.Enemy(
                            "test", 1, new TestEnemy.Provider()
                    ))
            ))
    );

}
package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record RaidSetting(
        int eventTimeMinutes,
        int maxWaves,
        int maxWaveTimeMinutes,
        BossBarSetting bossBar,
        int tickets,
        @Nullable String luckPermsGroup,
        @Nullable String world,
        int mobsDistanceChunks,
        int mobsGlowingEnemies,
        List<MobSetting> mobs
) {

    public static final RaidSetting DEFAULTS = new RaidSetting(
            30,
            5,
            6,
            BossBarSetting.DEFAULTS,
            20,
            null,
            null,
            2,
            10,
            Collections.singletonList(new MobSetting(
                    s -> 3, Collections.emptyList()
            ))
    );

}

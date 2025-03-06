package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public record BossBarSetting(
        boolean enable,
        BarColor color,
        BarStyle style,
        String text
) {
    public static final BossBarSetting DEFAULTS = new BossBarSetting(
            true,
            BarColor.PURPLE,
            BarStyle.SOLID,
            "&6Wave: &l%wave%/%max_waves%&f | &c敵残存数: &l%enemies%&f | &e残りチケット: &l%tickets%"
    );

}

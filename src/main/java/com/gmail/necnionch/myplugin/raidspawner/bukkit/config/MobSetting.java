package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public record MobSetting(
        Function<RaidSpawner, Integer> count,
        int distanceChunk,
        List<Enemy> enemies
) {

    public static class Enemy {

        private final String source;
        private final int priority;
        private final ConfigurationSection config;
        private @Nullable EnemyProvider<?> provider;

        public Enemy(String source, int priority, ConfigurationSection config, @Nullable EnemyProvider<?> provider) {
            this.source = source;
            this.priority = priority;
            this.provider = provider;
            this.config = config;
        }

        public String getSource() {
            return source;
        }

        public int getPriority() {
            return priority;
        }

        public ConfigurationSection getConfig() {
            return config;
        }

        @Nullable
        public EnemyProvider<?> getProvider() {
            return provider;
        }

        public void setProvider(@Nullable EnemyProvider<?> provider) {
            this.provider = provider;
        }

        public static Enemy serialize(ConfigurationSection config) {
            String source = config.getString("source");
            int priority = Math.max(1, config.getInt("priority"));
            return new Enemy(source, priority, config, null);
        }
    }

}

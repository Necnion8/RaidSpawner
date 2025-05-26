package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public record MobSetting(
        Predicate<ExpressionResource> condition,
        ConditionType conditionType,
        Function<ExpressionResource, Integer> count,
        @Nullable Function<ExpressionResource, Integer> maxWaves,
        @Nullable List<Enemy> enemies,
        @Nullable List<MobSetting> children
) {

    public @Nullable List<Enemy> selectEnemies(int count, Random random) {
        if (enemies == null)
            return null;

        int total = enemies.stream()
                .mapToInt(Enemy::getPriority)
                .sum();

        List<Enemy> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float target = random.nextFloat() * total;
            int current = 0;

            for (Enemy enemy : enemies) {
                current += enemy.getPriority();
                if (target <= current) {
                    result.add(enemy);
                    break;
                }
            }
        }
        return result;
    }

    public record ExpressionResource(
            RaidSpawner spawner,
            int groupIndex,
            int parentCount
    ) {}

    public enum ConditionType {
        ALL, ONE,
    }


    public static class Enemy {

        private final String source;
        private final int priority;
        private final SpawnLocation spawnLocation;
        private final ConfigurationSection config;
        private @Nullable EnemyProvider<?> provider;

        public Enemy(String source, int priority, SpawnLocation spawnLocation, ConfigurationSection config, @Nullable EnemyProvider<?> provider) {
            this.source = source;
            this.priority = priority;
            this.spawnLocation = spawnLocation;
            this.provider = provider;
            this.config = config;
        }

        public static Enemy parse(ConfigurationSection config, SpawnLocation spawnLocation, @Nullable EnemyProvider<?> provider) {
            return new Enemy(
                    config.getString("source"),
                    config.getInt("priority"),
                    spawnLocation,
                    config,
                    provider
            );
        }

        public String getSource() {
            return source;
        }

        public int getPriority() {
            return priority;
        }

        public SpawnLocation getSpawnLocation() {
            return spawnLocation;
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

    }

    public record SpawnLocation(int searchLimit, int allowWaterHeight, int allowLavaHeight) {

        public static SpawnLocation parse(@Nullable ConfigurationSection config, SpawnLocation defaults) {
            if (config == null)
                return defaults;
            return new SpawnLocation(
                    config.getInt("search-limit", defaults.searchLimit),
                    config.getInt("allow-water-height", defaults.allowWaterHeight),
                    config.getInt("allow-lava-height", defaults.allowLavaHeight)
            );
        }

        public static SpawnLocation parse(@Nullable ConfigurationSection config) {
            return parse(config, DEFAULTS);
        }

        public static final SpawnLocation DEFAULTS = new SpawnLocation(8, 1, 0);
    }

}

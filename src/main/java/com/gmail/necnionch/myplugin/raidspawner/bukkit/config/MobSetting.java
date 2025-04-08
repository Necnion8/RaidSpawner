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
    }

}

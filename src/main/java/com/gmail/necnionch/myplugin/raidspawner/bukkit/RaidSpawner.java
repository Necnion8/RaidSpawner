package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.MobSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnStartEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RaidSpawner {

    private final Land land;
    private final List<ConditionWrapper> conditions;
    private final RaidSetting setting;
    private final World world;
    private boolean running;
    private boolean lose;
    private int waves;
    private final List<Enemy> currentEnemies = new ArrayList<>();

    public RaidSpawner(Land land, List<ConditionWrapper> conditions, RaidSetting setting, World world) {
        this.land = land;
        this.conditions = conditions;
        this.setting = setting;
        this.world = world;
    }

    public Land getLand() {
        return land;
    }

    public List<ConditionWrapper> conditions() {
        return conditions;
    }

    public World getWorld() {
        return world;
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

    public List<Enemy> currentEnemies() {
        return currentEnemies;
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

            doWave();

        } else {
            clearSetWin();
        }

    }

    private void doWave() {
        currentEnemies.removeIf(e -> !e.isAlive());  // keep alive
        List<MobSetting.Enemy> selected = new ArrayList<>();
        Random random = new Random();

        for (MobSetting mobSetting : setting.mobs()) {
            List<MobSetting.Enemy> enemies = mobSetting.enemies();
            if (enemies.isEmpty())
                continue;

            for (int i = 0; i < mobSetting.count().apply(this); i++) {
                int total = enemies.stream()
                        .mapToInt(MobSetting.Enemy::getPriority)
                        .sum();

                int target = (int) (random.nextFloat() * total);
                int current = 0;

                for (MobSetting.Enemy enemy : enemies) {
                    current += enemy.getPriority();
                    if (current < target) {
                        selected.add(enemy);
                        break;
                    }
                }
            }
        }

        // get provider
        for (MobSetting.Enemy enemyItem : selected) {
            EnemyProvider<?> provider = enemyItem.getProvider();
            System.out.println("- enemy: source " + enemyItem.getSource());

            if (provider == null) {
                System.out.println("no provided");
            } else {
                Enemy enemy;
                try {
                    enemy = provider.create(enemyItem.getConfig());
                } catch (EnemyProvider.ConfigurationError e) {
                    e.printStackTrace();
                    continue;
                }
                System.out.println("get");
                currentEnemies.add(enemy);
            }
        }

        // summon
        System.out.println("total enemies " + currentEnemies.size());

        Container container = land.getContainer(world);
        if (container == null) {
            System.out.println("container is null");  // todo: check null

        } else {
            ArrayList<? extends ChunkCoordinate> chunks = new ArrayList<>(container.getChunks());
            ChunkCoordinate chunk = chunks.get(random.nextInt(chunks.size()));
            int x = chunk.getBlockX() / 2;
            int z = chunk.getBlockZ() / 2;
            Chunk bChunk = world.getChunkAt(chunk.getX(), chunk.getZ());
            // todo: wee
        }
        Area area = land.getDefaultArea();
        Location location = area.getSpawn().toLocation();

        currentEnemies.forEach(e -> {
            if (!e.isAlive()) {
                if (e.spawn(land, world, location)) {
                    System.out.println("spawn");
                } else {
                    System.out.println("cannot spawn");  // remove alive counter
                }
            }
        });

    }

}

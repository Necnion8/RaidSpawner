package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.MobSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnStartEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RaidSpawner {

    private final RaidSpawnerPlugin plugin = JavaPlugin.getPlugin(RaidSpawnerPlugin.class);
    private final Land land;
    private final List<ConditionWrapper> conditions;
    private final RaidSetting setting;
    private final World world;
    private final List<Chunk> spawnChunks;
    private boolean running;
    private boolean lose;
    private int waves;
    private final List<Enemy> currentEnemies = new ArrayList<>();

    public RaidSpawner(Land land, List<ConditionWrapper> conditions, RaidSetting setting, World world, List<Chunk> spawnChunks) {
        this.land = land;
        this.conditions = conditions;
        this.setting = setting;
        this.world = world;
        this.spawnChunks = Collections.unmodifiableList(spawnChunks);
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

    public void clear(RaidSpawnEndEvent.Result result) {
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

        RaidSpawnerUtil.d(() -> "tryNextWave | now wave: " + waves + " | land: " + land.getName());

        if (waves < setting.maxWaves()) {
            waves++;
            RaidSpawnerUtil.d(() -> "waves: " + waves);
            doWave();

        } else {
            clearSetWin();
        }

    }

    private void doWave() {
        Container container = land.getContainer(world);
        if (container == null) {
            System.out.println("container is null");  // todo: check null
            return;
        }

        currentEnemies.removeIf(e -> !e.isAlive());  // keep alive
        Random random = new Random();

        // select enemy
        RaidSpawnerUtil.d(() -> "setting.mobs -> " + setting.mobs().size() + " | land: " + land.getName());
        List<MobSetting.Enemy> enemySettings = new ArrayList<>();
        for (MobSetting mobSetting : setting.mobs()) {
            List<MobSetting.Enemy> enemies = mobSetting.enemies();
            RaidSpawnerUtil.d(() -> "  mob.enemies -> " + enemies.size());
            if (enemies.isEmpty())
                continue;

            int total = enemies.stream()
                    .mapToInt(MobSetting.Enemy::getPriority)
                    .sum();

            /*

            e: 10
            e: 20
            e: 10
            total = 40


             */

            int count = mobSetting.count().apply(this);
            RaidSpawnerUtil.d(() -> "  spawn count: " + count);
            for (int i = 0; i < count; i++) {
                float target = random.nextFloat() * total;
                int current = 0;

                for (MobSetting.Enemy enemy : enemies) {
                    current += enemy.getPriority();
                    if (target <= current) {
                        enemySettings.add(enemy);
                        break;
                    }
                }
            }
        }

        // get provider
        for (MobSetting.Enemy enemyItem : enemySettings) {
            EnemyProvider<?> provider = enemyItem.getProvider();
            if (provider == null) {
                provider = plugin.enemyProviders().get(enemyItem.getSource());
                enemyItem.setProvider(provider);
            }

            RaidSpawnerUtil.d(() -> "- enemy: source " + enemyItem.getSource());

            if (provider == null) {
                RaidSpawnerUtil.d(() -> "no provided");
            } else {
                Enemy enemy;
                try {
                    enemy = provider.create(enemyItem.getConfig());
                } catch (EnemyProvider.ConfigurationError e) {
                    e.printStackTrace();
                    continue;
                }
                currentEnemies.add(enemy);
            }
        }

        // summon
        RaidSpawnerUtil.d(() -> "total enemies " + currentEnemies.size());

        Chunk chunk = spawnChunks.get(random.nextInt(spawnChunks.size()));
        org.bukkit.Chunk bChunk = world.getChunkAt(chunk.x, chunk.z);

        currentEnemies.forEach(e -> {
            if (!e.isAlive()) {
                Block block = bChunk.getBlock(4 + random.nextInt(8), 0, 4 + random.nextInt(8));
                Location location = world.getHighestBlockAt(block.getX(), block.getZ()).getLocation().add(.5, 1, .5);

                if (e.spawn(this, world, location)) {
                    RaidSpawnerUtil.d(() -> "spawn " + e.getProvider().getSource() + " in " + world.getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                } else {
                    RaidSpawnerUtil.d(() -> "cannot spawn");  // TODO: remove alive counter
                }
            }
        });

    }


    public record Chunk(Land land, World world, int x, int z) {
        public String toString() {
            return x + "," + z + "," + world.getName();
        }
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.Action;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.MobSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnStartEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks.LuckPermsBridge;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks.PluginBridge;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RaidSpawner {

    private final RaidSpawnerPlugin plugin = JavaPlugin.getPlugin(RaidSpawnerPlugin.class);
    private final Land land;
    private final RaidSetting setting;
    private final World world;
    private final List<Chunk> spawnChunks;
    private final Rewards rewards;
    private boolean running;
    private boolean lose;
    private int waves;
    private final List<Enemy> currentEnemies = new ArrayList<>();

    public RaidSpawner(Land land, RaidSetting setting, World world, List<Chunk> spawnChunks, Rewards rewards) {
        this.land = land;
        this.setting = setting;
        this.world = world;
        this.spawnChunks = Collections.unmodifiableList(spawnChunks);
        this.rewards = rewards;
    }

    public Land getLand() {
        return land;
    }

    public Rewards getRewards() {
        return rewards;
    }

    public List<Chunk> getSpawnChunks() {
        return spawnChunks;
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
        rewards.rewardConditions.forEach(ConditionWrapper::start);

        RaidSpawnerUtil.getLogger().info("Raid started: " + land.getName());
        Bukkit.getPluginManager().callEvent(new RaidSpawnStartEvent(this));

        String groupName = setting.luckPermsGroup();
        if (groupName != null) {
            PluginBridge.getValid(LuckPermsBridge.class).ifPresent(perms -> {
                for (Player player : land.getOnlinePlayers()) {
                    perms.addPermissionGroup(player, groupName);
                }
            });
        }

        tryNextWave();
    }

    public void clear(RaidSpawnEndEvent.Result result) {
        RaidSpawnerUtil.getLogger().info("Raid ended: " + land.getName() + " (" + result.name() + ")");
        running = false;

        if (!RaidSpawnEndEvent.Result.CANCEL.equals(result)) {
            try {
                plugin.sendReward(this, result);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        rewards.rewardConditions.forEach(ConditionWrapper::clear);
        currentEnemies.forEach(enemy -> {
            try {
                enemy.remove();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        currentEnemies.clear();

        Bukkit.getPluginManager().callEvent(new RaidSpawnEndEvent(this, result));

        String groupName = setting.luckPermsGroup();
        if (groupName != null) {
            PluginBridge.getValid(LuckPermsBridge.class).ifPresent(perms -> {
                for (Player player : land.getOnlinePlayers()) {
                    try {
                        perms.removePermissionGroup(player, groupName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
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
        for (Iterator<Enemy> it = currentEnemies.iterator(); it.hasNext(); ) {
            Enemy enemy = it.next();
            if (!enemy.isAlive()) {
                int searchLimit = 3;
                Location location;

                for (int i = 0; i < searchLimit; i++) {
                    Chunk chunk = spawnChunks.get(random.nextInt(spawnChunks.size()));
                    location = selectRandomSpawnLocationByChunk(chunk.getBukkitChunk(), random, searchLimit <= i + 1);

                    if (location != null) {
                        final Location pos = location;
                        if (enemy.spawn(this, world, pos)) {
                            RaidSpawnerUtil.d(() -> "spawn " + enemy.getProvider().getSource() + " in " + world.getName() + " " + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ());
                        } else {
                            RaidSpawnerUtil.d(() -> "cannot spawn");
                            it.remove();
                        }
                        break;
                    }
                }
            }
        }
    }

    private @Nullable Location selectRandomSpawnLocationByChunk(org.bukkit.Chunk chunk, Random random, boolean ignoreBlockTest) {
        for (int i = 0; i < 8; i++) {  // limit 8 tests
            int blockX = 4 + random.nextInt(8);
            int blockZ = 4 + random.nextInt(8);
            Block block = chunk.getWorld().getHighestBlockAt(chunk.getX() << 4 | blockX & 0xF, chunk.getZ() << 4 | blockZ & 0xF);

            if (!ignoreBlockTest && (Material.WATER.equals(block.getType()) || Material.LAVA.equals(block.getType())))
                continue;

            return block.getLocation().add(.5, 1, .5);
        }
        return null;
    }


    public record Chunk(Land land, World world, int x, int z) {
        public String toString() {
            return x + "," + z + "," + world.getName();
        }

        public org.bukkit.Chunk getBukkitChunk() {
            return world.getChunkAt(x, z);
        }
    }

    public record Rewards(
            List<ConditionWrapper> rewardConditions,
            List<Action> noConditionWinActions,
            List<Action> loseActions
    ) {}

}

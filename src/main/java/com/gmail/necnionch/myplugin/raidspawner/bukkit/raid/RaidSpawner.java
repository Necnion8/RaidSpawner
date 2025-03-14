package com.gmail.necnionch.myplugin.raidspawner.bukkit.raid;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerAPI;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.Action;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.BossBarSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.MobSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnStartEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks.LuckPermsBridge;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks.PluginBridge;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.lang.Lang;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.framework.blockutil.UnloadedPosition;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class RaidSpawner {

    public static final Set<UUID> spawnedEntities = new HashSet<>();
    public static final Multimap<KeepChunk, UUID> keepChunksByEntities = LinkedHashMultimap.create();

    private final RaidSpawnerAPI api;
    private final Random random = new Random();
    private final Land land;
    private final RaidSetting setting;
    private final World world;
    private final List<Chunk> spawnChunks;
    private final Rewards rewards;
    private final List<ChunkCoordinate> landChunks;
    private boolean running;
    private int waves;
    private long endTime = -1;  // 負の値で開始時刻; 正の値で終了にかかった時間(ms)
    private int deathCount;  // プレイヤーの死亡回数
    private @Nullable KeyedBossBar bossBar;
    private @Nullable BukkitTask gameTickTask;
    private @Nullable BukkitTask currentWaveMaxTimer;
    private final List<Enemy> currentEnemies = new ArrayList<>();
    private @Nullable RaidEndResult endResult;
    private @Nullable RaidEndReason endReason;
    private @Nullable Map<Class<Action>, Boolean> endActionResults;

    public RaidSpawner(RaidSpawnerAPI api, Land land, RaidSetting setting, World world, List<Chunk> spawnChunks, List<ChunkCoordinate> landChunks, Rewards rewards) {
        this.api = api;
        this.land = land;
        this.setting = setting;
        this.world = world;
        this.spawnChunks = Collections.unmodifiableList(spawnChunks);
        this.landChunks = Collections.unmodifiableList(landChunks);
        this.rewards = rewards;
    }

    public Land getLand() {
        return land;
    }

    public RaidSetting getSetting() {
        return setting;
    }

    public Rewards getRewards() {
        return rewards;
    }

    public List<Chunk> getSpawnChunks() {
        return spawnChunks;
    }

    public List<ChunkCoordinate> getLandChunks() {
        return landChunks;
    }

    public World getWorld() {
        return world;
    }

    public @Nullable KeyedBossBar getBossBar() {
        return bossBar;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isLose() {
        return RaidEndResult.LOSE.equals(endResult);
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

    @Nullable
    public RaidEndResult getEndResult() {
        return endResult;
    }

    @Nullable
    public RaidEndReason getEndReason() {
        return endReason;
    }

    /**
     * 終了アクションの実行結果を返します
     */
    @Nullable
    public Map<Class<Action>, Boolean> getEndActionResults() {
        return endActionResults;
    }

    /**
     * イベント終了までにかかった時間(ミリ秒)
     * @return 終了していない場合は -1 を返します
     */
    public long getEndTime() {
        return endTime < 0 ? -1 : endTime;
    }

    /**
     * プレイヤーの死亡回数を返します
     */
    public int getDeathCount() {
        return deathCount;
    }

    /**
     * チケット数(死亡回数制限)を返します
     * @return {@link RaidSetting#tickets()} から {@link #getDeathCount()} を引いた値
     */
    public int getDeathCountTickets() {
        return setting.tickets() - deathCount;
    }

    /**
     * チケット(死亡回数制限)が有効なら true を返します
     */
    public boolean isEnableDeathCountTickets() {
        return 0 < setting.tickets();
    }

    /**
     * プレイヤーを襲撃イベント地点へテレポートします<br>
     * このワールド上に設定されているLandスポーン または、保有チャンクのランダムな位置にテレポートします。
     */
    public void teleportToSpawn(Collection<Player> players) {
        UnloadedPosition pos = land.getSpawnPosition();

        if (pos != null && pos.isTargetServer() && world.getName().equals(pos.getWorldName())) {
            Location location = pos.toLocation();
            if (location != null) {
                players.forEach(p -> p.teleport(location));
                return;
            }
        }

        if (landChunks.isEmpty())
            return;

        ChunkCoordinate chunk = landChunks.get(random.nextInt(landChunks.size()));
        Location location = selectRandomSpawnLocationByChunk(world.getChunkAt(chunk.getX(), chunk.getZ()), random, false);
        if (location != null) {
            players.forEach(p -> p.teleport(location));
        }
    }

    /**
     * 指定されたプレイヤーがこの襲撃イベントに関係するか
     */
    public boolean containsPlayer(Player player) {
        return containsPlayer(player.getUniqueId());
    }

    /**
     * 指定されたプレイヤーがこの襲撃イベントに関係するか
     */
    public boolean containsPlayer(UUID playerId) {
        return land.isTrusted(playerId);
    }

    /**
     * 指定された座標にもっとも近いプレイヤーを返します
     */
    public Optional<Player> findNearestPlayer(Location location) {
        return land.getOnlinePlayers().stream()
                .min(Comparator.comparingDouble(p -> p.getLocation().distance(location)));
    }

    /**
     * ウェーブ数を変更します
     * @throws IllegalArgumentException 開始していないイベント、または newWaves の値が無効
     */
    public void setWaves(int newWaves) {
        if (!running)
            throw new IllegalArgumentException("Not running raid");
        if (newWaves <= 0 || getMaxWaves() < newWaves)
            throw new IllegalArgumentException("Valid values are 1 to " + getMaxWaves());

        if (newWaves == waves)
            return;

        RaidSpawnerUtil.d(() -> "setWave to " + newWaves);
        waves = newWaves - 1;
        tryNextWave();
    }

    /**
     * 襲撃イベントを開始します
     */
    public void start() {
        running = true;
        endTime = -System.currentTimeMillis();
        deathCount = 0;
        RaidSpawnerUtil.getLogger().info("Raid started: " + land.getName());

        Collection<Player> players = land.getOnlinePlayers();
        api.getPluginLang().send(players, Lang.START_MESSAGE);

        Optional.ofNullable(createAndInitBossBar()).ifPresent(b -> players.forEach(b::addPlayer));

        if (api.getPluginConfig().isTeleportToLandInStart()) {
            teleportToSpawn(players);
        }

        Bukkit.getPluginManager().callEvent(new RaidSpawnStartEvent(this));
        rewards.rewardConditions.forEach(ConditionWrapper::start);

        String groupName = setting.luckPermsGroup();
        if (groupName != null) {
            PluginBridge.getValid(LuckPermsBridge.class).ifPresent(perms -> {
                for (Player player : players) {
                    perms.addPermissionGroup(player, groupName);
                }
            });
        }

        tryNextWave();

        if (gameTickTask != null) {
            gameTickTask.cancel();
        }
        gameTickTask = RaidSpawnerUtil.runTaskTimer(this::tick, 0);
    }

    /**
     * 襲撃イベントを終了します
     */
    public void clear(RaidEndResult result, @Nullable RaidEndReason reason) {
        RaidSpawnerUtil.getLogger().info("Raid ended: " + land.getName() + " (" + result.name() + ", " + Optional.ofNullable(reason).map(RaidEndReason::getType).orElse("none") + ")");
        endResult = result;
        endReason = reason;
        running = false;
        if (endTime < 0) {
            endTime = System.currentTimeMillis() + endTime;
        }

        if (gameTickTask != null) {
            gameTickTask.cancel();
            gameTickTask = null;
        }

        if (currentWaveMaxTimer != null) {
            currentWaveMaxTimer.cancel();
            currentWaveMaxTimer = null;
        }

        removeBossBar();

        Map<Class<Action>, Boolean> actionResults = null;
        try {
            actionResults = api.executeActions(this, result);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        this.endActionResults = actionResults;

        rewards.rewardConditions.forEach(ConditionWrapper::clear);
        currentEnemies.forEach(enemy -> {
            Optional.ofNullable(enemy.getEntity())
                    .map(Entity::getUniqueId)
                    .ifPresent(RaidSpawner::unsetKeepChunkWithEntity);
            try {
                enemy.remove();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (enemy instanceof Listener) {
                HandlerList.unregisterAll((Listener) enemy);
            }
            try {
                enemy.unload();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        currentEnemies.clear();

        Collection<Player> players = land.getOnlinePlayers();
        Lang message = null;
        if (RaidEndResult.CANCEL.equals(result)) {
            message = Lang.END_CANCEL_MESSAGE;
        } else if (RaidEndResult.WIN.equals(result)) {
            message = Lang.END_WIN_MESSAGE;
        } else if (RaidEndResult.LOSE.equals(result)) {
            if (RaidEndReason.TIMEOUT.equals(reason)) {
                message = Lang.END_LOSE_TIMEOUT_MESSAGE;
            } else if (RaidEndReason.NO_TICKETS.equals(reason)) {
                message = Lang.END_LOSE_NO_TICKETS_MESSAGE;
            } else {
                message = Lang.END_LOSE_MESSAGE;
            }
        }
        if (message != null) {
            api.getPluginLang().send(players, message);
        }

        Bukkit.getPluginManager().callEvent(new RaidSpawnEndEvent(this, result, reason));

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

    /**
     * 襲撃イベントを敗北として終了します<br>
     * @see #clear(RaidEndResult, RaidEndReason)
     */
    public void clearSetLose(@Nullable RaidEndReason reason) {
        clear(RaidEndResult.LOSE, reason);
    }


    public void onDeathPlayer(Player player) {
        if (!containsPlayer(player))
            return;

        if (isEnableDeathCountTickets() && (setting.tickets() - ++deathCount) <= 0) {
            clearSetLose(RaidEndReason.NO_TICKETS);
        }
    }

    public void onDeathEntity() {
        currentEnemies.stream()
                .filter(e -> !e.isAlive())
                .map(Enemy::getEntity)
                .filter(Objects::nonNull)
                .map(Entity::getUniqueId)
                .forEach(RaidSpawner::unsetKeepChunkWithEntity);

        if (running) {
            long aliveCount = currentEnemies.stream().filter(Enemy::isAlive).count();

            if (aliveCount <= 0) {
                RaidSpawnerUtil.d(() -> " -> no alive, to next");
                tryNextWave();

            } else if (aliveCount <= setting.mobsGrowingEnemies()) {
                PotionEffect growing = new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false);
                currentEnemies.stream()
                        .filter(Enemy::isAlive)
                        .map(Enemy::getEntity)
                        .filter(Objects::nonNull)
                        .forEach(e -> {
                            if (e instanceof LivingEntity) {
                                growing.apply((LivingEntity) e);
                            }
                        });
            }
        }
    }


    /**
     * 可能なら次のウェーブに移行します
     * @param fullWaveToWin 最大ウェーブに達したら勝利
     */
    public void tryNextWave(boolean fullWaveToWin) {
        if (!running)
            return;

        RaidSpawnerUtil.d(() -> "tryNextWave | now wave: " + waves + " | land: " + land.getName());

        if (waves < setting.maxWaves()) {
            waves++;
            RaidSpawnerUtil.d(() -> "waves: " + waves);
            doWave();

        } else if (fullWaveToWin) {
            clear(RaidEndResult.WIN, RaidEndReason.FULL_WAVES);
        }

    }

    /**
     * 可能なら次のウェーブに移行します。最大ウェーブに達したら勝利としてイベントを終了します。
     * @see #tryNextWave(boolean)
     */
    public void tryNextWave() {
        tryNextWave(true);
    }


    /**
     * ウェーブの開始処理をします
     */
    private void doWave() {
        currentEnemies.stream()
                .filter(e -> !e.isAlive())
                .map(Enemy::getEntity)
                .filter(Objects::nonNull)
                .map(Entity::getUniqueId)
                .forEach(RaidSpawner::unsetKeepChunkWithEntity);

        currentEnemies.removeIf(enemy -> {
            try {
                enemy.unload();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return !enemy.isAlive();
        });  // keep alive

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
            RaidSpawnerUtil.d(() -> "- enemy: source " + enemyItem.getSource());

            EnemyProvider<?> provider = enemyItem.getProvider();
            if (provider == null) {
                RaidSpawnerUtil.d(() -> "no provided");
            } else {
                Enemy enemy;
                try {
                    enemy = provider.create(enemyItem.getConfig());
                } catch (EnemyProvider.ConfigurationError e) {
                    RaidSpawnerUtil.getLogger().severe("Invalid enemy config: " + provider.getSource() + ": " + e.getMessage());
                    continue;
                } catch (Throwable e) {
                    RaidSpawnerUtil.getLogger().log(Level.SEVERE, "Invalid enemy config: " + provider.getSource(), e);
                    continue;
                }
                currentEnemies.add(enemy);
                if (enemy instanceof Listener) {
                    RaidSpawnerPlugin plugin = (RaidSpawnerPlugin) api;
                    plugin.getServer().getPluginManager().registerEvents((Listener) enemy, plugin);
                }
            }
        }

        // summon
        RaidSpawnerUtil.d(() -> "total enemies " + currentEnemies.size());
        for (Iterator<Enemy> it = currentEnemies.iterator(); it.hasNext(); ) {
            Enemy enemy = it.next();
            if (!enemy.isAlive()) {
                int searchLimit = 8;
                Location location;

                for (int i = 0; i < searchLimit; i++) {
                    Chunk chunk = spawnChunks.get(random.nextInt(spawnChunks.size()));
                    location = selectRandomSpawnLocationByChunk(chunk.getBukkitChunk(), random, searchLimit <= i + 1);
                    if (location == null)
                        continue;

                    final Location pos = location;
                    Entity spawned = enemy.spawn(this, world, pos);
                    if (spawned != null) {
                        RaidSpawnerUtil.d(() -> "spawn " + enemy.getProvider().getSource() + " in " + world.getName() + " " + pos.getBlockX() + ", " + pos.getBlockY() + ", " + pos.getBlockZ());
                        setKeepChunkWithEntity(spawned.getUniqueId());
                    } else {
                        RaidSpawnerUtil.d(() -> "cannot spawn");
                        it.remove();
                    }
                    break;
                }
            }
        }

        // set wave timer
        if (currentWaveMaxTimer != null) {
            currentWaveMaxTimer.cancel();
        }
        if (0 < setting.maxWaveTimeMinutes()) {
            currentWaveMaxTimer = RaidSpawnerUtil.runTaskLater(this::onWaveMaxTimer, setting.maxWaveTimeMinutes() * 60L * 20);
        }
    }

    /**
     * チャンクの範囲内でランダムな位置を返します
     * @param chunk 対象のチャンク
     * @param ignoreBlockTest ブロックをテストしません (false の場合は水や溶岩を避けます)
     */
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

    /**
     * ボスバーを作成します。既存のボスバーは削除されます。
     */
    private @Nullable KeyedBossBar createAndInitBossBar() {
        removeBossBar();
        BossBarSetting setting = this.setting.bossBar();
        if (setting.enable()) {
            NamespacedKey bossBarKey = new NamespacedKey(RaidSpawnerUtil.getPlugin(), "raid_" + UUID.randomUUID().toString().replace("-", ""));
            bossBar = Bukkit.createBossBar(bossBarKey, null, setting.color(), setting.style());
            return bossBar;
        }
        return null;
    }

    /**
     * ボスバーを非表示にしてサーバーから削除します
     */
    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
            Bukkit.removeBossBar(bossBar.getKey());
        }
    }

    private void updateBossBar() {
        if (bossBar == null)
            return;

        // progress
        long maxTime = setting.eventTimeMinutes() * 60L * 1000;
        double progress;
        if (maxTime == 0) {
            progress = 0;
        } else if (endTime < 0) {
            long gameTime = System.currentTimeMillis() + endTime;
            progress = (double) gameTime / maxTime;
        } else {
            progress = (double) endTime / maxTime;
        }
        bossBar.setProgress(1 - progress);

        // title
        String text = ChatColor.translateAlternateColorCodes('&', setting.bossBar().text())
                .replaceAll("%wave%", String.valueOf(waves))
                .replaceAll("%max_waves%", String.valueOf(getMaxWaves()))
                .replaceAll("%enemies%", String.valueOf(currentEnemies.stream().filter(Enemy::isAlive).count()))
                .replaceAll("%total_enemies%", String.valueOf(currentEnemies.size()))
                .replaceAll("%tickets%", String.valueOf(getDeathCountTickets()));
        bossBar.setTitle(text);

        // show
        if (!bossBar.isVisible()) {
            bossBar.setVisible(true);
        }
    }

    private void onWaveMaxTimer() {
        // ウェーブタイマーが経過したら次のウェーブに移動する
        tryNextWave(false);
    }

    private void tick() {
        updateBossBar();
    }


    private static void setKeepChunkWithEntity(UUID entityId) {
        spawnedEntities.add(entityId);
    }

    private static void unsetKeepChunkWithEntity(UUID entityId) {
        if (!keepChunksByEntities.containsValue(entityId))
            return;

        for (Iterator<Map.Entry<KeepChunk, Collection<UUID>>> it = keepChunksByEntities.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<KeepChunk, Collection<UUID>> e = it.next();
            KeepChunk key = e.getKey();
            Collection<UUID> entities = e.getValue();
            if (entities.contains(entityId)) {
                if (entities.size() == 1) {
                    it.remove();
                    boolean removed = key.world.removePluginChunkTicket(key.x, key.z, RaidSpawnerUtil.getPlugin());
                    RaidSpawnerUtil.d(() -> "removeChunkTicket : " + key + " | " + removed);
                } else {
                    entities.remove(entityId);
                }
            }
        }
    }

    public static void onEntitiesUnloadEvent(EntitiesUnloadEvent event) {
        KeepChunk key = new KeepChunk(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
        for (Entity entity : event.getEntities()) {
            if (spawnedEntities.contains(entity.getUniqueId())) {
                // keep
                boolean added = event.getChunk().addPluginChunkTicket(RaidSpawnerUtil.getPlugin());
                RaidSpawnerUtil.d(() -> "addChunkTicket : " + key + " | " + added);
                unsetKeepChunkWithEntity(entity.getUniqueId());  // cleanup olds
                keepChunksByEntities.put(key, entity.getUniqueId());
            }
        }
    }


    public record KeepChunk(World world, int x, int z) {
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
            List<Action> startActions,
            List<Action> noConditionWinActions,
            List<Action> loseActions
    ) {}

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.Actions;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSpawnerConfig;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnsPreStartEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks.PlaceholderReplacer;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.MythicEnemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.TestEnemy;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.framework.blockutil.UnloadedPosition;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.player.LandPlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class RaidSpawnerPlugin extends JavaPlugin implements Listener {
    private final RaidSpawnerConfig pluginConfig = new RaidSpawnerConfig(this);
    private final Timer timer = new Timer("RaidSpawner-Timer", true);
    private final Map<String, ConditionProvider<?>> conditionProviders = new HashMap<>();
    private final Map<String, ActionProvider<?>> actionProviders = new HashMap<>();
    private final Map<String, EnemyProvider<?>> enemyProviders = new HashMap<>();
    private PlaceholderReplacer placeholderReplacer = (p, s) -> s;
    //
    private final List<ConditionWrapper> startConditions = new ArrayList<>();
    private final Map<Land, RaidSpawner> raids = new HashMap<>();
    private @Nullable Multimap<String, RaidSpawner.Chunk> lastFindSpawnChunksResult;
    //
    private @Nullable LandsIntegration lands;
    private @Nullable BukkitTask gameEndTimer;

    @Override
    public void onLoad() {
        conditionProviders.clear();
        actionProviders.clear();
    }

    @Override
    public void onEnable() {
        lands = LandsIntegration.of(this);
        setupInternalProviders();

        if (pluginConfig.load()) {
            createStartConditions();
            startStartConditions();
        } else {
            // show after server startup
            getServer().getScheduler().runTask(this, () -> getLogger().warning(
                    "There is a configuration error, please fix configuration and reload."));
        }

        hookPlaceholderAPI();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Active condition types: " + String.join(", ", conditionProviders.keySet()));
        getLogger().info("Active action types: " + String.join(", ", actionProviders.keySet()));
        getLogger().info("Active enemy types: " + String.join(", ", enemyProviders.keySet()));
    }

    @Override
    public void onDisable() {
        try {
            clearStartConditions();
            clearRaidAll();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Exception in raids clear", e);
        }

        timer.cancel();

        try {
            startConditions.forEach(ConditionWrapper::unload);
            startConditions.clear();

            conditionProviders.clear();
            actionProviders.clear();

        } finally {
            lands = null;
            timer.purge();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // TODO: impl command
        if (args.length == 0) {
            sender.sendMessage("currentRaids: " + raids.size());
            if (!raids.isEmpty()) {
                sender.sendMessage(ChatColor.AQUA + "=== Raids ===");
                for (RaidSpawner spawner : raids.values()) {
                    sender.sendMessage(ChatColor.GOLD + "- raid: " + spawner.getLand().getName() + " land");
                    sender.sendMessage("  wave: " + spawner.getWave() + "/" + spawner.getMaxWaves());
                    List<Enemy> enemies = spawner.currentEnemies();
                    sender.sendMessage("  alive: " + enemies.stream().filter(Enemy::isAlive).count() + "/" + enemies.size());
                    sender.sendMessage("  running: " + spawner.isRunning() + " (lose: " + spawner.isLose() + ")");
                }
            }

            List<ConditionWrapper> cond = startConditions;
            if (!cond.isEmpty()) {
                sender.sendMessage(ChatColor.AQUA + "=== Auto Start ===");
                for (ConditionWrapper c : cond) {
                    sender.sendMessage(ChatColor.GOLD + "- type: " + c.getType());
                    sender.sendMessage("  activated: " + c.isActivated());
                    int remaining = Optional.ofNullable(c.getCondition().getRemainingTimePreview())
                            .map(v -> Math.round((float) v / 1000))
                            .orElse(-1);
                    sender.sendMessage("  remaining: " + remaining);
                }
            }

        } else if (sender instanceof Player && args.length == 1 && args[0].equalsIgnoreCase("me")) {
            LandPlayer landPlayer = getLandAPI().getLandPlayer(((Player) sender).getUniqueId());
            if (landPlayer == null) {
                sender.sendMessage(ChatColor.RED + "No Land Player");
                return true;
            }

            ArrayList<? extends Land> lands = new ArrayList<>(landPlayer.getLands());
            if (lands.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No Land joined");
                return true;
            }

            Land land = lands.get(0);
            sender.sendMessage("Start land raid: " + land.getName() + " (total " + lands.size() + " lands)");

            sender.sendMessage("result: " + startRaid(land));

        } else if (sender instanceof Player && args.length == 1 && args[0].equalsIgnoreCase("findchunks")) {
            onFindChunkCommand((Player) sender);

        } else if (sender instanceof Player && args.length == 1 && args[0].equalsIgnoreCase("givemap")) {
            onGiveMapCommand((Player) sender);
        }

        return true;
    }

    private void onFindChunkCommand(Player p) {
        findLandsRaidChunks(getLandAPI().getLands()).forEach((land, chunks) -> {
            p.sendMessage("- " + land.getName() + " -> spawn " + chunks.size() + " chunks");
        });
        ChunkViewRenderer.RENDERERS.forEach(ChunkViewRenderer::updateLandsList);
    }

    private void onGiveMapCommand(Player p) {
        PlayerInventory inv = p.getInventory();

        MapView view;
        ItemStack itemStack;
        MapMeta itemMeta;
        ItemStack mainHandItem = inv.getItemInMainHand();
        if (Material.FILLED_MAP.equals(mainHandItem.getType())) {
            // override map
            itemStack = mainHandItem;
            view = ((MapMeta) mainHandItem.getItemMeta()).getMapView();
        } else {
            itemStack = new ItemStack(Material.FILLED_MAP);
            view = Bukkit.createMap(p.getWorld());
        }
        itemMeta = (MapMeta) itemStack.getItemMeta();

        view.setScale(MapView.Scale.NORMAL);
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(new ChunkViewRenderer(this, p.getWorld()));
        itemMeta.setMapView(view);

        itemStack.setItemMeta(itemMeta);

        if (!itemStack.equals(mainHandItem)) {
            inv.setItemInMainHand(itemStack);
            inv.addItem(mainHandItem);
        }
        p.updateInventory();

        p.sendMessage("scale: " + view.getScale().name() + ", " + view.getCenterX());
    }

    private Function<Land, World> getLandSpawnOrConfigWorld() {
        String worldName = pluginConfig.getRaidSetting().world();
        if (worldName != null) {
            World world = getServer().getWorld(worldName);
            if (world == null)
                throw new IllegalArgumentException("World '" + worldName + "` is not loaded");
            return l -> world;
        }

        return land -> Optional.ofNullable(land.getSpawnPosition())
                .map(UnloadedPosition::getWorld)
                .orElse(null);
    }

    private Map<Land, List<RaidSpawner.Chunk>> findLandsRaidChunks(Collection<Land> lands) {
        int distanceChunks = pluginConfig.getRaidSetting().mobsDistanceChunks();
        Function<Land, World> worlds = getLandSpawnOrConfigWorld();  // throws IllegalArgumentException

        Map<Land, List<RaidSpawner.Chunk>> landsRaidChunks = new HashMap<>();
        Set<String> safeChunks = new HashSet<>();

        for (Land land : lands) {
            World world = worlds.apply(land);
            if (world == null) {
                getLogger().severe("Land '" + land.getName() + "' spawn world is null");
                continue;
            }

            List<RaidSpawner.Chunk> chunks = new ArrayList<>();
            landsRaidChunks.put(land, chunks);

            for (Container container : land.getContainers()) {
                for (ChunkCoordinate chunk : container.getChunks()) {
                    // safeチャンクをマークする
                    for (int x = chunk.getX() - distanceChunks; x <= chunk.getX() + distanceChunks; x++) {
                        for (int z = chunk.getZ() - distanceChunks; z <= chunk.getZ() + distanceChunks; z++) {
                            safeChunks.add(x + "," + z + "," + world.getName());
                        }
                    }

                    if (world.equals(container.getWorld().getWorld())) {
                        // spawnチャンクをマーク
                        int minX = chunk.getX() - distanceChunks - 1;
                        int maxX = chunk.getX() + distanceChunks + 1;
                        int minZ = chunk.getZ() - distanceChunks - 1;
                        int maxZ = chunk.getZ() + distanceChunks + 1;
                        for (int i = 0; i < distanceChunks * 2 + 2 + 1; i++) {
                            chunks.add(new RaidSpawner.Chunk(land, world, minX + i, minZ));
                            chunks.add(new RaidSpawner.Chunk(land, world, maxX, minZ + i));
                            chunks.add(new RaidSpawner.Chunk(land, world, maxX - i, maxZ));
                            chunks.add(new RaidSpawner.Chunk(land, world, minX, maxZ - i));
                        }
                    }

                }
            }
        }

        lastFindSpawnChunksResult = LinkedHashMultimap.create();
        for (Iterator<List<RaidSpawner.Chunk>> it = landsRaidChunks.values().iterator(); it.hasNext(); ) {
            List<RaidSpawner.Chunk> landRaidChunks = it.next();
            landRaidChunks.removeIf(c -> safeChunks.contains(c.toString()));
            if (landRaidChunks.isEmpty()) {
                it.remove();
            } else {
                landRaidChunks.forEach(chunk -> lastFindSpawnChunksResult.put(chunk.toString(), chunk));
            }
        }

        return landsRaidChunks;
    }

    @Nullable
    public Multimap<String, RaidSpawner.Chunk> getLastFindSpawnChunksResult() {
        return lastFindSpawnChunksResult;
    }

    public boolean hookPlaceholderAPI() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            setPlaceholderReplacer(PlaceholderAPI::setPlaceholders);
            return true;
        }
        return false;
    }

    public void setupInternalProviders() {
        Stream.of(
                new RealClockCondition.Provider(),
                new TimerCondition.Provider()
        )
                .forEachOrdered(cond -> conditionProviders.put(cond.getType(), cond));

        Stream.of(
                new LandRemoveChunkAction.Provider(),
                new PlayerCommandAction.Provider(),
                new PlayerExecuteCommandAction.Provider(),
                new LandCommandAction.Provider(),
                PlayerAddMoneyAction.Provider.createAndHookEconomy(this),
                PlayerRemoveMoneyAction.Provider.createAndHookEconomy(this)
        )
                .filter(Objects::nonNull)
                .forEachOrdered(action -> actionProviders.put(action.getType(), action));

        Stream.of(
                new TestEnemy.Provider(),
                MythicEnemy.Provider.createAndHookMythicMobs(this)
        )
                .filter(Objects::nonNull)
                .forEachOrdered(provider -> enemyProviders.put(provider.getSource(), provider));
    }

    public @NotNull LandsIntegration getLandAPI() {
        return Objects.requireNonNull(lands, "LandsIntegration is not hooked");
    }

    public Map<String, ConditionProvider<?>> conditionProviders() {
        return conditionProviders;
    }

    public Map<String, ActionProvider<?>> actionProviders() {
        return actionProviders;
    }

    public Map<String, EnemyProvider<?>> enemyProviders() {
        return enemyProviders;
    }

    // util

    public PlaceholderReplacer getPlaceholderReplacer() {
        return placeholderReplacer;
    }

    public void setPlaceholderReplacer(PlaceholderReplacer replacer) {
        this.placeholderReplacer = replacer;
    }

    public Condition createCondition(ConfigurationSection conditionConfig) throws IllegalArgumentException, ConditionProvider.ConfigurationError {
        String condType = conditionConfig.getString("type");
        if (!conditionProviders.containsKey(condType)) {
            throw new IllegalArgumentException("Unknown condition type: " + condType);
        }
        return conditionProviders.get(condType).create(conditionConfig);
    }

    public Action createAction(String type, Object value, @Nullable ConfigurationSection config) throws IllegalArgumentException, ActionProvider.ConfigurationError {
        if (!actionProviders.containsKey(type)) {
            throw new IllegalArgumentException(("Unknown action type: " + type));
        }
        return actionProviders.get(type).create(value, config);
    }

    // event start condition

    public void createStartConditions() {
        clearStartConditions();
        for (ConfigurationSection condConfig : pluginConfig.getStartConditions()) {
            String type = condConfig.getString("type");
            Condition condition;
            try {
                condition = createCondition(condConfig);
            } catch (IllegalArgumentException e) {
                getLogger().severe(e.getMessage());
                continue;
            } catch (ConditionProvider.ConfigurationError e) {
                getLogger().severe("Error condition config (in event-start, type " + type + "): " + e);
                continue;
            }
            startConditions.add(new ConditionWrapper(timer, condition, this::onStartTrigger));
        }
    }

    public void startStartConditions() {
        startConditions.forEach(ConditionWrapper::start);
    }

    public void clearStartConditions() {
        startConditions.forEach(ConditionWrapper::clear);
    }

    private void onStartTrigger(ConditionWrapper condition) {
        if (isRunningRaid()) {
            getLogger().warning("Already running raids (ignored)");
            return;
        }

        clearStartConditions();
        startRaidAll(condition.getCondition());
    }

    // raids

    /**
     * いずれかの襲撃イベントを実行している場合は true を返す
     */
    public boolean isRunningRaid() {
        return !raids.isEmpty();
    }

    /**
     * 指定されたLandで襲撃イベントを実行している場合は true を返す
     */
    public boolean isRunningRaid(Land land) {
        return raids.containsKey(land);
    }

    private boolean startRaidAll(@Nullable Condition reason) {
        if (isRunningRaid())
            throw new IllegalStateException("Already running raids");

        raids.clear();
        findLandsRaidChunks(getLandAPI().getLands())
                .forEach((key, value) -> raids.put(key, createRaidSpawner(value)));

        RaidSpawnsPreStartEvent myEvent = new RaidSpawnsPreStartEvent(raids.values(), reason);
        getServer().getPluginManager().callEvent(myEvent);
        if (myEvent.isCancelled() || raids.isEmpty()) {
            return false;
        }

        processRaidStart();
        return true;
    }

    private void clearRaidAll() {
        if (!isRunningRaid())
            return;

        if (gameEndTimer != null) {
            gameEndTimer.cancel();
            gameEndTimer = null;
        }

        raids.values().forEach(RaidSpawner::clear);
        raids.clear();
        getLogger().info("Raid Spawner Ended");
    }

    private void clearRaidAllAndRestart() {
        clearRaidAll();
        clearStartConditions();
        startStartConditions();
    }

    private boolean startRaid(Land land) {
        if (raids.containsKey(land))
            throw new IllegalStateException("Already running raids");

        if (!getLandAPI().getLands().contains(land))
            throw new IllegalStateException("Invalid land (no contains lands api)");

        World world = getLandSpawnOrConfigWorld().apply(land);
        if (world == null)
            throw new IllegalArgumentException("Land '" + land.getName() + "` spawn world is null");

        findLandsRaidChunks(Collections.singleton(land))
                .forEach((key, value) -> raids.put(key, createRaidSpawner(value)));
        return true;
    }

    private void processRaidStart() {
        clearStartConditions();
        raids.values().forEach(RaidSpawner::start);
        getLogger().info("Raid Spawner Started");

        // delay 1 tick
        RaidSpawnerUtil.runInMainThread(() -> {
            for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
                if (spawner.getLand().getOnlinePlayers().isEmpty()) {
                    spawner.clearSetLose();
                }
            }

        });

        // game end
        if (gameEndTimer != null) {
            gameEndTimer.cancel();
        }
        gameEndTimer = getServer().getScheduler().runTaskLater(this, () -> {
            new ArrayList<>(raids.values()).forEach(RaidSpawner::clearSetLose);
        }, 20L * 60 * pluginConfig.getRaidSetting().eventTimeMinutes());
    }


    private RaidSpawner createRaidSpawner(List<RaidSpawner.Chunk> raidChunks) {
        RaidSpawner.Chunk first = raidChunks.iterator().next();
        return createRaidSpawner(first.land(), first.world(), raidChunks);
    }

    private RaidSpawner createRaidSpawner(Land land, World world, List<RaidSpawner.Chunk> spawnChunks) {
        List<ConditionWrapper> conditions = new ArrayList<>();

        for (RaidSpawnerConfig.ConditionItem item : pluginConfig.getWinRewardConditions()) {
            // create condition
            String type = item.config().getString("type");
            ConditionWrapper condition;
            try {
                condition = new ConditionWrapper(timer, createCondition(item.config()), w -> {});
            } catch (IllegalArgumentException e) {
                getLogger().severe(e.getMessage());
                continue;
            } catch (ConditionProvider.ConfigurationError e) {
                getLogger().severe("Error condition config (in win, type " + type + "): " + e);
                continue;
            }

            // create actions
            for (Actions.Item aItem : item.actions().getPlayerActions()) {
                try {
                    condition.actions().add(createAction(aItem.type(), aItem.value(), aItem.config()));
                } catch (IllegalArgumentException e) {
                    getLogger().severe(e.getMessage());
                } catch (ActionProvider.ConfigurationError e) {
                    getLogger().severe("Error action config (in win, type " + type + "): " + e);
                }
            }

            conditions.add(condition);
        }

        return new RaidSpawner(land, conditions, pluginConfig.getRaidSetting(), world, spawnChunks);
    }


    public void sendReward(RaidSpawner spawner, RaidSpawnEndEvent.Result result) {
        // TODO: 必要な処理をイベント開始前に作成しとく
        List<Action> actions = new ArrayList<>();
        switch (result) {
            case LOSE -> {
                for (Actions.Item item : pluginConfig.getLoseRewardActions().getLandActions()) {
                    String type = item.type();
                    try {
                        actions.add(createAction(type, item.value(), item.config()));
                    } catch (IllegalArgumentException e) {
                        getLogger().severe(e.getMessage());
                    } catch (ActionProvider.ConfigurationError e) {
                        getLogger().severe("Error action config (in lose, type " + type + "): " + e);
                    }
                }
            }
            case WIN -> {
                for (ConditionWrapper condition : spawner.conditions()) {
                    if (condition.isActivated()) {  // TODO: timer条件の場合は反転しなければならない
                        actions.addAll(condition.actions());
                        break;
                    }
                }
            }
            default -> {
                return;
            }
        }

        for (Action action : actions) {
            if (action instanceof LandAction) {
                try {
                    ((LandAction) action).doAction(spawner, spawner.getLand());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } else if (action instanceof PlayerAction) {
                for (Player player : spawner.getLand().getOnlinePlayers()) {
                    try {
                        ((PlayerAction) action).doAction(spawner, player);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    // event

    @EventHandler(priority = EventPriority.HIGH)
    public void onEndRaid(RaidSpawnEndEvent event) {
        if (raids.values().remove(event.getRaid())) {
            getLogger().info("Raid ended: " + event.getLand().getName() + " (" + event.getResult().name() + ")");

            try {
                sendReward(event.getRaid(), event.getResult());
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (raids.isEmpty()) {
                getLogger().info("Auto start conditions restarting");
                startStartConditions();
            }
        }
    }

    @EventHandler
    public void onQuitPlayer(PlayerQuitEvent event) {
        for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
            Land land = spawner.getLand();
            if (land.getOnlinePlayers().isEmpty()) {
                spawner.clearSetLose();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeathEntity(EntityDeathEvent event) {
        for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
            if (spawner.currentEnemies().stream().noneMatch(Enemy::isAlive)) {
                System.out.println(" -> no alive, to next");
                spawner.tryNextWave();
            }
        }

    }


}

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.Actions;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.EventStart;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSpawnerConfig;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnsAllEndEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnsPreStartEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnsPreStartNotifyEvent;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.lang.Lang;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.lang.RaidSpawnerLang;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.map.ChunkViewRenderer;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.EnemyProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.MythicEnemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.TestEnemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.LandChunkFindResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndReason;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.framework.blockutil.UnloadedPosition;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class RaidSpawnerPlugin extends JavaPlugin implements Listener, RaidSpawnerAPI {
    private final RaidSpawnerConfig pluginConfig = new RaidSpawnerConfig(this);
    private final RaidSpawnerLang pluginLang = new RaidSpawnerLang(this);
    private final Timer timer = new Timer("RaidSpawner-Timer", true);
    private final Map<String, ConditionProvider<?>> conditionProviders = new HashMap<>();
    private final Map<String, ActionProvider<?>> landActionProviders = new HashMap<>();
    private final Map<String, ActionProvider<?>> playerActionProviders = new HashMap<>();
    private final Map<String, EnemyProvider<?>> enemyProviders = new HashMap<>();
    private PlaceholderReplacer placeholderReplacer = (p, s) -> s;
    private boolean enableDebug;
    //
    private final List<ConditionWrapper> startConditions = new ArrayList<>();
    private final Map<Land, RaidSpawner> raids = Collections.synchronizedMap(new HashMap<>());
    private @Nullable Multimap<String, RaidSpawner.Chunk> lastFindSpawnChunksResult;
    //
    private @Nullable LandsIntegration lands;
    private @Nullable JDAInterface jdaInterface;
    private @Nullable BukkitTask gameEndTimer;
    private @Nullable BukkitTask gamePreStartTimer;

    @Override
    public void onLoad() {
        PluginBridge.BRIDGES.clear();
        conditionProviders.clear();
        landActionProviders.clear();
        playerActionProviders.clear();
        enemyProviders.clear();
    }

    @Override
    public void onEnable() {
        lands = LandsIntegration.of(this);
        setupInternalProviders();

        if (pluginConfig.load()) {
            enableDebug = pluginConfig.isEnableDebug();
            createStartConditions();
            startStartConditions();
        } else {
            // show after server startup
            getServer().getScheduler().runTask(this, () -> getLogger().warning(
                    "There is a configuration error, please fix configuration and reload."));
        }
        pluginLang.load();

        Optional.ofNullable(getCommand("raidspawner"))
                .ifPresent(c -> c.setExecutor(new RaidSpawnerCommandHandler(this, lands)));

        hookPlaceholderAPI();
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                LuckPermsBridge bridge = new LuckPermsBridge();
                if (bridge.hook()) {
                    PluginBridge.put(bridge);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        jdaInterface = null;
        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            try {
                DiscordSRVBridge bridge;
                jdaInterface = bridge = new DiscordSRVBridge(getLogger());
                if (bridge.hook()) {
                    PluginBridge.put(bridge);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Active condition types: " + String.join(", ", conditionProviders.keySet()));
        getLogger().info("Active land action types: " + String.join(", ", landActionProviders.keySet()));
        getLogger().info("Active player action types: " + String.join(", ", playerActionProviders.keySet()));
        getLogger().info("Active enemy types: " + String.join(", ", enemyProviders.keySet()));

        enemyProviders.values().forEach(p -> {
            try {
                p.load();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDisable() {
        try {
            clearStartConditions();
            clearRaidAll(RaidEndResult.CANCEL, null);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Exception in raids clear", e);
        }

        timer.cancel();

        try {
            startConditions.forEach(ConditionWrapper::unload);
            startConditions.clear();

            conditionProviders.clear();
            landActionProviders.clear();
            playerActionProviders.clear();

            enemyProviders.values().forEach(p -> {
                try {
                    p.unload();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
            enemyProviders.clear();

            for (Iterator<PluginBridge> it = PluginBridge.BRIDGES.values().iterator(); it.hasNext(); ) {
                PluginBridge bridge = it.next();
                try {
                    if (bridge.isHooked()) {
                        bridge.unhook();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                it.remove();
            }

        } finally {
            lands = null;
            timer.purge();
            RaidSpawner.spawnedEntities.clear();
            RaidSpawner.keepChunksByEntities.clear();
        }
    }

    public void logDebug(Supplier<String> message) {
        if (enableDebug) {
            getLogger().warning("[DEBUG]: " + message.get());
        }
    }

    public RaidSpawnerAPI getAPI() {
        return this;
    }

    @Override
    public RaidSpawnerConfig getPluginConfig() {
        return pluginConfig;
    }

    @Override
    public RaidSpawnerLang getPluginLang() {
        return pluginLang;
    }

    private void onFindChunkCommand(Player p) {
        findLandsRaidChunks(getLandAPI().getLands()).forEach(result -> {
            p.sendMessage("- " + result.land().getName() + " -> spawn " + result.raidChunks().size() + " chunks");
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

    private List<LandChunkFindResult> findLandsRaidChunks(Collection<Land> lands) {
        int distanceChunks = pluginConfig.getRaidSetting().mobsDistanceChunks();
        Function<Land, World> worlds = getLandSpawnOrConfigWorld();  // throws IllegalArgumentException

        List<LandChunkFindResult> results = new ArrayList<>();
        Set<String> safeChunks = new HashSet<>();

        for (Land land : getLandAPI().getLands()) {
            if (lands.contains(land)) {  // is raid
                World world = worlds.apply(land);
                if (world == null) {
                    getLogger().severe("Land '" + land.getName() + "' spawn world is null");
                    continue;
                }

                List<RaidSpawner.Chunk> chunks = new ArrayList<>();
                List<ChunkCoordinate> landChunks = new ArrayList<>();

                for (Container container : land.getContainers()) {
                    for (ChunkCoordinate chunk : container.getChunks()) {
                        // safeチャンクをマークする
                        for (int x = chunk.getX() - distanceChunks; x <= chunk.getX() + distanceChunks; x++) {
                            for (int z = chunk.getZ() - distanceChunks; z <= chunk.getZ() + distanceChunks; z++) {
                                safeChunks.add(x + "," + z + "," + container.getWorld().getName());
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
                            // Landチャンク
                            landChunks.add(chunk);
                        }

                    }
                }

                results.add(new LandChunkFindResult(land, world, landChunks, chunks));

            } else {  // no raid
                for (Container container : land.getContainers()) {
                    for (ChunkCoordinate chunk : container.getChunks()) {
                        // safeチャンクをマークする
                        for (int x = chunk.getX() - distanceChunks; x <= chunk.getX() + distanceChunks; x++) {
                            for (int z = chunk.getZ() - distanceChunks; z <= chunk.getZ() + distanceChunks; z++) {
                                safeChunks.add(x + "," + z + "," + container.getWorld().getName());
                            }
                        }
                    }
                }
            }
        }

        lastFindSpawnChunksResult = LinkedHashMultimap.create();
        for (Iterator<LandChunkFindResult> it = results.iterator(); it.hasNext(); ) {
            List<RaidSpawner.Chunk> raidChunks = it.next().raidChunks();
            raidChunks.removeIf(c -> safeChunks.contains(c.toString()));
            if (raidChunks.isEmpty()) {
                it.remove();
            } else {
                raidChunks.forEach(c -> lastFindSpawnChunksResult.put(c.toString(), c));
            }
        }

        return results;
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
                new LandCommandAction.Provider()
        )
                .filter(Objects::nonNull)
                .forEachOrdered(action -> landActionProviders.put(action.getType(), action));

        Stream.of(
                new PlayerCommandAction.Provider(),
                new PlayerExecuteCommandAction.Provider(),
                PlayerAddMoneyAction.Provider.createAndHookEconomy(this),
                PlayerRemoveMoneyAction.Provider.createAndHookEconomy(this)
        )
                .filter(Objects::nonNull)
                .forEachOrdered(action -> playerActionProviders.put(action.getType(), action));

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

    @Nullable
    public JDAInterface getJDAInterface() {
        return jdaInterface;
    }

    @Override
    public Map<String, ConditionProvider<?>> conditionProviders() {
        return conditionProviders;
    }

    @Override
    public Map<String, ActionProvider<?>> landActionProviders() {
        return landActionProviders;
    }

    @Override
    public Map<String, ActionProvider<?>> playerActionProviders() {
        return playerActionProviders;
    }

    @Override
    public Map<String, EnemyProvider<?>> enemyProviders() {
        return enemyProviders;
    }

    @Override
    public Map<Land, RaidSpawner> getCurrentRaids() {
        return Collections.unmodifiableMap(raids);
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

    public Action createLandAction(String type, Object value, @Nullable ConfigurationSection config) throws IllegalArgumentException, ActionProvider.ConfigurationError {
        if (!landActionProviders.containsKey(type)) {
            throw new IllegalArgumentException(("Unknown land action type: " + type));
        }
        return landActionProviders.get(type).create(value, config);
    }

    public Action createPlayerAction(String type, Object value, @Nullable ConfigurationSection config) throws IllegalArgumentException, ActionProvider.ConfigurationError {
        if (!playerActionProviders.containsKey(type)) {
            throw new IllegalArgumentException(("Unknown player action type: " + type));
        }
        return playerActionProviders.get(type).create(value, config);
    }

    public Multimap<World, Chunk> getChunkTickets() {
        Multimap<World, Chunk> chunks = ArrayListMultimap.create();
        for (World world : getServer().getWorlds()) {
            Collection<Chunk> tickets = world.getPluginChunkTickets().get(this);
            if (tickets == null || tickets.isEmpty())
                continue;
            chunks.putAll(world, tickets);
        }
        return chunks;
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
                getLogger().severe("Error condition config (in event-start, type " + type + "): " + e.getMessage());
                continue;
            }
            startConditions.add(new ConditionWrapper(timer, condition, this::onStartTrigger));
        }
    }

    public void startStartConditions() {
        clearStartConditions();

        List<Long> delays = startConditions.stream()
                .map(ConditionWrapper::start)
                .filter(Objects::nonNull)
                .toList();

        EventStart.PreNotify preNotify = pluginConfig.getStartPreNotify();
        if (!delays.isEmpty() && preNotify.enable()) {
            delays.stream().mapToLong(v -> v).min().ifPresent(delay -> {
                delay = (Math.round(delay / 1000d) - preNotify.minutes() * 60L) * 20;
                if (0 < delay) {
                    gamePreStartTimer = getServer().getScheduler().runTaskLater(this, this::onPreStartNotify, delay);
                }
            });
        }
    }

    public void clearStartConditions() {
        startConditions.forEach(ConditionWrapper::clear);
        if (gamePreStartTimer != null) {
            gamePreStartTimer.cancel();
            gamePreStartTimer = null;
        }
    }

    @Override
    public boolean isStandbyAutoStart() {
        return !startConditions.isEmpty() && startConditions.stream().anyMatch(c -> !c.isActivated());
    }

    @Override
    public List<ConditionWrapper> getAutoStartConditions() {
        return Collections.unmodifiableList(startConditions);
    }

    private void onStartTrigger(ConditionWrapper condition) {
        if (isRunningRaid()) {
            getLogger().warning("Already running raids (ignored)");
            return;
        }

        clearStartConditions();
        startRaidAll(condition.getCondition());
    }

    private void onPreStartNotify() {
        getLogger().info("Send pre-start notify");
        RaidSpawnsPreStartNotifyEvent event = new RaidSpawnsPreStartNotifyEvent();
        getServer().getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        EventStart.PreNotify config = pluginConfig.getStartPreNotify();
        if (!config.enable())
            return;

        pluginLang.send(getServer().getOnlinePlayers(), Lang.PRESTART_NOTIFY_BROADCAST_MESSAGE);
    }

    // raids


    @Override
    public boolean isRunningRaid() {
        return raids.values().stream().anyMatch(RaidSpawner::isRunning);
    }

    @Override
    public boolean isRunningRaid(Land land) {
        return raids.containsKey(land) && raids.get(land).isRunning();
    }

    @Override
    public boolean startRaidAll(@Nullable Condition reason) {
        if (isRunningRaid())
            throw new IllegalStateException("Already running raids");

        raids.clear();
        findLandsRaidChunks(getLandAPI().getLands())
                .forEach(result -> raids.put(result.land(), createRaidSpawner(result)));

        RaidSpawnsPreStartEvent myEvent = new RaidSpawnsPreStartEvent(raids.values(), reason);
        getServer().getPluginManager().callEvent(myEvent);
        if (myEvent.isCancelled() || raids.isEmpty()) {
            return false;
        }

        processRaidStart();
        return true;
    }

    @Override
    public void clearRaidAll(@Nullable RaidEndResult result, @Nullable RaidEndReason reason) {
        if (!isRunningRaid())
            return;

        if (gameEndTimer != null) {
            gameEndTimer.cancel();
            gameEndTimer = null;
        }

        raids.values().forEach(r -> r.clear(Optional.ofNullable(result).orElse(RaidEndResult.CANCEL), reason));
        raids.clear();
        getLogger().info("Raid Spawner Ended");
    }

    @Override
    public boolean startRaid(Land land) {
        if (isRunningRaid(land))
            throw new IllegalStateException("Already running raids");

        if (!getLandAPI().getLands().contains(land))
            throw new IllegalStateException("Invalid land (no contains lands api)");

        World world = getLandSpawnOrConfigWorld().apply(land);
        if (world == null)
            throw new IllegalArgumentException("Land '" + land.getName() + "` spawn world is null");

        findLandsRaidChunks(Collections.singleton(land))
                .forEach(result -> raids.put(result.land(), createRaidSpawner(result)));

        processRaidStart();
        return true;
    }

    public void processRaidStart() {
        clearStartConditions();
        raids.values().forEach(RaidSpawner::start);

        // delay 1 tick
        RaidSpawnerUtil.runInMainThread(() -> {
            // 未所属キック
            if (pluginConfig.isNonMembersKick()) {
                for (Player p : getServer().getOnlinePlayers()) {
                    if (!p.hasPermission(RaidSpawnerUtil.NON_MEMBERS_KICK_PERMISSION) && getLandAPI().getLandPlayer(p.getUniqueId()) == null) {
                        p.kickPlayer(pluginLang.format(Lang.NON_MEMBERS_PLAYER_KICK_MESSAGE));
                    }
                }
            }

            // online players
            for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
                if (spawner.getLand().getOnlinePlayers().isEmpty()) {
                    logDebug(() -> "No online players | land: " + spawner.getLand().getName());
                    spawner.clearSetLose(RaidEndReason.NO_PLAYERS);
                }
            }

        });

        // game end
        if (gameEndTimer != null) {
            gameEndTimer.cancel();
        }
        gameEndTimer = getServer().getScheduler().runTaskLater(this, () -> {
            logDebug(() -> "Raid event timeout");
            new ArrayList<>(raids.values()).forEach(s -> s.clearSetLose(RaidEndReason.TIMEOUT));
        }, 20L * 60 * pluginConfig.getRaidSetting().eventTimeMinutes());
    }

    //

    public RaidSpawner createRaidSpawner(LandChunkFindResult landChunkFindResult) {
        List<ConditionWrapper> conditions = new ArrayList<>();

        // win cond
        for (RaidSpawnerConfig.ConditionItem item : pluginConfig.getWinRewardConditions()) {
            // create condition
            String type = item.config().getString("type");
            ConditionWrapper condition;
            try {
                condition = new ConditionWrapper(timer, createCondition(item.config()), w -> {});
            } catch (ConditionProvider.ConfigurationError e) {
                getLogger().severe("Error condition config (in win, type " + type + "): " + e.getMessage());
                continue;
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Exception in create win condition (type " + type + ")", e.getMessage());
                continue;
            }

            condition.actions().addAll(createActions("win", item.actions()));
            conditions.add(condition);
        }

        List<Action> winElseActions = createActions("win-else", pluginConfig.getWinRewardElseActions());
        List<Action> loseActions = createActions("lose", pluginConfig.getLoseRewardActions());

        return new RaidSpawner(
                landChunkFindResult.land(),
                pluginConfig.getRaidSetting(),
                landChunkFindResult.world(),
                landChunkFindResult.raidChunks(),
                landChunkFindResult.landChunks(),
                new RaidSpawner.Rewards(conditions, winElseActions, loseActions)
        );
    }

    public List<Action> createActions(String configName, Actions config) {
        List<Action> actions = new ArrayList<>();
        Stream.of(config.getPlayerActions())
                .map(i -> createPlayerAction(configName, i))
                .filter(Objects::nonNull)
                .forEachOrdered(actions::add);
        Stream.of(config.getLandActions())
                .map(i -> createLandAction(configName, i))
                .filter(Objects::nonNull)
                .forEachOrdered(actions::add);
        return actions;
    }

    private @Nullable Action createPlayerAction(String configName, Actions.Item item) {
        try {
            return createPlayerAction(item.type(), item.value(), item.config());
        } catch (ActionProvider.ConfigurationError e) {
            getLogger().severe("Error player action config (in " + configName + ", type " + item.type() + "): " + e.getMessage());
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Exception in create player action (in " + configName + ", type " + item.type() + ")", e);
        }
        return null;
    }

    private @Nullable Action createLandAction(String configName, Actions.Item item) {
        try {
            return createLandAction(item.type(), item.value(), item.config());
        } catch (ActionProvider.ConfigurationError e) {
            getLogger().severe("Error land action config (in " + configName + ", type " + item.type() + "): " + e.getMessage());
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Exception in create land action (in " + configName + ", type " + item.type() + ")", e);
        }
        return null;
    }

    public @Nullable Map<Class<Action>, Boolean> sendReward(RaidSpawner spawner, RaidEndResult result) {
        RaidSpawner.Rewards rewards = spawner.getRewards();
        List<Action> actions = null;

        logDebug(() -> "on sendReward | " + result.name() + " | land: " + spawner.getLand().getName());
        switch (result) {
            case LOSE -> {
                logDebug(() -> "  result: lose");
                actions = rewards.loseActions();
            }
            case WIN -> {
                for (int i = 0; i < rewards.rewardConditions().size(); i++) {
                    ConditionWrapper cond = rewards.rewardConditions().get(i);
                    if (cond.getCondition().isInvertTrigger() != cond.isActivated()) {
                        int index = i;
                        logDebug(() -> "  result: win (cond index: " + index + ")");
                        actions = cond.actions();
                        break;
                    }
                }
                if (actions == null) {
                    logDebug(() -> "  result: win (else cond)");
                    actions = rewards.noConditionWinActions();
                }
            }
            default -> {
                return null;
            }
        }

        Map<Class<Action>, Boolean> rewardResults = new HashMap<>();
        for (Action action : actions) {
            logDebug(() -> "reward: " + action.getProvider().getType() + " (" + action.getClass().getSimpleName() + ")");
            boolean rewardResult;
            if (action instanceof LandAction) {
                try {
                    rewardResult = ((LandAction) action).doAction(spawner, spawner.getLand());
                } catch (Throwable e) {
                    e.printStackTrace();
                    continue;
                }
            } else if (action instanceof PlayerAction) {
                rewardResult = true;
                for (Player player : spawner.getLand().getOnlinePlayers()) {
                    try {
                        ((PlayerAction) action).doAction(spawner, player);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } else {
                continue;
            }
            //noinspection unchecked
            rewardResults.put((Class<Action>) action.getClass(), rewardResult);
        }

        return rewardResults;
    }

    // event

    @EventHandler(priority = EventPriority.HIGH)
    public void onEndRaid(RaidSpawnEndEvent event) {
        if (!isRunningRaid()) {
            List<RaidSpawner> endRaids = new ArrayList<>(raids.values());
            endRaids.sort(Comparator.comparingLong(RaidSpawner::getEndTime));

            getServer().getPluginManager().callEvent(new RaidSpawnsAllEndEvent(endRaids));
            raids.clear();
            if (gameEndTimer != null) {
                gameEndTimer.cancel();
                gameEndTimer = null;
            }

            if (jdaInterface != null && pluginConfig.isSendResultToDiscordOnEventEnd()) {
                Long discordChannelId = pluginConfig.getDiscordChannelIdWithEnabled();
                if (discordChannelId != null) {
                    List<RaidSpawner> wins = endRaids.stream().filter(r -> !r.isLose()).toList();
                    List<RaidSpawner> loses = endRaids.stream().filter(RaidSpawner::isLose).toList();

                    StringBuilder sb = new StringBuilder();
                    if (wins.isEmpty()) {
                        sb.append("土地を守り抜いたLandはありませんでした･･･\n\n");
                    } else {
                        sb.append("土地を守り抜いたLand:\n");
                        for (RaidSpawner win : wins) {
                            sb.append("- ").append(win.getLand().getName()).append("\n");
                        }
                        sb.append("\n");
                    }

                    if (!loses.isEmpty()) {
                        sb.append("侵略されたLand:\n");
                        for (RaidSpawner lose : loses) {
                            sb.append("- ").append(lose.getLand().getName());
                            if (RaidEndReason.NO_PLAYERS.equals(lose.getEndReason())) {
                                sb.append(" (プレイヤー不在)");
                            }
                            sb.append("\n");
                        }
                    }

                    jdaInterface.sendMessage(sb.toString(), discordChannelId);
                }
            }

            getLogger().info("Auto start conditions restarting");
            startStartConditions();
        }
    }

    @EventHandler
    public void onLoginPlayer(PlayerLoginEvent event) {
        if (!isRunningRaid() || !pluginConfig.isNonMembersKick())
            return;

        // 未所属キック
        if (!event.getPlayer().hasPermission(RaidSpawnerUtil.NON_MEMBERS_KICK_PERMISSION) && getLandAPI().getLandPlayer(event.getPlayer().getUniqueId()) == null) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, pluginLang.format(Lang.NON_MEMBERS_JOIN_DENY_MESSAGE));
        }
    }

    @EventHandler
    public void onJoinPlayer(PlayerJoinEvent event) {
        String groupName = pluginConfig.getRaidSetting().luckPermsGroup();
        if (groupName != null && RaidSpawnerUtil.isRaidPlayer(event.getPlayer())) {
            PluginBridge.getValid(LuckPermsBridge.class).ifPresent(perms -> {
                try {
                    perms.addPermissionGroup(event.getPlayer(), groupName);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @EventHandler
    public void onQuitPlayer(PlayerQuitEvent event) {
        String groupName = pluginConfig.getRaidSetting().luckPermsGroup();
        if (groupName != null) {
            PluginBridge.getValid(LuckPermsBridge.class).ifPresent(perms -> {
                try {
                    perms.removePermissionGroup(event.getPlayer(), groupName);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }

        for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
            Land land = spawner.getLand();
            if (land.getOnlinePlayers().isEmpty()) {
                logDebug(() -> "No online players | land: " + spawner.getLand().getName());
                spawner.clearSetLose(RaidEndReason.NO_PLAYERS);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathEntity(EntityDeathEvent event) {
        for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
            spawner.onDeathEntity();
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageEntity(EntityDamageEvent event) {
        if (raids.isEmpty())
            return;

        if (event.getEntity() instanceof LivingEntity && ((LivingEntity) event.getEntity()).getHealth() - event.getFinalDamage() <= 0) {
            for (RaidSpawner spawner : new ArrayList<>(raids.values())) {
                spawner.onDeathEntity();
            }
        }

    }

    @EventHandler
    public void onUnloadEntities(EntitiesUnloadEvent event) {
        RaidSpawner.onEntitiesUnloadEvent(event);
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.Actions;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSpawnerConfig;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class RaidSpawnerPlugin extends JavaPlugin {
    private final RaidSpawnerConfig pluginConfig = new RaidSpawnerConfig(this);
    private final Timer timer = new Timer("RaidSpawner-Timer", true);
    private final Map<String, ConditionProvider<?>> conditionProviders = new HashMap<>();
    private final Map<String, ActionProvider<?>> actionProviders = new HashMap<>();
    //
    private final List<ConditionWrapper> startConditions = new ArrayList<>();
    private final Map<Land, RaidSpawner> raids = new HashMap<>();
    //
    private @Nullable LandsIntegration lands;

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

        getLogger().info("Active condition types: " + String.join(", ", conditionProviders.keySet()));
        getLogger().info("Active action types: " + String.join(", ", actionProviders.keySet()));
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

    public void setupInternalProviders() {
        Stream.of(
                new RealClockCondition.Provider(),
                new TimerCondition.Provider()
        )
                .forEachOrdered(cond -> conditionProviders.put(cond.getType(), cond));

        Stream.of(
                new LandRemoveChunkAction.Provider(),
                PlayerAddMoneyAction.Provider.createAndHookEconomy(this),
                PlayerRemoveMoneyAction.Provider.createAndHookEconomy(this)
        )
                .filter(Objects::nonNull)
                .forEachOrdered(action -> actionProviders.put(action.getType(), action));
    }

    public @NotNull LandsIntegration getLandAPI() {
        return Objects.requireNonNull(lands, "LandsIntegration is not hooked");
    }

    // util

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
        startRaidAll();
    }

    // raids

    public boolean isRunningRaid() {
        return !raids.isEmpty();
    }

    private void startRaidAll() {
        if (isRunningRaid())
            throw new IllegalStateException("Already running raids");

        raids.clear();
        for (Land land : getLandAPI().getLands()) {
            raids.put(land, createRaidSpawner(land));
        }

        clearStartConditions();
        raids.values().forEach(RaidSpawner::start);
        getLogger().info("Raid Spawner Started");


        // delay 1 tick
        RaidSpawnerUtil.runInMainThread(() -> {
            for (RaidSpawner spawner : raids.values()) {
                if (spawner.getLand().getOnlinePlayers().size() == 0) {
                    spawner.clearSetLose();
                }
            }

        });
    }

    private void clearRaidAll() {
        if (!isRunningRaid())
            return;

        raids.values().forEach(RaidSpawner::clear);
        raids.clear();
        getLogger().info("Raid Spawner Ended");
    }

    private void clearRaidAllAndRestart() {
        clearRaidAll();
        clearStartConditions();
        startStartConditions();
    }

    private RaidSpawner createRaidSpawner(Land land) {
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

        return new RaidSpawner(land, conditions);
    }


}

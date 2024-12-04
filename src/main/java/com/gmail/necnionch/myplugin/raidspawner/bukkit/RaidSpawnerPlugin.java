package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.Action;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.ActionProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.Actions;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSpawnerConfig;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public final class RaidSpawnerPlugin extends JavaPlugin {
    private final RaidSpawnerConfig pluginConfig = new RaidSpawnerConfig(this);
    private final Map<String, ConditionProvider<?>> conditionProviders = new HashMap<>();
    private final Map<String, ActionProvider<?>> actionProviders = new HashMap<>();
    private final List<ConditionWrapper> startConditions = new ArrayList<>();
    private final Timer timer = new Timer("RaidSpawnerTimer", true);

    @Override
    public void onEnable() {
        if (!pluginConfig.load()) {
            setEnabled(false);
            return;
        }

        setupInternalConditionProviders();

        createConditions();
        initConditions();
    }

    @Override
    public void onDisable() {
        timer.cancel();
        clearConditions();
        startConditions.forEach(ConditionWrapper::unload);
        conditionProviders.clear();
        startConditions.clear();
        actionProviders.clear();

        timer.purge();
    }


    public void setupInternalConditionProviders() {
        Stream.of(
                new RealClockCondition.Provider(),
                new TimerCondition.Provider()
        ).forEach(cond -> conditionProviders.put(cond.getType(), cond));
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


    public void createConditions() {
        clearConditions();
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
            startConditions.add(new ConditionWrapper(timer, condition, this::onTrigger));
        }
        getLogger().info("Loaded " + startConditions.size() + " conditions");
    }

    public void initConditions() {
        startConditions.forEach(ConditionWrapper::start);
    }

    public void clearConditions() {
        startConditions.forEach(ConditionWrapper::clear);
    }

    private void onTrigger(ConditionWrapper condition) {
        getLogger().warning("onTrigger by " + condition.getType());
        condition.start();
    }

    // create active raid

    private void startRaidAll() {

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

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.*;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.PluginConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class RaidSpawnerPlugin extends JavaPlugin {
    private final PluginConfig pluginConfig = new PluginConfig(this);
    private final Map<String, ConditionProvider<?>> conditionProviders = new HashMap<>();
    private final List<ConditionWrapper> startConditions = new ArrayList<>();
    private final Timer timer = new Timer("RaidSpawnerTimer", true);
    private final Consumer<Runnable> runInMainThread = task -> getServer().getScheduler().runTask(this, task);

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

        timer.purge();
    }


    public void setupInternalConditionProviders() {
        Stream.of(
                new RealClockCondition.Provider(),
                new TimerCondition.Provider()
        ).forEach(cond -> conditionProviders.put(cond.getType(), cond));
    }

    public void createConditions() {
        clearConditions();
        for (ConfigurationSection condConfig : pluginConfig.getEventStartConditions()) {
            String condType = condConfig.getString("type");
            if (!conditionProviders.containsKey(condType)) {
                getLogger().severe("Unknown condition type: " + condType);
                continue;
            }

            Condition condition;
            try {
                condition = conditionProviders.get(condType).create(condConfig);
            } catch (ConditionProvider.ConfigurationError e) {
                getLogger().severe("Error condition config (in event-start, type " + condType + "): " + e);
                continue;
            }

            startConditions.add(new ConditionWrapper(timer, runInMainThread, condition, this::onTrigger));
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



}

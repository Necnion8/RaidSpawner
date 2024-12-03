package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.Condition;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionProvider;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.PluginConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RaidSpawnerPlugin extends JavaPlugin {
    private final PluginConfig pluginConfig = new PluginConfig(this);
    private final Map<String, ConditionProvider<?>> conditionProviders = new HashMap<>();
    private final List<Condition> startConditions = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!pluginConfig.load()) {
            setEnabled(false);
            return;
        }

        createEventStartConditions();
        initConditions();
    }

    @Override
    public void onDisable() {
        conditionProviders.clear();
        startConditions.clear();
    }


    public void createEventStartConditions() {
        conditionProviders.clear();
        startConditions.clear();  // TODO: clear schedules
        for (ConfigurationSection condConfig : pluginConfig.getEventStartConditions()) {
            String condType = condConfig.getString("type", null);
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

            startConditions.add(condition);
        }
        getLogger().info("Loaded " + startConditions.size() + " conditions");
    }

    public void initConditions() {
        startConditions.forEach(this::startCondition);
    }

    public void startCondition(Condition condition) {
        // TODO: create condition wrapper
        Condition.Trigger trigger = new Condition.Trigger(() -> onTrigger(condition));
        condition.start(trigger);
    }

    public void clearCondition(Condition condition) {
        condition.clear();
    }

    private void onTrigger(Condition condition) {
        getLogger().warning("onTrigger by " + condition);
        clearCondition(condition);
        startCondition(condition);
    }



}

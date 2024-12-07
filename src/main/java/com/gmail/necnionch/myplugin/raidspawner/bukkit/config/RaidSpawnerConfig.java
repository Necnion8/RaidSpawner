package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.common.BukkitConfigDriver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RaidSpawnerConfig extends BukkitConfigDriver {

    private RaidSetting raidSetting = RaidSetting.DEFAULTS;

    public RaidSpawnerConfig(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onLoaded(FileConfiguration configuration) {
        ConfigurationSection config = Optional.ofNullable(configuration.getConfigurationSection("raid"))
                .orElseGet(MemoryConfiguration::new);

        raidSetting = new RaidSetting(
                config.getInt("event-time-minutes", RaidSetting.DEFAULTS.eventTimeMinutes()),
                config.getInt("waves", RaidSetting.DEFAULTS.maxWaves()),
                config.getString("luckperms-group", RaidSetting.DEFAULTS.luckPermsGroup()),
                RaidSetting.DEFAULTS.mobs()  // TODO: serialize mobs
        );
        return true;
    }

    private static List<ConfigurationSection> getConfigList(ConfigurationSection parent, String key) {
        /*
          https://bukkit.org/threads/getting-a-list-of-configurationsections.157524/
         */
        List<?> list = parent.getList(key);
        return list != null ? list.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> {
                    MemoryConfiguration child = new MemoryConfiguration();
                    //noinspection unchecked,rawtypes
                    child.addDefaults(((Map) obj));
                    return child;
                })
                .collect(Collectors.toList()) : null;
    }

    private Actions getActions(@Nullable ConfigurationSection config) {
        Actions.Item[] playerActions = Optional.ofNullable(config)
                .map(c -> c.getConfigurationSection("player"))
                .map(c -> c.getKeys(false).stream().map(type -> {
                    Object value = c.get(type);
                    ConfigurationSection valConfig = c.getConfigurationSection(type);
                    return new Actions.Item(type, value, valConfig);
                }).toArray(Actions.Item[]::new))
                .orElseGet(() -> new Actions.Item[0]);

        Actions.Item[] landActions = Optional.ofNullable(config)
                .map(c -> c.getConfigurationSection("land"))
                .map(c -> c.getKeys(false).stream().map(type -> {
                    Object value = c.get(type);
                    ConfigurationSection valConfig = c.getConfigurationSection(type);
                    return new Actions.Item(type, value, valConfig);
                }).toArray(Actions.Item[]::new))
                .orElseGet(() -> new Actions.Item[0]);

        return new Actions(playerActions, landActions);
    }

    //


    public RaidSetting getRaidSetting() {
        return raidSetting;
    }

    public List<ConfigurationSection> getStartConditions() {
        return Optional.ofNullable(config.getConfigurationSection("event-start"))
                .map(c -> getConfigList(c, "conditions"))
                .orElseGet(ArrayList::new);
    }

    public List<ConditionItem> getWinRewardConditions() {
        return Optional.ofNullable(config.getConfigurationSection("event-win-rewards"))
                .map(config -> getConfigList(config, "conditions"))
                .map(config -> config.stream()
                        .map(c -> new ConditionItem(c, getActions(c.getConfigurationSection("actions"))))
                        .collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }

    public Actions getWinRewardElseActions() {
        return getActions(config.getConfigurationSection("event-win-rewards.condition-else.actions"));
    }

    public Actions getLoseRewardActions() {
        return getActions(config.getConfigurationSection("event-lose-rewards.actions"));
    }

    public record ConditionItem(ConfigurationSection config, Actions actions) {
    }

}
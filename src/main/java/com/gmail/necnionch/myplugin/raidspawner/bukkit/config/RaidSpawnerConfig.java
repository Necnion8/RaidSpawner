package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.common.BukkitConfigDriver;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RaidSpawnerConfig extends BukkitConfigDriver {

    private boolean enableDebug;
    private RaidSetting raidSetting = RaidSetting.DEFAULTS;

    public RaidSpawnerConfig(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onLoaded(FileConfiguration configuration) {
        enableDebug = configuration.getBoolean("debug", false);

        ConfigurationSection config = Optional.ofNullable(configuration.getConfigurationSection("raid"))
                .orElseGet(MemoryConfiguration::new);

        raidSetting = new RaidSetting(
                config.getInt("event-time-minutes", RaidSetting.DEFAULTS.eventTimeMinutes()),
                config.getInt("waves", RaidSetting.DEFAULTS.maxWaves()),
                config.getString("luckperms-group", RaidSetting.DEFAULTS.luckPermsGroup()),
                config.getString("world", RaidSetting.DEFAULTS.world()),
                config.getInt("mobs-distance-chunks", RaidSetting.DEFAULTS.mobsDistanceChunks()),
                Optional.ofNullable(getConfigList(config, "mobs"))
                        .map(this::getMobSettings)
                        .orElse(RaidSetting.DEFAULTS.mobs())
        );
        return true;
    }

    //

    private static List<ConfigurationSection> getConfigList(ConfigurationSection parent, String key) {
        /*
          https://bukkit.org/threads/getting-a-list-of-configurationsections.157524/
         */
        List<?> list = parent.getList(key);
        return list != null ? list.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> createMemoryConfigurationFromMap((Map<?, ?>) obj))
                .collect(Collectors.toList()) : null;
    }

    private static void putMapToMemoryConfiguration(MemoryConfiguration configuration, Map<?, ?> map) {
        map.forEach((k, v) -> {
            if (v instanceof Map) {
                configuration.set((String) k, createMemoryConfigurationFromMap((Map<?, ?>) v));
            } else {
                configuration.set((String) k, v);
            }
        });
    }

    private static MemoryConfiguration createMemoryConfigurationFromMap(Map<?, ?> map) {
        MemoryConfiguration nest = new MemoryConfiguration();
        putMapToMemoryConfiguration(nest, map);
        return nest;
    }

    //

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

    private Function<RaidSpawner, Integer> createCountExpression(Object exprValue) {
        if (exprValue instanceof Number) {
            return s -> ((Number) exprValue).intValue();
        }
        Expression expr = new ExpressionBuilder((String) exprValue)
                .variables("land_players", "land_chunks", "wave", "online_players")
                .build();

        return spawner -> (int) Math.ceil(expr
                .setVariable("land_players", spawner.getLand().getTrustedPlayers().size())
                .setVariable("land_chunks", spawner.getLand().getChunksAmount())
                .setVariable("wave", spawner.getWave())
                .setVariable("online_players", spawner.getLand().getOnlinePlayers().size())
                .evaluate());
    }

    private List<MobSetting> getMobSettings(List<ConfigurationSection> config) {
        return config.stream().map(c -> new MobSetting(
                createCountExpression(c.get("count")),
                Optional.ofNullable(getConfigList(c, "enemies"))
                        .map(this::getMobEnemies)
                        .orElse(Collections.emptyList())
        )).toList();
    }

    private List<MobSetting.Enemy> getMobEnemies(List<ConfigurationSection> config) {
        return config.stream().map(c -> new MobSetting.Enemy(
                c.getString("source"),
                c.getInt("priority"),
                c,
                null
                )).collect(Collectors.toList());
    }

    //

    public boolean isEnableDebug() {
        return enableDebug;
    }

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

    public boolean isNonMembersKick() {
        return config.getBoolean("raid.non-members-kick.enable", true);
    }

    public String getNonMembersKickMessage() {
        return Optional.ofNullable(config.getString("raid.non-members-kick.kick-message"))
                .orElse("&c襲撃イベントが発生しているため、現在は参加できません。");
    }

    public Integer getStartNotifyMinutes() {
        if (config.getBoolean("event-start.enable")) {
            return config.getInt("event-start.pre-notify", 3);
        }
        return null;
    }

    //

    public record ConditionItem(ConfigurationSection config, Actions actions) {
    }

}
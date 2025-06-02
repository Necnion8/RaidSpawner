package com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.events.RaidSpawnEndEvent;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin;
import com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatManager;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.ActionType;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerAction;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStats;
import com.gmail.necnionch.myplugin.statbadge.bukkit.stats.PlayerActionStatsProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.util.UUID;

public class StatBadgeBridge implements PluginBridge, Listener {
    private StatManager api;
    private static final ActionType ACTION_RAID_WINS = new ActionType(RaidSpawnerUtil.getPlugin(), "wins");
    private static final ActionType ACTION_RAID_LOSES = new ActionType(RaidSpawnerUtil.getPlugin(), "loses");

    @Override
    public boolean hook() {
        api = null;
        if (!Bukkit.getPluginManager().isPluginEnabled(getPluginName()))
            return false;
        try {
            Class.forName("com.gmail.necnionch.myplugin.statbadge.bukkit.plugin.StatBadgePlugin");
            api = StatBadgePlugin.getStatManager();
        } catch (ClassNotFoundException | IllegalStateException e) {
            return false;
        }
        RaidSpawnerPlugin plugin = RaidSpawnerUtil.getPlugin();
        api.addPlayerActionStatsProvider(ACTION_RAID_WINS, new PlayerActionStatsProvider(plugin) {
            @Override
            public PlayerActionStats create(UUID uuid, String s, ConfigurationSection configurationSection) throws ConfigurationError {
                return new PlayerRaidCount(uuid, ACTION_RAID_WINS, 0);
            }
        });
        api.addPlayerActionStatsProvider(ACTION_RAID_LOSES, new PlayerActionStatsProvider(plugin) {
            @Override
            public PlayerActionStats create(UUID uuid, String s, ConfigurationSection configurationSection) throws ConfigurationError {
                return new PlayerRaidCount(uuid, ACTION_RAID_LOSES, 0);
            }
        });
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        return true;
    }

    @Override
    public void unhook() {
        HandlerList.unregisterAll(this);
        if (api != null) {
            api.removeProviders(RaidSpawnerUtil.getPlugin());
        }
        api = null;
    }

    @Override
    public boolean isHooked() {
        return api != null;
    }

    @Override
    public String getPluginName() {
        return "StatBadge";
    }


    @EventHandler
    public void onRaidEnd(RaidSpawnEndEvent event) {
        boolean win;
        switch (event.getResult()) {
            case WIN -> win = true;
            case LOSE -> win = false;
            default -> {
                return;
            }
        }

        for (Player player : event.getLand().getOnlinePlayers()) {
            addPlayerRaidCount(player, win ? ACTION_RAID_WINS : ACTION_RAID_LOSES);
        }
    }


    private void addPlayerRaidCount(Player player, ActionType actionType) {
        if (api == null)
            return;
        api.addAction(player, new PlayerAction(player.getUniqueId(), actionType, Instant.now(), null, null, null, 1));
    }


    public static class PlayerRaidCount extends PlayerActionStats {

        public PlayerRaidCount(UUID playerId, ActionType sourceActionType, long value) {
            super(playerId, sourceActionType, value);
        }

        @Override
        public KeyCondition getKeyCondition1() {
            return KeyCondition.ANY;
        }

        @Override
        public KeyCondition getKeyCondition2() {
            return KeyCondition.ANY;
        }

        @Override
        public KeyCondition getKeyCondition3() {
            return KeyCondition.ANY;
        }
    }

}

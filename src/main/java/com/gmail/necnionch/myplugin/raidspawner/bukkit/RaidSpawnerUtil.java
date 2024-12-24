package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RaidSpawnerUtil {
    public static final String NON_MEMBERS_KICK_PERMISSION = "raidspawner.bypass.non-members-kick";

    private static @Nullable Logger log;
    private static @Nullable RaidSpawnerPlugin plugin;

    public static RaidSpawnerPlugin getPlugin() {
        if (plugin == null) {
            plugin = JavaPlugin.getPlugin(RaidSpawnerPlugin.class);
        }
        return plugin;
    }

    public static void runInMainThread(Runnable task) {
        Bukkit.getScheduler().runTask(getPlugin(), task);
    }

    public static Logger getLogger() {
        if (log == null) {
            log = getPlugin().getLogger();
        }
        return log;
    }

    public static void d(Supplier<String> message) {
        getPlugin().logDebug(message);
    }

    public static String processPlaceholder(@Nullable Player player, String string) {
        return getPlugin().getPlaceholderReplacer().process(player, string);
    }

    public static Collection<RaidSpawner> getRaids() {
        return Collections.unmodifiableCollection(getPlugin().getCurrentRaids().values());
    }

    public static List<RaidSpawner> getRunningRaids() {
        return getPlugin().getCurrentRaids().values().stream()
                .filter(RaidSpawner::isRunning)
                .toList();
    }

    public static boolean isRaidPlayer(UUID playerId) {
        Set<Land> raids = getRaids().stream().map(RaidSpawner::getLand).collect(Collectors.toSet());
        return Optional.ofNullable(getPlugin().getLandAPI().getLandPlayer(playerId))
                .map(lp -> lp.getLands().stream().anyMatch(raids::contains))
                .orElse(false);
    }

    public static boolean isRaidPlayer(OfflinePlayer player) {
        return isRaidPlayer(player.getUniqueId());
    }


}

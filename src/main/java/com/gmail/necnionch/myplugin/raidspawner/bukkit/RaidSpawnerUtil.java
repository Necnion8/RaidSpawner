package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class RaidSpawnerUtil {
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

    public static String processPlaceholder(@Nullable Player player, String string) {
        return getPlugin().getPlaceholderReplacer().process(player, string);
    }

}

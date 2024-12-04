package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class RaidSpawnerUtil {
    private static @Nullable Logger log;

    public static RaidSpawnerPlugin getPlugin() {
        return JavaPlugin.getPlugin(RaidSpawnerPlugin.class);
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

}

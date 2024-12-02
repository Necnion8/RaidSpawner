package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MobManager;
import me.angeschossen.lands.api.LandsIntegration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RaidSpawnerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        LandsIntegration lands = LandsIntegration.of(this);
        System.out.println("Lands size -> " + lands.getLands().size());

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            MythicPlugin mythicPlugin = MythicProvider.get();
            MobManager mobs = mythicPlugin.getMobManager();
            System.out.println("MobTypes size -> " + mobs.getMobTypes().size());
        } else {
            getLogger().warning("MythicMobs unknown");
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MythicEnemy implements Enemy {

    private final Provider provider;
    private final MythicMob mob;
    private final String type;
    private final double level;
    private @Nullable ActiveMob activeMob;

    public MythicEnemy(Provider provider, MythicMob mob, String type, double level) {
        this.provider = provider;
        this.mob = mob;
        this.type = type;
        this.level = level;
    }

    public MythicMob getMob() {
        return mob;
    }

    public String getType() {
        return type;
    }

    public double getLevel() {
        return level;
    }

    @Override
    public boolean spawn(World world, Location location) {
        remove();
        activeMob = mob.spawn(BukkitAdapter.adapt(location), level);
        return activeMob != null;
    }

    @Override
    public boolean isAlive() {
        return activeMob != null && !activeMob.isDead();
    }

    @Override
    public boolean remove() {
        if (activeMob != null) {
            activeMob.remove();
            activeMob = null;
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public EnemyProvider<?> getProvider() {
        return provider;
    }


    public static class Provider extends EnemyProvider<MythicEnemy> {

        private final MobManager mobs;

        public Provider(MobManager mobs) {
            super("mythicmobs");
            this.mobs = mobs;

        }

        @Override
        public MythicEnemy create(ConfigurationSection config) throws ConfigurationError {
            String type = config.getString("type");
            double level = config.getDouble("level", 1);

            MythicMob mob = mobs.getMythicMob(type).orElseThrow(() -> new ConfigurationError("Unknown MythicMob Type: " + type));
            return new MythicEnemy(this, mob, type, level);
        }


        public static @Nullable Provider createAndHookMythicMobs(RaidSpawnerPlugin plugin) {
            try {
                Class.forName("io.lumine.mythic.api.MythicProvider");
            } catch (ClassNotFoundException e) {
                return null;
            }

            if (plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
                return new Provider(MythicProvider.get().getMobManager());
            }
            return null;
        }

    }

}

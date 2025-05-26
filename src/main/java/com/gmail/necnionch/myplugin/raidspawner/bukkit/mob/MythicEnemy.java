package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.MobSetting;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class MythicEnemy extends Enemy implements Listener {

    private final Provider provider;
    private final MythicMob mob;
    private final String type;
    private final double level;
    private @Nullable ActiveMob activeMob;
    private @Nullable RaidSpawner spawner;

    public MythicEnemy(MobSetting.Enemy config, Provider provider, MythicMob mob, String type, double level) {
        super(config);
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
    public @Nullable Entity spawn(RaidSpawner spawner, World world, Location location) {
        remove();
        this.activeMob = mob.spawn(BukkitAdapter.adapt(location), level);
        this.spawner = spawner;

        // override
        activeMob.setDespawnMode(DespawnMode.PERSISTENT);

        // set target
        if (!activeMob.hasTarget()) {
            spawner.findNearestPlayer(location).ifPresent(p -> activeMob.setTarget(BukkitAdapter.adapt(p)));
        }
        return activeMob.getEntity().getBukkitEntity();
    }

    @Override
    public boolean isAlive() {
        return activeMob != null && !activeMob.isDead();
    }

    @Override
    public boolean remove() {
        spawner = null;
        if (activeMob == null)
            return false;

        if (!activeMob.getEntity().isValid() && !activeMob.isDead()) {  // bug?
            RaidSpawnerUtil.getLogger().warning("MythicMob " + activeMob.getEntity().getUniqueId() + " is entity invalid");
        }
        activeMob.remove();
        activeMob = null;
        return true;
    }

    @Override
    public void unload() {
        remove();
    }

    @Nullable
    @Override
    public Entity getEntity() {
        if (activeMob != null)
            return activeMob.getEntity().getBukkitEntity();
        return null;
    }

    @Nullable
    @Override
    public Location getEntityLocation() {
        if (activeMob != null)
            return BukkitAdapter.adapt(activeMob.getLocation());
        return null;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }

    @Nullable
    @Override
    public Location searchRandomSpawnLocationByChunk(RaidSpawner spawner, Chunk chunk, Random random, boolean force) {
        return super.searchRandomSpawnLocationByChunk(spawner, chunk, random, force);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (spawner == null || activeMob == null || event.getEntity().getUniqueId() != activeMob.getUniqueId())
            return;

        if (event.getTarget() == null) {
            Player player = spawner.findNearestPlayer(event.getEntity().getLocation()).orElse(null);
            event.setTarget(player);
        }
    }


    public static class Provider extends EnemyProvider<MythicEnemy> {

        private final MobManager mobs;

        public Provider(MobManager mobs) {
            super("mythicmobs");
            this.mobs = mobs;
        }

        @Override
        public boolean isValid(MobSetting.Enemy enemy, ConfigurationSection config) throws ConfigurationError {
            String type = config.getString("type");
            mobs.getMythicMob(type).orElseThrow(() -> new ConfigurationError("Unknown MythicMob Type: " + type));
            return true;
        }

        @Override
        public MythicEnemy create(MobSetting.Enemy enemy, ConfigurationSection config) throws ConfigurationError {
            String type = config.getString("type");
            double level = config.getDouble("level", 1);

            MythicMob mob = mobs.getMythicMob(type).orElseThrow(() -> new ConfigurationError("Unknown MythicMob Type: " + type));
            return new MythicEnemy(enemy, this, mob, type, level);
        }

        public static @Nullable Provider createAndHookMythicMobs(RaidSpawnerPlugin plugin) {
            if (plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
                try {
                    Class.forName("io.lumine.mythic.api.MythicProvider");
                } catch (ClassNotFoundException e) {
                    return null;
                }
                return new Provider(MythicProvider.get().getMobManager());
            }
            return null;
        }

    }

}

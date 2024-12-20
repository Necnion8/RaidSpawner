package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.DespawnMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class MythicEnemy implements Enemy {

    private static final Set<MythicEnemy> aliveEnemies = new HashSet<>();
    private static final Set<UUID> removingEntities = new HashSet<>();
    private final Provider provider;
    private final MythicMob mob;
    private final String type;
    private final double level;
    private @Nullable ActiveMob activeMob;
    private boolean aliveEntityUnloaded;

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
    public @Nullable Entity spawn(RaidSpawner spawner, World world, Location location) {
        remove();
        aliveEnemies.add(this);
        System.out.println("on spawn | aliveEnemies size: " + aliveEnemies.size());
        activeMob = mob.spawn(BukkitAdapter.adapt(location), level);

        // override
        activeMob.setDespawnMode(DespawnMode.PERSISTENT);

        // set target
        if (!activeMob.hasTarget()) {
            spawner.getLand().getOnlinePlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distance(location)))
                    .ifPresent(p -> activeMob.setTarget(BukkitAdapter.adapt(p)));
        }
        return activeMob.getEntity().getBukkitEntity();
    }

    @Override
    public boolean isAlive() {
        return aliveEntityUnloaded || (activeMob != null && !activeMob.isDead());
    }

    @Override
    public boolean remove() {
        if (activeMob == null)
            return false;

        removingEntities.add(activeMob.getUniqueId());

        // + ", valid:" + activeMob.getEntity().isValid() + ", loaded:" + activeMob.getEntity().isLoaded());
        System.out.println("onRemove | valid:" + activeMob.getEntity().isValid() + " | " + activeMob.getUniqueId());
        if (activeMob.getEntity().isValid()) {
            aliveEnemies.remove(this);
            activeMob.remove();
            activeMob = null;
        } else {
            System.out.println("mm remove");
//            AbstractLocation pos = activeMob.getEntity().getLocation();
//            World world = BukkitAdapter.adapt(activeMob.getLocation().getWorld());
//            boolean forceLoaded = world.isChunkForceLoaded(pos.getChunkX(), pos.getChunkZ());
//            world.setChunkForceLoaded(pos.getChunkX(), pos.getChunkZ(), true);
//
//            Entity entity = Bukkit.getEntity(activeMob.getUniqueId());
//            if (entity == null) {
//                System.out.println("No entity");
//            } else {
//                entity.remove();
//                System.out.println("removed entity");
//            }

//            RaidSpawnerUtil.runInMainThread(() -> world.setChunkForceLoaded(pos.getChunkX(), pos.getChunkZ(), forceLoaded));
//            activeMob.getEntity().remove();

//            removingEntities.add(activeMob.getUniqueId());
//            AbstractLocation loc = activeMob.getLocation();
//            World world = BukkitAdapter.adapt(loc.getWorld());
//            Chunk chunk = world.getChunkAt(loc.getChunkX(), loc.getChunkZ());
//            chunk.load();
//            chunk.addPluginChunkTicket(RaidSpawnerUtil.getPlugin());
//            RaidSpawnerUtil.runInMainThread(() -> chunk.removePluginChunkTicket(RaidSpawnerUtil.getPlugin()));
//                aliveEnemies.remove(this);
        }
//        activeMob = null;
//        aliveEnemies.remove(this);
        return true;
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


    public static class Provider extends EnemyProvider<MythicEnemy> implements Listener {

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

        @Override
        public void load() {
            RaidSpawnerPlugin plugin = RaidSpawnerUtil.getPlugin();
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        @Override
        public void unload() {
            HandlerList.unregisterAll(this);
            aliveEnemies.clear();
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

        // events

        @EventHandler(priority = EventPriority.MONITOR)
        public void onUnloadEntities(EntitiesUnloadEvent event) {
            for (MythicEnemy enemy : aliveEnemies) {
                if (enemy.activeMob != null && event.getEntities().contains(enemy.activeMob.getEntity().getBukkitEntity())) {
                    enemy.aliveEntityUnloaded = true;
                    System.out.println("eUnload | mm unload (original entity object): " + enemy.activeMob.getUniqueId());
                }
            }

            for (Iterator<UUID> it = removingEntities.iterator(); it.hasNext(); ) {
                getActiveMob(it.next()).ifPresent(am -> {
                    System.out.println("eUnload | contains removingEntities(am): " + am.getUniqueId() + " | " + am.isDead() + " | " + am.getEntity().isValid());
//                    am.remove();
//                    it.remove();
                });
            }

            Map<UUID, Entity> entities = event.getEntities()
                    .stream()
                    .collect(Collectors.toMap(Entity::getUniqueId, e -> e));
            for (UUID uuid : removingEntities) {
                if (entities.containsKey(uuid)) {
                    System.out.println("eUnload | contains removingEntities: " + uuid);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onLoadEntities(EntitiesLoadEvent event) {
            if (aliveEnemies.isEmpty() && removingEntities.isEmpty())
                return;

            Map<UUID, Entity> entities = event.getEntities()
                    .stream()
                    .collect(Collectors.toMap(Entity::getUniqueId, e -> e));

            for (MythicEnemy enemy : aliveEnemies) {
                if (enemy.activeMob == null)
                    continue;

                Entity entity = entities.get(enemy.activeMob.getUniqueId());
                if (entity == null)
                    continue;

                enemy.aliveEntityUnloaded = false;
                getActiveMob(entity.getUniqueId()).ifPresentOrElse(am -> {
                    // reactive
                    enemy.activeMob = am;
                    System.out.println("eLoad | reactive: " + am.getUniqueId());
                }, () -> {
                    // no reactive, dead
                    System.out.println("eLoad | no reactive: " + enemy.activeMob.getUniqueId());
//                    enemy.remove();
//                    enemy.activeMob = null;
                });
            }

            for (Iterator<UUID> it = removingEntities.iterator(); it.hasNext(); ) {
                getActiveMob(it.next()).ifPresent(am -> {
                    System.out.println("eLoad | contains removingEntities(am): " + am.getUniqueId());
                    am.remove();
                    it.remove();
                    System.out.println("REMOVE");
                });
            }

            for (UUID uuid : removingEntities) {
                if (entities.containsKey(uuid)) {
                    System.out.println("eLoad | contains removingEntities: " + uuid);
                }
            }

        }

        private Optional<ActiveMob> getActiveMob(UUID uuid) {
            return mobs.getActiveMobs().stream()
                    .filter(am -> am.getUniqueId().equals(uuid))
                    .findFirst();
        }
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TestEnemy implements Enemy {

    private final Provider provider;
    private @Nullable LivingEntity entity;
    private boolean aliveEntityUnloaded;
    private final static Set<TestEnemy> aliveEnemies = new HashSet<>();

    public TestEnemy(Provider provider) {
        this.provider = provider;
    }

    @Override
    public @Nullable Entity spawn(RaidSpawner spawner, World world, Location location) {
        remove();
        aliveEnemies.add(this);
        entity = world.spawn(location, Zombie.class, z -> {
            z.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true), true);
            z.setLootTable(LootTables.EMPTY.getLootTable());
            z.addScoreboardTag("RAIDSPAWNER_TEST");
            z.setRemoveWhenFarAway(false);
            Optional.ofNullable(z.getEquipment()).ifPresent(inv -> {
                inv.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                inv.setItemInMainHandDropChance(0);
                inv.setHelmet(new ItemStack(Material.STONE_BUTTON));
                inv.setHelmetDropChance(0);
            });

            spawner.getLand().getOnlinePlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distance(location)))
                    .ifPresent(z::setTarget);

        });
        return entity;
    }

    @Override
    public boolean isAlive() {
        return aliveEntityUnloaded || (entity != null && entity.isValid());
    }

    @Override
    public boolean remove() {
        aliveEnemies.remove(this);
        if (entity != null) {
            if (entity.isValid()) {
                entity.remove();
            } else {
                Chunk chunk = entity.getLocation().getChunk();
                chunk.addPluginChunkTicket(RaidSpawnerUtil.getPlugin());
                try {
                    Entity newEntity = Bukkit.getEntity(entity.getUniqueId());
                    if (newEntity != null) {
                        newEntity.remove();
                    }
                } finally {
                    chunk.removePluginChunkTicket(RaidSpawnerUtil.getPlugin());
                }
            }
            entity = null;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public LivingEntity getEntity() {
        return entity;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends EnemyProvider<TestEnemy> implements Listener {

        public Provider() {
            super("test");
        }

        @Override
        public TestEnemy create(ConfigurationSection config) throws ConfigurationError {
            return new TestEnemy(this);
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

        // events

        @EventHandler
        public void onUnloadEntities(EntitiesUnloadEvent event) {
            for (TestEnemy enemy : aliveEnemies) {
                if (enemy.entity != null && event.getEntities().contains(enemy.entity)) {
                    enemy.aliveEntityUnloaded = true;
                }
            }
        }

        @EventHandler
        public void onLoadEntities(EntitiesLoadEvent event) {
            for (TestEnemy enemy : aliveEnemies) {
                if (enemy.entity == null)
                    continue;

                Entity entity = event.getEntities().stream()
                        .filter(e -> e.getUniqueId().equals(enemy.entity.getUniqueId()))
                        .findFirst()
                        .orElse(null);
                if (entity == null)
                    continue;

                enemy.aliveEntityUnloaded = false;
                try {
                    enemy.entity = (LivingEntity) entity;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}

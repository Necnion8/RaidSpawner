package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class VanillaEnemy implements Enemy {

    private final EntityType entityType;
    private final Consumer<LivingEntity> consumer;
    private final Provider provider;
    private @Nullable LivingEntity entity;
    private @Nullable RaidSpawner spawner;

    public VanillaEnemy(Provider provider, EntityType entityType, Consumer<LivingEntity> consumer) {
        this.provider = provider;
        this.entityType = entityType;
        this.consumer = consumer;
    }

    @Override
    public @Nullable Entity spawn(RaidSpawner spawner, World world, Location location) {
        remove();
        this.spawner = spawner;
        this.entity = (LivingEntity) world.spawnEntity(location, entityType);
        consumer.accept(entity);

        if (entity instanceof Mob) {
            spawner.findNearestPlayer(location).ifPresent(p -> ((Mob) entity).setTarget(p));
        }

        return entity;
    }

    @Override
    public boolean isAlive() {
        return entity != null && !entity.isDead();
    }

    @Override
    public boolean remove() {
        spawner = null;
        if (entity == null)
            return false;

        entity.remove();
        entity = null;
        return true;
    }

    @Override
    public void unload() {
        remove();
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


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (spawner == null || entity == null || event.getEntity().getUniqueId() != entity.getUniqueId())
            return;

        if (event.getTarget() == null) {
            Player player = spawner.findNearestPlayer(event.getEntity().getLocation()).orElse(null);
            event.setTarget(player);
        }
    }


    public static class Provider extends EnemyProvider<VanillaEnemy> {

        public Provider() {
            super("vanilla");
        }

        @Override
        public boolean isValid(ConfigurationSection config) throws ConfigurationError {
            EntityType type;
            try {
                type = EntityType.valueOf(config.getString("type", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationError(e);
            }
            if (type.getEntityClass() == null) {
                throw new ConfigurationError("Unknown entity class: " + config.getString("type"));
            } else if (!LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
                throw new ConfigurationError("Not living entity: " + type.name());
            }
            return true;
        }

        @Override
        public VanillaEnemy create(ConfigurationSection config) throws ConfigurationError {
            EntityType type;
            try {
                type = EntityType.valueOf(config.getString("type", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationError(e);
            }
            if (type.getEntityClass() == null) {
                throw new ConfigurationError("Unknown entity class: " + config.getString("type"));
            } else if (!LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
                throw new ConfigurationError("Not living entity: " + type.name());
            }

            double health = config.getDouble("health", 20);

            List<PotionEffect> effects = new ArrayList<>();
            ConfigurationSection effectsSection = config.getConfigurationSection("effects");
            if (effectsSection != null) {
                for (String key : effectsSection.getKeys(false)) {
                    int duration = effectsSection.getInt(key + ".duration");
                    int level = Math.max(1, effectsSection.getInt(key + ".level"));
                    PotionEffectType effectType = PotionEffectType.getByName(key.toUpperCase(Locale.ROOT));
                    if (effectType != null) {
                        effects.add(effectType.createEffect(duration, level - 1));
                    }
                }
            }

            return new VanillaEnemy(this, type, entity -> {
                entity.setHealth(health);
                entity.setRemoveWhenFarAway(false);
                Optional.ofNullable(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).ifPresent(a -> a.setBaseValue(health));
                if (entity instanceof Mob) {
                    ((Mob) entity).setLootTable(LootTables.EMPTY.getLootTable());
                }
                effects.forEach(e -> e.apply(entity));
            });
        }

    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TestEnemy implements Enemy, Listener {

    private final Provider provider;
    private @Nullable LivingEntity entity;
    private @Nullable RaidSpawner spawner;

    public TestEnemy(Provider provider) {
        this.provider = provider;
    }

    @Override
    public @Nullable Entity spawn(RaidSpawner spawner, World world, Location location) {
        remove();
        this.spawner = spawner;
        this.entity = world.spawn(location, Zombie.class, z -> {
            z.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true));
            z.setLootTable(LootTables.EMPTY.getLootTable());
            z.addScoreboardTag("RAIDSPAWNER_TEST");
            z.setRemoveWhenFarAway(false);
            Optional.ofNullable(z.getEquipment()).ifPresent(inv -> {
                inv.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                inv.setItemInMainHandDropChance(0);
                inv.setHelmet(new ItemStack(Material.STONE_BUTTON));
                inv.setHelmetDropChance(0);
            });

            spawner.findNearestPlayer(location).ifPresent(z::setTarget);

        });
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

        if (!entity.isValid() && !entity.isDead()) {  // bug?
            RaidSpawnerUtil.getLogger().warning("Test enemy " + entity.getUniqueId() + " is entity invalid");
        }
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


    public static class Provider extends EnemyProvider<TestEnemy> {

        public Provider() {
            super("test");
        }

        @Override
        public boolean isValid(ConfigurationSection config) {
            return true;
        }

        @Override
        public TestEnemy create(ConfigurationSection config) throws ConfigurationError {
            return new TestEnemy(this);
        }

    }

}

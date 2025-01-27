package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;

public class TestEnemy implements Enemy {

    private final Provider provider;
    private @Nullable LivingEntity entity;

    public TestEnemy(Provider provider) {
        this.provider = provider;
    }

    @Override
    public @Nullable Entity spawn(RaidSpawner spawner, World world, Location location) {
        remove();
        entity = world.spawn(location, Zombie.class, z -> {
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

            spawner.getLand().getOnlinePlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distance(location)))
                    .ifPresent(z::setTarget);

        });
        return entity;
    }

    @Override
    public boolean isAlive() {
        return entity != null && !entity.isDead();
    }

    @Override
    public boolean remove() {
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
    @Nullable
    public LivingEntity getEntity() {
        return entity;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends EnemyProvider<TestEnemy> {

        public Provider() {
            super("test");
        }

        @Override
        public TestEnemy create(ConfigurationSection config) throws ConfigurationError {
            return new TestEnemy(this);
        }

    }

}

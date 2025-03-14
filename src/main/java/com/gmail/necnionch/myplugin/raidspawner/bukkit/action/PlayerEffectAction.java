package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerEffectAction implements PlayerAction {

    private final Provider provider;
    private final List<PotionEffect> effects;

    public PlayerEffectAction(Provider provider, List<PotionEffect> effects) {
        this.provider = provider;
        this.effects = effects;
    }


    @Override
    public boolean doAction(RaidSpawner spawner, Player player) {
        effects.forEach(e -> e.apply(player));
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerEffectAction> {

        public Provider() {
            super("effects");
        }

        @Override
        public PlayerEffectAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            List<PotionEffect> effects = new ArrayList<>();

            if (config != null) {
                for (String effectName : config.getKeys(false)) {
                    ConfigurationSection eSection = Objects.requireNonNull(config.getConfigurationSection(effectName));

                    int duration = eSection.getInt("duration", 20 * 10);
                    int amplifier = Math.max(1, eSection.getInt("level")) - 1;
                    boolean silent = eSection.getBoolean("silent", false);

                    PotionEffectType effectType = PotionEffectType.getByName(effectName);
                    if (effectType == null) {
                        RaidSpawnerUtil.getLogger().warning("Unknown potion effect type: " + effectName);
                        continue;
                    }

                    effects.add(new PotionEffect(effectType, duration, amplifier, !silent, !silent, !silent));
                }
            }

            return new PlayerEffectAction(this, effects);
        }

    }

}

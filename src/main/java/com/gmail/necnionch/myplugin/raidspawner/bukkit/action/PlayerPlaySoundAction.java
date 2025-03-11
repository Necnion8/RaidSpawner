package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class PlayerPlaySoundAction implements PlayerAction {

    private final Provider provider;
    private final Sound sound;

    public PlayerPlaySoundAction(Provider provider, Sound sound) {
        this.provider = provider;
        this.sound = sound;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Player player) {
        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, 1f, 1f);
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerPlaySoundAction> {

        public Provider() {
            super("playsound");
        }

        @Override
        public PlayerPlaySoundAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            Sound sound;
            String soundId = String.valueOf(value).toUpperCase(Locale.ROOT);
            try {
                sound = Sound.valueOf(soundId);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationError("Unknown sound: " + soundId);
            }
            return new PlayerPlaySoundAction(this, sound);
        }

    }

}

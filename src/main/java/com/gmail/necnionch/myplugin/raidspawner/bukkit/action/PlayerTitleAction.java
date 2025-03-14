package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerTitleAction implements PlayerAction {

    private final Provider provider;
    private final @Nullable String title;
    private final @Nullable String subtext;
    private final int fadeIn;
    private final int fadeOut;
    private final int duration;

    public PlayerTitleAction(Provider provider, @Nullable String title, @Nullable String subtext, int fadeIn, int fadeOut, int duration) {
        this.provider = provider;
        this.title = title;
        this.subtext = subtext;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.duration = duration;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Player player) {
        player.sendTitle(
                Optional.ofNullable(title).map(s -> ChatColor.translateAlternateColorCodes('&', s)).orElse(""),
                Optional.ofNullable(subtext).map(s -> ChatColor.translateAlternateColorCodes('&', s)).orElse(""),
                fadeIn, duration, fadeOut
        );
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerTitleAction> {

        public Provider() {
            super("title");
        }

        @Override
        public PlayerTitleAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            String title;
            String subtext = null;
            int fadeIn = 20;
            int fadeOut = 20;
            int duration = 60;

            if (config == null) {
                title = String.valueOf(value);
            } else {
                title = config.getString("text");
                subtext = config.getString("subtext");
                fadeIn = config.getInt("fade-in", 20);
                fadeOut = config.getInt("fade-out", 20);
                duration = config.getInt("duration", 60);
            }

            return new PlayerTitleAction(this, title, subtext, fadeIn, fadeOut, duration);
        }

    }

}

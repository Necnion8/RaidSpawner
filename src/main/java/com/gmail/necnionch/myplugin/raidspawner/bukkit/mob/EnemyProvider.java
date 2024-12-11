package com.gmail.necnionch.myplugin.raidspawner.bukkit.mob;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class EnemyProvider<E extends Enemy> {

    private final String source;

    public EnemyProvider(@NotNull String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public abstract E create(ConfigurationSection config) throws ConfigurationError;


    public static class ConfigurationError extends Exception {
        public ConfigurationError(Throwable cause) {
            super(cause);
        }

        public ConfigurationError(String message) {
            super(message);
        }

        public ConfigurationError(String message, Throwable cause) {
            super(message, cause);
        }
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class ConditionProvider<C extends Condition> {

    private final String type;

    public ConditionProvider(@NotNull String type) {
        this.type = type;
    }

    public @NotNull String getType() {
        return type;
    }

    public abstract C create(ConfigurationSection config) throws ConfigurationError;


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

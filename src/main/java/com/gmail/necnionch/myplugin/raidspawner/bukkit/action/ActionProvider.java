package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ActionProvider<A extends Action> {

    private final String type;

    public ActionProvider(@NotNull String type) {
        this.type = type;
    }

    public @NotNull String getType() {
        return type;
    }

    public abstract A create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError;


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
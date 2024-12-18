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


    public static int parseInt(Object obj) throws ConfigurationError {
        try {
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            } else {
                return Integer.parseInt(String.valueOf(obj));
            }
        } catch (ClassCastException | NullPointerException e) {
            throw new ConfigurationError("Not number value: " + obj);
        } catch (NumberFormatException e) {
            throw new ConfigurationError(e);
        }
    }

    public static double parseDouble(Object obj) throws ConfigurationError {
        try {
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            } else {
                return Double.parseDouble(String.valueOf(obj));
            }
        } catch (ClassCastException | NullPointerException e) {
            throw new ConfigurationError("Not number value: " + obj);
        } catch (NumberFormatException e) {
            throw new ConfigurationError(e);
        }
    }

    public static long parseLong(Object obj) throws ConfigurationError {
        try {
            if (obj instanceof Number) {
                return ((Number) obj).longValue();
            } else {
                return Long.parseLong(String.valueOf(obj));
            }
        } catch (ClassCastException | NullPointerException e) {
            throw new ConfigurationError("Not number value: " + obj);
        } catch (NumberFormatException e) {
            throw new ConfigurationError(e);
        }
    }

    public static float parseFloat(Object obj) throws ConfigurationError {
        try {
            if (obj instanceof Number) {
                return ((Number) obj).floatValue();
            } else {
                return Float.parseFloat(String.valueOf(obj));
            }
        } catch (ClassCastException | NullPointerException e) {
            throw new ConfigurationError("Not number value: " + obj);
        } catch (NumberFormatException e) {
            throw new ConfigurationError(e);
        }
    }

    public static short parseShort(Object obj) throws ConfigurationError {
        try {
            if (obj instanceof Number) {
                return ((Number) obj).shortValue();
            } else {
                return Short.parseShort(String.valueOf(obj));
            }
        } catch (ClassCastException | NullPointerException e) {
            throw new ConfigurationError("Not number value: " + obj);
        } catch (NumberFormatException e) {
            throw new ConfigurationError(e);
        }
    }

}
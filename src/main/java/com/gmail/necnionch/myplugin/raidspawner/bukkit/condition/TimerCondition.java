package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimerCondition implements Condition {

    private final Provider provider;
    private final int minutes;
    private @Nullable Long startAt;

    public TimerCondition(Provider provider, int minutes) {
        this.provider = provider;
        this.minutes = minutes;
    }

    @Override
    public void start(Trigger trigger) {
        long delay = minutes * 60L * 1000;
        startAt = System.currentTimeMillis();
        trigger.actionOn(delay);
    }

    @Override
    public void clear() {
        startAt = null;
    }

    @Override
    public @Nullable Long getRemainingTimePreview() {
        return startAt != null ? startAt - System.currentTimeMillis() : null;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }

    public static class Provider extends ConditionProvider<TimerCondition> {

        public Provider() {
            super("timer");
        }

        @Override
        public TimerCondition create(ConfigurationSection config) throws ConfigurationError {
            int minutes = config.getInt("time-minutes");
            if (minutes < 1) {
                throw new ConfigurationError("Must be at least 1 minute");
            }
            return new TimerCondition(this, minutes);
        }

    }

}

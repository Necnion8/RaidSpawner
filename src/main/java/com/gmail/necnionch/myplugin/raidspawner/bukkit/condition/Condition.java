package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import me.angeschossen.lands.api.land.Land;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public interface Condition {
    void start(Trigger trigger);

    void clear();

    default void unload() {}

    @Nullable Long getRemainingTimePreview();

    @NotNull ConditionProvider<?> getProvider();


    class Trigger {
        private boolean activated;
        private @Nullable Long delayTime;
        private final Runnable action;
        private final @Nullable Land land;

        public Trigger(Runnable action, @Nullable Land land) {
            this.action = action;
            this.land = land;
        }

        public void actionOn(long millis) {
            if (activated)
                throw new IllegalStateException("Already activated");
            delayTime = millis;
        }

        public void actionOn(Date date) {
            if (activated)
                throw new IllegalStateException("Already activated");
            delayTime = date.getTime() - System.currentTimeMillis();
        }

        public void action() {
            if (activated)
                throw new IllegalStateException("Already activated");
            activated = true;
            action.run();
        }

        public boolean isActivated() {
            return activated;
        }

        public @Nullable Long getDelayTime() {
            return delayTime;
        }

        public @Nullable Land getLand() {
            return land;
        }

    }

}

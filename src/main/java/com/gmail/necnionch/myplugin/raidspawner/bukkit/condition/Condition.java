package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import org.jetbrains.annotations.Nullable;

import java.util.Date;

public interface Condition {
    void start(Trigger trigger);

    void clear();

    @Nullable Long getRemainingTimePreview();


    class Trigger {
        private boolean activated;
        private @Nullable Long delayTime;
        private final Runnable action;

        public Trigger(Runnable action) {
            this.action = action;
        }

        public void actionOn(long millis) {
            if (activated)
                throw new IllegalStateException("Already activated");
            delayTime = millis;
        }

        public void actionOn(Date date) {
            if (activated)
                throw new IllegalStateException("Already activated");
            delayTime = date.getTime();
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

        public void clear() {
            activated = false;
        }

        public @Nullable Long getDelayTime() {
            return delayTime;
        }

    }

}

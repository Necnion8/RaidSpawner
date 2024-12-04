package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class ConditionWrapper {

    private final Timer timer;
    private final Consumer<Runnable> runInMainThread;
    private final Condition condition;
    private final Consumer<ConditionWrapper> action;
    private @Nullable TimerTask timerTask;
    private @Nullable Condition.Trigger currentTrigger;

    public ConditionWrapper(Timer timer, Consumer<Runnable> runInMainThread, Condition condition, Consumer<ConditionWrapper> action) {
        this.timer = timer;
        this.runInMainThread = runInMainThread;
        this.condition = condition;
        this.action = action;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean isActivated() {
        return Optional.ofNullable(currentTrigger)
                .map(Condition.Trigger::isActivated)
                .orElse(false);
    }

    public void start() {
        clear();
        Condition.Trigger trigger = currentTrigger = new Condition.Trigger(() -> action.accept(this));
        condition.start(trigger);

        if (!trigger.isActivated() && trigger.getDelayTime() != null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runInMainThread.accept(trigger::action);
                }
            };
            timer.schedule(timerTask, trigger.getDelayTime());
        }
    }

    public void clear() {
        try {
            condition.clear();
        } finally {
            currentTrigger = null;
            if (timerTask != null) {
                timerTask.cancel();
            }
            timerTask = null;
        }
    }

    public void unload() {
        condition.unload();
    }

}

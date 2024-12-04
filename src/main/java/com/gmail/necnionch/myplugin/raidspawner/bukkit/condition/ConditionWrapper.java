package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class ConditionWrapper {

    private final Timer timer;
    private final Condition condition;
    private final Consumer<ConditionWrapper> action;
    private @Nullable TimerTask timerTask;
    private @Nullable Condition.Trigger currentTrigger;

    public ConditionWrapper(Timer timer, Condition condition, Consumer<ConditionWrapper> action) {
        this.timer = timer;
        this.condition = condition;
        this.action = action;
    }

    public Condition getCondition() {
        return condition;
    }

    public ConditionProvider<?> getConditionProvider() {
        return condition.getProvider();
    }

    public String getType() {
        return getConditionProvider().getType();
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
                    RaidSpawnerUtil.runInMainThread(trigger::action);
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
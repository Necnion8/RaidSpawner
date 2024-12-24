package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.action.Action;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class ConditionWrapper {

    private final Timer timer;
    private final Condition condition;
    private final Consumer<ConditionWrapper> handler;
    private @Nullable TimerTask timerTask;
    private @Nullable Condition.Trigger currentTrigger;
    private final List<Action> actions;

    public ConditionWrapper(Timer timer, Condition condition, Consumer<ConditionWrapper> handler, List<Action> actions) {
        this.timer = timer;
        this.condition = condition;
        this.handler = handler;
        this.actions = actions;
    }

    public ConditionWrapper(Timer timer, Condition condition, Consumer<ConditionWrapper> handler) {
        this(timer, condition, handler, new ArrayList<>());
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

    public @Nullable Long start() {
        if (currentTrigger != null && currentTrigger.isActivated()) {
            clear();
        }
        Condition.Trigger trigger = currentTrigger = new Condition.Trigger(() -> this.handler.accept(this), null);
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

        return trigger.getDelayTime();
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

    public List<Action> actions() {
        return actions;
    }
}

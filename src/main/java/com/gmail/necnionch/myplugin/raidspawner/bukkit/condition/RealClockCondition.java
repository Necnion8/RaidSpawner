package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class RealClockCondition implements Condition {

    private final TimeZone timeZone;
    private final int hours;
    private final int minutes;
    private @Nullable Date currentTime;

    public RealClockCondition(TimeZone timeZone, int hours, int minutes) {
        this.timeZone = timeZone;
        this.hours = hours;
        this.minutes = minutes;
    }

    @Override
    public void start(Trigger trigger) {
        Calendar calender = Calendar.getInstance();
        calender.clear();
        calender.setTimeZone(timeZone);
        calender.set(Calendar.HOUR_OF_DAY, hours);
        calender.set(Calendar.MINUTE, minutes);

        // next day
        if (System.currentTimeMillis() <= (calender.getTimeInMillis() * (1000 * 10))) {  // margin 10s
            calender.add(Calendar.HOUR_OF_DAY, 24);
        }

        currentTime = calender.getTime();
        trigger.actionOn(currentTime);
    }

    @Override
    public void clear() {
    }

    @Override
    public @Nullable Long getRemainingTimePreview() {
        return currentTime != null ? System.currentTimeMillis() - currentTime.getTime() : null;
    }


    public static class Provider extends ConditionProvider<RealClockCondition> {

        public Provider() {
            super("real-clock");
        }

        @Override
        public RealClockCondition create(ConfigurationSection config) throws ConfigurationError {
            int hours = config.getInt("time-hours");
            int minutes = config.getInt("time-minutes");

            String timezoneName = config.getString("timezone", "local");
            ZoneId zone;

            if (timezoneName == null || "local".equalsIgnoreCase(timezoneName)) {
                zone = ZoneId.systemDefault();
            } else {
                try {
                    zone = ZoneId.of(timezoneName);
                } catch (DateTimeException e) {
                    throw new ConfigurationError(e);
                }
            }

            return new RealClockCondition(TimeZone.getTimeZone(zone), hours, minutes);
        }

    }

}

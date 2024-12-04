package com.gmail.necnionch.myplugin.raidspawner.bukkit.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class RealClockCondition implements Condition {

    private final Provider provider;
    private final TimeZone timeZone;
    private final int hours;
    private final int minutes;
    private @Nullable Date targetTime;

    public RealClockCondition(Provider provider, TimeZone timeZone, int hours, int minutes) {
        this.provider = provider;
        this.timeZone = timeZone;
        this.hours = hours;
        this.minutes = minutes;
    }

    @Override
    public void start(Trigger trigger) {
        Calendar calender = Calendar.getInstance(timeZone);
        long currentMillis = calender.getTimeInMillis();
        calender.set(Calendar.HOUR_OF_DAY, hours);
        calender.set(Calendar.MINUTE, minutes);
        calender.set(Calendar.SECOND, 0);
        calender.set(Calendar.MILLISECOND, 0);

        // next day
        if (calender.getTimeInMillis() - currentMillis < 1000) {  // margin 1s
            calender.add(Calendar.HOUR_OF_DAY, 24);
        }

        targetTime = calender.getTime();
        trigger.actionOn(targetTime);
    }

    @Override
    public void clear() {
        targetTime = null;
    }

    @Override
    public @Nullable Long getRemainingTimePreview() {
        return targetTime != null ? targetTime.getTime() - System.currentTimeMillis() : null;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
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

            return new RealClockCondition(this, TimeZone.getTimeZone(zone), hours, minutes);
        }

    }

}

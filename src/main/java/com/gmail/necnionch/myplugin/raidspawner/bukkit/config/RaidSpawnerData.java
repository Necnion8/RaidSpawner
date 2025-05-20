package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.common.BukkitConfigDriver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RaidSpawnerData extends BukkitConfigDriver {

    private @Nullable LocalDate eventPauseAll;
    private final Map<String, LocalDate> eventPauseLands = new HashMap<>();

    public RaidSpawnerData(JavaPlugin plugin) {
        super(plugin, "data.yml", "data.yml");
    }

    @Override
    public boolean onLoaded(FileConfiguration config) {
        Object tmp = config.get("pause-event.all", null);
        eventPauseAll = null;
        if (tmp != null) {
            if (tmp instanceof Date) {
                eventPauseAll = ((Date) tmp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                try {
                    eventPauseAll = LocalDate.parse((String) tmp, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    getLogger().warning("Invalid date format: pause-event.all: " + e.getMessage());
                }
            }
        }

        eventPauseLands.clear();
        ConfigurationSection section = config.getConfigurationSection("pause-event.lands");
        if (section != null) {
            for (String landName : section.getKeys(false)) {
                tmp = section.get(landName, "");
                LocalDate pauseDate;
                if (tmp instanceof Date) {
                    pauseDate = ((Date) tmp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    try {
                        pauseDate = LocalDate.parse((String) tmp, DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (DateTimeParseException e) {
                        getLogger().warning("Invalid date format: pause-event.lands." + landName + ": " + e.getMessage());
                        continue;
                    }
                }
                eventPauseLands.put(landName, pauseDate);
            }
        }

        return true;
    }

    @Nullable
    public LocalDate getEventPauseDate() {
        return eventPauseAll;
    }

    public void setEventPauseData(@Nullable LocalDate date) {
        this.eventPauseAll = date;
    }

    public Map<String, LocalDate> eventPauseLandsDate() {
        return eventPauseLands;
    }

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LandCommandAction implements LandAction {

    private final Provider provider;
    private final List<String> commands;

    public LandCommandAction(Provider provider, List<String> commands) {
        this.provider = provider;
        this.commands = commands;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Land land) {
        for (String command : commands) {
            command = RaidSpawnerUtil.processPlaceholder(null, command);
            command = command.replace("%land%", land.getName());

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<LandCommandAction> {

        public Provider() {
            super("command");
        }

        @Override
        public LandCommandAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            List<String> list;
            if (value instanceof List<?>) {
                list = ((List<?>) value).stream().map(String::valueOf).toList();
            } else {
                try {
                    list = Collections.singletonList((String) value);
                } catch (ClassCastException e) {
                    throw new ConfigurationError("Not string value: " + value);
                }
            }
            return new LandCommandAction(this, list);
        }

    }

}

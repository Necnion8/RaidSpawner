package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PlayerCommandAction implements PlayerAction {

    private final Provider provider;
    private final List<String> commands;

    public PlayerCommandAction(Provider provider, List<String> commands) {
        this.provider = provider;
        this.commands = commands;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Player player) {
        for (String command : commands) {
            command = RaidSpawnerUtil.processPlaceholder(player, command);
            command = command.replace("%uuid%", player.getUniqueId().toString());
            command = command.replace("%player%", player.getName());

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerCommandAction> {

        public Provider() {
            super("command");
        }

        @Override
        public PlayerCommandAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            List<String> list;
            if (value instanceof List<?>) {
                list = ((List<?>) value).stream().map(String::valueOf).toList();
            } else {
                list = Collections.singletonList(String.valueOf(value));
            }
            return new PlayerCommandAction(this, list);
        }

    }

}

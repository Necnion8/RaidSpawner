package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawner;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PlayerExecuteCommandAction implements PlayerAction {

    private final Provider provider;
    private final List<String> commands;

    public PlayerExecuteCommandAction(Provider provider, List<String> commands) {
        this.provider = provider;
        this.commands = commands;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Player player) {
        for (String command : commands) {
            command = RaidSpawnerUtil.processPlaceholder(player, command);
            command = command.replace("%uuid%", player.getUniqueId().toString());
            command = command.replace("%player%", player.getName());

            Bukkit.dispatchCommand(player, command);
        }
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerExecuteCommandAction> {

        public Provider() {
            super("execute-command");
        }

        @Override
        public PlayerExecuteCommandAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            List<String> list;
            if (value instanceof List<?>) {
                list = ((List<?>) value).stream().map(String::valueOf).toList();
            } else {
                list = Collections.singletonList(String.valueOf(value));
            }
            return new PlayerExecuteCommandAction(this, list);
        }

    }

}

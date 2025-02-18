package com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class DiscordSRVBridge implements PluginBridge, JDAInterface {
    private final Logger log;
    private boolean hooked;

    public DiscordSRVBridge(Logger log) {
        this.log = log;
    }

    @Override
    public boolean hook() {
        hooked = false;
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV"))
            return false;

        try {
            Class.forName("github.scarsz.discordsrv.DiscordSRV");
        } catch (ClassNotFoundException | IllegalStateException e) {
            return false;
        }

        hooked = true;
        return true;
    }

    @Override
    public void unhook() {
        hooked = false;
    }

    @Override
    public boolean isHooked() {
        return hooked && DiscordSRV.getPlugin().isEnabled();
    }

    @Override
    public String getPluginName() {
        return "DiscordSRV";
    }


    public @Nullable JDA getJDA() {
        if (!isHooked())
            return null;

        return DiscordSRV.getPlugin().getJda();
    }

    @Override
    public void sendMessage(String message, long channelId) {
        JDA jda = getJDA();
        if (jda == null) {
            log.warning("Unable to send discord message: JDA is not available");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.warning("Unable to send discord message: Text channel (" + channelId + ") not found");
            return;
        }

        channel.sendMessage(message).queue(
                m -> {},
                err -> log.warning("Unable to send discord message: " + err.getMessage())
        );
    }

    @Override
    public void sendMessage(Message message, long channelId) {
        JDA jda = getJDA();
        if (jda == null) {
            log.warning("Unable to send discord message: JDA is not available");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.warning("Unable to send discord message: Text channel (" + channelId + ") not found");
            return;
        }

        channel.sendMessage(message).queue(
                m -> {},
                err -> log.warning("Unable to send discord message: " + err.getMessage())
        );
    }

}

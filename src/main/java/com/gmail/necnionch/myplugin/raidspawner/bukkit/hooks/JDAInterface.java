package com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;

public interface JDAInterface {
    void sendMessage(String message, long channelId);

    void sendMessage(Message message, long channelId);

}

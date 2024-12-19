package com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class LuckPermsBridge implements PluginBridge {
    private LuckPerms api;

    @Override
    public boolean hook() {
        api = null;
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms"))
            return false;
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            api = LuckPermsProvider.get();
        } catch (ClassNotFoundException | IllegalStateException e) {
            return false;
        }
        return true;
    }

    @Override
    public void unhook() {
        api = null;
    }

    @Override
    public boolean isHooked() {
        return api != null;
    }

    @Override
    public String getPluginName() {
        return "LuckPerms";
    }


    public boolean addPermissionGroup(OfflinePlayer player, String groupName) {
        return addPermissionGroup(player.getUniqueId(), groupName);
    }

    public boolean addPermissionGroup(UUID playerId, String groupName) {
        Group group = api.getGroupManager().getGroup(groupName);
        if (group != null) {
            api.getUserManager().modifyUser(playerId, user -> {
                user.data().add(Node.builder("group." + group.getName()).build());
            });
            return true;
        }
        return false;
    }

    public boolean removePermissionGroup(OfflinePlayer player, String groupName) {
        return removePermissionGroup(player.getUniqueId(), groupName);
    }

    public boolean removePermissionGroup(UUID playerId, String groupName) {
        Group group = api.getGroupManager().getGroup(groupName);
        if (group != null) {
            api.getUserManager().modifyUser(playerId, user -> {
                user.data().remove(Node.builder("group." + group.getName()).build());
            });
            return true;
        }
        return false;
    }

}

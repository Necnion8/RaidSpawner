package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import org.bukkit.entity.Player;

public interface PlayerAction extends Action {

    boolean doAction(RaidSpawner spawner, Player player);

}

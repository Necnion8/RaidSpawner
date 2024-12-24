package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import me.angeschossen.lands.api.land.Land;

public interface LandAction extends Action {

    boolean doAction(RaidSpawner spawner, Land land);

}

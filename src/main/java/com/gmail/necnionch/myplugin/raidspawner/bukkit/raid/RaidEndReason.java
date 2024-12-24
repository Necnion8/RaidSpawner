package com.gmail.necnionch.myplugin.raidspawner.bukkit.raid;

public interface RaidEndReason {

    String getType();

    RaidEndReason TIMEOUT = () -> "timeout";
    RaidEndReason NO_PLAYERS = () -> "no_players";
    RaidEndReason FULL_WAVES = () -> "full_waves";

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

public class EventStart {

    public record PreNotify(boolean enable, int minutes) {
    }

    public record StartNotify(boolean titleObfuscatedAnimation, boolean bindEffect, boolean teleportToLand) {
    }

}

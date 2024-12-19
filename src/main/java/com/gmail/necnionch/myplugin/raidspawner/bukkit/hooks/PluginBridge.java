package com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface PluginBridge {

    boolean hook();

    void unhook();

    boolean isHooked();

    String getPluginName();


    Map<Class<?>, PluginBridge> BRIDGES = new HashMap<>();

    static <B extends PluginBridge> Optional<B> get(Class<B> bridgeClass) {
        //noinspection unchecked
        return Optional.ofNullable((B) BRIDGES.get(bridgeClass));
    }

    static <B extends PluginBridge> Optional<B> getValid(Class<B> bridgeClass) {
        //noinspection unchecked
        return Optional.ofNullable((B) BRIDGES.get(bridgeClass)).filter(PluginBridge::isHooked);
    }

    static <B extends PluginBridge> void put(B bridge) {
        BRIDGES.put(bridge.getClass(), bridge);
    }

}

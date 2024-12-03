package com.gmail.necnionch.myplugin.raidspawner.bukkit.config;

import com.gmail.necnionch.myplugin.raidspawner.commit.BukkitConfigDriver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginConfig extends BukkitConfigDriver {

    public PluginConfig(JavaPlugin plugin) {
        super(plugin);
    }

    private static List<ConfigurationSection> getConfigList(ConfigurationSection parent, String key) {
        /*
          https://bukkit.org/threads/getting-a-list-of-configurationsections.157524/
         */
        List<?> list = parent.getList(key);
        return list != null ? list.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> {
                    MemoryConfiguration child = new MemoryConfiguration();
                    //noinspection unchecked,rawtypes
                    child.addDefaults(((Map) obj));
                    return child;
                })
                .collect(Collectors.toList()) : null;
    }

    //

    public List<ConfigurationSection> getEventStartConditions() {
        return Optional.ofNullable(config.getConfigurationSection("event-start"))
                .map(c -> getConfigList(c, "conditions"))
                .orElseGet(ArrayList::new);
    }

}

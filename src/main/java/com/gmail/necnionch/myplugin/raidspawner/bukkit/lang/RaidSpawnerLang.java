package com.gmail.necnionch.myplugin.raidspawner.bukkit.lang;

import com.gmail.necnionch.myplugin.raidspawner.common.BukkitConfigDriver;
import com.google.common.base.Charsets;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RaidSpawnerLang extends BukkitConfigDriver {
    public RaidSpawnerLang(JavaPlugin plugin) {
        super(plugin, "lang.yml", "lang.yml");
    }

    public String get(Lang lang) {
        return config.getString(lang.getKey(), lang.getDefaultText());
    }

    public String format(Lang lang, Object... args) {
        return String.format(ChatColor.translateAlternateColorCodes('&', get(lang)), args);
    }

    @Override
    public void generateNewFile(File file) throws IOException {
        if (!generateResourceFile(file)) {
            YamlConfiguration config = new YamlConfiguration();

            for (Lang lang : Lang.values()) {
                config.set(lang.getKey(), lang.getDefaultText());
            }

            try (OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
                stream.write(config.saveToString());
            }
        }
    }

}

package com.gmail.necnionch.myplugin.raidspawner.common;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Logger;


// version 4 (generateNewFile, generateResourceFile)
// version 3 (add:isExistFile(), getLogger())
@SuppressWarnings({"ResultOfMethodCallIgnored", "UnstableApiUsage"})
public class BukkitConfigDriver {
    private final JavaPlugin plugin;
    private final Logger logger;
    protected String fileName = "config.yml";
    protected String resourceFileName = "bukkit-config.yml";
    public FileConfiguration config = null;

    private String header = null;

    public BukkitConfigDriver(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public BukkitConfigDriver(JavaPlugin plugin, String fileName, String resourceFileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.resourceFileName = resourceFileName;
        this.logger = plugin.getLogger();
    }

    // add: v3
    public Logger getLogger() {
        return logger;
    }

    // add: v3
    public boolean isExistFile() {
        return new File(plugin.getDataFolder(), fileName).isFile();
    }

    // add: v4
    public void generateNewFile(File file) throws IOException {
        generateResourceFile(file);
    }

    // add: v4
    public boolean generateResourceFile(File file) throws IOException {
        file.createNewFile();
        try (InputStream resource = plugin.getResource(resourceFileName)) {
            if (resource == null)
                return false;
            try (OutputStream outputStream = new FileOutputStream(file)) {
                ByteStreams.copy(resource, outputStream);
            }
        }
        return true;
    }

    public boolean load() {
        try {
            if (!plugin.getDataFolder().exists())
                plugin.getDataFolder().mkdir();

            File file = new File(plugin.getDataFolder(), fileName);

            if (!file.exists()) {
                generateNewFile(file);
            }

            FileConfiguration config;
            if (file.exists()) {
                try (InputStreamReader stream = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8)) {
                    config = YamlConfiguration.loadConfiguration(stream);
                }
            } else {
                config = new YamlConfiguration();
            }

            this.config = config;
            return onLoaded(config);
        } catch (Exception e) {
            logger.severe("Could not load \"" + fileName + "\".");
            logger.severe(e.getClass().getName() + ": " + e.getLocalizedMessage());
            return false;
        }
    }

    public boolean save() {
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdir();

        File file = new File(plugin.getDataFolder(), fileName);
        if (config == null) return false;

        try (OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            stream.write(config.saveToString());
            return true;
        } catch (Exception e) {
            logger.severe("Could not save \"" + fileName + "\".");
            logger.severe(e.getClass().getName() + ": " + e.getLocalizedMessage());
            return false;
        }
    }

    public boolean onLoaded(FileConfiguration config) {
        return true;
    }


    public void header(String text) {header = text;}

    public String header() {return header;}

    public void addHeaderText(String title, String... comments) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        for (String c : comments)
            sb.append("  ").append(c).append("\n");

        sb.append("\n");

        String header = this.header;
        if (header == null) {
            header = "\n" + sb;
        } else if (!header.endsWith("\n")) {
            header += "\n" + sb;
        } else {
            header += sb.toString();
        }
        this.header = header;
    }

    public void saveHeaderIfNotContains(boolean save) {
        if (config != null && this.header != null) {
            String header = config.options().header();
            if (header == null || !header.contains(this.header)) {
                config.options().header(this.header);
                if (save)
                    save();
            }
        }
    }
}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.test;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkViewer extends MapRenderer {

    private final RaidSpawnerPlugin plugin;
    private final Player player;
    private final MinecraftFont font = MinecraftFont.Font;
    private int mapScaling = 3;

    private final Map<String, Land> chunks = new HashMap<>();

    public ChunkViewer(RaidSpawnerPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        plugin.getLandAPI().getLands().stream()
                .filter(land -> land.getContainer(player.getWorld()) != null)
                .forEach(land -> {
                    for (ChunkCoordinate chunk : land.getContainer(player.getWorld()).getChunks()) {
                        chunks.put(chunk.getX() + "," + chunk.getZ(), land);
                    }
                });
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!plugin.isEnabled()) {
            canvas.drawText(0, 0, font, "Plugin disabled");
            return;
        }

        Location location = player.getLocation();
        Chunk chunk = location.getChunk();

        // set cursor
        float yaw = location.getYaw();
        yaw = (yaw + 11.25f) % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        MapCursorCollection cursors = new MapCursorCollection();
        cursors.addCursor(new MapCursor((byte) 1, (byte) 1, (byte) (int) (yaw / 360 * 16), MapCursor.Type.WHITE_POINTER, true));
        canvas.setCursors(cursors);

        // draw chunk
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {

                int posX = location.getBlockX() + (x - 64) * mapScaling;
                int posZ = location.getBlockZ() + (y - 64) * mapScaling;

                int chunkX = (int) Math.floor(posX / 16f);
                int chunkZ = (int) Math.floor(posZ / 16f);

                boolean highlight = Math.floorMod(chunkX, 2) == Math.floorMod(chunkZ, 2);
                byte colorValue;
                if (chunks.containsKey(chunkX + "," + chunkZ)) {
                    colorValue = highlight ? MapPalette.LIGHT_GREEN : MapPalette.DARK_GREEN;
                } else if (plugin.spawnChunks.stream().anyMatch(pos -> pos.x() == chunkX && pos.z() == chunkZ)) {
                    colorValue = highlight ? MapPalette.matchColor(141, 127, 199) : MapPalette.matchColor(80, 44, 230);
                } else {
                    colorValue = highlight ? MapPalette.GRAY_1 : MapPalette.GRAY_2;
                }
                canvas.setPixel(x, y, colorValue);
            }
        }

        // draw land name
        Set<Land> notifiedLands = new HashSet<>();
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int posX = location.getBlockX() + (x - 64) * mapScaling;
                int posZ = location.getBlockZ() + (y - 64) * mapScaling;
                int chunkX = (int) Math.floor(posX / 16f);
                int chunkZ = (int) Math.floor(posZ / 16f);

                Land land = chunks.get(chunkX + "," + chunkZ);
                if (land != null && !notifiedLands.contains(land)) {
                    int x2 = x, y2 = y;
                    canvas.drawText(x2, y2, font, colored(MapPalette.LIGHT_BROWN, land.getName()));
                    notifiedLands.add(land);
                }
            }
        }


        // draw text
        String text = chunk.getX() + ", " + chunk.getZ();
        canvas.drawText(128 - font.getWidth(text), 128 - font.getHeight(), font, colored(MapPalette.RED, text));



    }

    private static String colored(byte colorMagicValue, String text) {
        return String.format("§%1$s;", colorMagicValue) + text;
    }



}

package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkViewRenderer extends MapRenderer {

    private final RaidSpawnerPlugin plugin;
    private final MinecraftFont font = MinecraftFont.Font;
    private int chunkScale = 3;

    private final Map<String, Land> chunks = new HashMap<>();

    public ChunkViewRenderer(RaidSpawnerPlugin plugin, World world) {
        this.plugin = plugin;

        plugin.getLandAPI().getLands().stream()
                .filter(land -> land.getContainer(world) != null)
                .forEach(land -> {
                    for (ChunkCoordinate chunk : land.getContainer(world).getChunks()) {
                        chunks.put(chunk.getX() + "," + chunk.getZ(), land);
                    }
                });
    }

    public int getChunkScale() {
        return chunkScale;
    }

    public void setChunkScale(int scale) {
        this.chunkScale = scale;
    }

    private void renderDisabled(MapCanvas canvas) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixel(x, y, MapPalette.matchColor(0, 0, 0));
            }
        }
        canvas.drawText(0, 0, font, colored(MapPalette.RED, "Plugin disabled"));
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!plugin.isEnabled()) {
            renderDisabled(canvas);
            return;
        }

        Location location = player.getLocation();
        renderCursor(canvas, location);
        renderChunks(canvas, location);
        renderLandNames(canvas, location);
        renderTexts(canvas, location);
    }

    private void renderCursor(MapCanvas canvas, Location location) {
        float yaw = location.getYaw();
        yaw = (yaw + 11.25f) % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        MapCursorCollection cursors = new MapCursorCollection();
        cursors.addCursor(new MapCursor((byte) 1, (byte) 1, (byte) (int) (yaw / 360 * 16), MapCursor.Type.WHITE_POINTER, true));
        canvas.setCursors(cursors);
    }

    private void renderChunks(MapCanvas canvas, Location location) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int posX = location.getBlockX() + (x - 64) * chunkScale;
                int posZ = location.getBlockZ() + (y - 64) * chunkScale;
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
    }

    private void renderLandNames(MapCanvas canvas, Location location) {
        Set<Land> notifiedLands = new HashSet<>();
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int posX = location.getBlockX() + (x - 64) * chunkScale;
                int posZ = location.getBlockZ() + (y - 64) * chunkScale;
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
    }

    private void renderTexts(MapCanvas canvas, Location location) {
        Chunk chunk = location.getChunk();
        String text = chunk.getX() + ", " + chunk.getZ();
        canvas.drawText(128 - font.getWidth(text), 128 - font.getHeight(), font, colored(MapPalette.RED, text));
    }


    private static String colored(byte colorMagicValue, String text) {
        return String.format("§%1$s;", colorMagicValue) + text;
    }

}

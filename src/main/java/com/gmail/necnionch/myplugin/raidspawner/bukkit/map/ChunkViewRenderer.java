package com.gmail.necnionch.myplugin.raidspawner.bukkit.map;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("deprecation")
public class ChunkViewRenderer extends MapRenderer {
    public static final Set<ChunkViewRenderer> RENDERERS = new HashSet<>();

    private final RaidSpawnerPlugin plugin;
    private final MinecraftFont font = MinecraftFont.Font;
    private final World world;
    private final Map<Land, RaidSpawner> raids;
    private int chunkScale = 3;

    private final Map<String, Land> landChunks = new HashMap<>();

    public ChunkViewRenderer(RaidSpawnerPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        this.raids = plugin.getCurrentRaids();
        RENDERERS.add(this);
    }

    public int getChunkScale() {
        return chunkScale;
    }

    public void setChunkScale(int scale) {
        this.chunkScale = scale;
    }

    public void updateLandsList() {
        landChunks.clear();
        plugin.getLandAPI().getLands().forEach(land -> Optional.ofNullable(land.getContainer(world))
                .map(Container::getChunks)
                .map(Collection::stream)
                .ifPresent(s -> s.forEach(c -> landChunks.put(c.getX() + "," + c.getZ(), land))));
    }

    @Override
    public void initialize(@NotNull MapView map) {
        updateLandsList();
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!plugin.isEnabled()) {
            renderDisabled(canvas);
            RENDERERS.remove(this);
            return;
        }

        Location location = player.getLocation();
        renderCursor(canvas, location);
        renderChunks(canvas, location);
        renderLandNames(canvas, location);
        renderTexts(canvas, location);
    }

    private void renderDisabled(MapCanvas canvas) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixel(x, y, MapPalette.matchColor(0, 0, 0));
            }
        }
        canvas.drawText(0, 0, font, colored(MapPalette.RED, "Plugin disabled"));
    }

    private void renderCursor(MapCanvas canvas, Location location) {
        MapCursorCollection cursors = new MapCursorCollection();

        double centerX = location.getX();
        double centerZ = location.getZ();
        // 生きてるモブを表示
        raids.values().stream().flatMap(s -> s.currentEnemies().stream())
                .filter(Enemy::isAlive)
                .map(Enemy::getEntityLocation)
                .filter(Objects::nonNull)
                .forEach(pos -> {
//                    int x = (int) Math.max(-128, Math.min((pos.getX() - centerX) / (chunkScale * 16), 127));
                    int x = (int) Math.max(-128, Math.min((pos.getX() - centerX) / (128f / 16 / chunkScale), 127));  // TODO: 微妙にズレてる
                    int z = (int) Math.max(-128, Math.min((pos.getZ() - centerZ) / (128f / 16 / chunkScale), 127));

                    cursors.addCursor(new MapCursor(
                            (byte) x,
                            (byte) z,
                            (byte) getMapCursorDirection(pos.getYaw()),
                            MapCursor.Type.RED_POINTER,
                            true
                    ));
                });

        cursors.addCursor(new MapCursor((byte) 1, (byte) 1, (byte) getMapCursorDirection(location.getYaw()), MapCursor.Type.WHITE_POINTER, true));
        canvas.setCursors(cursors);
    }

    private void renderChunks(MapCanvas canvas, Location location) {
        Multimap<String, RaidSpawner.Chunk> spawnChunks = plugin.getLastFindSpawnChunksResult();

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int posX = location.getBlockX() + (x - 64) * chunkScale;
                int posZ = location.getBlockZ() + (y - 64) * chunkScale;
                int chunkX = (int) Math.floor(posX / 16f);
                int chunkZ = (int) Math.floor(posZ / 16f);

                boolean highlight = Math.floorMod(chunkX, 2) == Math.floorMod(chunkZ, 2);
                byte colorValue;
                if (landChunks.containsKey(chunkX + "," + chunkZ)) {
                    colorValue = highlight ? MapPalette.LIGHT_GREEN : MapPalette.DARK_GREEN;
                } else if (spawnChunks != null && spawnChunks.values().stream().anyMatch(c -> world.equals(c.world()) && c.x() == chunkX && c.z() == chunkZ)) {
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

                Land land = landChunks.get(chunkX + "," + chunkZ);
                if (land != null && !notifiedLands.contains(land)) {
                    canvas.drawText(x, y, font, colored(MapPalette.LIGHT_BROWN, land.getName()));
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

    private static int getMapCursorDirection(float yaw) {
        yaw = (yaw + 11.25f) % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        return (int) (yaw / 360 * 16);
    }

}

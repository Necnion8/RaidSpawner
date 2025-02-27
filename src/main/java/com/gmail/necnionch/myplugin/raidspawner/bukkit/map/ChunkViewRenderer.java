package com.gmail.necnionch.myplugin.raidspawner.bukkit.map;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerAPI;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.LandChunkFindResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("deprecation")
public class ChunkViewRenderer extends MapRenderer {
    public static final Set<ChunkViewRenderer> RENDERERS = new HashSet<>();
    private static final Map<World, LandChunkFindResult> findResults = new HashMap<>();
    private static final Map<World, Map<String, Land>> chunkLands = new HashMap<>();
    private final MinecraftFont font = MinecraftFont.Font;
    private final Map<Land, RaidSpawner> raids;
    private int chunkScale = 3;

    public ChunkViewRenderer(RaidSpawnerAPI api) {
        this.raids = api.getCurrentRaids();
        RENDERERS.add(this);
    }

    public static void clearAll() {
        RENDERERS.clear();
        setChunksAndLands(null, null);
    }

    public int getChunkScale() {
        return chunkScale;
    }

    public void setChunkScale(int scale) {
        this.chunkScale = scale;
    }

    public static void setChunksAndLands(@Nullable Iterable<LandChunkFindResult> results, @Nullable Collection<Land> lands) {
        findResults.clear();
        if (results != null) {
            results.forEach(result -> findResults.put(result.world(), result));
        }
        chunkLands.clear();
        if (lands != null) {
            lands.stream()
                    .flatMap(land -> land.getContainers().stream())
                    .forEach(container -> {
                        Map<String, Land> chunksLand = chunkLands.computeIfAbsent(container.getWorld().getWorld(), w -> new HashMap<>());
                        container.getChunks().stream().map(c -> c.getX() + "," + c.getZ()).forEach(k -> chunksLand.put(k, container.getLand()));
                    });
        }
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (!RENDERERS.contains(this)) {
            renderDisabled(canvas);
            return;
        }

        Location location = player.getLocation();
        World world = player.getWorld();

        renderCursor(canvas, world, location);
        renderChunks(canvas, findResults.get(world), location);

        Map<String, Land> lands;
        if ((lands = chunkLands.get(world)) != null) {
            renderLandNames(canvas, lands, location);
        }

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

    private void renderCursor(MapCanvas canvas, World world, Location location) {
        MapCursorCollection cursors = new MapCursorCollection();

        double centerX = location.getX();
        double centerZ = location.getZ();
        // 生きてるモブを表示
        raids.values().stream().flatMap(s -> s.currentEnemies().stream())
                .filter(Enemy::isAlive)
                .map(Enemy::getEntityLocation)
                .filter(Objects::nonNull)
                .filter(loc -> world.equals(loc.getWorld()))
                .forEach(pos -> {
                    int x = (int) Math.floor((pos.getX() - centerX) / (chunkScale / 2f));
                    int z = (int) Math.floor((pos.getZ() - centerZ) / (chunkScale / 2f));
                    cursors.addCursor(new MapCursor(
                            (byte) Math.max(-128, Math.min(x, 127)),
                            (byte) Math.max(-128, Math.min(z, 127)),
                            (byte) getMapCursorDirection(pos.getYaw()),
                            MapCursor.Type.RED_POINTER,
                            true
                    ));

                });

        cursors.addCursor(new MapCursor((byte) 1, (byte) 1, (byte) getMapCursorDirection(location.getYaw()), MapCursor.Type.WHITE_POINTER, true));
        canvas.setCursors(cursors);
    }

    private void renderChunks(MapCanvas canvas, @Nullable LandChunkFindResult result, Location location) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int posX = location.getBlockX() + (x - 64) * chunkScale;
                int posZ = location.getBlockZ() + (y - 64) * chunkScale;
                int chunkX = (int) Math.floor(posX / 16f);
                int chunkZ = (int) Math.floor(posZ / 16f);

                boolean highlight = Math.floorMod(chunkX, 2) == Math.floorMod(chunkZ, 2);
                byte colorValue = highlight ? MapPalette.GRAY_1 : MapPalette.GRAY_2;
                if (result != null) {
                    if (result.landChunks().stream().anyMatch(c -> c.getX() == chunkX && c.getZ() == chunkZ)) {
                        colorValue = highlight ? MapPalette.LIGHT_GREEN : MapPalette.DARK_GREEN;
                    } else if (result.raidChunks().stream().anyMatch(c -> c.x() == chunkX && c.z() == chunkZ)) {
                        colorValue = highlight ? MapPalette.matchColor(141, 127, 199) : MapPalette.matchColor(80, 44, 230);
                    }
                }
                canvas.setPixel(x, y, colorValue);
            }
        }
    }

    private void renderLandNames(MapCanvas canvas, Map<String, Land> chunkLands, Location location) {
        Set<Land> notifiedLands = new HashSet<>();
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int posX = location.getBlockX() + (x - 64) * chunkScale;
                int posZ = location.getBlockZ() + (y - 64) * chunkScale;
                int chunkX = (int) Math.floor(posX / 16f);
                int chunkZ = (int) Math.floor(posZ / 16f);

                Land land = chunkLands.get(chunkX + "," + chunkZ);
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

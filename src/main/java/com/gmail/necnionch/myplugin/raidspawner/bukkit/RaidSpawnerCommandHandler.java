package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.config.RaidSpawnerData;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.lang.Lang;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.mob.Enemy;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndReason;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RaidSpawnerCommandHandler implements TabExecutor {

    private final RaidSpawnerAPI api;

    public RaidSpawnerCommandHandler(RaidSpawnerAPI api) {
        this.api = api;
    }

    public Land getLandOrError(int index, String[] args) {
        if (index < args.length) {
            String landName = args[index];
            return Optional.ofNullable(api.getLandAPI().getLandByName(landName))
                    .orElseThrow(() -> new ArgumentError(Lang.COMMAND_NOT_EXISTS_LAND, landName));
        }
        throw new ArgumentError(Lang.COMMAND_NOT_SPECIFIED_LAND);
    }

    public RaidEndResult parseEndResultOrError(String resultName) {
        try {
            return RaidEndResult.valueOf(resultName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ArgumentError(Lang.COMMAND_INVALID_STOP_RESULT);
        }
    }

    public Player getPlayer(CommandSender sender) {
        if (sender instanceof Player)
            return ((Player) sender);
        throw new ArgumentError(Lang.NON_PLAYER_SENDER_ERROR);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (1 <= args.length && ("status".equalsIgnoreCase(args[0]) || "s".equalsIgnoreCase(args[0]))) {
                executeStatusCommand(sender);
            } else if (1 <= args.length && ("startall".equalsIgnoreCase(args[0]) || "allstart".equalsIgnoreCase(args[0]))) {
                executeStartAllCommand(sender);
            } else if (1 <= args.length && "start".equalsIgnoreCase(args[0])) {
                Land land = getLandOrError(1, args);
                executeStartCommand(sender, land);
            } else if (1 <= args.length && "nextwave".equalsIgnoreCase(args[0])) {
                Land land = getLandOrError(1, args);
                executeSetWaveCommand(sender, land, -1);
            } else if (1 <= args.length && "setwave".equalsIgnoreCase(args[0])) {
                Land land = getLandOrError(1, args);
                int wave;
                try {
                    wave = Integer.parseInt(args[2]);
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                    throw new ArgumentError(Lang.COMMAND_SETWAVE_NOT_SPECIFIED_WAVE);
                }
                executeSetWaveCommand(sender, land, wave);
            } else if (1 <= args.length && ("stopall".equalsIgnoreCase(args[0]) || "allstop".equalsIgnoreCase(args[0]))) {
                executeStopAllCommand(sender, parseEndResultOrError(2 <= args.length ? args[1] : "cancel"));
            } else if (1 <= args.length && "stop".equalsIgnoreCase(args[0])) {
                Land land = getLandOrError(1, args);
                executeStopCommand(sender, land, parseEndResultOrError(3 <= args.length ? args[2] : "cancel"));
            } else if (1 <= args.length && "chunkmap".equalsIgnoreCase(args[0])) {
                executeGiveChunkMap(getPlayer(sender));
            } else if (1 <= args.length && "setnoraidday".equalsIgnoreCase(args[0])) {
                Land land;
                int argsIndex = 1;
                if (3 <= args.length) {
                    land = getLandOrError(argsIndex++, args);
                } else {
                    land = null;  // all
                }
                if (args.length <= argsIndex)
                    throw new ArgumentError(Lang.COMMAND_SETNORAIDDAY_INVALID_DAYS);

                LocalDate now = LocalDate.now(api.getTimeZone().toZoneId());
                LocalDate days;
                try {
                    int number = Integer.parseInt(args[argsIndex]);
                    days = now.plusDays(number - 1);

                    if (number == 0) {
                        executeUnsetNoRaidDay(sender, land);
                        return true;
                    } else if (number < 0) {
                        throw new ArgumentError(Lang.COMMAND_SETNORAIDDAY_INVALID_DAYS_OLD);
                    }

                } catch (NumberFormatException e) {
                    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                            .appendOptional(new DateTimeFormatterBuilder()
                                    .appendPattern("yyyy/")
                                    .toFormatter())
                            .appendPattern("M/d")
                            .parseDefaulting(ChronoField.YEAR_OF_ERA, now.getYear())
                            .toFormatter();
                    try {
                        days = LocalDate.parse(args[argsIndex], formatter);
                    } catch (DateTimeParseException e2) {
                        throw new ArgumentError(Lang.COMMAND_SETNORAIDDAY_INVALID_DAYS);
                    }

                    if (args[argsIndex].split("/").length <= 2 && days.isBefore(now))
                        days = days.plusYears(1);

                    if (days.isBefore(now))
                        throw new ArgumentError(Lang.COMMAND_SETNORAIDDAY_INVALID_DAYS_OLD);
                }
                executeSetNoRaidDay(sender, land, days);

            } else if (1 <= args.length && "unsetnoraidday".equalsIgnoreCase(args[0])) {
                executeUnsetNoRaidDay(sender, 2 <= args.length ? getLandOrError(1, args) : null);
            } else if (1 <= args.length && "reload".equalsIgnoreCase(args[0])) {
                executeReloadCommand(sender);
            } else if (1 <= args.length && "tphere".equalsIgnoreCase(args[0])) {
                executeTpHereCommand(sender);
            } else {
                String versionText = "v" + ((RaidSpawnerPlugin) api).getDescription().getVersion();
                sender.sendMessage(ChatColor.DARK_RED + "[" + ChatColor.DARK_GRAY + "##" + ChatColor.DARK_RED + "] " + ChatColor.RED + "RaidSpawner " + ChatColor.GRAY + versionText + ChatColor.DARK_RED + " [" + ChatColor.DARK_GRAY + "##" + ChatColor.DARK_RED + "]");
                sender.sendMessage(ChatColor.DARK_PURPLE + " /" + label + " " + ChatColor.WHITE + "start " + ChatColor.YELLOW + "(land)");
                sender.sendMessage(ChatColor.DARK_PURPLE + " /" + label + " " + ChatColor.WHITE + "allstart");
                sender.sendMessage(ChatColor.DARK_PURPLE + " /" + label + " " + ChatColor.WHITE + "stop " + ChatColor.YELLOW + "(land) " + ChatColor.GRAY + "<cancel/win/lose>");
                sender.sendMessage(ChatColor.DARK_PURPLE + " /" + label + " " + ChatColor.WHITE + "allstop " + ChatColor.GRAY + "<cancel/win/lose>");
                sender.sendMessage(ChatColor.DARK_GREEN + " /" + label + " " + ChatColor.WHITE + "setwave " + ChatColor.YELLOW + "(land) (wave)");
                sender.sendMessage(ChatColor.DARK_GREEN + " /" + label + " " + ChatColor.WHITE + "nextwave " + ChatColor.YELLOW + "(land)");
                sender.sendMessage(ChatColor.DARK_AQUA + " /" + label + " " + ChatColor.WHITE + ChatColor.UNDERLINE + "s" + ChatColor.WHITE + "tatus");
                sender.sendMessage(ChatColor.DARK_AQUA + " /" + label + " " + ChatColor.WHITE + "chunkmap");
                sender.sendMessage(ChatColor.DARK_AQUA + " /" + label + " " + ChatColor.WHITE + "setNoRaidDay " + ChatColor.YELLOW + "[land] (days)");
                sender.sendMessage(ChatColor.DARK_AQUA + " /" + label + " " + ChatColor.WHITE + "unsetNoRaidDay " + ChatColor.YELLOW + "[land]");
                sender.sendMessage(ChatColor.DARK_AQUA + " /" + label + " " + ChatColor.WHITE + "reload");
            }

        } catch (ArgumentError e) {
            e.sendTo(sender);
        }
        return true;
    }

    private void executeStatusCommand(CommandSender sender) {
        RaidSpawnerData pluginData = api.getPluginData();

        LocalDate now = LocalDate.now(api.getTimeZone().toZoneId());
        List<Map.Entry<String, LocalDate>> pauseLands = pluginData.eventPauseLandsDate().entrySet().stream()
                .filter(e -> !now.isAfter(e.getValue()))
                .sorted(Map.Entry.comparingByValue(LocalDate::compareTo))
                .toList();

        if (pluginData.getEventPauseDate() != null || !pauseLands.isEmpty()) {
            sender.sendMessage(ChatColor.DARK_GRAY + "=".repeat(30));
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "No Raid Days");


            LocalDate target = pluginData.getEventPauseDate();
            if (target != null) {
                sender.sendMessage("- " + ChatColor.DARK_PURPLE + "ALL: " + ((now.isAfter(target) ? ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() : ChatColor.WHITE) + formatDays(now, target)));
            }

            for (Map.Entry<String, LocalDate> e : pauseLands) {
                sender.sendMessage("- " + ChatColor.YELLOW + e.getKey() + ": " + ChatColor.WHITE + formatDays(now, e.getValue()));
            }
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "=".repeat(30));
        if (api.isRunningRaid()) {
            Collection<RaidSpawner> raids = api.getCurrentRaids().values();
            List<RaidSpawner> loses = raids.stream().filter(RaidSpawner::isLose).toList();
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Status: " + ChatColor.RED + "RAID RUNNING (" + (raids.size() - loses.size()) + "/" + raids.size() + ")");
            if (!loses.isEmpty()) {
                sender.sendMessage(ChatColor.DARK_RED + "Lose Lands: " + ChatColor.WHITE + loses.stream()
                        .map(RaidSpawner::getLand)
                        .map(Land::getName)
                        .collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.WHITE)));
            }
            for (RaidSpawner raid : raids) {
                if (loses.contains(raid))
                    continue;

                List<Enemy> enemies = raid.currentEnemies();
                sender.sendMessage(ChatColor.DARK_GREEN + "Alive Land: " +
                        ChatColor.WHITE + raid.getLand().getName() +
                        ChatColor.GRAY + "  (Wave " + ChatColor.WHITE + raid.getWave() + "/" + raid.getMaxWaves() +
                        ChatColor.GRAY + ", enemies: " + ChatColor.WHITE + enemies.stream().filter(Enemy::isAlive).count() + "/" + enemies.size() +
                        ChatColor.GRAY + ", players: " + ChatColor.WHITE + raid.getLand().getOnlinePlayers().size() + ChatColor.GRAY + ")");
            }

        } else if (api.isStandbyAutoStart()) {
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Status: " + ChatColor.AQUA + "AUTO START SCHEDULED");
            for (ConditionWrapper cond : api.getAutoStartConditions()) {
                float remaining = Optional.ofNullable(cond.getCondition().getRemainingTimePreview())
                        .map(v -> Math.round((float) v / 1000 / 6) / 10f)
                        .orElse(-1f);
                sender.sendMessage("- " + ChatColor.GRAY + cond.getType() + ChatColor.WHITE + " remaining " + remaining + "m " + (cond.isActivated() ? ChatColor.YELLOW + "(triggered)" : ""));
            }

        } else {
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Status: " + ChatColor.GRAY + "NOTHING");
        }

        Multimap<World, Chunk> tickets = api.getChunkTickets();
        if (!tickets.isEmpty()) {
            sender.sendMessage(ChatColor.DARK_GRAY + "=".repeat(30));
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Chunk Tickets (total " + tickets.values().size() + " chunks)");
            for (Map.Entry<World, Collection<Chunk>> e : tickets.asMap().entrySet()) {
                World world = e.getKey();
                Collection<Chunk> chunks = e.getValue();
                sender.sendMessage("- " + chunks.size() + " chunks (" + world.getName() + ")");
            }
        }

    }

    private void executeStartCommand(CommandSender sender, Land land) {
        if (api.isRunningRaid(land)) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_RAID_ALREADY_STARTED);
            return;
        }

        boolean result = false;
        try {
            result = api.startRaid(land);
        } catch (IllegalStateException ignored) {
        } catch (IllegalArgumentException e) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_RAID_UNKNOWN_SPAWN);
            return;
        }

        if (result) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_RAID_DONE, land.getName());
        } else {
            api.getPluginLang().send(sender, Lang.COMMAND_START_RAID_UNABLE_START);
        }
    }

    private void executeStartAllCommand(CommandSender sender) {
        if (api.isRunningRaid()) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_ALL_RAID_ALREADY_STARTED);
            return;
        }

        boolean result;
        try {
            result = api.startRaidAll(null);
        } catch (IllegalArgumentException e) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_ALL_RAID_UNKNOWN_SPAWN);
            return;
        }

        if (result) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_ALL_RAID_DONE, api.getCurrentRaids().size());
        } else {
            api.getPluginLang().send(sender, Lang.COMMAND_START_ALL_RAID_UNABLE_START);
        }
    }

    private void executeSetWaveCommand(CommandSender sender, Land land, int wave) {
        RaidSpawner spawner = api.getCurrentRaids().get(land);
        if (spawner == null || !spawner.isRunning()) {
            api.getPluginLang().send(sender, Lang.COMMAND_SETWAVE_RAID_NOT_RUNNING);
            return;
        }

        if (wave == -1) {
            wave = spawner.getWave() + 1;
            if (spawner.getMaxWaves() < wave) {
                api.getPluginLang().send(sender, Lang.COMMAND_SETWAVE_ALREADY_MAX_WAVE, land.getName());
                return;
            }
        }

        if (0 < wave && wave <= spawner.getMaxWaves()) {
            spawner.setWaves(wave);
            api.getPluginLang().send(sender, Lang.COMMAND_SETWAVE_DONE, land.getName(), wave);
        } else {
            api.getPluginLang().send(sender, Lang.COMMAND_SETWAVE_INVALID_WAVE, spawner.getMaxWaves());
        }
    }

    private void executeStopCommand(CommandSender sender, Land land, RaidEndResult result) {
        RaidSpawner raid;
        if (!api.isRunningRaid(land) || (raid = api.getCurrentRaids().get(land)) == null) {
            api.getPluginLang().send(sender, Lang.COMMAND_STOP_RAID_NOT_RUNNING);
            return;
        }

        raid.clear(result, RaidEndReason.COMMAND);
        api.getPluginLang().send(sender, Lang.COMMAND_STOP_RAID_DONE, result.name());
    }

    private void executeStopAllCommand(CommandSender sender, RaidEndResult result) {
        if (!api.isRunningRaid()) {
            api.getPluginLang().send(sender, Lang.COMMAND_STOP_ALL_RAID_NOT_RUNNING);
            return;
        }

        long count = api.getCurrentRaids().values().stream().filter(RaidSpawner::isRunning).count();
        api.clearRaidAll(result, RaidEndReason.COMMAND);
        api.getPluginLang().send(sender, Lang.COMMAND_STOP_ALL_RAID_DONE, result.name(), count);
    }

    private void executeSetNoRaidDay(CommandSender sender, @Nullable Land land, LocalDate days) {
        RaidSpawnerData pluginData = api.getPluginData();
        LocalDate now = LocalDate.now(api.getTimeZone().toZoneId());
        String daysText = formatDays(now, days);

        if (land != null) {
            LocalDate pause = pluginData.eventPauseLandsDate().get(land.getName());
            if (pause == null || !pause.isEqual(days)) {
                pluginData.eventPauseLandsDate().put(land.getName(), days);
                pluginData.save();
                api.getPluginLang().send(sender, Lang.COMMAND_SETNORAIDDAYS_DONE, land.getName(), daysText);
            } else {
                api.getPluginLang().send(sender, Lang.COMMAND_SETNORAIDDAYS_ALREADY, land.getName(), daysText);
            }
        } else {
            LocalDate pause = pluginData.getEventPauseDate();
            if (pause == null || !pause.isEqual(days)) {
                pluginData.setEventPauseData(days);
                pluginData.save();
                api.getPluginLang().send(sender, Lang.COMMAND_SETNORAIDDAYS_ALL_DONE, daysText);
            } else {
                api.getPluginLang().send(sender, Lang.COMMAND_SETNORAIDDAYS_ALREADY_ALL, daysText);
            }
        }

    }

    private void executeUnsetNoRaidDay(CommandSender sender, @Nullable Land land) {
        RaidSpawnerData pluginData = api.getPluginData();
        LocalDate now = LocalDate.now(api.getTimeZone().toZoneId());

        if (land != null) {
            LocalDate pause = pluginData.eventPauseLandsDate().get(land.getName());

            if (pluginData.eventPauseLandsDate().remove(land.getName()) != null) {
                pluginData.save();
            }

            if (pause == null || now.isAfter(pause)) {
                api.getPluginLang().send(sender, Lang.COMMAND_UNSETNORAIDDAYS_ALREADY, land.getName());
            } else {
                api.getPluginLang().send(sender, Lang.COMMAND_UNSETNORAIDDAYS_DONE, land.getName());
            }

        } else {
            LocalDate pause = pluginData.getEventPauseDate();

            if (pause != null) {
                pluginData.setEventPauseData(null);
                pluginData.save();
            }

            if (pause == null || now.isAfter(pause)) {
                api.getPluginLang().send(sender, Lang.COMMAND_UNSETNORAIDDAYS_ALREADY_ALL);
            } else {
                api.getPluginLang().send(sender, Lang.COMMAND_UNSETNORAIDDAYS_ALL_DONE);
            }

        }
    }

    private void executeGiveChunkMap(Player player) {
        PlayerInventory inv = player.getInventory();

        MapView view;
        ItemStack itemStack;
        MapMeta itemMeta;
        ItemStack mainHandItem = inv.getItemInMainHand();

        if (Material.FILLED_MAP.equals(mainHandItem.getType())) {
            // override map
            itemStack = mainHandItem;
            view = ((MapMeta) mainHandItem.getItemMeta()).getMapView();

        } else {
            itemStack = new ItemStack(Material.FILLED_MAP);
            view = Bukkit.createMap(player.getWorld());
        }
        itemMeta = (MapMeta) itemStack.getItemMeta();

        view.setScale(MapView.Scale.NORMAL);
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(api.getChunkViewRenderer());

        itemMeta.setMapView(view);
        itemStack.setItemMeta(itemMeta);

        if (!itemStack.equals(mainHandItem)) {
            inv.setItemInMainHand(itemStack);
            inv.addItem(mainHandItem);
        }
        player.updateInventory();
        api.updateChunkViewRendererChunks(true);
    }

    private void executeReloadCommand(CommandSender sender) {
        ((RaidSpawnerPlugin) api).reloadPluginConfig();
        api.getPluginLang().send(sender, Lang.COMMAND_RELOAD_DONE);
    }

    private void executeTpHereCommand(CommandSender sender) {
        Player player = getPlayer(sender);
        api.getLandAPI().getLandPlayer(player.getUniqueId()).getLands().stream()
                .map(land -> api.getCurrentRaids().get(land))
                .filter(Objects::nonNull)
                .flatMap(r -> r.currentEnemies().stream().filter(Enemy::isAlive))
                .map(Enemy::getEntity)
                .filter(Objects::nonNull)
                .forEach(e -> e.teleport(player));
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (1 == args.length) {
            List<String> list = Stream.of("status", "reload", "chunkmap", "start", "startall", "stop", "stopall", "nextwave", "setwave", "allstart", "allstop", "setnoraidday", "unsetnoraidday")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();

            if (list.isEmpty() && !args[0].isEmpty()) {
                 list = Stream.of("mapchunk", "tphere")
                         .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                         .collect(Collectors.toCollection(ArrayList::new));
                 if ("noraidday".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                     list.add("setnoraidday");
                     list.add("unsetnoraidday");
                 }
            }
            return list;
        } else if (2 == args.length && args[0].equalsIgnoreCase("start")) {
            return api.getLands().stream()
                    .filter(land -> !api.isRunningRaid(land))
                    .map(Land::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        } else if (2 == args.length && (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("nextwave") || args[0].equalsIgnoreCase("setwave"))) {
            return api.getLands().stream()
                    .filter(api::isRunningRaid)
                    .map(Land::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        } else if (2 == args.length && (args[0].equalsIgnoreCase("setnoraidday") || args[0].equalsIgnoreCase("unsetnoraidday"))) {
            return api.getLands().stream()
                    .map(Land::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        } else if ((3 == args.length && args[0].equalsIgnoreCase("stop")) || (2 == args.length && (args[0].equalsIgnoreCase("stopall") || args[0].equalsIgnoreCase("allstop")))) {
            return Stream.of(RaidEndResult.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(args[args.length - 1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return Collections.emptyList();
    }


    private class ArgumentError extends Error {

        private final Lang lang;
        private final Object[] args;

        public ArgumentError(Lang lang, Object... args) {
            this.lang = lang;
            this.args = args;
        }

        public void sendTo(CommandSender sender) {
            api.getPluginLang().send(sender, lang, args);
        }

    }

    public static String formatDays(LocalDate now, LocalDate target) {
        return target.format(target.getYear() == now.getYear() ? DateTimeFormatter.ofPattern("M/d") : DateTimeFormatter.ofPattern("yyyy/M/d"));
    }

}

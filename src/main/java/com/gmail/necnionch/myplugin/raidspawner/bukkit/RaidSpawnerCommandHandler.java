package com.gmail.necnionch.myplugin.raidspawner.bukkit;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.condition.ConditionWrapper;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.lang.Lang;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndReason;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidEndResult;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import com.google.common.collect.Multimap;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RaidSpawnerCommandHandler implements TabExecutor {

    private final RaidSpawnerAPI api;
    private final LandsIntegration lands;

    public RaidSpawnerCommandHandler(RaidSpawnerAPI api, LandsIntegration lands) {
        this.api = api;
        this.lands = lands;
    }

    public Land getLandOrError(int index, String[] args) {
        if (index < args.length) {
            String landName = args[index];
            return Optional.ofNullable(lands.getLandByName(landName))
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


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (1 <= args.length && "status".equalsIgnoreCase(args[0])) {
                executeStatusCommand(sender);
            } else if (1 <= args.length && "start-all".equalsIgnoreCase(args[0])) {
                executeStartAllCommand(sender);
            } else if (1 <= args.length && "start".equalsIgnoreCase(args[0])) {
                Land land = getLandOrError(2, args);
                executeStartCommand(sender, land);
            } else if (1 <= args.length && "stop-all".equalsIgnoreCase(args[0])) {
                executeStopAllCommand(sender, parseEndResultOrError(2 <= args.length ? args[1] : "cancel"));
            } else if (1 <= args.length && "stop".equalsIgnoreCase(args[0])) {
                Land land = getLandOrError(2, args);
                executeStopCommand(sender, land, parseEndResultOrError(3 <= args.length ? args[2] : "cancel"));
            } else if (1 <= args.length && "debugmap".equalsIgnoreCase(args[0])) {
                sender.sendMessage("Not implemented");  // TODO
            } else if (1 <= args.length && "reload".equalsIgnoreCase(args[0])) {
                executeReloadCommand(sender);
            } else {
                sender.sendMessage(ChatColor.DARK_GRAY + "## " + ChatColor.RED + "RaidSpawner " + ChatColor.DARK_GRAY + "##");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "status");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "reload");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "debugmap");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "start (land)");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "start-all");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "stop (land) " + ChatColor.GRAY + "<cancel/win/lose>");
                sender.sendMessage(ChatColor.GRAY + " /raidspawner " + ChatColor.WHITE + "stop-all " + ChatColor.GRAY + "<cancel/win/lose>");
            }

        } catch (ArgumentError e) {
            e.sendTo(sender);
        }
        return true;
    }

    private void executeStatusCommand(CommandSender sender) {
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
                sender.sendMessage(ChatColor.DARK_GREEN + "Alive Land: " + ChatColor.WHITE + raid.getLand().getName() + ChatColor.GRAY + "  (Wave " + (raid.getWave() + 1) + "/" + raid.getMaxWaves() + ")");
            }

        } else if (api.isStandbyAutoStart()) {
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Status: " + ChatColor.DARK_GREEN + "AUTO START SCHEDULED");
            for (ConditionWrapper cond : api.getAutoStartConditions()) {
                float remaining = Optional.ofNullable(cond.getCondition().getRemainingTimePreview())
                        .map(v -> Math.round((float) v / 1000 / 6) / 10f)
                        .orElse(-1f);
                sender.sendMessage("- " + ChatColor.GRAY + cond.getType() + ChatColor.WHITE + "remaining " + remaining + "m " + (cond.isActivated() ? ChatColor.YELLOW + "(triggered)" : ""));
            }

        } else {
            sender.sendMessage(ChatColor.GRAY + "=== " + ChatColor.GOLD + "Status: " + ChatColor.GRAY + "NOTHING");
            sender.sendMessage("");
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

        if (api.startRaidAll(null)) {
            api.getPluginLang().send(sender, Lang.COMMAND_START_ALL_RAID_DONE, api.getCurrentRaids().size());
        } else {
            api.getPluginLang().send(sender, Lang.COMMAND_START_ALL_RAID_UNABLE_START);
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

    private void executeReloadCommand(CommandSender sender) {
        api.getPluginConfig().load();
        api.getPluginLang().load();
        api.getPluginLang().send(sender, Lang.COMMAND_RELOAD_DONE);
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (1 == args.length) {
            return Stream.of("status", "reload", "debugmap", "start", "start-all", "stop", "stop-all")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        } else if (2 == args.length && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop"))) {
            return lands.getLands().stream()
                    .map(Land::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        } else if ((3 == args.length && args[0].equalsIgnoreCase("stop")) || (2 == args.length && args[0].equalsIgnoreCase("stop-all"))) {
            return Stream.of(RaidEndResult.values())
                    .map(Enum::name)
                    .filter(s -> s.startsWith(args[args.length - 1].toUpperCase(Locale.ROOT)))
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

}

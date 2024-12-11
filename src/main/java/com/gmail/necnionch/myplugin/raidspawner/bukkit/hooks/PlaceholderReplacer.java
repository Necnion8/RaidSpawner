package com.gmail.necnionch.myplugin.raidspawner.bukkit.hooks;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlaceholderReplacer {
    @NotNull String process(@Nullable Player player, @NotNull String string);
}

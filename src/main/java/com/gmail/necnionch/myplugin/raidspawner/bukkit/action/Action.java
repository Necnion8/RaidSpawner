package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import org.jetbrains.annotations.NotNull;

public interface Action {

    @NotNull ActionProvider<?> getProvider();

}

package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerPlugin;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import com.gmail.necnionch.myplugin.raidspawner.bukkit.raid.RaidSpawner;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerAddMoneyAction implements PlayerAction {

    private final Provider provider;
    private final Economy economy;
    private final double amount;

    public PlayerAddMoneyAction(Provider provider, double amount) {
        this.provider = provider;
        this.economy = provider.getEconomy();
        this.amount = amount;
    }

    @Override
    public boolean doAction(RaidSpawner spawner, Player player) {
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!EconomyResponse.ResponseType.SUCCESS.equals(response.type)) {
            RaidSpawnerUtil.getLogger().severe("Failed to deposit player (p:" + player.getUniqueId() + ", v:" + amount + "): " + response.errorMessage);
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerAddMoneyAction> {

        private final Economy economy;

        public Provider(Economy economy) {
            super("add-money");
            this.economy = economy;
        }

        @Override
        public PlayerAddMoneyAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            return new PlayerAddMoneyAction(this, parseDouble(value));
        }

        public Economy getEconomy() {
            return economy;
        }


        public static @Nullable Provider createAndHookEconomy(RaidSpawnerPlugin plugin) {
            try {
                Class.forName("net.milkbowl.vault.economy.Economy");
            } catch (ClassNotFoundException e) {
                return null;
            }
            return Optional.ofNullable(plugin.getServer().getServicesManager().getRegistration(Economy.class))
                    .map(RegisteredServiceProvider::getProvider)
                    .map(Provider::new)
                    .orElse(null);
        }

    }

}

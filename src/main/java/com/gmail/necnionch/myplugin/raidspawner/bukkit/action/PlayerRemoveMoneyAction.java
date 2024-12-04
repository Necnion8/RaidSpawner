package com.gmail.necnionch.myplugin.raidspawner.bukkit.action;

import com.gmail.necnionch.myplugin.raidspawner.bukkit.RaidSpawnerUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerRemoveMoneyAction implements PlayerAction {

    private final Provider provider;
    private final Economy economy;
    private final double amount;

    public PlayerRemoveMoneyAction(Provider provider, double amount) {
        this.provider = provider;
        this.economy = provider.getEconomy();
        this.amount = amount;
    }

    @Override
    public boolean doAction(Player player) {
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!EconomyResponse.ResponseType.SUCCESS.equals(response.type)) {
            RaidSpawnerUtil.getLogger().severe("Failed to withdraw player (p:" + player.getUniqueId() + ", v:" + amount + "): " + response.errorMessage);
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public Provider getProvider() {
        return provider;
    }


    public static class Provider extends ActionProvider<PlayerRemoveMoneyAction> {

        private final Economy economy;

        public Provider(Economy economy) {
            super("remove-money");
            this.economy = economy;
        }

        @Override
        public PlayerRemoveMoneyAction create(Object value, @Nullable ConfigurationSection config) throws ConfigurationError {
            try {
                return new PlayerRemoveMoneyAction(this, Double.parseDouble(((String) value)));
            } catch (ClassCastException | NullPointerException e) {
                throw new ConfigurationError("Not number value: " + value);
            } catch (NumberFormatException e) {
                throw new ConfigurationError(e);
            }
        }

        public Economy getEconomy() {
            return economy;
        }
    }

}
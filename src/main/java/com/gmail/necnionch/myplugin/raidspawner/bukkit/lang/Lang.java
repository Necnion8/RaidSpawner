package com.gmail.necnionch.myplugin.raidspawner.bukkit.lang;

public enum Lang {

    NON_MEMBERS_JOIN_DENY_MESSAGE("&c襲撃イベントが発生しているため、現在は参加できません。"),
    NON_MEMBERS_PLAYER_KICK_MESSAGE("&c襲撃イベントが発生しました。終了後に再参加できます。");

    private final String defaultText;

    Lang(String defaultText) {
        this.defaultText = defaultText;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public String getKey() {
        return name().replace("_", "-");
    }

}

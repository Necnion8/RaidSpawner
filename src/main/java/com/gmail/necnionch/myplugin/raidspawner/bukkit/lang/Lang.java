package com.gmail.necnionch.myplugin.raidspawner.bukkit.lang;

public enum Lang {

    PRESTART_NOTIFY_BROADCAST_MESSAGE("&4不穏な気配を感じる・・・"),
    START_MESSAGE(""),
    START_TITLE(""),
    START_SUBTITLE("&4&lまた奴らがやってくる"),
    END_CANCEL_MESSAGE(""),
    END_WIN_MESSAGE(""),
    END_LOSE_MESSAGE("&4襲撃者からの侵略を防ぎ切ることができなかった・・・"),
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

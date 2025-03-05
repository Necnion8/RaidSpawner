package com.gmail.necnionch.myplugin.raidspawner.bukkit.lang;

public enum Lang {
    COMMAND_NOT_EXISTS_LAND("&cその名前の Land はありません"),
    COMMAND_NOT_SPECIFIED_LAND("&cLand を指定してください"),
    COMMAND_INVALID_STOP_RESULT("&ccancel, lose, win のいずれかで指定してください"),
    COMMAND_START_RAID_ALREADY_STARTED("&cすでに襲撃イベントを実行しています"),
    COMMAND_START_RAID_UNABLE_START("&c襲撃イベントを開始できませんでした"),
    COMMAND_START_RAID_UNKNOWN_SPAWN("&cLandのスポーン設定が有効ではありません"),
    COMMAND_START_RAID_DONE("&6襲撃イベントを %1$s Land で開始しました"),
    COMMAND_START_ALL_RAID_ALREADY_STARTED("&cすでに襲撃イベントを実行しています"),
    COMMAND_START_ALL_RAID_UNABLE_START("&c襲撃イベントを開始できませんでした"),
    COMMAND_START_ALL_RAID_UNKNOWN_SPAWN("&cLandのスポーン設定が有効ではありません"),
    COMMAND_START_ALL_RAID_DONE("&6襲撃イベントを %1$s Land で開始しました"),
    COMMAND_STOP_RAID_NOT_RUNNING("&c襲撃イベントは開始していません"),
    COMMAND_STOP_RAID_DONE("&6襲撃イベントを終了しました (result: %1$s)"),
    COMMAND_STOP_ALL_RAID_NOT_RUNNING("&c襲撃イベントは開始していません"),
    COMMAND_STOP_ALL_RAID_DONE("&6%2$s Land の襲撃イベントを終了しました (result: %1$s)"),
    COMMAND_SETWAVE_RAID_NOT_RUNNING("&c襲撃イベントが開始していないか、既に終了しています"),
    COMMAND_SETWAVE_INVALID_WAVE("&cウェーブ数は 1 から %1$s の数値で指定してください"),
    COMMAND_SETWAVE_NOT_SPECIFIED_WAVE("&cウェーブ数を数値で指定してください"),
    COMMAND_SETWAVE_ALREADY_MAX_WAVE("&c%1$s Land の襲撃イベントは最大ウェーブに達しています"),
    COMMAND_SETWAVE_DONE("&6%1$s Land の襲撃ウェーブ数を %2$s に変更しました"),
    COMMAND_RELOAD_DONE("&6設定ファイルを再読み込みしました"),

    NON_PLAYER_SENDER_ERROR("&cプレイヤーのみ実行できます"),

    PRESTART_NOTIFY_BROADCAST_MESSAGE("&4不穏な気配を感じる・・・"),
    START_MESSAGE(""),
    START_TITLE(""),
    START_SUBTITLE("&4&lまた奴らがやってくる"),
    END_CANCEL_MESSAGE(""),
    END_WIN_MESSAGE(""),
    END_LOSE_MESSAGE("&4襲撃者からの侵略を防ぎ切ることができなかった・・・"),
    END_LOSE_TIMEOUT_MESSAGE("&4襲撃者からの侵略を防ぎ切ることができなかった・・・"),
    END_LOSE_NO_TICKETS_MESSAGE("&4襲撃者からの侵略を防ぎ切ることができなかった・・・"),
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

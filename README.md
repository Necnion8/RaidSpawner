# RaidSpawner
Landsプラグインと連携して各Landにモブ襲撃を起こすプラグイン

## 前提
- Spigot 1.17 以上
- Java 17 以上
- [Lands](https://www.spigotmc.org/resources/53313)
- [Vault](https://dev.bukkit.org/projects/vault) (optional)
- [MythicMobs](https://www.spigotmc.org/resources/5702) (optional)
- [PlaceholderAPI](https://www.spigotmc.org/resources/6245) (optional)
- [LuckPerms](https://www.spigotmc.org/resources/28140) (optional)
- [DiscordSRV](https://modrinth.com/plugin/discordsrv) (optional, v1.29.0 でテスト)
- [StatBadge](https://github.com/Necnion8/StatBadge) (optional, v1.0.x でテスト)

## コマンドと権限
| コマンド                                | サブコマンド / 説明                                               | 権限                                  | デフォルト |
|-------------------------------------|-----------------------------------------------------------|:------------------------------------|:-----:|
| /raidspawner<br><sup>管理者用コマンド</sup> |                                                           | raidspawner.command.raidspawner     | OPのみ  |
| 〃                                   | status *or* s<br><sup>プラグインや襲撃の状態を表示</sup>                | 〃                                   |   〃   |
| 〃                                   | reload<br><sup>設定ファイルの再読み込み</sup>                         | 〃                                   |   〃   |
| 〃                                   | chunkmap<br><sup>チャンク表示マップを与えます</sup>                     | 〃                                   |   〃   |
| 〃                                   | start (land)<br><sup>襲撃イベントを手動で開始</sup>                   | 〃                                   |   〃   |
| 〃                                   | allstart<br><sup>襲撃イベントを手動で開始</sup>                       | 〃                                   |   〃   |
| 〃                                   | stop (land) <cancel/win/lose><br><sup>襲撃イベントの強制終了</sup>   | 〃                                   |   〃   |
| 〃                                   | allstop <cancel/win/lose><br><sup>襲撃イベントの強制終了</sup>       | 〃                                   |   〃   |
| 〃                                   | setwave (land) (wave)<br><sup>現在のウェーブ数を変更</sup>           | 〃                                   |   〃   |
| 〃                                   | nextwave (land)<br><sup>現在のウェーブをスキップ</sup>                | 〃                                   |   〃   |
| 〃                                   | setNoRaidDay [land] (days)<br><sup>襲撃イベントを休止する日数を設定</sup> | 〃                                   |   〃   |
| 〃                                   | unsetNoRaidDay [land]<br><sup>襲撃イベント休止設定を解除</sup>         | 〃                                   |   〃   |
|                                     | <sup>Land未参加でもキックしない</sup>                                | raidspawner.bypass.non-members-kick |   〃   |

## 設定

### 条件一覧
- `real-clock` - 現実時刻
  - `time-hours` - 設定: 時間
  - `time-minutes` - 設定: 分
  - `timezone` - (省略可) タイムゾーン(`Asia/Tokyo`など) または `local` でシステム時刻
- `timer` - タイマー
  - `time-minutes` - 設定: 分

### アクション一覧
#### プレイヤーアクション
```yml
## コマンドの実行
command:  # 利用可能な値: %uuid%, %player%
  - "effect give %uuid% instant_health 1 10 false"
  - "say %player% Healed!"

## プレイヤーに対してコマンド実行
execute-command:  # 利用可能な値: %uuid%, %player%
  - "say Hi!"

## 所持金の追加
add-money: 2000

## 所持金の削除
remove-money: 1000

## 音の再生  https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
playsound: entity_evoker_cast_spell

## タイトル表示
title:
  text: "&cゲーム開始！"  # 省略可
  subtext: "&4全てのモンスターを討伐せよ！！"  # 省略可
  fade-in: 20  # 単位: tick
  fade-out: 20  # 単位: tick
  duration: 60  # 単位: tick

## ポーション効果の付与
effects:
  regeneration:  # エフェクトID  https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html
    duration: 200  # 単位: tick
    level: 2  # 効果レベル (デフォルト: 1)
    silent: true  # 非表示にするか (デフォルト: false)
```

#### Land アクション
```yml
## コマンドの実行
command:  # 利用可能な値: %land%
  - "say Hello, %land% Land!"
  
## Landチャンクの削除
remove-chunk:
  value: 2  # チャンク数
  keep-land: true  # チャンクを全て失ったLandを削除しない (デフォルト: false)
```

### 敵モブ一覧
- `test` - テストエンティティ。ダイヤ剣を持っていて、発光しています。
  ```yml
  - source: test
  ```
  
- `mythicmobs` - MythicMobsのモブ。プラグインが必要です。
  ```yml
  - source: mythicmobs
    type: SkeletalKnight
    level: 2  # 省略可: デフォルト 1
  ```

- `vanilla` - バニラのシンプルなモブ
  ```yml
  - source: vanilla
    type: zombie  # エンティティタイプ
    health: 20  # 省略可: デフォルト 20
    effects:  # 付与するポーション効果
      speed:  # ポーションタイプ
        duration: 1200  # 単位: tick (= 1分)
        level: 1  # 効果レベル。省略可: デフォルト 1
      strength:
        duration: 1200
        level: 2
  ```
  [エンティティタイプ一覧](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html) | [ポーション効果一覧](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html)

### アクション設定例
```yml
actions:
  player:  # 省略可
    execute-command: "say Hi"
    command:
      - "title %uuid% times 10 40 10"
      - 'title %uuid% title "Mission Completed!"'
    add-money: 1000
  land:  # 省略可
    remove-chunk:
      value: 2  # 2 chunks
      keep-land: false  # 省略可
```

### デフォルトの設定
[./plugins/RaidSpawner/config.yml](src%2Fmain%2Fresources%2Fbukkit-config.yml)

 
## ライセンス
- [exp4j](https://github.com/fasseg/exp4j) - Apache License 2.0

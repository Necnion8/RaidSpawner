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

## コマンドと権限
| コマンド                                | サブコマンド / 説明                                             | 権限                                  | デフォルト |
|-------------------------------------|---------------------------------------------------------|:------------------------------------|:-----:|
| /raidspawner<br><sup>管理者用コマンド</sup> |                                                         | raidspawner.command.raidspawner     | OPのみ  |
| 〃                                   | status<br><sup>プラグインや襲撃の状態を表示</sup>                     | 〃                                   |   〃   |
| 〃                                   | reload<br><sup>設定ファイルの再読み込み</sup>                       | 〃                                   |   〃   |
| 〃                                   | chunkmap<br><sup>チャンク表示マップを与えます</sup>                   | 〃                                   |   〃   |
| 〃                                   | start (land)<br><sup>襲撃イベントを手動で開始</sup>                 | 〃                                   |   〃   |
| 〃                                   | startall<br><sup>襲撃イベントを手動で開始</sup>                     | 〃                                   |   〃   |
| 〃                                   | stop (land) <cancel/win/lose><br><sup>襲撃イベントの強制終了</sup> | 〃                                   |   〃   |
| 〃                                   | stopall <cancel/win/lose><br><sup>襲撃イベントの強制終了</sup>     | 〃                                   |   〃   |
| 〃                                   | setwave (land) (wave)<br><sup>現在のウェーブ数を変更</sup>         | 〃                                   |   〃   |
| 〃                                   | nextwave (land)<br><sup>現在のウェーブをスキップ</sup>              | 〃                                   |   〃   |
|                                     | <sup>Land未参加でもキックしない</sup>                              | raidspawner.bypass.non-members-kick |   〃   |

## 設定

### 条件一覧
- `real-clock` - 現実時刻
  - `time-hours` - 設定: 時間
  - `time-minutes` - 設定: 分
  - `timezone` - タイムゾーン または `local` でシステム時刻
- `timer` - タイマー
  - `time-minutes` - 設定: 分

### アクション一覧
- for Player
  - `command` - コマンド実行<sup>※1</sup>
  - `execute-command` - プレイヤーに対してコマンド実行<sup>※1</sup>
  - `add-money` - 所持金の追加
  - `remove-money` - 所持金の削除
  - `playsound` - 音の再生 [IDリスト](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html)
- for Land
  - `command` - コマンド実行<sup>※1</sup>
  - `remove-chunk` - Landチャンクの削除
    - `value` - チャンク数 (int)
    - `keep-land` - チャンクを全て失ったLandを削除しない (bool, optional)

<sup>※1</sup> 文字列またはリストで、１つまたは複数のコマンドを指定できます。

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

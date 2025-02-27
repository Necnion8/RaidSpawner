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
  - `command` - コマンド実行
  - `execute-command` - プレイヤーに対してコマンド実行
  - `add-money` - 所持金の追加
  - `remove-money` - 所持金の削除
- for Land
  - `command` - コマンド実行
  - `remove-chunk` - Landチャンクの削除
    - `value` - チャンク数 (int)
    - `keep-land` - チャンクを全て失ったLandを削除しない (bool, optional)

### アクション設定例
```yml
actions:
  player:  # 省略可
    execute-command: "say Hi"
    add-money: 1000
  land:  # 省略可
    remove-chunk:
      value: 2  # 2 chunks
      keep-land: false  # 省略可
```

### 設定ファイル サンプル
<details>
    <summary>
        config.yml
    </summary>

> [./plugins/RaidSpawner/config.yml](src%2Fmain%2Fresources%2Fbukkit-config.yml)
> ```yml
> # イベント開始設定
> event-start:
>   # いずれかの条件を満たすと実行
>   conditions:
>     - type: real-clock
>       timezone: Asia/Tokyo  # or local
>       time-hours: 22
>       time-minutes: 30
>
>   # 開始前通知
>   pre-notify:
>     enable: true
>     minutes: 3
>
>   # 開始通知
>   start-notify:
>     # 盲目の付与
>     blind-effect: true
>     # Landスポーンが設定されていなければ、敷地内のランダムな位置にテレポートします
>     teleport-to-land: true
>
> # 襲撃設定
> raid:
>   # Land未所属プレイヤーを退出または参加させない設定
>   non-members-kick-enable: true
>   # ゲーム時間
>   event-time-minutes: 30
>   # ウェーブ数
>   waves: 5
>   # イベント中に付与する権限グループ
>   luckperms-group: in-raidspawner
>   # Landがあるワールド (nullでLandのスポーン設定されたワールドを選択)
>   world:
>   # スポーン位置: Landから離すチャンク数
>   mobs-distance-chunks: 2
>   # モブ設定
>   mobs:
>     - count: "ceil( land_players * (3 + land_chunks * 1.5) * (1 + 0.1 * wave) )"  # or number value
>       # スポーン対象
>       enemies:
>         - source: mythicmobs
>           type: MOB_TYPE
>           priority: 10
>           level: 1
>
> # 成功報酬設定
> event-win-rewards:
>   # 条件を満たしたもののみ実行
>   conditions:
>     - type: timer
>       time-minutes: 15
>       actions:
>
>   # どの条件も満たされていない場合のアクション
>   condition-else:
>     actions:
>
> # 失敗報酬設定
> event-lose-rewards:
>   actions:
>
> # イベント終了設定
> event-end:
>   send-result-to-discord: true
>
> # Discord連携設定
> discord:
>   # 連携機能の有効 (DiscordSRVが必要です)
>   enable: false
>   # 通知を送信するチャンネルID
>   channel: 000000000000000000
> ```
</details>
 
## ライセンス
- [exp4j](https://github.com/fasseg/exp4j) - Apache License 2.0

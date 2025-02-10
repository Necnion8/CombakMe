# CombakMe
しばらくログインしていないプレイヤーに通知メッセージを送信するプラグイン

プレイヤーは DiscordSRV によってアカウントがリンクされている必要があり、メッセージは DiscordSRV を経由してユーザーのDM宛に送信されます。  
通知する回数と頻度には十分に注意して使用してください。

## 前提
- Spigot 1.13 以上
- [DiscordSRV](https://modrinth.com/plugin/discordsrv) (v1.29.0 でテスト)

## 権限
| ノード                     | 説明            | デフォルト |
|:------------------------|:--------------|:-----:|
| combakme.disable-notify | 通知しないプレイヤー権限  |       |

## 設定
> [./plugins/CombakeMe/config.yml](src%2Fmain%2Fresources%2Fbukkit-config.yml)
> ```yml
> # 通知メッセージの ON/OFF
> # ※ この設定に関わらず、権限 combakme.disable-notify があるプレイヤーは送信しません
> enable-messages: true
>
> # 各時間ごとにメッセージの内容を設定できます
> schedules:
>   - schedule-time: 12h0m  # 最終ログインから12時間0分
>     contents:  # ランダムで送信する内容を決定します
>       - "サーバーをログアウトしてから１２時間経過しました"
>
>   - schedule-time: 12h0m,23h30m  # 最終ログインから12時間0分から23時間30分の間に1回
>     contents:
>       - "やり残したことはありませんか？"
>
>   - schedule-time: 24h  # 最終ログインから24時間0分 (0分は省略可)
>     contents:
>       - "サーバーをログアウトしてから２４時間経過しました"
>
>   - schedule-time: 24h,47h30m
>     contents:
>       - "今日も参加しませんか？"
>
>   - schedule-time: 48h
>     contents:
>       - "サーバーをログアウトしてから４８時間経過しました"
>
>   - schedule-time: 48h,71h30m
>     contents:
>       - "今日は参加しませんか？"
>
> # 通知がスケジュールされていない場合のメッセージ設定 (繰り返し)
> unscheduled-message:
>   enable: false
>   timer-time: 2h,4h  # 2時間から4時間の範囲で繰り返し通知する (通知間隔には気を付けてください)
>   contents: []
>
>
> # データベース設定
> database:
>   type: mysql  # 使用できるデータベースの種類: sqlite, mysql
>   sqlite:
>     filename: ./plugin.db
>     options: {}
>   mysql:
>     username: root
>     password: "password"
>     address: localhost:3306
>     database: "combakme"
>     options:
>       autoReconnect: true
> ```

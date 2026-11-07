# G510 Unified Wi-Fi Analyzer
NetSpot + WiFi Analyzer + Fing風の機能を、MediaTek Genio 510 / Android 14向けに統合したroot不要の診断アプリです。

## 機能
- 接続中SSID/BSSID/RSSI/Channel/周波数/Link TX/RX/IP/Gateway/DNS
- 周辺AP一覧、WPA/WPA2/WPA3/Open、帯域幅、結果鮮度
- 2.4/5/6 GHzチャンネル重畳グラフ、混雑チャンネル診断
- 同一IPv4 /24内の到達可能端末探索（Fing風の簡易版）
- 地点名とRSSIを保存する簡易サイトサーベイ
- G510横画面Compose UI

## Android 14上の制限
Wi-Fiスキャンには位置情報権限と、端末側の位置情報ONが必要になる場合があります。Androidはスキャン頻度を制限するため、更新要求が拒否された場合はキャッシュ結果を表示します。通常アプリではAPのクライアント一覧、非公開MACベンダーDB、802.11モニターモード、全チャネルの生スペクトラム、他端末のパケット内容は取得できません。

## Build
Android Studioでフォルダーを開き、JDK 17を指定してGradle Sync後、G510へRunしてください。自分が管理するネットワークでのみLAN探索を使用してください。

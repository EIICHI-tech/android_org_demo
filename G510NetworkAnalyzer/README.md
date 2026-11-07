# G510 Network Analyzer
MediaTek Genio 510 / Android 14向けのroot不要ネットワーク診断アプリ。Ethernet/Wi-Fi/Mobile/VPN、IP、DNS、Gateway、推定帯域を表示し、Ping相当、DNS、TCP、HTTPレイテンシ、3秒連続監視、グラフ、100件履歴に対応。

## Build
Android Studioで開き、JDK 17を選択してGradle Sync後、G510 EVKへRun。CLIは `./gradlew assembleDebug`。

## 制限
非root版のため、全アプリのパケットキャプチャ、ARP、他プロセス通信は取得しません。`InetAddress.isReachable`は環境によりICMP応答を得られない場合があります。
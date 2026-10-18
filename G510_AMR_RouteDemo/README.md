# G510 AMR Route Demo

G510 / Android 14向け、自動搬送ロボットのルート検索展示デモです。

## 主な機能
- 20 x 12の工場グリッドマップ
- 出発点、目的地、障害物のタップ編集
- A*アルゴリズムによる最短経路検索
- AMR走行アニメーション、一時停止、リセット
- 距離、推定時間、探索ノード数、バッテリー表示
- 通路閉塞を想定した障害物追加と再探索
- シナリオプリセット

## 開き方
1. Android Studioで本フォルダーを開きます。
2. Gradle JDKを `Embedded JDK 17` にします。
3. Gradle Sync後、appを実行します。

Windowsビルド: `gradlew.bat assembleDebug`
APK: `app/build/outputs/apk/debug/app-debug.apk`

パッケージ: `jp.co.demo.g510amr`

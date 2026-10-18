# G510 AI Cafe Concierge PoC

営業提案資料の体験をG510 Androidで再現するオフラインPoCです。

## 実装済み
- カフェ店員アバター表示
- ローカル会話デモ（おすすめ、甘くない飲み物、注文方法、デザート、カフェラテ）
- ドリンク、フード、デザート各6商品
- タッチ注文、カート、数量、合計
- 注文確認と注文完了
- 商品追加時の確認音
- 横画面固定、ネット接続不要

## 重要
この版はChatGPT APIへ接続していません。自由会話ではなく、営業デモ向けの固定会話ロジックです。API接続する場合は、APIキーをAPKへ埋め込まず、中継サーバー経由にしてください。

## ビルド環境
Android Gradle Plugin 8.5.2 / Gradle 8.7 / JDK 17
Android StudioでGradle 9.6以降を強制しないでください。

Build > Build APK(s)
APK: app/build/outputs/apk/debug/app-debug.apk
Install: adb install -r app-debug.apk
Package: com.kagafei.g510aicafe

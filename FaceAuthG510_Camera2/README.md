# FaceAuthG510 Camera2 完全版

G510ログで確認された CameraX CameraValidator 問題を回避するため、CameraXを完全に廃止し、Camera2 APIで実装した顔認証デモです。

## 対応内容
- Camera ID一覧からFRONTを選択。G510ではID 0を選択
- TextureViewによる1280x720プレビュー
- ImageReader JPEG撮影
- ML Kit bundled顔検出
- 顔登録、本人確認、一致度表示、登録消去
- Camera2のエラーコードを画面表示
- アプリIDは旧版と同じ com.kagafei.faceauth。`adb install -r` で更新可能

## ビルドと導入
1. Android Studioでこのフォルダーを開く
2. Gradle Sync
3. Build > Build APK(s)
4. PowerShell:
   adb install -r .\app\build\outputs\apk\debug\app-debug.apk
5. 起動:
   adb shell am force-stop com.kagafei.faceauth
   adb shell am start -n com.kagafei.faceauth/.MainActivity

## 注意
初回Gradle Syncは依存関係取得にネット接続が必要です。実行時はオフラインです。これは展示用PoCで、生体検知や写真・画面再生攻撃対策、正式なFAR/FRR評価は含みません。

# FaceAuthG510 Camera2 修正版完全版

CameraXを使用せず、G510のCamera2 APIを直接利用する顔認証PoCです。
今回の版では、前版のビルド時に発生した Kotlin Duplicate class 対策として次を実施しています。

- AppCompat、CameraX、CameraX Viewを削除
- ActivityをAndroid標準の android.app.Activity に変更
- kotlin-stdlib-jdk7 / kotlin-stdlib-jdk8を全構成から除外
- kotlin-stdlibを1.8.22に統一
- Camera ID一覧からFRONTを選択し、G510ではCamera ID 0を使用
- TextureView + Camera2 + ImageReaderでプレビューと撮影
- ML Kit bundled顔検出

## 推奨ビルド手順

1. 旧プロジェクトとは別フォルダーへ展開
2. Android Studioで本フォルダーを開く
3. File > Sync Project with Gradle Files
4. Build > Clean Project
5. Build > Rebuild Project
6. Build > Build APK(s)

## G510への更新

```powershell
adb uninstall com.kagafei.faceauth
adb install .\app\build\outputs\apk\debug\app-debug.apk
adb shell pm grant com.kagafei.faceauth android.permission.CAMERA
adb shell am start -n com.kagafei.faceauth/.MainActivity
```

## 注意
展示用PoCです。写真・画面によるなりすまし防止、生体検知、正式なFAR/FRR評価は含みません。

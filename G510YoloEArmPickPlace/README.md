# G510PickPlace Front Camera Fix

このZIPは、既存の G510YoloEArmPickPlace プロジェクトへ上書きする修正ファイル一式です。
プロジェクト名フォルダーは別名 `G510PickPlace_FrontCamera_Fix` ですが、既存クラスとの互換性を保つためパッケージ名は `jp.co.kagafei.g510pickplace` のままです。

## 修正内容
- セルフオーダー版と同じく FRONT カメラを優先して選択
- CameraX初期化時から FRONT カメラだけに制限
- `providerFuture.get()` を try/catch で保護
- Manifestへ `G510CameraXApplication` を登録
- `camera.front` を必須、汎用/back camera feature を非必須へ変更

## 上書き先
ZIP内の `app` フォルダーを、既存プロジェクトのルートへコピーしてください。

- `app/src/main/java/jp/co/kagafei/g510pickplace/MainActivity.kt`
- `app/src/main/java/jp/co/kagafei/g510pickplace/G510CameraXApplication.kt`
- `app/src/main/AndroidManifest.xml`

## ビルド
```powershell
cd C:\G510\android_org_demo\G510YoloEArmPickPlace
.\gradlew.bat clean
.\gradlew.bat assembleDebug
adb uninstall jp.co.kagafei.g510pickplace
adb install .\app\build\outputs\apk\debug\app-debug.apk
```

## 確認
```powershell
adb logcat -c
adb shell monkey -p jp.co.kagafei.g510pickplace -c android.intent.category.LAUNCHER 1
adb logcat -d | findstr /i "G510PickPlaceCamera CameraValidator AndroidRuntime"
```

正常時は `Camera bound successfully` が表示されます。

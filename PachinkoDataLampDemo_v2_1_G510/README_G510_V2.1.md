# G510向けアイコン修正版 V2.1

## 修正点
- Adaptive Icon / Vector XMLを廃止
- `drawable-nodpi/g510_launcher_icon.png` をManifestから直接指定
- mdpiからxxxhdpiまで従来型PNGアイコンを収録
- 通常LAUNCHERとLEANBACK_LAUNCHERを維持
- versionCode 4 / versionName 2.1
- Java、Kotlin、JVM Toolchain 17を維持

## 推奨インストール
ランチャーの古いアイコン情報を残さないため、V2.0を一度アンインストールしてからV2.1を入れます。

```powershell
adb uninstall jp.co.demo.pachinkodatalamp
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n jp.co.demo.pachinkodatalamp/.MainActivity
```

または、ビルド後に `install_g510_v2_1.bat` を実行します。

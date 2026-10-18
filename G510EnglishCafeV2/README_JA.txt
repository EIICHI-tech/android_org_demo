G510 English Cafe V2
====================

修正内容
- Composeを廃止し、標準XMLレイアウトへ変更
- JavaとKotlinのJVMターゲットを17に統一
- compileSdk/targetSdk 35
- Google TTSパッケージ com.google.android.tts を明示指定
- arm64-v8a端末を含むAndroid 8.0以降に対応

Android Studioでのビルド
1. ZIPを展開
2. Android Studioで G510EnglishCafeV2 フォルダーを開く
3. 上部にSync Nowが出たら押す
4. Settings > Build Tools > Gradle > Gradle JDK をEmbedded JDK 17にする
5. Build > Build Bundle(s) / APK(s) > Build APK(s)

APK:
app\build\outputs\apk\debug\app-debug.apk

G510へインストール:
adb install -r .\app\build\outputs\apk\debug\app-debug.apk

起動:
adb shell am start -n com.example.g510englishcafe/.MainActivity

既存アプリと署名が競合する場合:
adb uninstall com.example.g510englishcafe
adb install .\app\build\outputs\apk\debug\app-debug.apk

端末確認:
adb shell settings get secure tts_default_synth
adb shell pm list packages | findstr com.google.android.tts

注意:
- 初回のみGradle/SDK依存ファイルの取得にネット接続が必要になる場合があります。
- 音声認識は端末のRecognitionServiceを使用します。
- 採点は認識単語と模範文の一致率によるデモ用簡易採点です。

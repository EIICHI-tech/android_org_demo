# G510 English Cafe

Genio 510 / Android向けの英会話教室デモです。

## 主な機能
- 4シーン: カフェ、自己紹介、旅行、ビジネス
- Google TTSを明示指定した英語読み上げ
- Android SpeechRecognizerによる英語音声入力
- 英単語一致率による簡易スコア
- 日本語字幕、英語お手本、再試行フィードバック
- 展示向け横画面UI

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle Sync
3. Build > Build APK(s)
4. `app/build/outputs/apk/debug/app-debug.apk` をG510へ転送

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## 端末確認
```powershell
adb shell settings get secure tts_default_synth
adb shell pm list packages | findstr google.android.tts
adb shell getprop ro.product.cpu.abi
```

期待値は `com.google.android.tts` と `arm64-v8a` です。

## 注意
- 初回起動時にマイク権限を許可してください。
- 音声認識は端末のRecognitionServiceに依存します。完全オフライン化する場合はWhisper等への置換が必要です。
- 現在の採点は発音音素解析ではなく、認識語と模範文の単語一致率です。

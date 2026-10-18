# G510 Voice Order Fallback

G510で `SpeechRecognizer.isRecognitionAvailable()` が false になる構成を回避する検証用Androidアプリです。

## 方針
- `SpeechRecognizer` と `isRecognitionAvailable()` を使用しません。
- `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` を直接起動します。
- 対応Activityがない場合や起動失敗時は、クラッシュや「この端末では使えません」で停止せず、手入力へ切り替えます。
- パッケージ名は `com.kagafei.g510.voiceorder` です。

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle Sync
3. G510をADB接続
4. Run、または `./gradlew assembleDebug`

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 確認ログ (PowerShell)
`adb logcat -s G510VoiceOrder:* ActivityTaskManager:*`

## 重要
G510側に `android.speech.action.RECOGNIZE_SPEECH` を処理するActivityが登録されていない場合、Google翻訳内の音声認識が動いていても、外部アプリからGoogle翻訳内部の認識機能を直接利用することはできません。その場合、本アプリは確実に手入力へフォールバックします。完全オフライン音声認識が必要な場合は、Whisper/Vosk等のモデルと実装を別途組み込む必要があります。

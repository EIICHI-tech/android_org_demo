# VoiceCam AI 1.1 Fixed
G510 Android 14向け修正版です。

主な修正:
- onEndOfSpeechでは次セッションを開始しない
- onResults/onErrorだけを認識セッション終端として扱う
- 部分認識結果ではコマンドを実行しない
- TTS中にcancel()を連打しない
- TTS完了後800ms待って認識再開
- VoiceCamSpeechタグの診断ログ追加
- INTERNET権限追加

実行前に競合アプリを停止:
adb shell am force-stop com.ashida.g510tobacco

ログ:
adb logcat -c
adb logcat | findstr /i "VoiceCamSpeech RecognitionService SodaSpeechRecognizer"

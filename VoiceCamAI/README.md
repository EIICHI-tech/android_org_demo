# VoiceCam AI for G510 / Android 14
完全ハンズフリー自撮りカメラ。CameraX、Android SpeechRecognizer、TextToSpeechを使用します。

## ビルド
1. Android Studioでこのフォルダーを開く
2. JDK 17を選択してGradle Sync
3. G510へRun

## 音声
「Hi カメラ」→「撮って」「3秒後に撮って」「ズーム」「ズームアウト」「明るく」「暗く」「カメラ切り替え」「顔を中央にして」「肌を明るく」「終了」

## 注意
SpeechRecognizerはOS搭載の認識サービスを使います。端末構成によりネットワークが必要です。Android API上は真の常時ホットワード専用APIではないため、認識セッション終了後に自動再開する方式です。TTS中は認識を停止して自己認識を防ぎます。

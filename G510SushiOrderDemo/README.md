# G510 Sushi Order Demo

Android標準APIだけで構成した回転寿司向けPoCです。

## 現在の動作
- Camera2によるカメラプレビュー
- 「着席を検知（デモ）」で着席イベントを模擬
- Android SpeechRecognizerで人数と飲み物を音声入力
- Android TextToSpeechで質問と復唱
- 横画面UI

## ビルド
Android Studioでこのフォルダーを開き、Build > Build APK(s)。
compileSdk 35を使用します。

## G510への導入
adb install -r app/build/outputs/apk/debug/app-debug.apk

## 重要
着席の自動人物判定は未実装です。現在はボタンで着席イベントを模擬します。実装時は人物検出モデルまたは在席センサーを接続してください。
SpeechRecognizerはG510 Androidイメージの音声認識サービス構成に依存します。

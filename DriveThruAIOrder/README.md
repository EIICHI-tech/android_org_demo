# GENIO Drive Order

Genio G510 / Android 14向けの写真メニュー付き音声注文デモです。

## 主な機能
- Android `SpeechRecognizer`（端末上のGoogle音声認識サービスを利用）
- 「HI GENIO」「ハイジェニオ」で注文開始
- 写真メニュー、音声注文、追加、キャンセル、復唱、確定
- Room経由でSQLiteの `orders` テーブルへ保存
- Android `TextToSpeech` による案内

## ビルド
1. Android Studioでこのフォルダーを開く
2. Android SDK 35をインストール
3. Gradle Sync
4. G510（Android 14）をUSBデバッグ接続
5. `Run` または `./gradlew assembleDebug`
6. APK: `app/build/outputs/apk/debug/app-debug.apk`

## 実機条件
- Google音声認識サービスがインストールされ、既定の音声入力として利用可能であること
- 初回起動時にマイク権限を許可すること
- クラウド認識を使う場合はネットワーク接続が必要

## 音声例
- 「HI GENIO」
- 「ダブルチーズバーガーを2個」
- 「ポテトを1つ」
- 「以上です」
- 「はい」

## 注意
`SpeechRecognizer`による常時再待受はPoC向けです。量産品の低消費電力ウェイクワードには専用Wake Wordエンジンを推奨します。
商品画像はAI生成の架空メニュー画像です。実在ブランドのロゴや商品名は含めていません。

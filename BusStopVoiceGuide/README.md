# BusStopVoiceGuide

G510 / Android 14 の横長タブレット向け、完全ハンズフリーのバス停音声案内デモです。

## 動作
1. 初回起動時だけマイク権限を許可
2. 「HI BASUTEI」「ハイ バス停」「はい バス停」と話す
3. 「どちらに行きたいですか？」に対して目的地を話す
4. 乗り場、発車時刻、行き先、所要時間、料金を表示・読み上げ
5. 「次のバス」「もう一度」「料金」「乗り場」「終了」に対応

## Android Studio
- JDK 17
- compileSdk 35
- minSdk 26
- Kotlin 2.0.20
- Android Gradle Plugin 8.6.1

Android Studioでこのフォルダーを開き、Gradle Sync後に実機へ実行してください。
音声認識サービスと日本語TTSが端末に必要です。G510がオフラインの場合、端末の音声認識サービス設定によっては認識できません。

## 時刻表の変更
`app/src/main/assets/timetable.json` を編集します。同じ目的地に複数便を登録できます。
発車時刻を過ぎた便は翌日便として扱います。

## 実運用前の注意
このプロジェクトの時刻・運賃はデモ用架空データです。実運用時はバス事業者のGTFS/GTFS-Realtimeまたは公式APIに置換してください。
Android標準SpeechRecognizerは専用ウェイクワードDSPではないため、常時運用ではPorcupine等のウェイクワードエンジンも検討してください。

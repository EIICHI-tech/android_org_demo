# CarClimateVoiceAndroid

車載ナビ風の横画面で、音声認識と音声合成によりエアコンを操作するAndroidデモです。

## 主な機能
- 横画面固定のナビ＋空調UI
- Android SpeechRecognizerによる日本語音声認識
- Android TextToSpeechによる日本語応答
- 温度、風量、電源、AUTO、A/C、内外気、デフロスター操作
- 1発話内の複数命令を順に処理
- 誤認識時は車両操作を行わず再入力を案内

## 動作環境
- Android Studio
- JDK 17
- minSdk 26 / targetSdk 35 / compileSdk 35
- 実機推奨

## 開き方
1. Android StudioでこのフォルダーをOpen
2. Gradle Syncを実行
3. JDK 17を選択
4. Android実機を接続してRun
5. 初回起動でマイク権限を許可

## 音声例
- 24度にして
- 暑い / 寒い
- 風量を最大にして
- エアコンをつけて / 切って
- 内気循環 / 外気導入
- 前の曇りを取って
- 24度にして、風量を最大にして

## 実車連携
本アプリは展示用UIデモであり、CAN、LIN、車載Ethernetには接続しません。`ClimateViewModel.processCommand()`で確定した操作を、実車SDKまたはゲートウェイAPIへ渡す形で拡張できます。

# Hospital Voice Reception

G510 Android 14向け、縦型の病院音声受付デモです。

## 機能
- Jetpack Composeによる縦型受付UI
- Compose Canvas製の受付アバター
- Android SpeechRecognizerによる日本語音声認識
- TextToSpeechによる回答読み上げ
- 初診・再診・診療科受付
- 院内案内、受付時間、薬受渡案内
- 緊急キーワード検出
- タッチ操作フォールバック

## 開き方
1. Android Studioでこのフォルダーを開く
2. Gradle Syncを実行
3. G510をADB接続
4. appを実行

## 注意
- 初回起動時にマイク権限を許可してください。
- SpeechRecognizerは端末にインストールされた音声認識サービスを利用します。
- 診療時間、受付番号、院内位置はデモデータです。
- 医療診断や薬の判断を行うアプリではありません。

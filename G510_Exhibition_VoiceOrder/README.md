# G510 展示会向け 会話型AIセルフオーダー 完成版

## 搭載機能
- CameraX + ML Kit Face Detectionによる入店人数検知
- カメラが使えない場合の手動人数設定
- 写真付きメニュー4品
- 写真タップ注文
- 日本語の会話型注文解析
- 注文追加、取り消し、確認、確定
- TextToSpeechによる音声ガイド
- G510対策済み音声認識
  - `SpeechRecognizer.isRecognitionAvailable()`を使用しない
  - `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`を直接起動
  - 起動不可時は写真メニュー・手入力へ安全にフォールバック
- 横画面展示UI

## ビルド
Android Studioでフォルダーを開き、JDK 17を選択してGradle Sync後にRunしてください。
CLIの場合は、Android StudioでGradle Wrapperを生成した後 `gradlew.bat assembleDebug` を実行します。

## インストール
`adb install -r app/build/outputs/apk/debug/app-debug.apk`

## 操作例
- 「バーガーを2つ」
- 「カフェラテを1つ」
- 「おすすめは？」
- 「以上です」
- 「はい」
- 「注文を取り消して」

## 注意
人数検知は顔検出数を来店人数として扱う展示用実装です。逆光、横顔、遮蔽、カメラ画角により検出数は変動します。本番用途では追跡ID、検出安定化、プライバシー表示、カメラ設置調整を追加してください。画像はアプリ内に同梱済みです。

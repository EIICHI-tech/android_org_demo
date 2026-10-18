# G510 会話セルフオーダーデモ

Android標準の SpeechRecognizer と TextToSpeech を使い、会話で商品追加・取消・注文確定ができる横画面デモです。

## 操作
1. Android Studioでこのフォルダーを開く
2. Gradle Sync後、G510をUSBデバッグ接続してRun
3. 初回のマイク権限を許可
4. 「サーモンを2皿」「まぐろ1つ追加」「取り消して」「以上です」などと発話

## 主な変更箇所
- メニュー: `MainActivity.kt` の `menu`
- 画面: `activity_main.xml`
- 会話ロジック: `processUtterance()`

## 注意
- Android標準音声認識サービスを利用します。端末の音声認識サービス設定やネットワーク状態により認識品質が変わります。
- 展示会ノイズ対策として、発話中のみ認識するプッシュ・トゥ・トーク方式です。

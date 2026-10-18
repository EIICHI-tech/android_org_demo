# eArm G510 Controller

SIYEENOVE eArmをG510 Android端末からWi-Fiで操作するJetpack Composeアプリです。

## 前提
1. ESP32-C3へ `web_app_control_eArm.py` を `main.py` として書き込みます。
2. eArmを起動します。
3. G510のWi-Fi設定で、インターネット未接続のSSID `eArm` に接続します。
4. アプリを起動します。`ONLINE`になれば操作できます。

## 安全
- 操作ボタンは長押し中のみ動作し、指を離すと停止コマンドを送ります。
- ACTION_CANCEL相当でも停止します。
- アプリがバックグラウンドに移ると全8停止コマンドを送ります。
- 緊急停止ボタンは全8停止コマンドを個別送信します。
- グリッパーへ長時間負荷をかけないでください。

## ビルド
- Android Studioで本フォルダーを開きます。
- JDK 17を選択します。
- Gradle Sync後、G510へRunします。
- minSdk 26 / targetSdk 35 / landscape固定です。

## 音声コマンド例
- 左、右
- 上腕を上げて、上腕を下げて
- 前腕を上げて、前腕を下げて
- 開いて、閉じて、つかんで
- 停止

音声認識は端末側のRecognizerIntentを使います。端末の音声認識エンジン設定によってはネットワークが必要な場合があります。手動操作は外部ネットワーク不要です。

# G510 D2カメラ 音声ガイドセルフレジ V2

起動約0.5秒後に、APK内蔵WAVで次の案内を再生します。
「いらっしゃいませ。セルフレジでのお会計ありがとうございます。商品のバーコードを読み取らせてください。」

バーコード登録成功時は、APK内蔵の1000Hz確認音を再生します。Google TTSは不要です。
右上の「音声案内」で再生し直せます。音量はAndroidのメディア音量で調整してください。

Build > Build APK(s)
APK: app/build/outputs/apk/debug/app-debug.apk
Install: adb install -r app-debug.apk
Package: com.kagafei.g510d2voice

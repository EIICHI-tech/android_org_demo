# G510 AI Vocal OFF Prototype

Android Studioで開いて動作確認できる暫定版です。

## 実装済み
- Android 14 / MediaTek Genio G510向け
- ローカル音源選択
- 権利を持つHTTPS音源ファイルの直接URLダウンロード
- MP3 / M4A / WAV / AACデコード
- Stereo中央定位ボーカル減衰
- 歌手ON / OFF
- 除去強度調整
- versionCode 5

## 重要
この暫定版は公式Spleeter 2-stem学習済みモデル版ではありません。
現在の歌手OFFは左右相関を使ったDSP方式です。
Spleeterモデルの数値整合性、TFLite変換、G510実機検証を満たした完成版とは区別してください。

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle JDK 17を選択
3. Gradle Sync
4. G510をUSB接続してRun

APK更新:
adb install -r app/build/outputs/apk/debug/app-debug.apk

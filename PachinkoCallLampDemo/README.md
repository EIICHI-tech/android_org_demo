# パチンコ呼び出しランプ Android デモ

Genio 510を含むAndroid端末向けの、実機風フルスクリーン呼び出しランプデモです。

## 収録機能
- 実機風LEDフレーム（待機、赤点滅、黄ウェーブ、完了レインボー）
- 通常呼出、玉詰まり、計数機、緊急の4モード
- オフラインで鳴る内蔵チャイム WAV
- 日本語TTS。端末に日本語TTSがない場合もチャイムは再生
- スタッフ到着、対応完了、自動進行
- 横画面、フルスクリーン、外部画像ライブラリ不要

## Android Studio
1. このフォルダーを Android Studio で開く
2. Gradle Syncを実行する。初回はGradleとAndroid依存関係を取得するためネット接続が必要
3. G510をUSBデバッグ接続
4. Runを押す

推奨: JDK 17、Android SDK 35。minSdk 26。

## コマンドビルド
Windows: `gradlew.bat assembleDebug`
Linux/macOS: `./gradlew assembleDebug`
APK: `app/build/outputs/apk/debug/app-debug.apk`

## G510でTTSが動かない場合
内蔵チャイムはTTSに依存せず再生されます。日本語案内も必要な場合は端末の日本語TTSエンジンを有効化してください。

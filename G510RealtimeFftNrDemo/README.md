# G510 Realtime FFT + Noise Reduction Demo

Android 14 (API 34) 向けの最小構成デモです。

## 実装済み
- G510マイクをAudioRecordで48 kHz / PCM16 / mono取得
- 1024ポイントFFTをリアルタイム表示
- RAWとNR後のスペクトルを重ねて表示
- タッチ位置の周波数、RAW/NR強度をdBFS表示
- デモ用スペクトル減算法ノイズリダクション
- RMSとRAW-NR差の表示

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle Syncを実行
3. G510をUSBデバッグ接続
4. Run、または Build > Build APK(s)

コマンドライン環境にGradleがある場合:

    gradle :app:assembleDebug

生成先:

    app/build/outputs/apk/debug/app-debug.apk

## 注意
- 本NRは効果可視化用のスペクトル減算法で、製品向けVPNRではありません。
- 最初の約0.8秒をノイズ推定に使うため、その間は話さず背景ノイズだけを入力してください。
- AudioSource.UNPROCESSEDを使用しますが、BSP側で未対応の場合はMICへ変更してください。
- 48 kHz入力が使えないBSPではサンプルレートとNR条件の変更が必要です。

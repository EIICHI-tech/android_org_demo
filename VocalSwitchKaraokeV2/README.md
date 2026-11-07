# Vocal Switch Karaoke V2

G510 Android 14向けの横画面デモです。

## V1からの変更
- 手書きのasset WebViewを廃止
- android-youtube-player 13.1.0を使用
- ローカルWAV再生をMedia3 ExoPlayer 1.11.1へ変更
- MODIFY_AUDIO_SETTINGSとAndroid 12以降のBLUETOOTH_CONNECTを追加
- 500 msごとの同期監視、180 ms超で補正
- Activityをlandscape固定
- AudioRecordの初期化確認と確実なreleaseを追加

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle JDK 17を選ぶ
3. Sync Project with Gradle Files
4. G510を接続してRun

## 操作
1. READY表示を待つ
2. YouTube URLまたは11文字の動画IDを入力
3. 伴奏WAVと歌声WAVを選択
4. 原曲、歌手OFF、歌手のみ、ミックスを切り替える
5. 映像とのズレは同期補正または再同期で調整する

## 音源条件
PCM WAV、48 kHz推奨。2つのWAVは同じ開始位置と長さにしてください。YouTube音声の抽出は行わず、自作または権利処理済み音源を使用してください。

# Vocal Switch Karaoke / G510 Android 14

YouTube公式IFrame Playerで映像を表示し、権利処理済みの `instrumental.wav` と `vocals.wav` を同期再生するデモです。YouTubeから音声をダウンロード・抽出しません。

## ビルド
1. Android Studioでこのフォルダーを開く
2. JDK 17を選択
3. Gradle Sync
4. G510 Android 14をUSB接続
5. Run

## 使い方
1. YouTube URLまたは11文字の動画IDを入力
2. 「YouTubeを読み込む」
3. 同じ曲・同じ先頭位置・同じ長さの伴奏WAVと歌声WAVを選択
4. ORIGINAL / KARAOKE / VOCAL_ONLY / MIXを切り替え
5. ズレがあれば同期補正を調整
6. マイク録音はアプリ専用Musicフォルダーへ48 kHz mono WAVで保存

## 音源条件
- WAV PCM、48 kHz推奨
- 2ファイルは完全に同じ開始時刻と長さ
- デモには自作、許諾済み、または適切なライセンスの音源を使用

## 注意
YouTubeの映像とローカル音源は完全なサンプル同期にはならず、ネットワークバッファリングでずれる場合があります。本実装は500 ms周期で監視し、180 msを超えた場合に補正します。本格製品では自社ホスト映像とMedia3の単一タイムラインを推奨します。

# Sonic Prism for G510

G510 Android向けリアルタイム音声特徴量ダッシュボードです。

## 表示
- 64-band Mel Spectrogram
- waveform
- Energy / Pitch / ZCR
- Spectral Centroid / 85% Roll-off / Flux / Flatness
- Crest Factor / Voice Activity
- dBFS / app CPU / app PSS RAM / rendering update FPS

## 音声設定
- 16 kHz, mono, PCM 16-bit
- FFT 1024, Hop 256, Hann window
- Mel range 50-8000 Hz
- AudioSource.UNPROCESSED。初期化不可の場合はMICへ自動フォールバック

## 実行
1. Android Studioで本フォルダーをOpen
2. Gradle Sync
3. G510をUSB接続
4. Run
5. マイク権限を許可
6. START

アプリ表示名は `Sonic Prism` です。

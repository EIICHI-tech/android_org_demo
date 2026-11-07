# SMOS / NMOS Monitor for Genio 510 Android

Android Studioで開き、JDK 17 / Android SDK 35でビルドします。

## 実行
1. プロジェクトをAndroid Studioで開く
2. Gradle Sync
3. G510 EVKをUSB接続
4. Run
5. 「マイク許可」→「測定開始」

## 指標
- SMOS proxy: 発話中の入力レベル、クリッピング率、ゼロ交差率から1.0〜5.0で算出
- NMOS proxy: 非発話区間から追従推定したノイズフロアを1.0〜5.0へ変換
- SNR: 現在のRMSレベルと推定ノイズフロアの差

この実装はリアルタイムデモおよびNR ON/OFF相対比較用です。ITU-T P.835準拠の主観評価やMicrosoft DNSMOSモデルの推論結果ではありません。正式評価には校正済み音源、規定された試験条件、または対応する学習済みモデルが必要です。

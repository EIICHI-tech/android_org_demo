# EndoscopeDemo

G510 / Android 14向けの展示会用Android UIデモです。

## 機能
- 内視鏡風デモ映像（Compose描画）
- 録画開始・停止とタイマー表示（実ファイル保存なし）
- 静止画保存カウンター（実ファイル保存なし）
- 1.0x～2.5xズーム
- 画像強調表示ON/OFF
- LIVE / PREVIOUS比較表示
- 患者・検査情報のダミー表示
- 通常ランチャーとLEANBACK_LAUNCHER対応

## 実行
1. Android Studioでこのフォルダーを開く
2. Gradle JDKに17を選択
3. Gradle Sync
4. Android 8.0(API 26)以上の端末でRun

## 注意
これは展示用UIモックです。医療機器、診断、実患者データ、実録画、USBカメラ入力には対応しません。

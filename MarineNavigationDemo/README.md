# MarineNavigationDemo

G510 / Android 14向けの展示会用、オフライン船舶ナビゲーションUIデモです。

## 含まれる機能
- 海図風描画、自船移動、目的地、航路
- 航行開始・停止
- SOG、COG、距離、ETA、風速、波高のデモ表示
- 回転レーダー
- AISターゲット一覧
- 衝突警告デモ
- 昼間・夜間表示切替
- 通常ランチャー / LEANBACK_LAUNCHER

## 実行
1. Android Studioでこのフォルダーを開く
2. Gradle JDKに17を指定
3. Gradle Sync
4. Android 8.0(API 26)以上でRun

## 注意
展示用シミュレーションです。GPS、AIS、レーダー、電子海図、気象APIには接続しません。実航海には使用できません。

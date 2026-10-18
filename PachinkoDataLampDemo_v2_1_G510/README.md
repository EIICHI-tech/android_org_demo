# データランプ型 呼び出しランプデモ v2.0

添付イメージの特徴を参考にしつつ、商標・製品写真を複製せず独自UIで再構成したAndroidデモです。

## 主な機能
- 虹色LED外周、呼出時の赤点滅、スタッフ対応時の黄色演出
- 3桁大型デジタル表示、回転数、当たり回数、最大出玉
- 折れ線グラフ、当たり履歴画面
- 呼出、休憩、データ切替、スタッフ到着、音声ON/OFF、リセット
- 内蔵WAVチャイム、日本語TTS
- 通常ランチャーとAndroid TV系ランチャーの両方へアイコン登録
- Java/Kotlin/JVM Toolchainを17に統一

## ビルド
1. Android Studioで `PachinkoDataLampDemo_v2` を開く
2. Gradle JDKを `Embedded JDK 17` に設定
3. Sync、Clean Project、Rebuild Project
4. APK: `app/build/outputs/apk/debug/app-debug.apk`

## 既存版との共存
パッケージ名を `jp.co.demo.pachinkodatalamp` に変更してあるため、旧版 `jp.co.demo.pachinkolamp` と共存できます。

## インストール
`adb install -r app\build\outputs\apk\debug\app-debug.apk`

## 起動
`adb shell am start -n jp.co.demo.pachinkodatalamp/.MainActivity`

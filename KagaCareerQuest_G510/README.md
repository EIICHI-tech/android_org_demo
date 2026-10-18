# KAGA Career Quest for G510

G510の横画面展示向けに作成した、完全オフライン動作の架空キャリア育成シミュレーションです。
実在社員を評価・採点したり、昇進を予測する機能はありません。

## 内容
- Kotlin / Android標準APIのみ
- 横画面固定、タッチ操作
- FAE・営業・開発・PMの4コース
- 3つの架空業務シナリオ
- 技術理解・顧客対応・連携・リスク管理の学習フィードバック
- ネットワーク、カメラ、個人情報権限は不使用

## Android Studioでビルド
1. Android Studioで本フォルダーを開きます。
2. Gradle Syncを実行します。
3. Build > Build APK(s) を実行します。
4. 生成APKは `app/build/outputs/apk/debug/app-debug.apk` にあります。

## G510へインストール
```powershell
adb devices
adb install -r app-debug.apk
```

## 動作条件
- Android 7.0 (API 24) 以上
- 推奨: 1280x720以上の横画面

## 用途上の注意
本アプリは研修・展示専用です。人事判断、実在社員の評価、昇進推薦、勤務データ分析には利用しないでください。

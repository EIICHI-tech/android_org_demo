# G510 Home Energy Monitor Fixed

G510 / Android 14向けの展示用ホームエネルギーモニターです。太陽光、家庭負荷、蓄電池、EV、給湯器、電力会社の電力フローと、天候別の発電予測を疑似データで表示します。

## JVMエラー修正内容
- Java `sourceCompatibility` / `targetCompatibility`: 17
- Kotlin `jvmTarget`: 17
- Kotlin JVM toolchain: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin / Compose plugin: 2.0.21

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle JDKを `Embedded JDK 17` に設定
3. Gradle Sync
4. `app` を実行

Windowsコマンドライン: `gradlew.bat assembleDebug`

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 操作
右上の天候表示をタップすると「晴れ → くもり → 雨」を切り替えられます。表示値は展示用の疑似データで、外部APIや実機センサーには接続していません。

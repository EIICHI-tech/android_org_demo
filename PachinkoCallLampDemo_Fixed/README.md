# 修正版 パチンコ呼び出しランプ Androidデモ v1.1

## 修正
JavaとKotlinのJVMターゲットを17に統一し、`kotlin { jvmToolchain(17) }`を追加しました。

## ビルド
1. Android Studioでこのフォルダーを開く
2. Gradle JDKを Embedded JDK 17 に設定
3. Sync Project with Gradle Files
4. Build > Clean Project
5. Build > Rebuild Project

APK: `app/build/outputs/apk/debug/app-debug.apk`

# G510 zRAM ModeA / ModeD

1つのソースから2つのAPKを生成します。

- ModeA: zRAM OFF測定用
- ModeD: zRAM ON測定用

重要: APK自身はzRAMをON/OFFしません。ModeA/ModeDは測定役割と表示名を分けるコンパイルスイッチです。実際のzRAM設定は各Androidイメージ側で変更します。

## Build

    gradlew.bat assembleModeADebug assembleModeDDebug

出力:

    app/build/outputs/apk/modeA/debug/G510_ModeA_ZRAM_OFF_debug.apk
    app/build/outputs/apk/modeD/debug/G510_ModeD_ZRAM_ON_debug.apk

同じGradleビルドで生成した2 APKをインストールしてください。署名レベル権限によりModeDがModeAの保存結果を読みます。

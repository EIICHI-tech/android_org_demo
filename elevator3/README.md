# Hi Elevator 15F Landscape

横長タブレット向けのエレベーターデモです。左側に大型ドア、右側に幅300dpの操作盤を配置しています。

- 画面方向: landscape固定
- 階数: 1Fから15F
- 階数ボタン: 48dp x 48dpの真円、3列 x 5段
- 開、閉、呼出: 48dpの小型円形ボタン
- 音声: 「Hi エレベーター」から階数受付
- ERROR_NO_MATCH=7後は自動的に待受を再開
- TTS終了後にSpeechRecognizerを再開

Logcat: `adb logcat | findstr HiElevatorSpeech`

このアプリは表示・音声デモであり、実際のエレベーターは制御しません。

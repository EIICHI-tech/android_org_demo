# HospitalVoiceReception CommandFix

「HI 病院」は認識するが、その後の「初診」などを認識しない問題を修正した完全版です。

## 主な修正
- WAKEモードとCOMMANDモードを別状態として管理
- TTSの `UtteranceProgressListener.onDone()` 後、650ms待ってCOMMAND認識開始
- 各認識セッションにgeneration番号を付与し、古いコールバックを破棄
- COMMAND認識の無音終了時間を1.6秒へ延長
- `SpeechRecognizer` をモード切替ごとに破棄・再生成
- NO_MATCH、TIMEOUT、RECOGNIZER_BUSY時に自動再試行
- 「初診」「初めて」「初回」「初めて来た」を初診として処理

## 動作
1. 初回だけマイク権限を許可
2. 「HI 病院」と発話
3. 「はい、お伺いします」の読み上げが完全に終わるまで待つ
4. 「初診です」などを発話
5. 案内終了後は自動的にウェイク待機へ戻る

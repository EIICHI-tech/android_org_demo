G510 YOLOv8 MJPEG エリア表示 + Pose調整 完全版

含まれる機能:
- ESP32-CAM /stream のMJPEG読込
- YOLOv8n Float32 TFLite検出
- LEFT / TARGET / RIGHT の3エリア常時表示
- 検出枠、中心点、クラス、信頼度、エリア名表示
- 自動把持はTARGETエリアで3フレーム連続検出した場合のみ開始
- A/B/C/D 各±5度調整
- HOME/SEARCH/PICK/GRIP/LIFT/DROP_ABOVE/DROP_DOWN/RELEASE
- Pose保存
- STARTボタンIDを startButton に統一
- カメラ、eArm通信エラーでクラッシュしない
- eArm MicroPython main.pyでC軸のみ反転

必須作業:
1. yolov8n_float32.tflite を app/src/main/assets/ に配置してください。
2. モデル出力は [1,84,8400] を想定しています。
3. esp32_earm/secrets.py のSSIDとパスワードを変更してください。
4. esp32_earm/main.py と secrets.py をeArm ESP32へ転送してください。
5. 初回は物体なしでPoseを確認してください。

既定接続:
eArm: http://172.20.10.13
Camera: http://172.20.10.14/stream

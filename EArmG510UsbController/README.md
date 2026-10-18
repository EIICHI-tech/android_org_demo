# eArm G510 USB-C版
1. `firmware/usb_serial_eArm_main.py` をESP32-C3へ `main.py` として保存します。
2. eArmを再起動し、G510とeArmをUSB-C OTG対応ケーブルで接続します。
3. アプリを起動しUSB使用許可を承認します。
4. `USB ONLINE`後に操作します。

通信はCH340、115200bps、8-N-1、ASCII改行区切りです。G510がUSBホスト/OTGとしてCH340を認識できる必要があります。ロボットには18650バッテリーを装着し電源をONにしてください。
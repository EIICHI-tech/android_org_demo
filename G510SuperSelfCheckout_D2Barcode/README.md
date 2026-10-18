# G510 D2カメラ バーコードセルフレジ

写真36商品版へD2カメラの連続バーコード読取りを統合したAndroid Studioプロジェクトです。

## 機能
- Camera ID 0～3切替（D2カメラIDを実機で選択）
- EAN-13/JAN-13、EAN-8/JAN-8、UPC-A、QR読取り
- 読取り成功音、未登録JAN表示、2.5秒の重複登録抑止
- 36商品写真、手動商品追加、カート、税込合計、支払いデモ
- オフライン商品マスター

## ビルド
初回Gradle同期時はZXing依存関係取得のためPC側インターネット接続が必要です。
Android Studio: Build > Build APK(s)
APK: app/build/outputs/apk/debug/app-debug.apk

## G510へ導入
adb install -r app-debug.apk
Package: com.kagafei.g510d2checkout

## D2カメラ選択
起動後、右上のCamera IDを選び「カメラ切替」。D2映像が表示されるIDを使用してください。

## デモJAN
食パン: 4901234500006
牛乳: 4901234500013
たまご: 4901234500020

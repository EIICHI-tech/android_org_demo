# 世界写真ジャーニー 縦画面完成版

## 修正内容
- `AndroidManifest.xml` を `android:screenOrientation="portrait"` に変更
- 上43%を地図、下57%を写真・情報・操作ボタンに変更
- EXIF GPSと撮影日時を読み取り、時系列で経路表示
- 複数写真、自動再生、マーカー選択、旅程全体表示に対応
- Google Maps APIキーをSecrets Gradle Plugin経由でManifestへ設定

## Google Maps APIキー設定
1. Google Cloud Consoleでプロジェクトを作成
2. Maps SDK for Androidを有効化
3. 課金アカウントを関連付け
4. APIキーを作成
5. APIキーのアプリケーション制限をAndroidアプリに設定
6. パッケージ名 `jp.eiichi.worldphotojourney` と、デバッグまたはリリース署名のSHA-1を登録
7. プロジェクト直下の `local.properties` に次を追加

```properties
sdk.dir=C\:\Users\ashida\AppData\Local\Android\Sdk
MAPS_API_KEY=ここに実際のAPIキー
```

APIキーは第三者へ配布できないためZIPには実キーを含めていません。`local.properties.example`を参考にしてください。

## SHA-1確認
Android StudioのGradleウィンドウで `app > Tasks > android > signingReport` を実行するか、ターミナルでGradleの`signingReport`を実行し、Variant `debug` のSHA1をGoogle Cloud Consoleへ登録します。

## 白い地図の確認
```powershell
adb logcat -c
adb logcat | findstr /i "Google Maps Android API Authorization API_KEY"
```
`Authorization failure` が出る場合は、Maps SDK有効化、課金設定、パッケージ名、SHA-1、APIキー制限を確認してください。設定変更後はアプリをアンインストールして再インストールしてください。

## 実行
Android Studioでプロジェクトを開き、Gradle Sync後、縦向きのAndroidタブレットまたはG510でRunしてください。

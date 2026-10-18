# VoiceLlmChat Complete

Android SpeechRecognizerで日本語音声を文字化し、OpenAI互換APIへ送信して、ストリーミング回答をAndroid TTSで読み上げるデモです。

## Android Studioでの実行
1. このフォルダーをAndroid Studioで開きます。
2. JDK 17を選択してGradle Syncを実行します。
3. G510またはAndroid端末を接続します。
4. Runを実行し、初回起動時にマイクを許可します。
5. アプリ右上の「設定」でEndpointとModelを保存します。

## Ollama接続例
- Endpoint: `http://192.168.0.10:11434`
- Model: `qwen2.5:3b`
- API Key: 空欄

本版は `android:usesCleartextTraffic="true"` と `network_security_config.xml` を含み、LAN内HTTP接続を許可しています。

## PC側の注意
Android端末とPCを同じLANへ接続し、OllamaをLANから到達可能な状態で起動し、Windows FirewallでTCP 11434を許可してください。

## 本番運用上の注意
デモ用にHTTPを許可しています。本番ではHTTPSを使い、APIキーをAndroid端末へ直接保存せず、安全なバックエンドを利用してください。

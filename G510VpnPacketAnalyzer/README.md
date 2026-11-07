# G510 VPN Packet Analyzer
G510 Android 14向けのVpnService学習用パケットアナライザーです。root不要で、VPN承認後にTUNへ流れるIPv4/IPv6送信パケットを解析し、TCP/UDP/ICMP、送信元/宛先IP、ポート、サイズ、TCPフラグを表示します。

## 重要
本版は安全な「capture-only」版です。TUNパケットを外部ネットワークへ転送しないため、キャプチャ中は端末のインターネット通信が停止します。停止ボタンで即時復旧します。実通信を維持する完全版には、VPNゲートウェイまたはtun2socks等のユーザー空間TCP/IP転送エンジンが必要です。

## Build
Android Studioで開きJDK 17を選択、Gradle Sync後にG510へRun。開始時のVPN確認を許可してください。自分が管理する端末・通信だけで使用してください。

# OffGrid

> 离线语音通讯应用

OffGrid 是一款开源 Android 应用，让徒步、骑行等户外活动团队在没有移动网络时，由一台手机开启热点（Wi-Fi Direct Group Owner），其他手机接入该热点，组成局域网实现实时语音通讯与位置共享。

## 核心特性

- 📡 **离线语音群聊**：不依赖基站或互联网
- 🔗 **星型组网**：一台设备作为 Group Owner，其他设备作为 Client 接入，无需多跳中继
- 🧭 **位置共享**：在罗盘页显示队友相对方位与距离
- 👥 **邻居发现**：基于 UDP 广播的 NodeId、HELLO 心跳与邻居老化
- 🔋 **后台保活**：前台服务 + WakeLock + WiFiLock + 电池优化白名单引导
- 🔒 **隐私优先**：无账号、无服务器、数据不上云
- 📱 **Android 12+**：针对 Android 12+ 优化，通过 Wi-Fi Direct Group Owner / Client 星型组网实现低延迟语音

## 当前状态

- ✅ M1：Wi-Fi Direct 直连、IPv4 通信、Opus 延迟验证
- ✅ M2：双机直连语音通话
- ✅ M3：双机稳定 + 位置共享（详见 `docs/M3_ALPHA_TEST.md`）
- ✅ M4-T5：手动网络配置入口（Group Owner / Client）与故障排查提示（详见 `docs/M4-T5_MANUAL_NETWORK_CONFIG.md`）
- ✅ M4-T7：完善开发者文档（详见 `docs/DEVELOPER_GUIDE.md`）
- ✅ M4-T8：Beta 版本测试（详见 `docs/M4-T8_BETA_TEST_REPORT.md`）
- ❌ M4-T6：省电模式已从产品需求中删除，不再开发和测试

## 技术栈

- Kotlin
- Jetpack Compose
- Wi-Fi Direct
- Opus 音频编解码（自编译 Xiph libopus + JNI）
- UDP over IPv4（192.168.49.x/24）
- 自定义局域网包格式（NodeId、Type）

## 快速开始

见 [BUILD.md](./BUILD.md)。

## 文档

- [构建指南](./BUILD.md)
- [开发者指南](./docs/DEVELOPER_GUIDE.md)
- [贡献指南](./CONTRIBUTING.md)
- [架构设计](./docs/ARCHITECTURE.md)
- [项目计划](./docs/PROJECT_PLAN.md)
- [PRD](./docs/PRD.md)
- [M2 语音管道](./docs/M2_VOICE_PIPELINE.md)
- [M3 Alpha 测试](./docs/M3_ALPHA_TEST.md)
- [M4-T5 手动网络配置](./docs/M4-T5_MANUAL_NETWORK_CONFIG.md)
- [M4-T8 Beta 测试报告](./docs/M4-T8_BETA_TEST_REPORT.md)

## 贡献

欢迎提交 Issue 和 Pull Request！请先阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 许可

[MIT License](./LICENSE)

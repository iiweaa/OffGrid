# Changelog

## [1.0.0] - 2026-06-28

### 新增
- Wi-Fi Direct 直连语音通话：支持 Group Owner / Client 角色发现并建立 P2P 连接。
- UDP IPv4 Mesh 数据包传输（192.168.49.x/24），支持 NodeId、TTL、心跳 HELLO。
- Opus 语音编解码：16 kHz 单声道、20 ms 帧、24 kbps，端到端延迟满足 < 200 ms 目标。
- 音频外设切换：有线/蓝牙耳机插入、拔出自动路由，扬声器回退。
- 位置共享：基于 GPS 的相对方位与距离显示。
- 前台服务保活：锁屏 / 后台保持语音连接。

### 变更
- 将 `com.github.martoreto:opuscodec` 替换为自编译 Xiph libopus v1.4 + JNI，消除许可证风险并补齐 `arm64-v8a` 原生库。
- 产品决策：户外实测取消；省电模式从需求中删除，相关代码保留但不维护。

### 修复
- 蓝牙耳机已连接但音频未走蓝牙的问题（BC-8）。
- 部分机型 Wi-Fi Direct 建组 / 发现失败时的手动配置入口与提示。

### 文档
- 新增 `docs/M5-T3_LICENSE_COMPLIANCE.md`、`docs/M5-T3_OPUS_REPLACEMENT.md`、`docs/M5-T3_REGRESSION_REPORT.md`。
- 更新 `README.md`、`BUILD.md`、`DEVELOPER_GUIDE.md`、`ARCHITECTURE.md`、`docs/PROGRESS_TRACKING.md`。

### 测试
- M4 Beta 测试：BC-1 ~ BC-3、BC-5 ~ BC-8 全部通过，无 P0/P1 缺陷。
- M5-T3-2 回归：BC-1 / BC-5 / BC-8 在一加 11 / 华为 Mate 30 Pro 5G 上通过。

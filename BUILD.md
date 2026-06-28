# OffGrid 构建指南

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK API 34 / build-tools 34.0.0
- 至少两台 Android 12+ 真机（用于 Wi-Fi Direct 直连语音测试）

## 本地构建

```bash
# 1. 克隆仓库
git clone <repo-url>
cd OffGrid

# 2. 使用 Gradle Wrapper 构建
./gradlew assembleDebug

# 3. 安装到已连接设备
./gradlew installDebug
```

## 运行测试

```bash
# 单元测试
./gradlew test

# 连接设备测试（如有）
./gradlew connectedAndroidTest
```

## 项目结构

```
app/src/main/java/com/offgrid/app/
├── MainActivity.kt                 # 主入口与底部导航
├── OffGridApplication.kt           # Application 类
├── WifiDirectTestActivity.kt       # Wi-Fi Direct 建组/连接测试（Direct Connection Test）
├── OpusLatencyTestActivity.kt      # Opus 纯编解码延迟测试
├── audio/
│   ├── AudioEngine.kt              # 采集、Opus 编解码、播放
│   ├── media/MediaButtonHandler.kt # 蓝牙耳机按键处理
│   └── router/AudioRouter.kt       # 音频设备切换监听
├── link/
│   ├── LinkManager.kt              # UDP Socket 发现与收发
│   ├── CapabilityStateHolder.kt    # 设备 AP-STA 并发能力状态
│   ├── WifiDirectCapability*.kt    # 并发能力检测
│   ├── location/                   # GPS 获取、相对方位/距离计算、位置广播
│   ├── neighbor/                   # 邻居表与老化逻辑
│   ├── node/                       # NodeId 生成与持久化
│   ├── packet/                     # Mesh 包格式与序列化
│   ├── signal/                     # HELLO 心跳与信令
│   └── wifidirect/                 # NetworkRole/NetworkConfig/WifiDirectConnector
├── power/
│   └── PowerSavingConfig.kt        # 省电模式配置
├── service/
│   ├── VoiceService.kt             # 前台语音服务
│   ├── VoiceState.kt               # 通话状态与 VoiceStateHolder
│   └── keepalive/KeepAliveHelper.kt# 后台保活辅助
├── ui/screens/                     # Home / Call / Peers / Settings / Onboarding
├── ui/theme/                       # 主题、颜色、字体、深色模式
└── util/
    └── BatteryOptimizationHelper.kt# 电池优化白名单引导
```

## MVP 真机测试流程

1. 在两台手机上分别安装并启动 App。
2. 在两台设备上授予：位置、附近设备、录音、通知权限。
3. 进入 **Settings → Connection**：
   - 设备 A 选择 **Group Owner**。
   - 设备 B 选择 **Client**。
4. 返回底部导航 **Call**，设备 A 点击 **Start Call** 创建 Group；
   设备 B 点击 **Start Call** 扫描并加入该 Group。
5. 等待界面显示邻居节点与 IP 地址（`192.168.49.x`）后进行双向通话。

> 也可使用 **Direct Connection Test** 页面（开发者工具）手动创建 Group / 发现 Peer / 连接。

## CI

GitHub Actions 会在每次 push 和 PR 时自动运行 `./gradlew build`。

## 常见问题

### Wi-Fi Direct 测试无响应

- 确保授予「附近设备」与「位置」权限
- 确保两台设备都开启了 Wi-Fi
- 部分机型可能需要关闭 WLAN 助理/自动切换网络

### 编译失败

- 检查 JDK 版本是否为 17
- 检查 Android SDK 是否包含 API 34
- 尝试 `./gradlew clean build`

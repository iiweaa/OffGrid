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
├── MainActivity.kt                 # 主入口与 Compose Navigation
├── OffGridApplication.kt           # Application 类
├── WifiDirectTestActivity.kt       # Wi-Fi Direct 建组/连接测试
├── OpusLatencyTestActivity.kt      # Opus 纯编解码延迟测试
├── audio/
│   └── AudioEngine.kt              # AudioRecord / Opus / AudioTrack
├── link/
│   └── LinkManager.kt              # UDP 发现与收发
├── service/
│   ├── VoiceService.kt             # 前台语音服务
│   └── VoiceState.kt               # 通话状态与 VoiceStateHolder
└── ui/screens/
    ├── HomeScreen.kt               # 首页入口
    ├── CallScreen.kt               # 通话界面
    └── CallViewModel.kt            # 通话 ViewModel
```

## MVP 真机测试流程

1. 在两台手机上分别安装并启动 App。
2. 进入 **Wi-Fi Direct Test**：
   - 设备 A 点击 **Create Group** 成为 Group Owner。
   - 设备 B 点击 **Discover** → **List Peers**，找到设备 A 后点击 **Connect**。
   - 在设备 A 的系统弹窗中点击**接受**。
3. 返回首页，进入 **Start Direct Call**，两台设备分别点击 **Start Call**。
4. 等待界面显示 `Peer: 192.168.49.x` 后进行双向通话。

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

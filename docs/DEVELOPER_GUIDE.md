# OffGrid 开发者指南

> 目标：让新贡献者能够独立完成环境搭建、编译、安装与基础调试。

---

## 1. 开发环境

### 1.1 必备工具

| 工具 | 版本 | 说明 |
|------|------|------|
| OpenJDK | 17 | 项目使用 Java 17 语法与 Gradle 8.x |
| Gradle | 8.2 | 通过 `gradlew` 自动下载，无需手动安装 |
| Android SDK | API 34 | `compileSdk=34`，`targetSdk=34`，`minSdk=31` |
| Android Studio | 最新稳定版 | 推荐，用于 UI 预览与真机调试 |
| Git | 任意 | 代码版本控制 |

### 1.2 环境变量

```bash
export ANDROID_SDK_ROOT=/home/fei/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
```

确保 `adb` 可用：

```bash
adb version
```

---

## 2. 克隆与构建

### 2.1 克隆仓库

```bash
git clone git@github.com:iiweaa/OffGrid.git
cd OffGrid
```

### 2.2 首次构建

```bash
./gradlew clean build
```

构建产物：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/app-release-unsigned.apk`

### 2.3 安装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. 项目结构

```
OffGrid/
├── app/src/main/java/com/offgrid/app/
│   ├── MainActivity.kt              # 应用入口 + 底部导航
│   ├── OffGridApplication.kt        # Application 类
│   ├── WifiDirectTestActivity.kt    # Direct Connection Test 工具页
│   ├── OpusLatencyTestActivity.kt   # Opus 延迟测试页
│   ├── audio/                       # AudioEngine、AudioRouter、MediaButtonHandler
│   ├── link/                        # LinkManager、CapabilityStateHolder、并发能力检测
│   ├── link/wifidirect/             # NetworkRole / NetworkConfig / WifiDirectConnector
│   ├── link/location/               # 位置获取与 Haversine 计算
│   ├── link/neighbor/               # 邻居表与老化逻辑
│   ├── link/node/                   # NodeId 生成与持久化
│   ├── link/packet/                 # Mesh 包格式与序列化
│   ├── link/signal/                 # HELLO 心跳与信令
│   ├── power/                       # PowerSavingConfig
│   ├── service/                     # VoiceService、VoiceState、KeepAliveHelper
│   ├── ui/screens/                  # Home / Call / Peers / Settings / Onboarding
│   ├── ui/theme/                    # Theme、Color、Type、ThemePreference
│   └── util/                        # 工具类（如 BatteryOptimizationHelper）
├── app/src/main/res/                # 布局、字符串、主题资源
├── docs/                            # 项目文档（PRD、架构、设计、测试方案等）
└── .skill/                          # Professional Skill 任务与规则
```

---

## 4. 架构概述

### 4.1 技术栈

- **UI**：Jetpack Compose + Material3
- **架构**：MVVM（Screen + ViewModel + StateHolder）
- **状态管理**：`VoiceStateHolder`、`CapabilityStateHolder` 等 StateFlow
- **网络**：Wi-Fi Direct + UDP Socket（端口 4242）
- **音频**：Opus 编解码（`com.github.martoreto:opuscodec`）
- **定位**：Android LocationManager（不依赖 Google Play Services）

### 4.2 通话数据流

```
麦克风 → AudioRecord → OpusEncoder → LinkManager → UDP → 对端
对端 UDP → LinkManager → OpusDecoder → AudioTrack → 扬声器
```

### 4.3 关键状态机

- **VoiceService**：Idle → Foreground → Audio/Location/Link 启动 → Running
- **WifiDirectConnector**：Idle → Connecting → GroupCreated / Connected → Failed
- **SignalingEngine**：定时发送 HELLO，维护邻居表与老化

---

## 5. 本地调试

### 5.1 日志过滤

```bash
adb logcat -s VoiceService:D AudioEngine:D LinkManager:D WifiDirectConnector:D LocationEngine:D
```

### 5.2 常用 Gradle 任务

```bash
./gradlew assembleDebug          # 只打 debug 包
./gradlew test                   # 运行单元测试
./gradlew lint                   # 运行 lint
./gradlew installDebug           # 构建并安装 debug 包
```

### 5.3 Wi-Fi Direct 调试

- 使用 **Direct Connection Test** 页面手动创建 Group / 发现 Peer / 连接；
- 若两台手机无法互通，先确认：
  1. 位置权限与附近设备权限已授予；
  2. 仅一台作为 Group Owner；
  3. 两台设备连接同一 Wi-Fi Direct Group 后处于 `192.168.49.x` 子网。

---

## 6. 测试

### 6.1 单元测试

```bash
./gradlew test
```

### 6.2 真机测试

| 测试项 | 文档 |
|--------|------|
| M4-T5 手动网络配置三种模式 | `docs/M4-T5_MANUAL_NETWORK_CONFIG.md` 第 7 节 |
| M4-T6 省电模式 15 分钟功耗对比 | `docs/M4-T6_POWER_SAVING.md` 第 4.5 节（当前按项目决策暂不执行，脚本保留） |
| M3 Alpha 集成测试 | `docs/M3_ALPHA_TEST.md` |

---

## 7. 常见构建问题

| 现象 | 原因 | 解决 |
|------|------|------|
| `compileSdkVersion 34` 找不到 | SDK 未安装 | 安装 `platforms;android-34` 与 `build-tools;34.0.0` |
| Opus native lib 加载失败 | 缺少对应 ABI | 当前目标设备需 `arm64-v8a`；模拟器需对应 x86_64 镜像 |
| lint 报错 | 新增字符串未提取 | 放入 `app/src/main/res/values/strings.xml` |

---

## 8. 贡献流程

1. 从 `main` 切出功能分支；
2. 开发完成后本地执行 `./gradlew clean build`；
3. 通过真机或模拟器验证功能；
4. 整理变更文件清单，交由 `pro-general-git` 提交；
5. 更新 `docs/PROGRESS_TRACKING.md` 与 `.skill/TASKS.md`。

---

## 9. 相关文档

- `docs/PROJECT_PLAN.md` — 项目计划与里程碑
- `docs/ARCHITECTURE.md` — 系统架构
- `docs/DESIGN_SYSTEM.md` — UI 设计系统
- `docs/PRD.md` — 产品需求文档
- `docs/PROGRESS_TRACKING.md` — 任务进度

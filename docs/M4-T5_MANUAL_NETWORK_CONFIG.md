# M4-T5 手动网络配置入口与故障排查提示

> 任务文档 v1.0
> 负责人：pro-android-app
> 依赖：M3-T3（LinkManager/NeighborTable）、M4-T4-UI（设计系统）
> 工期：4d

---

## 1. 背景

当前主流程（Call → VoiceService）默认假设 Wi-Fi Direct Group 已经由用户通过 **Direct Connection Test** 手动建立。普通用户很难理解：

- 为什么点击 Start Call 后找不到对端？
- 该由哪台手机创建 Group？
- 连接失败时应该检查什么？

M4-T5 需要在主应用内提供可理解的网络配置入口，并在连接失败时给出明确提示。

---

## 2. 目标

- 用户在 Settings 中可选择 Wi-Fi Direct 角色：**Auto / Group Owner / Client**。
- 点击 Start Call 时，应用根据角色自动完成 Group 创建或连接。
- 出现连接失败、无对端、权限不足等情况时，给出可操作的故障排查提示。
- 在一加 11 与华为 Mate 30 Pro 5G 上验证 Auto / GO / Client 三种模式均可通话。

---

## 3. 范围

### 3.1 In Scope

- Settings 中新增 **Connection** 配置区。
- `NetworkConfig` 数据类与本地持久化（SharedPreferences / DataStore）。
- `WifiDirectConnector`：封装 Wi-Fi Direct Group 创建、发现、连接、状态监听。
- VoiceService / Call 流程接入：启动通话前先完成组网。
- CallScreen 状态展示：组网中 / 等待对端 / 已连接 / 失败 + 提示。
- 故障排查提示组件（SnackBar / Dialog / 内嵌提示）。

### 3.2 Out of Scope

- 多跳中继自动路由（M3 已标记为硬件受限，仅保留扩展结构）。
- 自动选择最佳 GO 的复杂算法（M4-T5 仅做简单发现-连接）。
- 持续后台自动重连的完整策略（可在 M4-T6 或 M5 深化）。

---

## 4. 功能需求

### 4.1 角色配置

| 角色 | 行为 |
|------|------|
| **Auto（默认）** | 启动通话时先扫描 5s；若发现已有 GO，则以 Client 连接；否则本机创建 Group 成为 GO。 |
| **Group Owner** | 启动通话时直接创建 Group，并等待 Client 加入。 |
| **Client** | 启动通话时扫描并连接第一个可用的 GO；若未发现，提示用户检查对端是否已建组。 |

### 4.2 Settings UI

在 `SettingsScreen` 新增 **Connection** 卡片：

```
┌─────────────────────────────┐
│  Connection                 │
│  ┌─────────────────────┐    │
│  │ Role                │    │
│  │ [Auto ▼]            │    │  ExposedDropdownMenuBox
│  └─────────────────────┘    │
│                             │
│  [GO Config]  (conditional) │
│  ├─ Group Name: DIRECT-...  │
│  └─ Passphrase: ******      │
│                             │
│  [Troubleshooting tips]     │
│  当前机型不支持 AP-STA 并发，│
│  只能一台作为 GO。          │
└─────────────────────────────┘
```

- **Role** 下拉框：Auto / Group Owner / Client。
- **GO Config** 仅在 Role = Group Owner 时显示，允许用户自定义 Group Name 与 Passphrase；若留空则使用系统默认。
- **提示文案**：
  - 目标机型不支持 AP-STA 并发时，显示「当前设备不支持 Wi-Fi Direct AP-STA 并发，两台手机只能一台作为 Group Owner」。
  - 该提示通过 `WifiDirectCapabilityChecker` 动态判断。

### 4.3 通话流程接入

CallScreen 点击 **Start Call** 后的状态机：

```
Idle
  ↓ Start Call
Connecting Network
  ↓ group formed & peer found
Connected
  ↓ start VoiceService
In Call
```

- `CallViewModel` 新增 `networkState`（Idle / Connecting / Connected / Failed）。
- `VoiceService` 启动前，先调用 `WifiDirectConnector.establishGroup()`。
- 组网成功后，再启动 `audioEngine` / `linkManager` 等原有逻辑。
- 组网失败时，更新 `VoiceState.lastError` 并停止。

### 4.4 故障排查提示

出现以下场景时，在 CallScreen 显示提示：

| 场景 | 提示文案 |
|------|----------|
| Wi-Fi Direct 未启用 | 请前往系统设置开启 Wi-Fi Direct。 |
| 权限被拒绝 | 需要位置与附近设备权限才能发现 peers。 |
| 未发现任何 GO（Client 模式） | 未找到 Group Owner，请确认对端已选择「Group Owner」并点击 Start Call。 |
| 创建 Group 失败 | 创建 Group 失败，请尝试切换为 Client 模式连接对端。 |
| 当前机型不支持并发 | 当前机型不支持 AP-STA 并发，请确保只有一台手机作为 Group Owner。 |
| 对端未连接 | 已创建 Group，等待对端加入… |

提示使用 Material3 `Snackbar` 或 Card 内嵌错误提示，颜色按 `error` / `secondary` token。

---

## 5. UI/UX 规范

- 全部使用 `docs/DESIGN_SYSTEM.md` 的 design token。
- 设置页卡片：`surfaceVariant`，`16dp` 圆角，`16dp` 内边距。
- 下拉框：`ExposedDropdownMenuBox`，与主题切换组件风格一致。
- 提示文字：`bodyMedium`，`onSurfaceVariant`；错误提示用 `error` 色。
- CallScreen 增加网络状态 Chip：
  - Connecting → `secondaryContainer`
  - Connected → `primaryContainer`
  - Failed → `errorContainer`

---

## 6. 建议架构改动

### 6.1 新增文件

- `app/src/main/java/com/offgrid/app/link/wifidirect/NetworkRole.kt`
  - 枚举：`AUTO`, `GROUP_OWNER`, `CLIENT`
- `app/src/main/java/com/offgrid/app/link/wifidirect/NetworkConfig.kt`
  - 数据类 + 持久化读写
- `app/src/main/java/com/offgrid/app/link/wifidirect/WifiDirectConnector.kt`
  - 封装 `WifiP2pManager` 的 createGroup / discoverPeers / connect / removeGroup
  - 返回状态：Connecting / GroupCreated / ConnectedToGroup / Failed(reason)
- `app/src/main/java/com/offgrid/app/link/wifidirect/NetworkError.kt`
  - 错误类型枚举与提示文案映射

### 6.2 修改文件

- `app/src/main/java/com/offgrid/app/ui/screens/SettingsScreen.kt`
  - 新增 Connection 配置区
- `app/src/main/java/com/offgrid/app/ui/screens/CallScreen.kt`
  - 显示网络状态与提示
- `app/src/main/java/com/offgrid/app/ui/screens/CallViewModel.kt`
  - 管理组网状态机，先组网再启动 VoiceService
- `app/src/main/java/com/offgrid/app/service/VoiceService.kt`
  - 可选：接收通过 Intent 传入的 "network ready" 信号，或仍由 ViewModel 控制
- `app/src/main/res/values/strings.xml`
  - 新增所有用户可见文案

### 6.3 可复用代码

- `WifiDirectTestActivity.kt` 中的 `createGroup`、`discoverPeers`、`connectTo` 逻辑可直接抽取到 `WifiDirectConnector.kt`。

---

## 7. 验收标准

- [ ] Settings 中可切换 Auto / Group Owner / Client，且持久化。
- [ ] 选择 Group Owner 时显示 Group Name / Passphrase 输入项。
- [ ] 目标机型不支持 AP-STA 并发时显示提示。
- [ ] 点击 Start Call 后，CallScreen 显示网络连接状态。
- [ ] Group Owner 模式下，对端 Client 加入后通话正常。
- [ ] Client 模式下，可发现并连接对端 Group Owner 后通话正常。
- [ ] Auto 模式下，两台手机按先到先得原则一方为 GO、一方为 Client，通话正常。
- [ ] 连接失败时显示对应排查提示，不崩溃。
- [ ] `./gradlew clean build` 通过，lint 无新增错误。
- [ ] 一加 11 / 华为 Mate 30 Pro 5G 真机验证通过。

---

## 8. 测试计划

| 用例 | 步骤 | 预期 |
|------|------|------|
| TC-1 GO 模式 | A 选 GO 点击 Start Call，B 选 Client 点击 Start Call | A 与 B 通话双向正常 |
| TC-2 Client 模式 | B 先 GO，A 后 Client | 同 TC-1 |
| TC-3 Auto 模式 | A、B 均选 Auto 同时点击 Start Call | 一方自动成 GO，另一方自动连接 |
| TC-4 无对端 | A 选 Client，B 未启动 | A 显示「未找到 Group Owner」提示 |
| TC-5 权限拒绝 | 撤销位置权限后点击 Start Call | 显示权限提示并引导跳转设置 |
| TC-6 切换角色 | 在 Settings 中切换角色后重新进入 Call | 新角色生效 |

---

## 9. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 目标机型 Wi-Fi Direct API 行为差异 | 高 | 在一加/华为两台设备上分别验证；保留手动模式兜底。 |
| Auto 模式两台设备同时创建 Group 导致无法互通 | 中 | 增加随机退避或让用户手动选择角色；提示不支持并发机型建议手动。 |
| 组网耗时过长导致用户以为卡死 | 中 | 明确显示「连接中…」与倒计时，超时后给出失败提示。 |
| 后台启动 VoiceService 时无法弹出 Wi-Fi Direct 提示 | 低 | 组网逻辑放在 CallScreen / ViewModel 中，Service 只负责已组网后的保活。 |

---

## 10. 任务拆分建议

| 子任务 | 工时 | 说明 |
|--------|------|------|
| T1 数据层：NetworkRole / NetworkConfig / 持久化 | 0.5d | 先定义 enum 与 SharedPreferences 读写 |
| T2 Settings UI：Connection 配置区 | 1d | 按设计系统实现卡片与下拉框 |
| T3 WifiDirectConnector 封装 | 1.5d | 抽取并抽象 WifiDirectTestActivity 逻辑 |
| T4 Call 流程接入与状态展示 | 1d | ViewModel 状态机 + CallScreen UI |
| T5 故障排查提示与文案 | 0.5d | 错误映射 + Snackbar / Card 提示 |
| T6 真机验证与修复 | 1d | 一加 + 华为双机验证 |

> 总工时约 5.5d，按 4d 排期需压缩 UI/验证或并行推进。

---

## 11. 移交清单

移交 `pro-android-app` 时需提供：

- 本任务文档：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`
- 设计规范：`docs/DESIGN_SYSTEM.md`、`docs/UI_DESIGNS.md`
- 参考实现：`app/src/main/java/com/offgrid/app/WifiDirectTestActivity.kt`
- 核心接入点：`app/src/main/java/com/offgrid/app/ui/screens/CallViewModel.kt`、`VoiceService.kt`

代码开发完成后，移交 `pro-general-git` 提交，并更新 `docs/PROGRESS_TRACKING.md` 中 M4-T5 状态为「已完成」。

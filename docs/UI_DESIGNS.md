# OffGrid UI 高保真设计稿

> 设计稿文档 v1.0
> 阶段：M3.5 UI/UX 设计冲刺

---

## 1. 引导页（Onboarding）

### 1.1 权限引导

```
┌─────────────────────────────┐
│                             │
│     [Mic Icon Large]        │
│                             │
│   需要麦克风权限            │
│   用于离线语音通话          │
│                             │
│   [  授予麦克风权限  ]      │
│                             │
│   1 / 3                     │
└─────────────────────────────┘
```

- 标题：`headlineLarge`，居中。
- 说明：`bodyLarge`，居中，灰色。
- 按钮：Primary Button，底部居中。
- 步骤指示器：小圆点，当前步骤高亮。

### 1.2 Wi-Fi Direct 引导

```
┌─────────────────────────────┐
│                             │
│     [Wifi Icon Large]       │
│                             │
│   使用 Wi-Fi Direct 组网    │
│   一台手机当热点，其余接入  │
│                             │
│   [    下一步    ]          │
│                             │
└─────────────────────────────┘
```

### 1.3 电池白名单引导

```
┌─────────────────────────────┐
│                             │
│   [Battery Icon Large]      │
│                             │
│   保持后台运行              │
│   锁屏后仍能接收语音与位置  │
│                             │
│   [  加入电池白名单  ]      │
│                             │
└─────────────────────────────┘
```

---

## 2. 首页（Home）

### 布局

```
┌─────────────────────────────┐
│  OffGrid                    │ headlineLarge
│  离线语音通讯                │ bodyLarge
├─────────────────────────────┤
│  [Capability Status Card]   │
├─────────────────────────────┤
│  [Battery Whitelist Card]   │ (conditional)
├─────────────────────────────┤
│                             │
│  [  Start Direct Call  ]    │ Primary Button
│                             │
│  [  Peer Compass  ]         │ Secondary Button
│                             │
│  [  Settings  ]             │ Secondary Button
│                             │
├─────────────────────────────┤
│  开发者工具                  │ titleMedium
│  [Wi-Fi Direct Test]        │ TextButton
│  [Opus Latency Test]        │ TextButton
└─────────────────────────────┘
```

### 能力状态卡

- 已选择 Group Owner：`primaryContainer` 背景，绿色图标 + 「角色：热点」。
- 已选择 Client：`primaryContainer` 背景，蓝色图标 + 「角色：接入」。
- 未选择角色 / 未知：`secondaryContainer` 背景，提示「请在设置中选择组网角色」。

---

## 3. 通话页（Call）

### 布局

```
┌─────────────────────────────┐
│  ← Direct Call              │
├─────────────────────────────┤
│                             │
│        [Mic Icon]           │
│        通话中                │ headlineMedium
│                             │
│  Peer: 192.168.49.xxx       │ bodyMedium
│  Neighbors: 1               │ bodyMedium
│                             │
│  [  End Call  ]             │ error background
│                             │
├─────────────────────────────┤
│  Neighbors (1)              │ titleMedium
│  ┌─────────────────────┐    │
│  │ ● A1B2C3D4 @ ...    │    │ Neighbor chip
│  └─────────────────────┘    │
├─────────────────────────────┤
│  Location                   │ titleMedium
│  My: 39.90, 116.40 (±3m)    │ bodyMedium
│  Peer locations: 1          │ bodyMedium
└─────────────────────────────┘
```

### 状态

- 未通话：标题「Ready」，按钮为 Primary「Start Call」。
- 通话中：大图标 + 通话中文字，按钮为红色「End Call」。
- 音频引擎失败：底部红色错误文字。

---

## 4. 队友罗盘页（Peer Compass）

### 布局

```
┌─────────────────────────────┐
│  ← Peer Compass             │
├─────────────────────────────┤
│                             │
│       [Compass Canvas]      │
│      (320dp × 320dp)        │
│                             │
│          N                  │
│     W  ●─────→  E           │
│          S                  │
│                             │
├─────────────────────────────┤
│  Peer List (1)              │ titleMedium
│  ┌─────────────────────┐    │
│  │ A1B2C3D4 — 25 m,    │    │
│  │ 087°                │    │
│  └─────────────────────┘    │
└─────────────────────────────┘
```

### 罗盘元素

- 外圈：灰色圆环，N/E/S/W 标签在圆环外侧。
- 中心：本机位置点，primary 色。
- 队友标记：
  - 从中心到标记的 secondary 色连线。
  - 标记点为 secondary 色圆点。
  - 标记下方显示短 NodeId + 距离。

### 空态

- 无 GPS：`「Waiting for GPS fix...」`。
- 无队友：`「No peer locations received yet」`。

---

## 5. 设置页（Settings）

### 布局

```
┌─────────────────────────────┐
│  Settings                   │ headlineLarge
├─────────────────────────────┤
│  Permissions                │ titleMedium
│  ├─ 麦克风      [ granted ] │
│  ├─ 位置        [ granted ] │
│  └─ 附近设备    [ granted ] │
├─────────────────────────────┤
│  Keep Alive                 │ titleMedium
│  ├─ 电池白名单  [ open ]    │
│  └─ 锁屏保活说明             │ bodyMedium
├─────────────────────────────┤
│  Audio                      │ titleMedium
│  ├─ 音频输出    [ Speaker ] │ (future)
│  └─ 通话音量    [ max ]     │ (future)
├─────────────────────────────┤
│  Developer Tools            │ titleMedium
│  ├─ Wi-Fi Direct Test       │
│  └─ Opus Latency Test       │
├─────────────────────────────┤
│  About                      │ titleMedium
│  ├─ Version 0.1.0           │
│  └─ Open Source Licenses    │
└─────────────────────────────┘
```

### 列表项

- 左侧图标 + 标题，`titleMedium`。
- 右侧状态或箭头，`bodyMedium`，`onSurfaceVariant`。
- 分隔线：1dp，`surfaceVariant`。

---

## 6. 响应式与适配

- 小屏手机（<360dp）：罗盘尺寸降至 `280dp`，页面内边距降至 `16dp`。
- 大屏手机/折叠屏：内容居中，最大宽度 `600dp`。
- 横屏：Call 页与 Compass 页采用左右分栏（未来优化）。

---

## 7. 关键交互说明

| 页面 | 交互 | 反馈 |
|------|------|------|
| Home | 点击 Start Direct Call | 跳转 Call 页，若未授权则先请求权限 |
| Home | 点击 Peer Compass | 跳转 Peer Compass |
| Home | 点击电池白名单卡片 | 跳转系统设置 |
| Call | 点击 Start Call | 启动 VoiceService，按钮变为 End Call |
| Call | 点击 End Call | 停止 VoiceService，返回 Ready 态 |
| Peer Compass | 自动刷新 | 收到新位置包时更新标记与列表 |
| Settings | 点击权限项 | 跳转系统应用权限设置 |

---

## 8. 交付物清单

- [x] 信息架构：`docs/INFO_ARCHITECTURE.md`
- [x] 设计系统：`docs/DESIGN_SYSTEM.md`
- [x] 高保真设计稿：`docs/UI_DESIGNS.md`
- [ ] Figma/Sketch 源文件（M4 可由设计师补充）
- [ ] 切图与图标资源（M4 补充）

---

## 9. Direct Connection Test 页（直连测试，M4-T4-UI 重构）

> 适用范围：`WifiDirectTestActivity`（界面向普通用户展示为 **Direct Connection Test / 直连测试**）
> 目标：将原本信息堆叠的测试页重构为信息分层、操作分区、视觉简洁的开发者工具页。

### 9.1 设计原则

- **一页一意图**：顶部看状态，中部操作，下部看结果。
- **状态可视化**：用卡片+颜色+文字共同表达 Wi-Fi Direct 与当前组网角色状态。
- **操作可预测**：按钮按功能分组，禁用状态给出明确原因（或置灰 + 辅助说明）。
- **日志可折叠**：默认收起，避免开发者工具页一屏信息过载。

### 9.2 布局

```
┌─────────────────────────────────────┐
│  ← Direct Connection Test           │  TopAppBar (titleLarge)
├─────────────────────────────────────┤
│  Status                             │  titleMedium
│  ┌─────────────────────────────┐    │
│  │  Wi-Fi Direct    Enabled    │    │  bodyMedium
│  │  Group           Idle       │    │
│  │  Role            Unknown    │    │
│  └─────────────────────────────┘    │  surfaceVariant, 16dp radius
├─────────────────────────────────────┤
│  Actions                            │  titleMedium
│  ┌─────────────────────────────┐    │
│  │  [   Create Group   ]       │    │  Primary Button
│  │  [   Remove Group   ]       │    │  Outlined Button
│  │                             │    │
│  │  [Discover]  [List Peers]   │    │  OutlinedButton Row
│  │                             │    │
│  └─────────────────────────────┘    │  surfaceVariant, 16dp radius
├─────────────────────────────────────┤
│  Group Info                         │  titleMedium (conditional)
│  ┌─────────────────────────────┐    │
│  │  SSID:    DIRECT-OffGrid-XX │    │
│  │  Pass:    xxxxxxxx          │    │
│  │  Role:    Group Owner       │    │
│  └─────────────────────────────┘    │  surfaceVariant, 16dp radius
├─────────────────────────────────────┤
│  Devices (0)                        │  titleMedium
│  ┌─────────────────────────────┐    │
│  │  [Phone] OnePlus 11         │    │  ListItem
│  │  a1:b2:c3:d4:e5:f6   [Conn]│    │  trailing Connect Button
│  └─────────────────────────────┘    │  surfaceVariant, 16dp radius
├─────────────────────────────────────┤
│  Logs  [expand ▼]                   │  titleMedium + trailing icon
│  ┌─────────────────────────────┐    │
│  │ 21:34:12  P2P enabled       │    │  bodySmall, maxHeight 200dp
│  │ 21:34:45  Group created     │    │  scrollable
│  └─────────────────────────────┘    │  surfaceVariant, 16dp radius
└─────────────────────────────────────┘
```

### 9.3 信息分层与组件规范

#### 顶部状态卡（Status Card）

- **背景**：`surfaceVariant`（`#E0E0E0`）。
- **圆角**：`16dp`。
- **内边距**：`16dp`。
- **行布局**：左侧固定宽度标签（`onSurfaceVariant`），右侧状态值（`bodyLarge`）。
- **状态值颜色**：
  - `Enabled` / `Group Owner` / `Client` → `primary`
  - `Disabled` / `Failed` → `error`
  - `Idle` / `Unknown` / `Scanning` → `onSurfaceVariant`
- **辅助图标**：状态行前可增加 24dp 图标（可选），避免纯颜色表达状态。

#### 操作卡（Actions Card）

- **背景**：`surfaceVariant`，`16dp` 圆角，`16dp` 内边距。
- **按钮分组**：
  1. 组管理：`Create Group`（Primary）在上，`Remove Group`（Outlined）在下。
  2. 发现：`Discover` 与 `List Peers` 并排，各占一半宽度。
- **禁用规则**：
  - `Create Group` 在已有 Group 时建议禁用或提示先 Remove。
- **加载/进行态**：异步操作按钮显示为禁用并替换文字为「Creating…」「Scanning…」「Connecting…」。

#### Group Info 卡（条件显示）

- 仅在 `groupFormed == true` 时显示。
- 展示 `SSID`、`Passphrase`、`Role`。
- `Passphrase` 右侧可增加小型复制图标按钮（`Icons.Default.ContentCopy`），点击后 Snackbar 提示「已复制」。

#### 设备列表卡（Devices Card）

- **背景**：`surfaceVariant`，`16dp` 圆角。
- **空态**：列表为空时显示居中 `Icons.Default.Devices` + `bodyLarge`「No devices discovered yet」+ `bodyMedium`「Tap Discover to scan」。
- **列表项**：使用 Material3 `ListItem`。
  - **Leading**：`Icons.Default.Smartphone`（或按设备类型区分，默认手机图标）。
  - **Headline**：`deviceName`，`bodyLarge`。
  - **Supporting**：`deviceAddress` + 设备状态（Available / Invited / Connected / Failed），`bodyMedium`，`onSurfaceVariant`。
  - **Trailing**：`Connect` OutlinedButton；已连接/邀请中时显示状态 Chip 并禁用按钮。
- **最小触控目标**：Connect 按钮满足 48dp。

#### 日志卡（Logs Card）

- **默认折叠**：仅显示标题行 + 最近 2 条日志预览（或仅显示条数）。
- **展开**：点击标题行右侧图标切换，展开后高度最大 `200dp`，内部垂直滚动。
- **时间格式**：`HH:mm:ss`，`bodySmall`，`onSurfaceVariant`。
- **自动滚动**：展开时若新增日志，列表自动滚动到底部。

### 9.4 交互说明

| 操作 | 触发 | 反馈 |
|------|------|------|
| Create Group | 点击按钮 | 按钮变为「Creating…」并禁用；成功后 Status 卡更新为 Group Owner；Logs 新增记录 |
| Remove Group | 点击按钮 | 按钮变为「Removing…」；成功后 Group Info 卡隐藏，Status 回到 Idle |
| Discover | 点击按钮 | 按钮变为「Scanning…」并禁用；扫描期间设备列表可继续显示上次结果 |
| List Peers | 点击按钮 | 立即刷新设备列表；无设备时显示空态 |
| Connect | 点击设备行 Connect | 按钮变为「Connecting…」；连接成功后该设备行显示 Connected 状态 |
| Probe Concurrency | 点击按钮 | 仅 Client 状态下可用；探测完成后 Status 卡 Multi-hop 状态更新 |
| Logs 标题 | 点击展开/收起 | 图标旋转 180°，内容区展开/收起 |
| Group Info Passphrase | 点击复制图标 | Snackbar：「Passphrase copied」 |

### 9.5 设计 Token 使用

| 元素 | Token | 值 |
|------|-------|-----|
| 页面背景 | `background` | `#FFFFFF`（Light）/ `#1C1B1F`（Dark） |
| 卡片背景 | `surfaceVariant` | `#E0E0E0` / `#49454F` |
| 主要操作按钮 | `primary` / `onPrimary` | `#2E7D32` / `#FFFFFF` |
| 次要/描边按钮 | `outline` / `primary` | `#79747E` / `#2E7D32` |
| 成功状态文字 | `primary` | `#2E7D32` |
| 错误/不可用状态 | `error` | `#B00020` |
| 次级文字 | `onSurfaceVariant` | `#49454F` / `#CAC4D0` |
| 页面水平内边距 | `spaceL` | `24dp` |
| 卡片内边距 | `spaceM` | `16dp` |
| 卡片间距 | `spaceM` | `16dp` |
| 卡片圆角 | - | `16dp` |
| 按钮圆角 | - | `12dp` |

### 9.6 可用性检查

- [ ] 状态不仅依赖颜色，也使用文字标签。
- [ ] 所有按钮具备 `contentDescription`。
- [ ] 触控目标 ≥ 48dp。
- [ ] 日志默认折叠，减少首屏信息密度。
- [ ] 深色模式下卡片/按钮/文字对比度 ≥ 4.5:1。


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
│   无需路由器，两台手机直连  │
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
│  离线网状语音通讯            │ bodyLarge
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

- 支持多跳：`primaryContainer` 背景，绿色图标 + 「多跳：可用」。
- 不支持/未知：`secondaryContainer` 或 `errorContainer` 背景，提示「多跳：未知/不支持」。

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

# OffGrid 设计系统

> 设计系统文档 v1.0
> 阶段：M3.5 UI/UX 设计冲刺

---

## 1. 设计原则

- **户外优先**：高对比度、大触控区域、清晰的状态反馈。
- **离线信任感**：绿色/大地色系，传递自然、安全、去中心化。
- **最小干扰**：减少不必要装饰，核心功能一键触达。
- **状态即界面**：通过颜色与图标即时表达网络、通话、电量状态。

---

## 2. 色彩

### 主色板

| Token | 色值 | 用途 |
|-------|------|------|
| `primary` | `#2E7D32` | 主按钮、通话中状态、成功提示 |
| `onPrimary` | `#FFFFFF` | 主按钮文字 |
| `primaryContainer` | `#C8E6C9` | 支持多跳/正常状态卡片背景 |
| `secondary` | `#F9A825` | 队友标记、高亮、警告 |
| `onSecondary` | `#000000` |  secondary 按钮文字 |
| `secondaryContainer` | `#FFF8E1` | 能力未知/提示卡片背景 |
| `error` | `#B00020` | 错误、不支持、离线 |
| `errorContainer` | `#FFDAD6` | 错误/电池优化提示卡片背景 |
| `background` | `#FFFFFF` | 页面背景 |
| `surface` | `#F5F5F5` | 卡片背景 |
| `surfaceVariant` | `#E0E0E0` | 次级卡片、罗盘背景 |
| `onBackground` | `#1C1B1F` | 正文文字 |
| `onSurface` | `#1C1B1F` | 卡片文字 |
| `onSurfaceVariant` | `#49454F` | 次级文字 |

### 深色模式（预留）

深色模式在 M4 实现，主色保持不变，背景使用 `#1C1B1F`，surface 使用 `#313033`。

---

## 3. 字体

使用系统默认无衬线字体（Roboto/Noto Sans），遵循 Material 3 类型比例：

| Token | 字号 | 字重 | 用途 |
|-------|------|------|------|
| `displayLarge` | 57sp | 400 | 启动页大标题 |
| `headlineLarge` | 32sp | 400 | 页面标题 |
| `headlineMedium` | 28sp | 400 | 页面副标题 |
| `titleLarge` | 22sp | 500 | 卡片标题 |
| `titleMedium` | 16sp | 500 | 列表项标题 |
| `bodyLarge` | 16sp | 400 | 正文、按钮文字 |
| `bodyMedium` | 14sp | 400 | 次要说明 |
| `labelLarge` | 14sp | 500 | 按钮标签 |
| `labelMedium` | 12sp | 500 | 小标签、时间戳 |

---

## 4. 间距与布局

采用 4dp 网格：

| Token | 值 | 用途 |
|-------|-----|------|
| `spaceXs` | 4dp | 图标与文字间距 |
| `spaceS` | 8dp | 紧凑元素间距 |
| `spaceM` | 16dp | 卡片内边距、列表项间距 |
| `spaceL` | 24dp | 页面水平内边距 |
| `spaceXl` | 32dp | 大模块间距 |
| `space2Xl` | 48dp | 页面标题与内容间距 |

### 安全区域

- 页面水平内边距统一为 `24dp`。
- 底部导航栏高度 `80dp`（含图标、标签与内边距）。
- 按钮最小高度 `48dp`，最小宽度 `88dp`。

---

## 5. 组件

### 5.1 Primary Button

- 背景：`primary`
- 文字：`onPrimary`，`labelLarge`
- 圆角：`12dp`
- 最小高度：`48dp`
- 状态：按压时 alpha 0.8。

### 5.2 Secondary Button

- 背景：`secondaryContainer`
- 文字：`onSecondaryContainer`，`labelLarge`
- 圆角：`12dp`
- 最小高度：`48dp`

### 5.3 Status Card

- 背景：`surface`
- 圆角：`16dp`
- 内边距：`16dp`
- 标题：`titleMedium`
- 内容：`bodyMedium`

### 5.4 Neighbor Chip

- 背景：`primaryContainer`
- 圆角：`8dp`
- 图标 + NodeId 短码 + IP
- 字体：`labelMedium`

### 5.5 Compass Card

- 背景：`surfaceVariant`
- 圆角：`24dp`
- 内边距：`16dp`
- 中心点：`primary` 色圆点
- 队友标记：`secondary` 色圆点 + 连线

---

## 6. 图标

使用 Material Icons（Outlined/Filled）：

| 用途 | 图标 |
|------|------|
| Home | `Icons.Default.Home` |
| Call / Phone | `Icons.Default.Phone` |
| End Call | `Icons.Default.CallEnd` |
| Peers / Compass | `Icons.Default.Explore` |
| Settings | `Icons.Default.Settings` |
| Location | `Icons.Default.LocationOn` |
| Mic | `Icons.Default.Mic` |
| Volume / Speaker | `Icons.Default.VolumeUp` |
| Warning | `Icons.Default.Warning` |
| Wifi | `Icons.Default.Wifi` |
| People | `Icons.Default.People` |

---

## 7. 动画与动效

- 页面切换：默认 Material 滑动过渡。
- 按钮按压：缩放 0.98 + 背景变暗。
- 状态变化：卡片背景色过渡 200ms。
- 罗盘刷新：队友标记位置变化使用 300ms 线性插值（未来优化）。

---

## 8. 可访问性

- 所有可点击元素最小尺寸 `48dp × 48dp`。
- 文字与背景对比度 ≥ 4.5:1。
- 状态不仅依赖颜色，也依赖文字/图标说明。
- 支持系统字体大小调整。

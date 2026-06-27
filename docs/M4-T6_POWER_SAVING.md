# M4-T6 省电模式

> 任务文档 v1.0
> 负责人：pro-android-app
> 依赖：M4-T1（后台保活）、M4-T3（蓝牙耳机控制）
> 工期：3d

---

## 1. 背景

当前 OffGrid 在通话过程中持续保持高功耗：

- AudioEngine 始终以固定 Opus 码率编码；
- LocationEngine 以较高频率请求 GPS 更新；
- 屏幕常亮或频繁刷新 UI；
- 后台保活持有 WakeLock + WiFiLock。

在户外无充电场景下，续航是核心体验。M4-T6 需要引入可开关的「省电模式」，在牺牲部分音质/位置精度/界面刷新率的前提下，显著延长通话续航。

---

## 2. 目标

- 在 Settings 中新增 **Power saving mode** 开关，持久化到 SharedPreferences。
- 省电模式下，端到端通话功耗下降 ≥ 20%（相对普通模式，同场景 15 分钟测试）。
- 省电模式开启时，UI 与通知中给出明确提示。
- 在一加 11 / 华为 Mate 30 Pro 5G 上验证续航改善。

---

## 3. 范围

### 3.1 In Scope

- Settings 新增 **Power saving mode** 开关。
- `PowerSavingConfig` 数据类与本地持久化。
- AudioEngine 支持动态码率切换（省电模式使用更低码率/帧长）。
- LocationEngine 支持动态更新间隔切换（省电模式延长 GPS 间隔）。
- CallScreen / VoiceService 根据模式更新 UI 与通知提示。
- 15 分钟真机功耗对比测试方案。

### 3.2 Out of Scope

- 深度睡眠/息屏后完全关闭语音接收（属于 M5 户外实测后优化）；
- 硬件级调制解调器省电（超出应用层能力）；
- 自动根据电量触发省电模式（M4-T6 仅做手动开关）。

---

## 4. 功能需求

### 4.1 Settings 开关

在 Settings 的 **Connection** 或新增 **Power** 分区中放置开关：

```
┌─────────────────────────────┐
│  Power                       │
│  ┌─────────────────────┐    │
│  │ Power saving mode   │    │
│  │ [              ●  ] │    │  Switch
│  └─────────────────────┘    │
│  降低音质与定位频率以延长续航 │
└─────────────────────────────┘
```

- 默认关闭；
- 切换后立即生效（已运行通话则下一周期生效）。

### 4.2 AudioEngine 动态码率

当前 AudioEngine 使用固定 Opus 配置。需支持：

| 模式 | 目标码率 | 帧长 | 备注 |
|------|---------|------|------|
| 普通 | ~24 kbps | 20ms | 保持现有音质 |
| 省电 | ~12 kbps | 40ms | 降低码率，增加帧长，减少编码次数 |

- AudioEngine 提供 `setPowerSaving(enabled: Boolean)` 接口；
- 切换时安全重启 Opus 编码器（避免正在编码的帧丢失）。

### 4.3 LocationEngine 动态间隔

当前 LocationEngine 使用默认最小间隔。需支持：

| 模式 | GPS 更新间隔 | 位置广播间隔 |
|------|-------------|-------------|
| 普通 | 1s | 5s |
| 省电 | 5s | 15s |

- LocationEngine 提供 `setPowerSaving(enabled: Boolean)` 接口；
- 切换时重新请求位置更新。

### 4.4 VoiceService / CallScreen 提示

- VoiceService 通知内容在开启省电模式时追加「省电模式」提示；
- CallScreen 状态区显示 small icon / chip 提示当前处于省电模式；
- 切换开关后，若通话正在运行，给出 Snackbar 提示「省电模式已开启，将在下一秒生效」。

### 4.5 功耗测试

测试场景：

- 两台设备建立 Wi-Fi Direct 通话；
- 通话 15 分钟；
- 记录电量消耗百分比；
- 对比普通模式与省电模式各 3 次取平均。

验收：省电模式平均耗电下降 ≥ 20%。

---

## 5. UI/UX 规范

- 使用 `docs/DESIGN_SYSTEM.md` token；
- 开关使用 Material3 `Switch`；
- 提示文字使用 `bodySmall`，颜色 `onSurfaceVariant`。

---

## 6. 建议架构改动

### 6.1 新增/修改文件

- `app/src/main/java/com/offgrid/app/power/PowerSavingConfig.kt`
  - 开关持久化
- `app/src/main/java/com/offgrid/app/audio/AudioEngine.kt`
  - 动态码率/帧长
- `app/src/main/java/com/offgrid/app/link/location/LocationEngine.kt`
  - 动态更新间隔
- `app/src/main/java/com/offgrid/app/service/VoiceService.kt`
  - 读取开关并传递给 AudioEngine / LocationEngine
  - 通知文案更新
- `app/src/main/java/com/offgrid/app/ui/screens/SettingsScreen.kt`
  - 新增 Power saving mode 开关
- `app/src/main/java/com/offgrid/app/ui/screens/CallScreen.kt`
  - 显示省电模式提示
- `app/src/main/res/values/strings.xml`
  - 新增文案

### 6.2 持久化键值

- SharedPreferences name: `offgrid_power`
- Key: `power_saving_enabled`

---

## 7. 验收标准

- [ ] Settings 中可开关 Power saving mode，且持久化。
- [ ] 省电模式下 AudioEngine 实际码率下降。
- [ ] 省电模式下 LocationEngine 更新间隔延长。
- [ ] 通话中通知与 CallScreen 显示省电模式提示。
- [ ] `./gradlew clean build` 通过，lint 无新增错误。
- [ ] 一加 11 / 华为 Mate 30 Pro 5G 真机 15 分钟测试，省电模式耗电下降 ≥ 20%。

---

## 8. 任务拆分建议

| 子任务 | 工时 | 说明 |
|--------|------|------|
| T1 PowerSavingConfig + Settings UI | 0.5d | 开关持久化 + Settings 卡片 |
| T2 AudioEngine 动态码率 | 1d | 支持 12kbps/40ms，接口切换 |
| T3 LocationEngine 动态间隔 | 0.5d | GPS/广播间隔切换 |
| T4 VoiceService / CallScreen 提示 | 0.5d | 通知与 UI 提示 |
| T5 真机功耗测试 | 0.5d | 一加 + 华为双机对比 |

> 总工时约 3d。

---

## 9. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| Opus 动态切换导致爆音/丢帧 | 高 | 在帧边界安全重启编码器，测试验证 |
| 省电模式省电效果不达 20% | 高 | 增加屏幕亮度降低、降低采样率等额外措施 |
| 真机功耗测试环境不稳定 | 中 | 控制变量（亮度、后台应用、距离），多次取平均 |

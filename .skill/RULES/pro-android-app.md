# pro-android-app 项目级规则

> 本文件由 `pro-android-app` 维护，记录本项目 Android 开发特有的约束与快速入口。

---

## 项目约束

- `minSdk=31`，`targetSdk=34`，`compileSdk=34`
- Jetpack Compose BOM `2023.08.00`
- 默认使用 Material3 + 自定义 design token（见 `docs/DESIGN_SYSTEM.md`）
- 所有新页面遵循 MVVM 架构：`Screen` + `ViewModel` + `StateHolder`

## 当前任务

- **M4-T5**：手动网络配置入口与故障排查提示
  - 需求文档：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`
  - 核心接入点：`VoiceService.kt`、`CallViewModel.kt`、`SettingsScreen.kt`、`CallScreen.kt`
  - 参考实现：`WifiDirectTestActivity.kt`

## 强制检查

- 修改 UI 后必须真机或模拟器验证；
- 新增字符串必须加入 `app/src/main/res/values/strings.xml`；
- 完成前必须执行 `./gradlew clean build` 通过。

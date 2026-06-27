# pro-android-app 项目级规则

> 本文件由 `pro-android-app` 维护，记录本项目 Android 开发特有的约束与快速入口。

---

## 项目约束

- `minSdk=31`，`targetSdk=34`，`compileSdk=34`
- Jetpack Compose BOM `2023.08.00`
- 默认使用 Material3 + 自定义 design token（见 `docs/DESIGN_SYSTEM.md`）
- 所有新页面遵循 MVVM 架构：`Screen` + `ViewModel` + `StateHolder`

## 当前任务

- **M4-T5-FIX**：修复 Auto 模式 Group 断连死锁
  - 文档：`docs/BUG_M4-T5_AUTO_MODE_GROUP_DROP.md`
  - 目标：Auto 模式可稳定建组并恢复重连，TC-3 成功率 ≥ 2/3
  - 动作：修改 `WifiDirectConnector.kt`，增加连接丢失检测、取消 stale job、Auto 竞态缓解
  - 状态：代码完成，`./gradlew clean build` 通过，待 `pro-android-system-test` 复验

- **M4-T7**：完善开发文档
  - 文档：`docs/DEVELOPER_GUIDE.md`
  - 目标：新贡献者可独立构建
  - 动作：技术审阅、补充架构细节与调试技巧

## 历史参考

- M4-T5 已完成：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`，代码 `9d4a5ee`
- M4-T6 代码已完成：`docs/M4-T6_POWER_SAVING.md`，代码 `81cce23`

## 强制检查

- 修改 UI 后必须真机或模拟器验证；
- 新增字符串必须加入 `app/src/main/res/values/strings.xml`；
- 完成前必须执行 `./gradlew clean build` 通过。

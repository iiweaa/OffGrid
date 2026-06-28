# pro-android-app 项目级规则

> 本文件由 `pro-android-app` 维护，记录本项目 Android 开发特有的约束与快速入口。

---

## 项目约束

- `minSdk=31`，`targetSdk=34`，`compileSdk=34`
- Jetpack Compose BOM `2023.08.00`
- 默认使用 Material3 + 自定义 design token（见 `docs/DESIGN_SYSTEM.md`）
- 所有新页面遵循 MVVM 架构：`Screen` + `ViewModel` + `StateHolder`

## 当前任务

- **M5-T3-2**：arm64 Opus 原生库替换评估与实施（本周进行）
  - 文档：`docs/M5-T3_OPUS_REPLACEMENT.md`
  - 依赖：M5-T3-1
  - 目标：方案已输出，待 NDK 环境就绪后实施替换与真机验证

## 历史参考

- M4-T5 已完成：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`，代码 `9d4a5ee`
- M4-T6 省电模式：**已从产品需求中删除**，代码 `81cce23` 保留但不再维护

## 强制检查

- 修改 UI 后必须真机或模拟器验证；
- 新增字符串必须加入 `app/src/main/res/values/strings.xml`；
- 完成前必须执行 `./gradlew clean build` 通过。

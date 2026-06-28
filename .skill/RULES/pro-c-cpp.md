# pro-c-cpp 项目级规则

> 本文件由 `pro-general-pm` 维护，记录 OffGrid 项目中 Native / C / C++ 相关任务的约束与快速入口。
> 与共享规范冲突时，以本文件及最新版 `skills/pro-c-cpp/SKILL.md` 为准。

---

## 当前任务

- **M5-T3-2**：arm64 Opus 原生库替换评估与实施（本周进行）
  - 负责人：`pro-android-app` / `pro-c-cpp`
  - 文档：`docs/M5-T3_OPUS_REPLACEMENT.md`
  - 依赖：`M5-T3-1` 许可审查结论
  - 目标：方案已输出，待 NDK 环境就绪后实施 libopus JNI 封装与编译集成

---

## 项目约束

- Native 代码优先使用 Android NDK + CMake；
- 所有 Native 改动必须在 `arm64-v8a` 与 `armeabi-v7a` 上验证；
- 新增 JNI 接口需同步更新 `docs/ARCHITECTURE.md`；
- 完成前必须执行 `./gradlew clean build` 通过。

---

## 相关文档

- `docs/M5-T3_RELEASE_POLISH_PLAN.md`
- `docs/M1_OPUS_LATENCY_TEST.md`
- `docs/M2_VOICE_PIPELINE.md`
- `docs/ARCHITECTURE.md`
- `app/build.gradle.kts`

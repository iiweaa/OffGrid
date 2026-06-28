# M5-T3 发布前打磨计划

> 任务 ID：M5-T3  
> 负责人：`pro-general-pm` / `pro-android-app` / `pro-c-cpp`  
> 依赖：M4-T8（Beta 测试通过，无 P0/P1 缺陷）  
> 目标：在 v1.0 Release 前处理 1~2 项 P2 级已知问题，降低发布风险。

---

## 背景

当前 Beta 测试 **无 P0/P1 缺陷**，但 `docs/M1_OPUS_LATENCY_TEST.md` 与 `docs/M2_VOICE_PIPELINE.md` 记录了若干 P2/P3 级已知问题。考虑到 v1.0 发布后的社区传播与可维护性，优先处理以下两项：

1. **第三方 Opus 依赖许可合规**（高风险）。
2. **arm64 原生库缺失**（技术债务，影响长期兼容性与性能）。

---

## 范围

### 在范围内

- 审查 `com.github.martoreto:opuscodec:v1.2.1.2` 的许可证状态；
- 评估替换为自编译 libopus + JNI 或其他有明确许可证的封装库的可行性；
- 若替换方案可行，完成代码实现、真机验证与文档更新；
- 若保留现有依赖，形成书面风险结论并更新 `README.md` / `CONTRIBUTING.md`。

### 不在范围内

- 新增产品功能；
- 大规模架构重构；
- 户外实测（已取消）。

---

## 子任务拆分

### M5-T3-1：第三方 Opus 依赖许可合规审查

| 项目 | 内容 |
|------|------|
| 负责人 | `pro-general-pm`（主导）/ `pro-android-app`（配合确认代码依赖） |
| 目标 | 确认 `opuscodec` 的许可证是否允许在 MIT 项目中分发 |
| 输入 | `app/build.gradle.kts` 中的 `com.github.martoreto:opuscodec:v1.2.1.2` |
| 输出 | `docs/M5-T3_LICENSE_COMPLIANCE.md` |
| 关键检查项 | 1. 上游仓库是否包含 LICENSE 文件；<br>2. 是否有明确的 MIT / BSD / Apache-2.0 等兼容许可证；<br>3. 若许可证缺失，评估发布风险与替代方案；<br>4. 列出所有依赖的许可证清单（含 AndroidX、Compose、Opus 等）。 |
| 验收标准 | 文档明确给出「可保留 / 必须替换 / 建议替换」结论，并说明理由。 |

### M5-T3-2：arm64 原生库替换可行性评估与实施

| 项目 | 内容 |
|------|------|
| 负责人 | `pro-android-app` / `pro-c-cpp` |
| 目标 | 将 `opuscodec` 替换为自带 `arm64-v8a` 原生库的 Opus 封装方案，或证明现有方案足够 |
| 输入 | `docs/M1_OPUS_LATENCY_TEST.md`、`docs/M2_VOICE_PIPELINE.md`、`docs/M5-T3_LICENSE_COMPLIANCE.md` |
| 输出 | `docs/M5-T3_OPUS_REPLACEMENT.md`；若实施替换，输出代码改动与验证报告 |
| 候选方案 | 1. 自编译 libopus + JNI（推荐，完全可控）；<br>2. 使用其他有明确许可证的 Android Opus 封装库；<br>3. 保留 `opuscodec` 并仅在 32-bit 兼容模式下运行。 |
| 验收标准 | 若替换：一加 11 / 华为 Mate 30 Pro 5G 真机通话正常，`./gradlew clean build` 通过，APK 包含 `arm64-v8a` 原生库；若不替换：文档明确记录风险与理由。 |

### M5-T3-3（可选）：AudioTrack 延迟优化预研

| 项目 | 内容 |
|------|------|
| 负责人 | `pro-android-app` |
| 目标 | 评估 `AudioTrack` 偶发 100+ ms 阻塞的优化空间 |
| 输出 | `docs/M5-T3_AUDIOTRACK_LATENCY.md`（可选） |
| 说明 | 仅当 M5-T3-2 提前完成且有余力时启动；不阻塞 v1.0 发布。 |

---

## 决策树

```
M5-T3-1 许可审查
    ├── 结论：可保留 opuscodec
    │       └── 进入 M5-T3-2 评估 arm64 替换（可选）
    ├── 结论：建议替换
    │       └── M5-T3-2 必须输出替换方案并实施
    └── 结论：必须替换
            └── M5-T3-2 必须输出替换方案并实施
```

---

## 执行排期

| 阶段 | 时间 | 任务 | 输出 |
|------|------|------|------|
| Day 1 | 0.5d | M5-T3-1 许可审查 | `docs/M5-T3_LICENSE_COMPLIANCE.md` |
| Day 2~3 | 2d | M5-T3-2 方案评估与实现 | `docs/M5-T3_OPUS_REPLACEMENT.md` + 代码改动 |
| Day 4 | 0.5d | 真机验证与文档更新 | 验证记录、更新 README / DEVELOPER_GUIDE |
| Day 5 | 0.5d | PM 验收、任务状态归档 | M5-T3 标记为「已完成」 |

---

## 风险与应对

| 风险 | 概率 | 影响 | 应对策略 |
|------|------|------|----------|
| `opuscodec` 无明确许可证，发布存在法律风险 | 高 | 高 | 优先完成 M5-T3-1；若风险不可接受，必须实施替换 |
| 自编译 libopus + JNI 工作量超出预期 | 中 | 中 | 先出可行性评估文档，再决定是否实施；必要时仅做评估不实施 |
| 替换后真机通话质量下降 | 中 | 高 | 替换后必须执行 M4-T8 核心用例回归（BC-1 / BC-5 / BC-8） |
| M5-T3-2 延期阻塞 M5-T4 Release | 中 | 高 | 若替换不可行，保留原依赖 + 风险说明，不阻塞发布 |

---

## 验收标准

M5-T3 整体完成标准：

- [x] `docs/M5-T3_LICENSE_COMPLIANCE.md` 完成，结论明确；
- [x] `docs/M5-T3_OPUS_REPLACEMENT.md` 完成，方案决策明确；
- [x] 已实施替换，真机回归测试通过（BC-1 / BC-5 / BC-8），报告见 `docs/M5-T3_REGRESSION_REPORT.md`；
- [x] `./gradlew clean build` 通过；
- [x] 相关文档（README / BUILD.md / DEVELOPER_GUIDE / ARCHITECTURE）已同步；
- [x] 无新增 P0/P1 缺陷。

---

## 相关文档

- `docs/M1_OPUS_LATENCY_TEST.md`
- `docs/M2_VOICE_PIPELINE.md`
- `docs/M4-T8_BETA_TEST_REPORT.md`
- `docs/PROJECT_PLAN.md`
- `app/build.gradle.kts`

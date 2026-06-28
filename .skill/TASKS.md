# OffGrid 任务指派表

> 本文件由 `pro-general-pm` 维护，用于记录当前迭代中已指派给各专业 Skill 的任务。
> 任务状态：待办池 / 本周进行 / 等待 review / 已完成 / 已归档

---

## 当前迭代任务（v1.0 发布冲刺）

| ID | 任务名称 | 负责人 | 状态 | 截止时间 | 依赖 | 验收标准 | 备注 |
|----|----------|--------|------|----------|------|----------|------|
| **M4-T5** | **手动网络配置入口与故障排查提示** | **`pro-android-app`** | **已完成** | **2026-07-04** | **M3-T3** | **见 `docs/M4-T5_MANUAL_NETWORK_CONFIG.md` 第 7 节** | **代码已 push（`9d4a5ee`），M4-T5-VERIFY 全部通过** |
| **M4-T5-FIX** | **修复 Auto 模式 Group 断连死锁** | **`pro-android-app`** | **已完成** | **2026-07-05** | **M4-T5** | **按决策移除 Auto 模式，仅保留手动 GO/Client；GO 复用已有 Group 避免 BUSY** | Auto 模式因 `setDeviceName()` 失效及第三方 P2P 干扰无法稳定识别对端，已删除相关代码；GO 侧增加已有 Group 复用逻辑，TC-1 复测通过 |
| **M4-T5-VERIFY** | **M4-T5 真机网络验证** | **`pro-android-system-test`** | **已完成** | **2026-07-07** | **M4-T5, M4-T5-FIX** | **一加/华为 GO/Client 两种模式通话均正常，故障提示正确** | TC-1/TC-2/TC-3 全部通过，无 ANR/Crash；见 `docs/M4-T5_VERIFICATION.md` |
| **M4-T7** | **完善开发文档** | **`pro-android-app`** | **已完成** | **2026-07-08** | **-** | **新贡献者可按 BUILD.md / DEVELOPER_GUIDE.md 独立构建** | BUILD.md / README.md / DEVELOPER_GUIDE.md / ARCHITECTURE.md 已同步当前代码；`./gradlew clean build` 通过 |
| **M4-T8** | **Beta 版本测试** | **`pro-android-system-test`** | **已完成** | **2026-07-12** | **M4-T1~T7** | **Beta 测试报告，无 P0/P1 Bug** | **BC-1~BC-8 全部通过；修复蓝牙耳机音频路由问题；报告见 `docs/M4-T8_BETA_TEST_REPORT.md`** |
| **M5-T1** | **户外实测方案** | **`pro-general-pm`** | **已取消** | **2026-06-30** | **M4-T8** | **输出 `docs/M5-T1_OUTDOOR_TEST_PLAN.md` 并通过评审** | **按用户决策，户外测试不再执行，本方案仅作归档** |
| **M5-T2** | **户外实测执行** | **`pro-android-system-test`** | **已取消** | **2026-07-05** | **M5-T1** | **按方案执行并输出 `docs/M5-T2_OUTDOOR_TEST_REPORT.md`** | **按用户决策，户外测试不再执行** |
| **M5-T3-1** | **Opus 依赖许可合规审查** | **`pro-general-pm`** | **已完成** | **2026-07-02** | **M4-T8** | **输出 `docs/M5-T3_LICENSE_COMPLIANCE.md`，结论明确** | **`docs/M5-T3_LICENSE_COMPLIANCE.md` 已完成，建议替换 `opuscodec`** |
| **M5-T3-2** | **arm64 Opus 原生库替换评估与实施** | **`pro-android-app` / `pro-c-cpp`** | **已完成** | **2026-07-07** | **M5-T3-1** | **输出 `docs/M5-T3_OPUS_REPLACEMENT.md`；若替换，真机回归通过** | **替换实施完成，BC-1/BC-5/BC-8 回归通过；报告见 `docs/M5-T3_REGRESSION_REPORT.md`** |
| **M5-T4** | **发布 v1.0 Release** | PM / Android Dev | 等待 review | 2026-07-09 | M5-T3-1, M5-T3-2 | GitHub Release + Tag + APK | Tag `v1.0.0` 已推送；GitHub Release 页面与 APK 上传需手动完成（缺少 API Token） |
| **M5-T5** | **发布推广** | PM | 待办池 | 2026-07-12 | M5-T4 | 发布到合适的技术社区/论坛 | |
| **M5-T6** | **收集首批用户反馈，建立 Issue 模板** | PM | 待办池 | 2026-07-12 | M5-T4 | GitHub Issue 模板、讨论区开启 | |

---

## v1.1 需求冻结池（已识别但不在 v1.0 范围）

> 以下需求/变更已由 `pro-general-req-translate` 完成文档草案，但按用户决策**不进入 v1.0**，统一纳入 v1.1 规划。

| ID | 任务名称 | 来源 | 状态 | 负责人 | 验收标准 | 备注 |
|----|----------|------|------|--------|----------|------|
| V1.1-T1 | PRD v1.2：组网方式从 mesh/多跳改为星型单跳（Group Owner + Client） | 用户决策 | 待办池 | `pro-general-req-translate` / `pro-general-pm` | PRD v1.2 定稿并同步 ARCHITECTURE/PROJECT_PLAN/UI_DESIGNS 等文档 | 当前工作区已存在未提交的 PRD v1.2 等文档修改，待 v1.0 发布后再由 `pro-general-git` 提交到独立分支或 v1.1 迭代 |

---

## 说明

- 本表为 Skill 授权校验的主要来源；
- 任务完成后由负责人更新状态为「等待 review」，并由 PM/QA 验收后改为「已完成」；
- git 提交由 `pro-general-git` 统一处理，本表不记录提交信息；
- v1.0 范围已冻结：M5-T4 完成后，所有新增/变更需求统一进入 v1.1 需求池，不在当前版本继续修改。

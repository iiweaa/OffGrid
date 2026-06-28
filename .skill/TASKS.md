# OffGrid 任务指派表

> 本文件由 `pro-general-pm` 维护，用于记录当前迭代中已指派给各专业 Skill 的任务。
> 任务状态：待办池 / 本周进行 / 等待 review / 已完成 / 已归档

---

## 当前迭代任务

| ID | 任务名称 | 负责人 | 状态 | 截止时间 | 依赖 | 验收标准 | 备注 |
|----|----------|--------|------|----------|------|----------|------|
| M4-T5 | 手动网络配置入口与故障排查提示 | `pro-android-app` | 等待 review | 2026-07-04 | M3-T3 | 见 `docs/M4-T5_MANUAL_NETWORK_CONFIG.md` 第 7 节 | 代码已 push（`9d4a5ee`），真机验证拆分为 M4-T5-VERIFY |
| M4-T5-OPT | 任务分配机制与对话流程优化 | `pro-general-mechanism` | 已完成 | 2026-06-28 | - | 输出 `.skill/` 初始化结构与流程优化建议，并被采纳 | 已同步更新 skill 仓库，`.skill/` 标准结构已落地 |
| **M4-T5-OPT-FIX** | **验收任务拆分机制补强** | **`pro-general-mechanism`** | **本周进行** | **2026-06-28** | **M4-T5-OPT** | 修复「代码完成但缺少测试验证任务分配」的流程缺陷，同步更新共享规范与项目规则 | 已拆分 M4-T5-VERIFY / M4-T6-VERIFY，更新 `pro-general-pm` 规则 7 与 `_shared/PROFESSIONAL_MODE.md` |
| **M4-T5-FIX** | **修复 Auto 模式 Group 断连死锁** | **`pro-android-app`** | **已完成** | **2026-07-05** | **M4-T5** | **按决策移除 Auto 模式，仅保留手动 GO/Client；GO 复用已有 Group 避免 BUSY** | Auto 模式因 `setDeviceName()` 失效及第三方 P2P 干扰无法稳定识别对端，已删除相关代码；GO 侧增加已有 Group 复用逻辑，TC-1 复测通过 |
| **M4-T5-VERIFY** | **M4-T5 真机网络验证** | **`pro-android-system-test`** | **已完成** | **2026-07-07** | **M4-T5, M4-T5-FIX** | **一加/华为 GO/Client 两种模式通话均正常，故障提示正确** | TC-1/TC-2/TC-3 全部通过，无 ANR/Crash；见 `docs/M4-T5_VERIFICATION.md` |
| M4-T6 | 省电模式 | `pro-android-app` | 等待 review | 2026-07-07 | M4-T1 | 省电模式耗电下降 ≥ 20% | 代码已 push（`81cce23`），功耗验证拆分为 M4-T6-VERIFY |
| **M4-T6-VERIFY** | **M4-T6 真机功耗验证** | **`pro-android-system-test`** | **已跳过** | **2026-07-09** | **M4-T6** | **15 分钟普通/省电耗电对比，省电 ≥ 20%** | 按用户决策暂不测试，保留脚本与方案；M4-T6 代码仍待 review |
| **M4-T7** | **完善开发文档** | **`pro-android-app`** | **已完成** | **2026-07-08** | **-** | **新贡献者可按 BUILD.md / DEVELOPER_GUIDE.md 独立构建** | BUILD.md / README.md / DEVELOPER_GUIDE.md / ARCHITECTURE.md 已同步当前代码；`./gradlew clean build` 通过 |

---

## 说明

- 本表为 Skill 授权校验的主要来源；
- 任务完成后由负责人更新状态为「等待 review」，并由 PM/QA 验收后改为「已完成」；
- git 提交由 `pro-general-git` 统一处理，本表不记录提交信息。

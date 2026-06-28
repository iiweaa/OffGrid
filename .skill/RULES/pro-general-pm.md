# pro-general-pm 项目级规则

> 本文件由 `pro-general-pm` 维护，记录 OffGrid 项目在 PM 任务指派与验收流程方面的约束。
> 与共享规范冲突时，以本文件及最新版 `skills/pro-general-pm/SKILL.md` 为准。

---

## 当前活跃任务

- ~~**M5-T1 / M5-T2**：户外实测方案与执行~~（已取消）
- **M5-T3-1**：Opus 依赖许可合规审查（`pro-general-pm`）→ **等待 review**
- **M5-T3-2**：arm64 Opus 原生库替换评估与实施（待 `M5-T3-1` 验收后由 `pro-android-app` / `pro-c-cpp` 执行）
- **M5-T4**：发布 v1.0 Release（待 `M5-T3-1` / `M5-T3-2` 完成后启动）

---

## 项目约束

### 1. 任务指派必须同步拆分验证任务

当验收标准包含开发 Skill 无法独立完成的验证项时，必须在指派开发任务的同时，指派独立验证任务：

| 开发任务 | 验证任务 | 验证 Skill | 验证文档 |
|----------|----------|------------|----------|
| M4-T5 | M4-T5-VERIFY | `pro-android-system-test` | `docs/M4-T5_VERIFICATION.md` |

> M4-T6 省电模式已按产品决策从产品需求中删除，对应验证任务 M4-T6-VERIFY 同步取消。

### 2. 验证任务独立性要求

- 验证任务 ID 必须为 `{TASK-ID}-VERIFY`（或 `-BENCHMARK` / `-SECURITY` / `-E2E`）；
- 验证任务负责人**不得**与开发任务负责人相同；
- 必须输出可执行的验证文档，含环境、步骤、数据记录表、验收公式；
- 开发任务状态保持「等待 review」，直至验证任务完成并由 PM/QA 验收。

### 3. 禁止行为

- ❌ 将验证工作以备注形式挂在开发任务下；
- ❌ 开发任务在验证未完成时标记为「已完成」。

---

## 相关文档

- 专业模式共享规范：`skills/_shared/PROFESSIONAL_MODE.md`
- PM Skill 规则：`skills/pro-general-pm/SKILL.md`
- 任务指派表：`.skill/TASKS.md`
- 进度跟踪：`docs/PROGRESS_TRACKING.md`

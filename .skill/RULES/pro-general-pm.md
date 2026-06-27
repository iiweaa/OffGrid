# pro-general-pm 项目级规则

> 本文件由 `pro-general-pm` 维护，记录 OffGrid 项目在 PM 任务指派与验收流程方面的约束。
> 与共享规范冲突时，以本文件及最新版 `skills/pro-general-pm/SKILL.md` 为准。

---

## 当前活跃任务

- **M4-T5 / M4-T5-VERIFY**：手动网络配置 + 真机网络验证
- **M4-T6 / M4-T6-VERIFY**：省电模式 + 真机功耗验证
- **M4-T7**：开发文档完善

---

## 项目约束

### 1. 任务指派必须同步拆分验证任务

当验收标准包含开发 Skill 无法独立完成的验证项时，必须在指派开发任务的同时，指派独立验证任务：

| 开发任务 | 验证任务 | 验证 Skill | 验证文档 |
|----------|----------|------------|----------|
| M4-T5 | M4-T5-VERIFY | `pro-android-system-test` | `docs/M4-T5_VERIFICATION.md` |
| M4-T6 | M4-T6-VERIFY | `pro-android-system-test` | `docs/M4-T6_VERIFICATION.md` |

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

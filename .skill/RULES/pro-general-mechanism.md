# pro-general-mechanism 项目级规则

> 本文件由 `pro-general-mechanism` 维护，记录 OffGrid 项目在专业模式机制方面的约束与入口。

---

## 当前任务

- **M4-T5-OPT**：任务分配机制与对话流程优化
  - 目标：减少 Skill 因 `.skill/` 基础设施缺失导致的零操作拦截与对话往返
  - 需求：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md` 实施过程中暴露出的指派流程问题
  - 已落地：`.skill/INDEX.md`、`.skill/ROLES.md`、`.skill/TASKS.md`、`.skill/RULES/pro-android-app.md`
  - 已同步更新 skill 仓库：`pro-general-pm` v1.0.4、`_shared/PROFESSIONAL_MODE.md` v2.4.1、`pro-general-mechanism` v1.0.4

## 项目约束

- 所有 professional skill 的任务指派必须经过 `pro-general-pm`，并写入 `.skill/TASKS.md`；
- 任务进入「本周进行」时，必须同步初始化 `.skill/` 标准结构；
- 机制调整优先在项目 `.skill/` 中验证，再同步到 skill 仓库共享规范。

## 关键文档

- M4-T5 需求：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`
- 进度跟踪：`docs/PROGRESS_TRACKING.md`
- Skill 索引：`.skill/INDEX.md`
- 角色映射：`.skill/ROLES.md`

# OffGrid Skill 索引

> 本项目采用 professional skill 机制执行任务。
> 本索引用于快速定位活跃任务、项目级规则与相关文档。

---

## 当前活跃任务

见 [`.skill/TASKS.md`](./TASKS.md)。

当前重点：

- **M4-T5 / M4-T5-VERIFY**：手动网络配置代码已 push，真机验证由 `pro-android-system-test` 执行
- **M4-T6 / M4-T6-VERIFY**：省电模式代码已 push，功耗验证由 `pro-android-system-test` 执行
- **M4-T7**：开发文档完善，技术审阅 `pro-android-app`，最终验收 `pro-general-pm`

---

## 项目级规则

项目级规则存放在 [`.skill/RULES/`](./RULES/) 下，每个 professional skill 对应一个 `{skill-id}.md` 文件。

当前已初始化的规则文件：

- [`.skill/RULES/pro-android-app.md`](./RULES/pro-android-app.md)
- [`.skill/RULES/pro-general-mechanism.md`](./RULES/pro-general-mechanism.md)
- [`.skill/RULES/pro-general-pm.md`](./RULES/pro-general-pm.md)

---

## Professional Skill 清单

| Skill ID | 职责 | 备注 |
|----------|------|------|
| `pro-android-app` | Android App 开发、UI、业务逻辑 | 负责 M4-T5 / M4-T6 实现，M4-T7 技术审阅 |
| `pro-android-system-test` | Android 系统/产品真机测试、功耗/性能验证 | 负责 M4-T5-VERIFY / M4-T6-VERIFY |
| `pro-general-pm` | 项目规划、任务拆分、进度跟踪、验收 | 维护本索引与 TASKS.md |
| `pro-general-git` | Git 操作、提交、分支管理 | 代码完成后统一处理提交 |
| `pro-general-mechanism` | professional skill 体系机制设计与规则优化 | 维护 `.skill/` 标准结构与共享规范 |

---

## 角色映射（历史兼容）

旧文档中的角色名与 professional skill 的映射见 [`.skill/ROLES.md`](./ROLES.md)。

---

## 关键文档快速入口

- M4-T5 需求文档：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`
- M4-T5 验证方案：`docs/M4-T5_VERIFICATION.md`
- M4-T6 验证方案：`docs/M4-T6_VERIFICATION.md`
- 设计系统：`docs/DESIGN_SYSTEM.md`
- 开发指南：`docs/DEVELOPER_GUIDE.md`
- 进度跟踪：`docs/PROGRESS_TRACKING.md`

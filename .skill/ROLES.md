# OffGrid 角色映射表

> 本文档用于兼容历史文档中的角色命名，并明确其对应的 professional skill。
> 新任务指派、文档署名、验收人均应使用 **Skill ID**。

---

## 映射表

| 历史角色名 | Professional Skill ID | 说明 |
|------------|----------------------|------|
| Android Dev | `pro-android-app` | Android 应用开发 |
| PM / 项目经理 | `pro-general-pm` | 项目规划与任务管理 |
| Git 管理员 | `pro-general-git` | Git 提交与分支管理 |
| QA | `pro-android-system-test` / `pro-general-pm` | 视测试范围由对应 Skill 承担 |
| Designer | `pro-design-ui-design` | UI/UX 设计 |

---

## 使用规范

- 历史文档（如 `docs/PROJECT_PLAN.md`、`docs/PROGRESS_TRACKING.md`）保留原角色名，仅作只读参考；
- 自 M4 起，新增任务指派统一使用 Skill ID；
- `.skill/TASKS.md` 中不再出现历史角色名。

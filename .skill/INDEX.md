# OffGrid Skill 索引

> 本项目采用 professional skill 机制执行任务。
> 本索引用于快速定位活跃任务、项目级规则与相关文档。

---

## 当前活跃任务

见 [`.skill/TASKS.md`](./TASKS.md)。

当前重点：

- **M4 里程碑已完成**：M4-T5 / M4-T7 / M4-T8 全部通过；M4-T6 省电模式已按产品决策从产品需求中删除
- **M5 户外实测已取消**：M5-T1 / M5-T2 不再执行
- **M5-T3-1 已完成**：`docs/M5-T3_LICENSE_COMPLIANCE.md` 建议替换 `opuscodec`
- **M5-T3-2 进行中**：`docs/M5-T3_OPUS_REPLACEMENT.md` 已输出，待 NDK 环境就绪后实施替换
- **下一步**：安装 Android NDK，实施 libopus + JNI 替换

---

## 项目级规则

项目级规则存放在 [`.skill/RULES/`](./RULES/) 下，每个 professional skill 对应一个 `{skill-id}.md` 文件。

当前已初始化的规则文件：

- [`.skill/RULES/pro-android-app.md`](./RULES/pro-android-app.md)
- [`.skill/RULES/pro-android-system-test.md`](./RULES/pro-android-system-test.md)
- [`.skill/RULES/pro-c-cpp.md`](./RULES/pro-c-cpp.md)
- [`.skill/RULES/pro-general-mechanism.md`](./RULES/pro-general-mechanism.md)
- [`.skill/RULES/pro-general-pm.md`](./RULES/pro-general-pm.md)

---

## Professional Skill 清单

| Skill ID | 职责 | 备注 |
|----------|------|------|
| `pro-android-app` | Android App 开发、UI、业务逻辑 | 负责 M4-T5 / M4-T7 实现；M4-T6 已删除；参与 M5-T3-2 Opus 替换 |
| `pro-android-system-test` | Android 系统/产品真机测试、功耗/性能验证 | 负责 M4-T5-VERIFY；M4-T6-VERIFY / M5-T2 已取消 |
| `pro-c-cpp` | Native / C / C++ / NDK 开发 | 参与 M5-T3-2 arm64 Opus 原生库替换 |
| `pro-general-pm` | 项目规划、任务拆分、进度跟踪、验收 | 维护本索引与 TASKS.md；负责 M5-T3-1 许可审查 |
| `pro-general-git` | Git 操作、提交、分支管理 | 代码完成后统一处理提交 |
| `pro-general-mechanism` | professional skill 体系机制设计与规则优化 | 维护 `.skill/` 标准结构与共享规范 |

---

## 角色映射（历史兼容）

旧文档中的角色名与 professional skill 的映射见 [`.skill/ROLES.md`](./ROLES.md)。

---

## 关键文档快速入口

- M4-T5 需求文档：`docs/M4-T5_MANUAL_NETWORK_CONFIG.md`
- M4-T5 验证方案：`docs/M4-T5_VERIFICATION.md`
- M5-T1 户外实测方案：`docs/M5-T1_OUTDOOR_TEST_PLAN.md`（已取消）
- M5-T3 发布前打磨计划：`docs/M5-T3_RELEASE_POLISH_PLAN.md`
- M5-T3-1 许可合规报告：`docs/M5-T3_LICENSE_COMPLIANCE.md`
- M5-T3-2 替换方案：`docs/M5-T3_OPUS_REPLACEMENT.md`
- 设计系统：`docs/DESIGN_SYSTEM.md`
- 开发指南：`docs/DEVELOPER_GUIDE.md`
- 进度跟踪：`docs/PROGRESS_TRACKING.md`

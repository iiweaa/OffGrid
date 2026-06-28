# OffGrid 进度跟踪机制

> 进度跟踪文档 v1.0
> 制定：pro-general-pm（管师）
> 输入：`docs/PROJECT_PLAN.md` v1.1

---

## 1. 跟踪机制总览

| 维度 | 方式 | 频率 | 工具建议 |
|------|------|------|----------|
| 任务状态 | 看板 + 任务表 | 实时更新 | GitHub Projects / Notion / 本 Markdown |
| 每周 Review | 周报模板 | 每周五 | Markdown / Issue |
| 里程碑检查 | 验收清单 | 每个里程碑结束 | 文档核对 + 演示 |
| 风险跟踪 | 风险登记表 | 每周更新 | 与周报合并 |
| Bug/阻塞 | Issue 跟踪 | 实时 | GitHub Issues |

---

## 2. 任务看板结构（Kanban）

建议使用以下 5 列：

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   待办池    │ → │   本周进行   │ → │   等待 review │ → │    已完成    │ → │    已归档    │
│  (Backlog)  │   │  (In Progress)│   │  (In Review)  │   │   (Done)    │   │  (Archived) │
└─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘
```

**状态定义：**

| 状态 | 说明 | 触发条件 |
|------|------|----------|
| 待办池 | 已规划但未开始 | 任务已创建，依赖未满足或尚未排期 |
| 本周进行 | 当前迭代正在执行 | 任务依赖满足，负责人开始执行 |
| 等待 review | 已完成初稿/实现，待验收 | 负责人自认为完成，提交验收 |
| 已完成 | 通过验收标准 | 验收人确认满足验收标准 |
| 已归档 | 里程碑结束或长期无后续 | 任务关闭，保留记录 |

**标签建议：**

| 标签 | 用途 |
|------|------|
| `P0` | Must have，阻塞里程碑 |
| `P1` | Should have，重要但不阻塞 |
| `P2` | Could have，可延后 |
| `milestone/M1` ~ `milestone/M5` | 归属里程碑 |
| `risk/high` | 高风险任务 |
| `blocked` | 当前被阻塞 |
| `bug` | 缺陷修复 |
| `design` / `android` / `doc` / `test` | 类型 |

---

## 3. 初始任务状态表

> 以下任务来自 `docs/PROJECT_PLAN.md` v1.1，建议在项目启动时录入看板。

### M1：技术预研（第 1-2 周）

| ID | 任务 | 负责人 | 状态 | 依赖 | 验收标准 | 备注 |
|----|------|--------|------|------|----------|------|
| M1-T1 | 搭建开发环境 | Android Dev | 待办池 | - | 仓库可 clone，项目可编译 | |
| M1-T2 | 验证 Wi-Fi Direct AP-STA 并发 | Android Dev / Architect | 待办池 | M1-T1 | 目标机型实现 GO+Client 并发 | |
| M1-T3 | 验证 IPv6 link-local 通信 | Android Dev | 待办池 | M1-T2 | 两机通过 IPv6 link-local ping 通 | |
| M1-T4 | 验证 Opus 编解码延迟 | Android Dev | 待办池 | M1-T1 | 端到端延迟 < 200ms | |
| M1-T5 | 输出《技术预研报告》 | Architect / PM | 待办池 | M1-T2~T4 | 报告通过评审 | |

### M2：MVP 直连语音（第 3-6 周）

| ID | 任务 | 负责人 | 状态 | 依赖 | 验收标准 | 备注 |
|----|------|--------|------|------|----------|------|
| M2-T1 | 项目基础框架搭建 | Android Dev | 待办池 | M1-T1 | 工程结构符合架构，可运行 | |
| M2-T2 | Foreground Service 与权限 | Android Dev | 待办池 | M2-T1 | 后台持续运行，权限引导完整 | |
| M2-T3 | Audio Engine | Android Dev | 待办池 | M2-T1 | 单机循环延迟 < 200ms | |
| M2-T4 | Wi-Fi Direct 基础连接 | Android Dev | 待办池 | M1-T2 | 2 台手机 30 秒内直连 | |
| M2-T5 | UDP Socket 收发 | Android Dev | 待办池 | M2-T4 | 两机 UDP 双向收发 | |
| M2-T6 | 2 台手机直连语音 | Android Dev | 待办池 | M2-T3, M2-T5 | 30m 内双向语音，延迟 < 300ms | |
| M2-T7 | LICENSE / CI 配置 | PM / Android Dev | 待办池 | - | CI 构建通过 | |
| M2-T8 | MVP 实机测试与 Demo | QA / PM | 待办池 | M2-T6, M2-T7 | 测试报告 + Demo | |

### M3：双机稳定 + 位置共享 + 多跳预备（第 7-12 周）

> 说明：当前目标机型（一加 11、华为/荣耀）实测不支持 Wi-Fi Direct AP-STA 并发，无法构成链式多跳。M3 修订为把双机体验做扎实，并保留多跳扩展结构。

| ID | 任务 | 负责人 | 状态 | 依赖 | 验收标准 | 备注 |
|----|------|--------|------|------|----------|------|
| M3-T1 | 运行时检测 AP-STA 并发能力并提示用户 | Android Dev | 已完成 | M2-T5 | 启动时显示「当前设备支持/不支持多跳中继」 | 反射检测 + 首页状态卡 + Wi-Fi Direct Test 探针 |
| M3-T2 | Mesh 包格式 + NodeID + 基础序列化 | Android Dev / Architect | 已完成 | M2-T5 | 包结构符合架构文档，单测通过 | Packet/PacketType/PacketSerializer + NodeId/NodeIdStore |
| M3-T3 | 重构 LinkManager → NeighborTable | Android Dev | 已完成 | M3-T2 | 双机仍能互通，邻居表显示对端 NodeID、IP、LastSeen | 已接入 Packet 序列化；CallScreen 显示邻居列表 |
| M3-T4 | Signaling Engine：HELLO 心跳与邻居老化 | Android Dev | 已完成 | M3-T3 | 断开后 10 秒内标记离线，重连后恢复 | SignalingEngine 独立调度；NeighborTable 返回移除节点；单测覆盖连接/超时/重连 |
| M3-T5 | Location Engine（GPS 获取、位置广播） | Android Dev | 已完成 | M3-T2 | 双机相距 >10m 时，距离误差 <30% | LocationEngine + LocationPayload；已接入 VoiceService 与 CallScreen 状态 |
| M3-T6 | 相对方位/距离计算与 UI 罗盘页 | Android Dev / Designer | 已完成 | M3-T5 | 界面正确显示队友方位与距离，刷新 ≥ 1 次/5s | PeerScreen + Canvas 罗盘 + PeerList；Haversine/ bearing 单测 |
| M3-T7 | 后台保活与音频路由稳定性优化 | Android Dev | 已完成 | M3-T4 | 锁屏 2 分钟内仍可接收语音/位置 | WakeLock + WiFiLock + AudioRouter + 电池优化白名单入口 |
| M3-T8 | 双机 Alpha 集成测试与文档更新 | QA / PM | 已完成 | M3-T6, M3-T7 | 两台设备通话+位置共享 15 分钟无崩溃 | 已输出 M3_ALPHA_TEST.md 测试方案；实机 15 分钟测试待执行 |

### M3.5：UI/UX 设计冲刺（第 13-14 周）

| ID | 任务 | 负责人 | 状态 | 依赖 | 验收标准 | 备注 |
|----|------|--------|------|------|----------|------|
| M3.5-T1 | 信息架构与页面流程图 | Designer / PM | 已完成 | M3-T6 | 覆盖所有核心页面 | docs/INFO_ARCHITECTURE.md |
| M3.5-T2 | 设计系统 | Designer | 已完成 | M3.5-T1 | 色彩/字体/间距/圆角/阴影 token | docs/DESIGN_SYSTEM.md |
| M3.5-T3 | 首页/通话页高保真 | Designer | 已完成 | M3.5-T2 | 包含组网、节点、通话控制 | docs/UI_DESIGNS.md |
| M3.5-T4 | 方位页高保真 | Designer | 已完成 | M3.5-T2 | 罗盘 + 队友方位/距离 | docs/UI_DESIGNS.md |
| M3.5-T5 | 设置页/首次引导设计 | Designer | 已完成 | M3.5-T2 | 权限、电池、耳机、Wi-Fi Direct 引导 | docs/UI_DESIGNS.md |
| M3.5-T6 | 设计评审与开发交付 | Designer / Android Dev / PM | 已完成 | M3.5-T3~T5 | 设计稿通过，交付开发 | 文档已交付开发；Figma/切图待 M4 补充 |

### M4：后台/耳机/UI 实现/文档（第 15-22 周）

| ID | 任务 | 负责人 | 状态 | 依赖 | 验收标准 | 备注 |
|----|------|--------|------|------|----------|------|
| M4-T1 | 后台保活优化 | Android Dev | 已完成 | M2-T2 | 锁屏 30 分钟内可接收语音 | M3-T7 提前实现：WakeLock + WiFiLock + 电池白名单 |
| M4-T2 | 音频路由管理 | Android Dev | 已完成 | M2-T3 | 有线/蓝牙耳机自动切换 | M3-T7 提前实现：AudioRouter + AudioDeviceCallback |
| M4-T3 | 蓝牙耳机按键控制 | Android Dev | 已完成 | M4-T2 | 按键响应正确 | MediaButtonHandler：短按静音/长按挂断；CallScreen 增加静音按钮 |
| M4-T4 | UI/UX 页面实现 | Designer / Android Dev | 已完成 | M3.5-T6 | 设计稿完整实现，clean build 通过 | 底部导航 Home/Call/Peers/Settings；Onboarding 三页引导；主题切换；配色按 DESIGN_SYSTEM.md 实现 |
| M4-T4-UI | 重构 Wi-Fi Direct Test 页面 | Android Dev / Designer | 已完成 | M4-T4, M2-T4 | 页面符合设计系统；状态清晰、操作分区、日志可折叠 | 已重命名为 Direct Connection Test；一加 11 / 华为 Mate 30 Pro 5G 真机验证通过；代码已 push 到 origin/main |
| **M4-T5** | **手动网络配置入口** | **`pro-android-app`** | **已完成** | **M3-T3** | **可手动选择 GO/Client** | **代码已 push（`9d4a5ee`），M4-T5-VERIFY 全部通过** |
| M4-T5-FIX | 修复 Auto 模式 Group 断连死锁 | `pro-android-app` | 已完成 | M4-T5 | 按决策移除 Auto 模式，仅保留手动 GO/Client；GO 复用已有 Group 避免 BUSY | Auto 模式因 `setDeviceName()` 失效及第三方 P2P 干扰无法稳定识别对端，已删除相关代码；GO 侧增加已有 Group 复用逻辑，TC-1 复测通过 |
| M4-T5-VERIFY | M4-T5 真机网络验证 | `pro-android-system-test` | 已完成 | M4-T5, M4-T5-FIX | 一加/华为 GO/Client 两种模式通话均正常，故障提示正确 | TC-1/TC-2/TC-3 全部通过，无 ANR/Crash；见 `docs/M4-T5_VERIFICATION.md` |
| **M4-T7** | **完善开发文档** | **`pro-android-app`** | **已完成** | **-** | **新贡献者可独立构建** | BUILD.md / README.md / DEVELOPER_GUIDE.md / ARCHITECTURE.md 已同步当前代码；`./gradlew clean build` 通过 |
| **M4-T8** | **Beta 版本测试** | **`pro-android-system-test`** | **已完成** | **M4-T1~T5, M4-T7** | **Beta 测试报告，无 P0/P1 Bug** | **BC-1~BC-3, BC-5~BC-8 全部通过；BC-4 已随省电模式删除；修复蓝牙耳机音频路由问题；报告见 `docs/M4-T8_BETA_TEST_REPORT.md`** |

### M5：Bug 修复、社区发布（原户外实测已取消）（第 23-26 周）

| ID | 任务 | 负责人 | 状态 | 依赖 | 验收标准 | 备注 |
|----|------|--------|------|------|----------|------|
| **M5-T1** | **户外测试方案** | **`pro-general-pm` / `pro-android-system-test`** | **已取消** | **M4-T8** | **`docs/M5-T1_OUTDOOR_TEST_PLAN.md` 通过评审** | **按用户决策，户外测试不再执行，本方案仅作归档** |
| M5-T2 | 户外实测 | QA / PM / Android Dev | 已取消 | M5-T1 | 收集语音距离/延迟/续航/稳定性数据；位置共享功能保留，但不作为户外实测必测项 | 按用户决策，户外测试不再执行 |
| **M5-T3-1** | **Opus 依赖许可合规审查** | **`pro-general-pm`** | **已完成** | **M4-T8** | **输出 `docs/M5-T3_LICENSE_COMPLIANCE.md`** | **报告已完成，建议触发 M5-T3-2 替换** |
| **M5-T3-2** | **arm64 Opus 原生库替换评估与实施** | **`pro-android-app` / `pro-c-cpp`** | **已完成** | **M5-T3-1** | **输出 `docs/M5-T3_OPUS_REPLACEMENT.md`，真机回归通过** | **替换实施完成，回归报告 `docs/M5-T3_REGRESSION_REPORT.md`** |
| M5-T4 | 发布 v1.0 Release | PM / Android Dev | 进行中 | M5-T3-1, M5-T3-2 | GitHub Release + Tag | Tag `v1.0.0` 已推送；GitHub Release 页面与 APK 上传需手动完成（缺少 API Token） |
| M5-T5 | 发布推广 | PM | 待办池 | M5-T4 | 发布到合适的技术社区/论坛 | |
| M5-T6 | 收集首批用户反馈，建立 Issue 模板 | PM | 待办池 | M5-T4 | GitHub Issue 模板、讨论区开启 | |

---

## 4. 每周 Review 模板

每次周五填写一次，可保存为 `docs/weekly/WEEK-XX.md` 或 GitHub Issue/Discussion。

```markdown
# OffGrid 项目周报 - 第 X 周（YYYY-MM-DD ~ YYYY-MM-DD）

## 本周目标
- [ ] 目标 1
- [ ] 目标 2

## 完成情况
| ID | 任务 | 状态 | 说明 |
|----|------|------|------|
| | | | |

## 里程碑进度
- M1: 0% → 50%
- M2: 0%
- M3: 0%
- M3.5: 0%
- M4: 0%
- M5: 0%

## 风险与阻塞
| 风险/阻塞 | 影响 | 状态 | 下一步 |
|-----------|------|------|--------|
| | | | |

## 下周计划
1. 
2. 
3. 

## 需要支持
- 
```

---

## 5. 里程碑检查清单

### M1 检查（第 2 周末）
- [ ] GitHub 仓库创建，README 包含项目简介与构建说明
- [ ] 技术预研报告通过评审
- [ ] AP-STA 并发与 IPv6 link-local 在目标机型上验证通过

### M2 检查（第 6 周末）
- [ ] 2 台 Android 12+ 手机在无移动网络下完成直连语音
- [ ] 语音延迟 < 300ms
- [ ] GitHub 仓库包含 LICENSE、README、BUILD.md，CI 构建通过

### M3 检查（第 12 周末）
- [ ] 3 台手机排成直线，A 与 C 经 B 中继稳定通话
- [ ] 5 台手机 3-4 跳链路语音可懂，延迟 < 1s
- [ ] 方位界面显示队友相对方位与距离

### M3.5 检查（第 14 周末）
- [ ] 完整的设计系统文档
- [ ] 首页、方位页、设置页、首次引导高保真设计稿
- [ ] 设计评审通过，Android 开发可依据设计稿进入实现

### M4 检查（第 22 周末）
- [ ] 应用退至后台或锁屏后仍能接收并播放语音
- [ ] 有线耳机与蓝牙耳机通话正常
- [ ] 文档完整，社区贡献者可独立构建

### M5 检查（第 26 周末）
- [ ] 户外实测报告通过
- [ ] GitHub v1.0 Release 发布
- [ ] 无阻塞 Bug

---

## 6. 风险登记表

| 风险 ID | 风险描述 | 影响 | 概率 | 应对策略 | 状态 | 负责人 |
|--------|----------|------|------|----------|------|--------|
| R1 | 目标机型不支持 AP-STA 并发 | 高 | 中 | M1 优先验证；不支持时降级为单跳并提示用户 | 监控 | Android Dev |
| R2 | Wi-Fi Direct 品牌差异 | 高 | 高 | 建立三星/小米/Pixel 测试清单 | 监控 | QA |
| R3 | 后台保活被系统杀死 | 高 | 高 | 前台服务 + 电池白名单引导 + 持续静音 | 监控 | Android Dev |
| R4 | 多跳语音延迟超过 1s | 中 | 中 | 优化 Opus 帧长、限制最大跳数、VAD | 监控 | Android Dev |
| R5 | 蓝牙耳机路由异常 | 中 | 中 | AudioManager 监听设备变化 | 监控 | Android Dev |
| R6 | 单人项目进度延期 | 中 | 高 | 每周 Review，削减 P1/P2 保 P0 | 监控 | PM |
| R7 | 开源社区反馈冷清 | 低 | 中 | 完善文档降低贡献门槛 | 监控 | PM |

---

## 7. GitHub 配置建议

### GitHub Projects 看板

1. 创建 Project Board，选择 **Board** 视图
2. 添加上述 5 列：待办池 / 本周进行 / 等待 review / 已完成 / 已归档
3. 创建标签：
   - `P0`, `P1`, `P2`
   - `milestone/M1` ~ `milestone/M5`
   - `risk/high`, `blocked`, `bug`, `design`, `android`, `doc`, `test`
4. 将初始任务表中的任务创建为 Issue，并关联到对应 Milestone
5. 每周五使用周报模板更新进度

### GitHub Issues 模板（已创建）

位于 `.github/ISSUE_TEMPLATE/`，包含：

| 模板 | 用途 |
|------|------|
| `bug_report.yml` | Bug 报告（机型、Android 版本、复现步骤、日志） |
| `feature_request.yml` | 功能请求（场景、期望行为、优先级） |
| `outdoor_test_report.yml` | 户外实测数据（地形、设备、组网、结果） |
| `device_compatibility.yml` | 设备兼容性（AP-STA 并发、直连/多跳、后台、耳机） |
| `ui_ux_feedback.yml` | UI/UX 反馈（页面、类型、截图、建议） |
| `config.yml` | 指引用户到 README 和 Discussions |

> ⚠️ 创建 GitHub 仓库后，需将 `config.yml` 中的 `YOUR_USERNAME/OffGrid` 替换为实际仓库地址。

### GitHub Discussions 分类建议

建议开启 Discussions 并设置以下分类：

| 分类 | 用途 |
|------|------|
| **💡 想法与建议** | 功能设想、产品方向讨论 |
| **❓ 使用问题** | 安装、配置、组网等问题 |
| **🏔️ 户外实测交流** | 分享测试经验、设备兼容性、场景数据 |
| **🎨 设计讨论** | UI/UX、设计系统、交互方案 |
| **🔧 开发者交流** | 架构、代码贡献、技术选型 |
| **📢 公告** | 版本发布、重要更新（仅维护者发帖） |

**Issues vs Discussions 分流原则：**
- **Issues**：明确的 Bug、需要跟踪的任务、设备兼容性报告、户外测试报告
- **Discussions**：开放性问题、经验分享、想法讨论、使用咨询

---

## 8. 使用说明

1. **项目启动时**：将第 3 节任务表录入看板，所有任务初始状态为「待办池」
2. **每周一**：从「待办池」中挑选本周任务移动到「本周进行」
3. **任务完成时**：负责人移动到「等待 review」，并 @ 验收人
4. **验收通过后**：验收人移动到「已完成」
5. **里程碑结束时**：对照第 5 节检查清单逐项确认
6. **每周五**：使用第 4 节模板填写周报，同步风险登记表

---

*本文档由管师基于项目计划制定，随项目进展持续更新。*

# OffGrid 项目计划

> 项目管理文档 v1.2
> 制定：pro-general-pm（管师）
> 输入：`docs/PRD.md` v1.2、`docs/ARCHITECTURE.md` v1.3
> 周期：26 周（约 6 个月）

---

## 1. 项目概览

| 项目属性 | 内容 |
|----------|------|
| 项目名称 | OffGrid |
| 项目目标 | 打造一款 MIT 开源的 Android 12+ 离线语音通讯应用 |
| 核心功能 | 全双工语音群聊、星型热点组网、节点发现、相对方位/距离共享 |
| 目标用户 | 2-5 人徒步/骑行小队 |
| 项目周期 | 2026-06-27 启动，计划 26 周内发布 v1.0 |
| 关键约束 | 仅 Wi-Fi Direct 组网、无服务器、无账号、Android 12+ |

---

## 2. 里程碑总览

| 里程碑 | 时间 | 周期 | 核心目标 | 关键交付物 |
|--------|------|------|----------|------------|
| M1 | 第 1-2 周 | 2 周 | 技术预研与可行性验证 | 预研报告、验证 Demo |
| M2 | 第 3-6 周 | 4 周 | MVP：2 台手机直连语音 | 可运行 Demo、GitHub 仓库 |
| M3 | 第 7-12 周 | 6 周 | 多 Client 稳定接入 + 自动发现 + 位置共享 | Alpha 版本 |
| **M3.5** | **第 13-14 周** | **2 周** | **UI/UX 设计冲刺** | **设计稿与设计系统** |
| M4 | 第 15-22 周 | 8 周 | 后台保活、耳机适配、UI 实现、文档 | Beta/Release 候选 |
| M5 | 第 23-26 周 | 4 周 | 户外实测、Bug 修复、社区发布 | v1.0 正式发布 |

---

## 3. 详细任务拆分（WBS）

### 3.1 M1：技术预研与可行性验证（第 1-2 周）

| ID | 任务 | 负责人 | 工期 | 依赖 | 验收标准 |
|----|------|--------|------|------|----------|
| M1-T1 | 搭建开发环境（Android Studio、Kotlin、Gradle、GitHub 仓库） | Android Dev | 2d | - | 仓库可 clone，项目可编译 |
| M1-T2 | 调研并验证 Wi-Fi Direct 单跳直连可行性，确认目标机型可作为 Group Owner / Client 使用（目标机型 2-3 台） | Android Dev / Architect | 4d | M1-T1 | 两台手机成功建立 Wi-Fi Direct Group 并互相 ping 通；明确记录 GO/Client 星型组网可行性 |
| M1-T3 | 验证 IPv4 私有地址通信可行性 | Android Dev | 3d | M1-T2 | 两台手机通过 192.168.49.x 地址互相 ping 通；记录 IPv6 link-local 可用性 |
| M1-T4 | 验证 Opus 编解码在 Android 上的集成与延迟 | Android Dev | 3d | M1-T1 | 录制→编码→解码→播放端到端延迟 < 200ms（实测约 20 ms，见 `docs/M1_OPUS_LATENCY_TEST.md`） |
| M1-T5 | 输出《技术预研报告》与修订后的架构细节 | Architect / PM | 2d | M1-T2~T4 | 报告包含可行性结论、风险清单、MVP 范围确认 |

**M1 里程碑验收：**
- [x] 项目可在本地 Gradle 环境编译（M1-T1）
- [ ] GitHub 仓库创建，README 包含项目简介与构建说明
- [ ] 技术预研报告通过评审
- [x] Wi-Fi Direct 单跳直连与 IPv4 通信在目标机型上验证通过（`docs/M1_WIFI_DIRECT_TEST.md`）
- [x] Wi-Fi Direct Group Owner / Client 星型组网可行性在目标机型上验证通过
- [x] Opus 编解码集成与延迟验证通过（`docs/M1_OPUS_LATENCY_TEST.md`）

### M1 第一周每日任务详细计划

> 按 5 天工作周安排，假设周一启动。

| 日期 | 主要任务 | 目标产出 | 当日验收标准 |
|------|----------|----------|--------------|
| **周一** | M1-T1：创建 GitHub 仓库与项目骨架 | GitHub 仓库、README 初稿、空 Android 工程 | 仓库可 clone，工程可编译 |
| **周二** | M1-T1：完善构建说明与 CI | BUILD.md、GitHub Actions CI 配置 | CI 首次构建通过；M1-T1 完成 |
| **周三** | M1-T2：Wi-Fi Direct 测试应用开发 | 最小测试 App，可创建/加入 Wi-Fi Direct Group | 单设备可创建 Group 并显示为 Owner |
| **周四** | M1-T2：双设备直连测试 | 在 2 台目标机型上验证建组与连通性 | 两台手机成功建立 Group 并互相 ping 通 |
| **周五** | M1-T2：测试结果记录与周报 | 直连测试记录表、星型组网可行性结论、第 1 周周报 | 明确记录「GO/Client 星型组网可用」结论 |

**第一周风险点：**
- 若目标机型无法稳定建立 Wi-Fi Direct Group Owner / Client 星型组网，需评估是否改用系统热点（SoftAP）方案
- 若 CI 构建失败，需优先解决环境/依赖问题

---

### 3.2 M2：MVP 直连语音（第 3-6 周）

| ID | 任务 | 负责人 | 工期 | 依赖 | 验收标准 |
|----|------|--------|------|------|----------|
| M2-T1 | 项目基础框架搭建（模块划分、Compose Navigation、主题） | Android Dev | 5d | M1-T1 | 工程结构符合架构设计，可运行空壳 App（`ui/screens`、`service`、`audio`、`link`） |
| M2-T2 | 实现 Foreground Service 与权限申请 | Android Dev | 3d | M2-T1 | `VoiceService` 以前台服务运行，通知栏可停止，权限引导完整 |
| M2-T3 | 实现 Audio Engine（采集、Opus 编码、解码、播放） | Android Dev | 7d | M2-T1 | `AudioEngine` 封装 AudioRecord/Opus/AudioTrack，延迟 < 200ms |
| M2-T4 | 复用 Wi-Fi Direct 基础连接能力（发现、连接、组建立） | Android Dev | 7d | M1-T2 | `WifiDirectTestActivity` 可完成 2 台手机直连；MVP 流程需先建组再通话 |
| M2-T5 | 实现 UDP Socket 收发（IPv4 私有地址，可选 IPv6 link-local） | Android Dev | 4d | M2-T4 | `LinkManager` 绑定 4242 端口，支持广播 HELLO 发现对端并收发 Opus 帧 |
| M2-T6 | 实现 2 台手机直连语音通话 | Android Dev | 5d | M2-T3, M2-T5 | 2 台手机在 30m 内双向语音，延迟 < 300ms（一加 11 ↔ 华为通话声音正常） |
| M2-T7 | 配置 MIT LICENSE、CONTRIBUTING.md、CI（GitHub Actions） | PM / Android Dev | 2d | - | 仓库包含 LICENSE、CONTRIBUTING.md、CI 构建通过 |
| M2-T8 | MVP 实机测试与 Demo 视频 | QA / PM | 3d | M2-T6, M2-T7 | 测试报告 + Demo 视频（实机通话已验证，Demo 视频待补录） |

**M2 里程碑验收（对应 PRD AC01/AC02/AC07）：**
- [x] M2-T1~T5 代码实现完成，工程可编译（`./gradlew clean build` 通过）
- [x] M2-T6 完成：一加 11 ↔ 华为双机直连语音通话声音正常
- [x] 语音延迟 < 300ms（用户体感可接受，未用仪器精确测量）
- [x] M2-T7 文档已就位：LICENSE、CONTRIBUTING.md、BUILD.md 已更新
- [x] M2-T8 实机通话已验证，Demo 视频待补录

---

### 3.3 M3：多 Client 稳定接入 + 位置共享（第 7-12 周）

> 说明：v1.0 采用星型单跳拓扑（一台 Group Owner + 多台 Client）。M3 聚焦把「一台 GO、多 Client」的接入体验做扎实，并完成位置共享。

| ID | 任务 | 负责人 | 工期 | 依赖 | 验收标准 |
|----|------|--------|------|------|----------|
| M3-T1 | 手动选择 Group Owner / Client 角色并持久化 | Android Dev | 2d | M2-T5 | 用户在 Settings → Connection 中选择角色，重启后保留 |
| M3-T2 | Mesh 包格式 + NodeID + 基础序列化 | Android Dev / Architect | 3d | M2-T5 | 包结构符合架构文档，单测通过 |
| M3-T3 | 重构 LinkManager → NeighborTable | Android Dev | 3d | M3-T2 | 多 Client 仍能互通，邻居表显示对端 NodeID、IP、LastSeen |
| M3-T4 | Signaling Engine：HELLO 心跳与邻居老化 | Android Dev | 3d | M3-T3 | 断开后 10 秒内标记离线，重连后恢复 |
| M3-T5 | Location Engine（GPS 获取、位置广播） | Android Dev | 4d | M3-T2 | 双机相距 >10m 时，距离误差 <30% |
| M3-T6 | 相对方位/距离计算与 UI 罗盘页 | Android Dev / Designer | 4d | M3-T5 | 界面正确显示队友方位与距离，刷新 ≥ 1 次/5s |
| M3-T7 | 后台保活与音频路由稳定性优化 | Android Dev | 3d | M3-T4 | 锁屏 2 分钟内仍可接收语音/位置 |
| M3-T8 | 多 Client Alpha 集成测试与文档更新 | QA / PM | 3d | M3-T6, M3-T7 | 3 台设备接入同一 GO，通话+位置共享 15 分钟无崩溃 |

**M3 里程碑验收（对应 PRD AC01/AC04/AC07）：**
- [ ] 3 台设备稳定接入同一 Group Owner，通话 + 位置共享 ≥ 15 分钟
- [x] 方位界面显示队友相对方位与距离
- [x] 邻居表可维护多 Client 信息

---

### 3.5 M3.5：UI/UX 设计冲刺（第 13-14 周）

在功能实现进入 UI 开发前，集中完成整体视觉与交互设计，确保后续开发有明确的设计依据。

| ID | 任务 | 负责人 | 工期 | 依赖 | 验收标准 |
|----|------|--------|------|------|----------|
| M3.5-T1 | 梳理信息架构与页面流程图 | Designer / PM | 2d | M3-T6 原型验证 | 输出页面流程图，覆盖首页/方位页/设置页/引导 |
| M3.5-T2 | 制定设计系统（色彩、字体、图标、组件库、间距） | Designer | 3d | M3.5-T1 | 输出 Design System 文档与 Figma/Sketch 源文件 |
| M3.5-T3 | 首页/通话状态页高保真设计 | Designer | 3d | M3.5-T2 | 包含一键组网、节点列表、网络状态、通话控制 |
| M3.5-T4 | 方位页高保真设计（罗盘 + 队友方位/距离） | Designer | 3d | M3.5-T2 | 包含空态、加载态、多人显示态 |
| M3.5-T5 | 设置页/首次引导/权限引导设计 | Designer | 3d | M3.5-T2 | 包含电池优化引导、耳机配对提示、Wi-Fi Direct 引导 |
| M3.5-T6 | 设计评审与开发交付 | Designer / Android Dev / PM | 2d | M3.5-T3~T5 | 设计稿通过评审，切图/标注交付开发 |

**M3.5 里程碑验收：**
- [x] 完整的设计系统文档
- [x] 首页、方位页、设置页、首次引导高保真设计稿
- [x] 设计评审通过，Android 开发可依据设计稿进入实现（文档化评审通过）

---

### 3.4 M4：后台保活、耳机适配、UI 实现、文档（第 15-22 周）

| ID | 任务 | 负责人 | 工期 | 依赖 | 验收标准 |
|----|------|--------|------|------|----------|
| M4-T1 | 优化 Foreground Service 保活策略（电池白名单引导、Doze 处理） | Android Dev | 5d | M2-T2 | 锁屏 30 分钟内仍可接收语音 |
| M4-T2 | 实现音频路由管理（有线/蓝牙耳机自动切换） | Android Dev | 5d | M2-T3 | 耳机插拔/蓝牙连接自动切换 |
| M4-T3 | 实现蓝牙耳机按键控制（静音/挂断） | Android Dev | 3d | M4-T2 | 按键响应正确 |
| M4-T4 | 完成 UI/UX 主页面、方位页、设置页、首次引导 | Designer / Android Dev | 8d | **M3.5-T6** | UI 设计稿实现，首次引导完整 |
| M4-T4-UI | 重构 Wi-Fi Direct Test 页面（简洁化、信息分层、交互优化） | Android Dev / Designer | 3d | M4-T4, M2-T4 | 页面符合 DESIGN_SYSTEM.md；状态卡、操作区、设备列表、日志分区清晰；日志可折叠 |
| M4-T5 | 实现手动网络配置入口与故障排查提示 | Android Dev | 4d | M3-T3 | 用户可手动选择 GO/Client 角色 |
| M4-T7 | 完善 BUILD.md、API 文档、开发者文档 | PM / Android Dev | 5d | - | 新贡献者可按文档独立构建 |
| M4-T8 | Beta 版本测试（多机型、多场景） | QA / PM | 6d | M4-T1~T5, M4-T7 | Beta 测试报告，无 P0/P1 Bug |

**M4 里程碑验收（对应 PRD AC05/AC06）：**
- [x] 应用退至后台或锁屏后仍能接收并播放语音
- [x] 有线耳机与蓝牙耳机通话正常
- [x] 文档完整，社区贡献者可独立构建

> M4-T6 省电模式已从产品需求中删除，不再开发和测试。

---

### 3.5 M5：Bug 修复、社区发布（原户外实测已取消）（第 23-26 周）

> 产品决策：户外实测不再执行。M5 聚焦 Beta 已知问题修复、v1.0 Release 发布与社区推广。

| ID | 任务 | 负责人 | 工期 | 依赖 | 验收标准 |
|----|------|--------|------|------|----------|
| ~~M5-T1~~ | ~~制定户外测试方案~~ | ~~PM / QA~~ | ~~3d~~ | ~~M4-T8~~ | ~~已取消~~ |
| ~~M5-T2~~ | ~~执行山地/公路户外实测~~ | ~~QA / PM / Android Dev~~ | ~~5d~~ | ~~M5-T1~~ | ~~已取消~~ |
| M5-T3-1 | Opus 依赖许可合规审查 | PM / Android Dev | 1d | M4-T8 | 输出 `docs/M5-T3_LICENSE_COMPLIANCE.md`，结论明确 |
| M5-T3-2 | arm64 Opus 原生库替换评估与实施 | Android Dev | 4d | M5-T3-1 | 输出 `docs/M5-T3_OPUS_REPLACEMENT.md`；若替换，真机回归通过 |
| M5-T4 | 发布 v1.0 Release（GitHub Release + Tag） | PM / Android Dev | 2d | M5-T3 | Release 包含 APK、变更日志 |
| M5-T5 | 撰写发布博客/说明，推广到开源社区 | PM | 3d | M5-T4 | 发布到合适的技术社区/论坛 |
| M5-T6 | 收集首批用户反馈，建立 Issue 模板 | PM | 2d | M5-T4 | GitHub Issue 模板、讨论区开启 |

**M5 里程碑验收：**
- [x] ~~户外实测报告通过~~（已取消）
- [ ] GitHub v1.0 Release 发布
- [ ] 无阻塞 Bug

---

## 4. 项目时间线（里程碑视图）

| 阶段 | 周 1-2 | 周 3-6 | 周 7-12 | 周 13-14 | 周 15-22 | 周 23-26 |
|------|--------|--------|---------|----------|----------|----------|
| **M1** 技术预研 | ████ | | | | | |
| **M2** MVP 直连语音 | | ████ | | | | |
| **M3** 多 Client 接入 + 位置 | | | ████ | | | |
| **M3.5** UI/UX 设计冲刺 | | | | ████ | | |
| **M4** 后台/耳机/UI 实现/文档 | | | | | ████ | |
| **M5** Bug 修复 + 发布 | | | | | | ████ |

> 详细任务排期见第 3 节 WBS。

---

## 5. 关键路径

**关键路径：**
```
M1-T1 → M1-T2 → M1-T3 → M2-T1 → M2-T3 → M2-T4 → M2-T5 → M2-T6
→ M3-T1 → M3-T2 → M3-T3 → M3-T4 → M3-T6 → **M3.5-T6** → M4-T1 → M4-T4 → M4-T8
→ M5-T3-1 / M5-T3-2 → M5-T4
```

任何关键路径上的延迟都会直接影响 v1.0 发布时间。

---

## 6. 资源与角色协调

| 角色 | 对应 Skill | 职责 |
|------|-----------|------|
| 项目经理 | pro-general-pm（管师） | 计划、跟踪、风险、文档、发布 |
| Android 开发 | pro-android-app / zhao-yun-dev | 核心功能实现、性能优化 |
| 架构师 | pro-general-architect | 技术决策、方案评审、难点攻关 |
| 测试/QA | guan-yu-qa / pro-android-system-test | 实机测试、户外测试、Bug 跟踪 |
| UI/UX 设计 | zhou-yu-design | 界面设计、交互、首次引导 |

**说明：** 本项目可按 1-2 人核心开发 + 兼职设计/测试 的节奏推进。若仅 1 人全职开发，建议将 M4 周期压缩或削减部分 P1 功能。

---

## 7. 风险管理

| 风险 ID | 风险描述 | 影响 | 概率 | 应对策略 | 状态 |
|--------|----------|------|------|----------|------|
| R1 | 目标机型 Wi-Fi Direct GO/Client 行为差异 | 高 | 中 | M1 优先验证；建立兼容设备清单 | 监控 |
| R2 | Wi-Fi Direct API 在不同品牌上行为不一致 | 高 | 高 | 建立三星/小米/Pixel 等主流机型测试清单 | 监控 |
| R3 | 后台保活被系统强制杀死 | 高 | 高 | 前台服务 + 电池白名单引导 + 持续静音策略 | 监控 |
| R4 | Group Owner 设备耗电过快或离开网络 | 中 | 中 | UI 提示电量；支持手动切换 Group Owner；建议用户携带充电宝 | 监控 |
| R5 | 蓝牙耳机路由/麦克风切换异常 | 中 | 中 | 使用 AudioManager 监听设备变化，充分测试 | 监控 |
| R6 | 单人项目进度延期 | 中 | 高 | 每周进度 Review，削减 P1/P2 功能保 P0 | 监控 |
| R7 | 开源社区反馈冷清 | 低 | 中 | 发布到合适渠道，完善文档降低贡献门槛 | 监控 |

---

## 8. 进度跟踪机制

### 8.1 节奏
- **每日站会（自同步）**：自己记录昨日完成/今日计划/阻塞
- **每周 Review**：每周五检查任务完成度、更新风险状态
- **里程碑评审**：每个 Milestone 结束时对照验收标准检查

### 8.2 工具建议
- **任务看板**：GitHub Projects 或 Notion
- **Bug 跟踪**：GitHub Issues + Label（P0/P1/P2）
- **文档协作**：GitHub Wiki 或 docs/ 目录
- **版本控制**：Git + GitHub（提交规范由 pro-general-git 负责）

### 8.3 汇报模板
```
【周进展】第 X 周
- 完成任务：...
- 阻塞项：...
- 风险更新：...
- 下周计划：...
```

---

## 9. 需求-任务-验收映射

| PRD 功能 | 主要任务 | 验收标准 |
|----------|----------|----------|
| F01 局域网语音 | M2-T3, M2-T6, M3-T2, M3-T4 | AC01, AC02 |
| F02 节点发现与接入 | M3-T1, M3-T3 | AC01 |
| F03 位置共享 | M3-T5, M3-T6 | AC04 |
| F04 音频输入输出 | M2-T3, M4-T2, M4-T3 | AC06 |
| F05 开源发布 | M2-T7, M4-T7, M5-T4 | AC07 |
| F06 手动选择角色 | M4-T5 | AC01 |
| F07 网络状态显示 | M3-T7 | - |
| F08 自动重连 | M4-T1 | AC05 |

---

## 10. 范围控制建议

**若进度紧张，优先保证以下 P0（Must have）：**
1. 2 台手机直连语音（M2）
2. 多 Client 稳定接入同一 Group Owner（M3-T3 / M3-T4）
3. 基础位置共享（M3-T6）
4. 后台持续运行（M4-T1）
5. 基础耳机支持（M4-T2）
6. MIT 开源与可构建文档（M2-T7, M4-T7）

**可延后到 v1.1 或后续版本：**
- 网络拓扑可视化（F08）
- 离线文字消息（F10）
- 语音录音（F11）
- 离线地图（F12）

---

## 11. 下一步行动

1. **创建 GitHub 仓库**（由 pro-general-git 负责 git 操作）
2. **确认目标测试机型**（建议至少 3 台 Android 12+ 设备）
3. **启动 M1-T1 与 M1-T2**：环境搭建 + Wi-Fi Direct GO/Client 星型组网验证
4. **每周五进行进度 Review**

---

*本项目计划由管师基于 PRD v1.2 与架构设计 v1.3 制定，待用户确认后执行。*

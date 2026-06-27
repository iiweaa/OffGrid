# M4-T5 真机验证方案

> 验证目标：确认手动网络配置入口与故障排查提示在一加 11 / 华为 Mate 30 Pro 5G 上 Group Owner / Client 两种角色模式均可正常通话。

---

## 1. 测试环境

| 项目 | 要求 |
|------|------|
| 设备 A | 一加 11（`62978a15`） |
| 设备 B | 华为/荣耀 Mate 30 Pro 5G（`2KE0219B02018194`） |
| 应用版本 | 最新 `main` 分支 debug APK（`9d4a5ee` 及之后） |
| 网络 | 关闭移动数据与 Wi-Fi，仅保留 Wi-Fi Direct |
| 权限 | 已授予位置、附近设备、录音、通知权限 |

---

## 2. 通用准备

1. 在两台设备上安装同一版本 APK；
2. 进入 **Settings → Connection**，确认角色选择可用；
3. 确认目标机型 AP-STA 并发不支持提示显示正确。

---

## 3. 测试用例

### TC-1 Group Owner + Client 模式

| 步骤 | 设备 A | 设备 B | 预期结果 |
|------|--------|--------|----------|
| 1 | Settings → Connection → Group Owner | Settings → Connection → Client | — |
| 2 | 进入 Call，点击 Start Call | — | A 显示「Creating group…」→「Group created, waiting for peer」 |
| 3 | — | 进入 Call，点击 Start Call | B 显示「Scanning for Group Owner…」→ 连接成功 →「Connected」 |
| 4 | 双方通话 1 分钟 | 双方通话 1 分钟 | 双向语音清晰，无爆音/断续；Neighbors 列表显示对端 |
| 5 | 点击 End Call | 点击 End Call | 通话结束，状态回到 Idle |

### TC-2 Client + Group Owner 模式

| 步骤 | 设备 A | 设备 B | 预期结果 |
|------|--------|--------|----------|
| 1 | Client | Group Owner | — |
| 2 | — | 先 Start Call | B 创建 Group |
| 3 | 后 Start Call | — | A 发现并连接 B，通话正常 |

### TC-3 故障排查提示

| 场景 | 操作 | 预期提示 |
|------|------|----------|
| 权限不足 | 撤销位置权限后点击 Start Call | 「Location and Nearby devices permissions are required…」 |
| Client 未发现 GO | A 选 Client，B 不启动 | 「No Group Owner found…」 |
| GO 创建失败 | 在已限制 Wi-Fi Direct 的场景下选 GO | 「Failed to create group…」 |

---

## 4. 验收标准

- [x] TC-1 双向通话正常；
- [ ] TC-2 双向通话正常；
- [ ] TC-3 各提示文案正确、不崩溃；
- [x] 测试过程中无 ANR / Crash（TC-1 复测）。

---

## 5. 测试结果记录

| 用例 | 设备 A 角色 | 设备 B 角色 | 结果 | 备注 |
|------|-------------|-------------|------|------|
| TC-1 | GO | Client | ☑ 通过 | 一加 GO，华为 Client，Group 建立，clientCount=1，VoiceService 启动并交换音频包 |
| TC-1（复测） | GO | Client | ☑ 通过 | 2026-06-28 移除 Auto 后复测：一加 GO 复用已有 `DIRECT-AO-一加 11`，华为 Client 发现并加入，双方 LinkManager 收发语音包 |
| TC-2 | Client | GO | ☐ 未执行 | |
| TC-3 | — | — | ☐ 未执行 | |

---

## 6. Auto 模式已移除

由于目标设备上 `setDeviceName()` 反射调用未生效，且环境中存在第三方 P2P 设备（如 `MateView`），Auto 模式无法可靠识别 OffGrid 对端。为降低复杂度，**M4-T5 已移除 Auto 角色**，仅保留手动 **Group Owner / Client** 两种模式。

用户需在一台设备上选择 **Group Owner**，另一台选择 **Client**，然后分别点击 Start Call。

## 7. 历史记录：Auto 模式失败分析

### 7.1 现象

- 两台设备均选择 Auto，首次点击 Start Call 后：
  - 华为（`2KE0219B02018194`）创建 Group：`DIRECT-XQ-187******86的Mate 30`
  - 一加（`62978a15`）发现该 Group 并以 Client 身份加入，`formed=true, isGO=false`
  - 双方 VoiceService 启动，AudioEngine 开始收发 Opus 音频包
- 约 8 秒后，Group 被移除：
  - 华为侧：`AP-STA-DISCONNECTED ce:...:80:1a`，`reason 0 (UNKNOWN)`
  - 一加侧：`Group removed`，随后 `Peers found: 0`
- 再次点击 Start Call 后：
  - 一加创建 Group：`DIRECT-AO-一加 11`
  - 应用反复输出 `Establish already in progress`
  - 华为未再成功连接

### 7.2 关键日志时间线

```text
00:02:19  华为 createGroup -> DIRECT-XQ-...
00:02:22  一加作为 Client 加入 Group
00:02:23  双方 VoiceService 启动，音频通路建立
00:02:30  华为 AP-STA-DISCONNECTED（Client 掉线）
00:02:32  一加 Group removed
00:02:54  一加再次创建 Group -> DIRECT-AO-一加 11
00:03:09  应用输出 Establish already in progress（后续持续）
```

### 7.3 初步结论

- Auto 模式**能够完成初始角色协商并建立通话**，说明「扫描后退化为 GO」的策略在时序上是可行的；
- 但 Group 在建立后迅速断开，且断开后状态机进入「Establish already in progress」死锁，无法自动恢复；
- 进一步分析 `logs/m4t5-auto-retest-20260628-003327/` 发现：目标设备上 `setDeviceName()` 反射未生效，设备名仍为系统默认值，导致 Auto 无法识别 OffGrid 对端；环境中还存在 `MateView` 等第三方 P2P 设备，干扰地址排序；
- **GO/Client 角色逻辑本身正常**（TC-1 已通过），失败根因是 Auto 的「对端识别」机制。

### 7.4 已实施修复（v4）

- `WifiDirectConnector`：
  - Group 断开后 `handleConnectionLost()` 清理状态机；
  - `establish()` 取消 stale job，消除「Establish already in progress」死锁；
  - 增加 DNS-SD 服务发现（`_offgrid._tcp`），不再依赖 `setDeviceName()`；
  - Auto / GO 发现把 DNS-SD 服务 peer 与 `OffGrid-` 前缀设备名取并集；
  - 每次 establish 前调用 `removeStaleGroup()`，清理历史残留 Group；
  - 增强 peer/地址/服务日志。

### 7.5 复测要求

- 由 `pro-android-system-test` 重新执行 TC-3 至少 3 轮，成功率 ≥ 2/3 方可验收通过；
- 建议复测前在系统设置中「忘记/删除」之前的 Wi-Fi Direct Group，避免残留；
- 复测时两台设备均需解锁、应用在前台、权限已授予，并同时点击 Start Call。

---

## 8. M4-T5-FIX 回归与最终修复

### 8.1 回归现象

移除 Auto 模式后，手动 GO/Client 出现退化：

- 一加 GO：`createGroup failed: 2`（`BUSY`），因为此前测试残留的 P2P Group 未被清理；
- 华为 Client：`connect failed: 0`（`ERROR`），多由 GO 侧状态异常或 discovery 干扰引起。

### 8.2 根因

- 旧 APK 受 Gradle 增量编译影响，实际运行的仍是包含 `removeStaleGroup()` / `createGroup` 重试等 Auto 修复代码的版本；
- 即使源码回退到原始 TC-1 基线并 `clean build`，目标设备上残留的 P2P Group 仍会导致 GO 侧 `createGroup()` 返回 `BUSY`。

### 8.3 修复

在 `WifiDirectConnector.createGroupAsOwner()` 中：

1. 调用 `createGroup()` 前先检查当前是否已经是 Group Owner，若是则直接复用现有 Group；
2. 若 `createGroup()` 失败，再拉取一次 Group 信息确认是否已是 GO，是则继续等待 Client，而不是直接报错。

这样即使系统保留了上次测试的 DIRECT Group，GO 模式也不会再报 `BUSY`，且 Client 能正常发现并加入。

### 8.4 验证

2026-06-28 复测 TC-1：

- 一加（`62978a15`）选 **Group Owner**，点击 Start Call 后提示复用已有 Group；
- 华为（`2KE0219B02018194`）选 **Client**，点击 Start Call 后扫描到 GO 并连接；
- 双方进入通话界面，LinkManager 持续收发语音包，无崩溃/ANR。

## 9. 风险提示

- 手动模式要求两台设备分别选择 **Group Owner** 与 **Client**，不能同时选同一角色；
- 若系统设置中手动「忘记」了之前的 DIRECT 网络，GO 会重新创建 Group，逻辑同样成立；
- 环境中第三方 P2P 设备（如 `MateView`）不会再干扰手动模式，因为 Client 只连接标记为 `isGroupOwner` 的 peer。

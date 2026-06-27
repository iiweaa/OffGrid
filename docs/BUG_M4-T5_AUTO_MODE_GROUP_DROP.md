# Bug 记录：M4-T5 Auto 模式 Group 建立后迅速断开并重连死锁

> **Bug ID**: BUG-M4-T5-AUTO-001  
> **关联任务**: M4-T5 / M4-T5-VERIFY  
> **发现时间**: 2026-06-28  
> **发现人**: `pro-android-system-test`  
> **负责人**: `pro-android-app`（待指派确认）  
> **优先级**: P1（阻塞 M4-T5 验收）  
> **状态**: 已关闭（按产品决策移除 Auto 模式）

---

## 1. 环境信息

| 项目 | 内容 |
|------|------|
| 应用版本 | `main` 分支 debug APK（commit `de9d13c` 之后） |
| 设备 A | 一加 11（`62978a15`），Android 14 |
| 设备 B | 华为 Mate 30 Pro 5G（`2KE0219B02018194`），HarmonyOS / Android 12 |
| 网络环境 | 移动数据与 Wi-Fi 均关闭，仅 Wi-Fi Direct |
| 权限 | 已手动授予位置、附近设备、录音、通知权限 |
| 测试脚本 | `scripts/run_m4t5_verification.sh` |
| 日志目录 | `logs/m4t5-20260628-000000/`、`logs/m4t5-auto-retest-20260628-003327/`、`logs/m4t5-auto-v3-20260628-003947/` |

---

## 2. 问题描述

M4-T5 手动网络配置中的 **Auto 模式** 在真机验证时：

- 初次协商可以成功：一方自动创建 Group，另一方自动扫描并连接；
- 但 Group 建立后约 **8 秒** 即被移除；
- 断开后再次点击 Start Call，应用陷入 **「Establish already in progress」** 死锁，无法恢复。

该问题导致 Auto 模式无法稳定完成通话，阻塞 M4-T5 验收。

---

## 3. 复现步骤

1. 两台设备安装最新 debug APK；
2. 进入 **Settings → Connection**，均选择 **Auto**；
3. 进入 Call，两台设备同时（或先后）点击 **Start Call**；
4. 观察：初次可能建立 Group 并进入通话；
5. 等待 10 秒左右，通话中断，状态回到 Idle 或持续 Connecting；
6. 再次点击 Start Call，状态卡死，出现「Establish already in progress」。

**复现率**: 当前验证 1/1 失败（需修复后多轮验证确认）。

---

## 4. 期望结果

- Auto 模式与 GO/Client 模式一样，能够稳定建立 Group 并保持通话；
- 若 Group 意外断开，应用应正确清理状态并允许用户重新建立连接；
- 不应出现「Establish already in progress」死锁。

---

## 5. 实际结果

- Group 建立后 8s 内断开；
- 断开后无法重连，状态机死锁。

---

## 6. 关键日志时间线

### 6.1 设备 A（一加 11，`62978a15`）

```text
00:02:23.574  D WifiDirectConnector: Peers found: 0
00:02:24.038  D WifiDirectConnector: Peers found: 2
00:02:24.621  D WifiDirectConnector: Connection info: formed=false, isGO=false
00:02:24.623  D WifiDirectConnector: Group info: DIRECT-XQ-187******86的Mate 30 , clients=0
...
00:02:27.732  D WifiDirectConnector: Connection info: formed=true, isGO=false
00:02:27.733  D WifiDirectConnector: Group info: DIRECT-XQ-187******86的Mate 30 , clients=0
00:02:32.568  D WifiDirectConnector: Group removed
00:02:32.579  D WifiDirectConnector: Peers found: 0
...
00:02:54.845  D WifiDirectConnector: Connection info: formed=true, isGO=true
00:02:54.846  D WifiDirectConnector: Group info: DIRECT-AO-一加 11, clients=0
00:03:09.482  D WifiDirectConnector: Establish already in progress
00:03:11.599  D WifiDirectConnector: Establish already in progress
00:03:12.271  D WifiDirectConnector: Establish already in progress
00:03:12.852  D WifiDirectConnector: Establish already in progress
```

### 6.2 设备 B（华为 Mate 30 Pro 5G，`2KE0219B02018194`）

```text
00:02:19.231  I WifiP2pManager: createGroup, pid:32326
00:02:19.312  D WifiDirectConnector: Connection info: formed=true, isGO=true
00:02:19.313  D WifiDirectConnector: Group info: DIRECT-XQ-187******86的Mate 30 , clients=0
00:02:22.942  D WifiDirectConnector: Peers found: 1
00:02:23.007  D WifiDirectConnector: Group info: DIRECT-XQ-187******86的Mate 30 , clients=1
00:02:30.840  I wpa_supplicant: p2p-p2p0-12:  * reason 0 (UNKNOWN)
00:02:30.840  I wpa_supplicant: p2p-p2p0-12:  * address ce:**:**:**:80:1a
00:02:30.842  I wpa_supplicant: AP-STA-DISCONNECTED ce:**:**:**:80:1a
00:02:30.848  I wpa_supplicant: P2P: Remove client ce:**:**:**:80:1a from group
00:02:31.703  D WifiDirectConnector: Group removed
```

### 6.3 音频通路已建立（证明通话曾成功）

华为侧：

```text
00:02:23.403  D AudioEngine: Power saving mode: false
00:02:23.778  D AudioEngine: recordLoop started
00:02:27.486  D AudioEngine: playPacket len=65 decoded=320 playMax=10463
...
```

一加侧：

```text
00:02:28.206  D AudioEngine: Power saving mode: false
00:02:28.472  D AudioEngine: recordLoop started
00:02:29.030  D AudioEngine: playPacket len=62 decoded=320 playMax=11788
...
```

---

## 7. 根因分析

### 7.1 直接原因（v1/v2 修复前）

1. **Group 断连清理不彻底**：Group 被系统移除后，`WifiDirectConnector` 内部状态机未及时 reset，导致后续连接请求被误判为「已有连接在进行中」。
2. **Auto 模式重入问题**：Auto 模式下同时存在「扫描等待」与「创建 Group」两条路径，在 Group 断开后可能产生竞态，导致 `connect()` 被重复调用。

### 7.2 深层原因（v3 仍未解决）

通过分析 `logs/m4t5-auto-retest-20260628-003327/` 的系统层日志（`wpa_supplicant`）发现：

- **一加 11** 的 P2P 设备名为 `一加 11`；
- **华为 Mate 30 Pro 5G** 的 P2P 设备名为 `187******86的Mate 30 Pro 5G`；
- 环境中还存在第三个 P2P 设备 `MateView 28-5609`（华为显示器）。

这说明 `WifiP2pManager.setDeviceName()` 反射调用**在两台目标设备上均未生效**，应用无法把设备名改成 `OffGrid-` 前缀。后果：

- Auto 模式依赖 `deviceName.startsWith("OffGrid-")` 识别对端，导致 **OffGrid 对端被过滤掉**；
- 过滤失败后两台 Auto 设备都认为「没有 OffGrid  peer」，进入 Phase 3 随机退避，极易同时建组；
- 环境中的 `MateView` 也会被当作潜在 peer，进一步干扰地址排序决胜。

**结论**：Auto 模式失败的真正根因是「对端识别机制」失效，而不是 GO/Client 角色逻辑本身（TC-1 GO+Client 已通过，证明角色逻辑正确）。

### 7.3 修复方向 v4

1. **不再依赖设备名前缀** 识别 OffGrid 设备；
2. 引入 **Wi-Fi Direct DNS-SD 服务发现**（`_offgrid._tcp`），每台设备广播并发现 OffGrid 服务；
3. 保留设备名前缀作为辅助，但与 DNS-SD 取并集；
4. 在 `establish()` / `autoRole()` 开始时主动 `removeStaleGroup()`，清理历史残留 Group；
5. 增加更详细的 peer/地址/服务日志，便于真机排查。

---

## 8. 结论

由于目标设备上 `setDeviceName()` 反射调用未生效，且环境中存在第三方 P2P 设备（如 `MateView`），Auto 模式在真机上无法可靠识别 OffGrid 对端。经产品决策，**M4-T5 直接移除 Auto 角色**，仅保留手动 **Group Owner / Client** 两种模式。原 Auto 模式相关代码（DNS-SD 服务发现、地址排序决胜、随机退避等）已一并删除。

### 8.1 手动模式回归修复

移除 Auto 后，GO 模式因系统残留 P2P Group 导致 `createGroup()` 返回 `BUSY`（`2`）。`WifiDirectConnector.createGroupAsOwner()` 已增加「复用已有 GO Group」逻辑：若当前已是 Group Owner，则跳过 `createGroup()` 直接等待 Client；若 `createGroup()` 失败但确认已是 GO，也继续等待。2026-06-28 复测 TC-1（一加 GO + 华为 Client）通过。

## 9. 已实施修复

### 8.1 v1/v2/v3（已完成）

1. `WIFI_P2P_CONNECTION_CHANGED_ACTION` 中监听 `groupFormed` 从 `true` 到 `false`，调用 `handleConnectionLost()` 清理状态；
2. `establish()` 遇到 stale job 但当前未连接时主动取消旧任务，消除「Establish already in progress」死锁；
3. Auto 模式增加地址排序决胜、随机退避、`stopPeerDiscovery()`、`createGroup` 重试与失败 fallback。

### 8.2 v4（当前）

1. **DNS-SD 服务发现**：`WifiDirectConnector.start()` 注册 `_offgrid._tcp` 本地服务与响应监听器；
2. **Auto 对端识别**：`autoRole()` 先用 DNS-SD 发现 OffGrid peer，再与 `OffGrid-` 前缀设备名取并集；
3. **GO 发现也使用并集**：`discoverGroupOwner()` 把 `isOffGridPeer()` 和 `isOffGridServicePeer()` 同时纳入判断；
4. **清理残留 Group**：`establish()` 与 `autoRole()` 开始时调用 `removeStaleGroup()`；
5. **更详细日志**：peer 列表打印地址、名称、GO 标志、是否 OffGrid；本机地址与设备名在初始化后打印。

---

## 9. 验收标准

- [ ] 修复后重新执行 TC-3（Auto + Auto）至少 3 轮，成功率 ≥ 2/3；
- [ ] 任意一轮 Group 断开后，应用不崩溃、不死锁，可重新点击 Start Call 建立连接；
- [ ] TC-1 / TC-2 不受影响，保持通过；
- [ ] TC-4 故障提示用例通过。

---

## 10. 相关文件

- `app/src/main/java/com/offgrid/app/link/wifidirect/WifiDirectConnector.kt`
- `app/src/main/java/com/offgrid/app/ui/screens/CallViewModel.kt`
- `docs/M4-T5_VERIFICATION.md`
- `logs/m4t5-20260628-000000/`

---

## 11. 修复摘要

### 11.1 修改文件

- `app/src/main/java/com/offgrid/app/link/wifidirect/WifiDirectConnector.kt`
- `docs/BUG_M4-T5_AUTO_MODE_GROUP_DROP.md`
- `docs/M4-T5_VERIFICATION.md`

### 11.2 主要改动

1. **连接丢失检测**：在 `WIFI_P2P_CONNECTION_CHANGED_ACTION` 回调中比较上一次与当前 `groupFormed` 状态；若从 `true` 变为 `false`，则调用 `handleConnectionLost()` 取消当前任务、reset 状态机并将状态置为 `Idle`。
2. **取消死锁的 stale job**：`establish()` 在检测到已有活跃任务但当前并未连接时，主动取消旧任务并重新建立，避免「Establish already in progress」死锁。
3. **Auto 模式竞态缓解**：
   - `discoverGroupOwner()` 在找到 GO 或超时后调用 `stopPeerDiscovery()`，避免 discovery 干扰后续建组/连接；
   - `autoRole()` 在未发现 GO 时增加随机 backoff，并在 backoff 后再次扫描，降低两台 Auto 设备同时建组的概率。
4. **v4 根本修复：DNS-SD 服务发现**：
   - `start()` 注册 `_offgrid._tcp` 本地服务与 DNS-SD 响应监听器；
   - `discoverOffGridPeers()` 使用 `WifiP2pDnsSdServiceRequest` 发现对端，**不再依赖 `setDeviceName()`**；
   - Auto / GO 发现同时接受 `OffGrid-` 前缀设备名与 DNS-SD 服务 peer；
   - 任何 establish 流程开始前调用 `removeStaleGroup()`，清理历史残留 Group。
5. **日志增强**：peer 列表、本机地址/名称、DNS-SD 发现结果均有明确 TAG 输出。

### 11.3 L1 验证

- `./gradlew clean build` 通过（debug + release，lint 无错误）；
- debug / release APK 均已成功构建。

### 11.4 备注

- `logs/m4t5-auto-v3-20260628-003947/` 中**没有记录到实际的 establish 流程**（两台设备均未输出 `WifiDirectConnector` 的 establish/phase 日志），因此无法据此判断 v3 失败原因；可作为参考的日志是 `logs/m4t5-auto-retest-20260628-003327/`；
- Auto 模式已移除，本 Bug 随功能删除而关闭；后续验证请关注 `docs/M4-T5_VERIFICATION.md` 中的 TC-1 / TC-2。

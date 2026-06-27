# M4-T5 真机验证方案

> 验证目标：确认手动网络配置入口与故障排查提示在一加 11 / 华为 Mate 30 Pro 5G 上三种角色模式均可正常通话。

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

### TC-3 Auto + Auto 模式

| 步骤 | 设备 A | 设备 B | 预期结果 |
|------|--------|--------|----------|
| 1 | Auto | Auto | — |
| 2 | 同时点击 Start Call | 同时点击 Start Call | 一方自动创建 Group，另一方自动连接；最终通话正常 |

### TC-4 故障排查提示

| 场景 | 操作 | 预期提示 |
|------|------|----------|
| 权限不足 | 撤销位置权限后点击 Start Call | 「Location and Nearby devices permissions are required…」 |
| Client 未发现 GO | A 选 Client，B 不启动 | 「No Group Owner found…」 |
| GO 创建失败 | 在已限制 Wi-Fi Direct 的场景下选 GO | 「Failed to create group…」 |

---

## 4. 验收标准

- [ ] TC-1 双向通话正常；
- [ ] TC-2 双向通话正常；
- [ ] TC-3 至少 3 次尝试中 2 次成功；
- [ ] TC-4 各提示文案正确、不崩溃；
- [ ] 测试过程中无 ANR / Crash。

---

## 5. 测试结果记录

| 用例 | 设备 A 角色 | 设备 B 角色 | 结果 | 备注 |
|------|-------------|-------------|------|------|
| TC-1 | GO | Client | ☐ 通过 / ☐ 失败 | |
| TC-2 | Client | GO | ☐ 通过 / ☐ 失败 | |
| TC-3 | Auto | Auto | ☐ 通过 / ☐ 失败 | |
| TC-4 | — | — | ☐ 通过 / ☐ 失败 | |

---

## 6. 风险提示

- 两台设备同时选 Auto 可能都创建 Group 导致无法互通，属已知硬件/协议限制；
- 若 TC-3 成功率低于 2/3，需回到 M4-T5 优化 Auto 模式策略。

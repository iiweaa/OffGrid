# M1-T2 Wi-Fi Direct 基础能力测试记录

> 测试日期：2026-06-27  
> 设备 A：一加 11（Android 14 / ColorOS）  
> 设备 B：华为/荣耀（Android / EMUI/HarmonyOS，`2KE0219B02018194`）  
> 测试 APK：`app-debug.apk`（`WifiDirectTestActivity`）  
> 测试目标：验证 Wi-Fi Direct 建组、双机互联、AP-STA 并发可行性与 L3 连通性。

---

## 1. 测试环境

| 项 | 设备 A（一加 11） | 设备 B（华为/荣耀） |
|---|---|---|
| Android 版本 | 14 | - |
| 系统 | ColorOS | EMUI/HarmonyOS |
| Wi-Fi 状态 | 打开，未连接路由器 | 打开，未连接路由器 |
| 位置服务 | 开启 | 开启 |
| 授予权限 | NEARBY_WIFI_DEVICES、LOCATION | NEARBY_WIFI_DEVICES、LOCATION |

---

## 2. 单设备测试结果

| 测试项 | 设备 A | 设备 B |
|--------|--------|--------|
| P2P 状态检测 | ✅ `P2P state: ENABLED` | ✅ `P2P state: ENABLED` |
| 本机设备信息 | ✅ 一加 11 | ✅ （未截图记录名称） |
| Create Group | ✅ 可成为 GO | ✅ 可成为 GO |
| Remove Group | ✅ 可解散 | ✅ 可解散 |
| Discover Peers | ✅ 可扫描 | ✅ 可扫描 |

> **注意**：Create Group 在通话状态下会卡在 `CONNECTING`，结束通话后恢复正常。

---

## 3. 双设备互联测试

### 3.1 测试步骤

1. 设备 A 点击 **Create Group**，成为 Group Owner。
2. 设备 B 点击 **Discover** → **List Peers**，发现设备 A。
3. 设备 B 点击设备 A 右侧的 **Connect**。

### 3.2 结果

| 项 | 结果 |
|---|---|
| 设备 A 建组 | ✅ `SSID=DIRECT-AO-一加 11`，`pass=vq05iXHM`，`isGO=true` |
| 设备 B 发现 A | ✅ 列表显示「一加 11 (ce:06:6e:86:80:1a)」 |
| 设备 B 连接 A | ✅ 设备 B `groupFormed=true, isGO=false`，加入同一 Group |
| 双机 L3 连通（IPv4） | ✅ B 能 ping 通 A：`192.168.49.1`，RTT 20~322 ms |
| 双机 L3 连通（IPv6 link-local） | ❌ B 的 P2P 接口未分配 IPv6 地址，ping6 报 `Network is unreachable` |

接口信息：

```
# 设备 A（GO）
p2p0: inet 192.168.49.1/24
      inet6 fe80::cc06:6eff:fe86:801a/64 scope link

# 设备 B（Client）
p2p-p2p0-1: inet 192.168.49.105/24
            # 无 IPv6 地址
```

---

## 4. AP-STA 并发测试

### 4.1 测试目标

验证同一台设备能否 **同时作为 Wi-Fi Direct Group Owner 和 Client**（P2P-P2P 并发），这是多跳 Mesh 链式组网的关键假设。

### 4.2 测试步骤

1. 设备 A 为 GO，设备 B 为 Client，建立 Group。
2. 在设备 B 仍连接 A 的情况下，点击 **Create Group**，尝试让 B 同时成为 GO。

### 4.3 结果

| 项 | 结果 |
|---|---|
| B 同时成为 GO | ❌ 失败 |
| 现象 | 点击后无变化，`groupFormed=true, isGO=false` 仍保持 A 的 Group；`dumpsys wifip2p` 仍显示 `mDetailedState CONNECTED` |
| 结论 | **该华为/荣耀设备不支持通过标准 `WifiP2pManager.createGroup()` 在作为 Client 时同时创建 Group** |

> 反向测试（A 作为 Client 后再 Create Group）因需要第三台设备而未进行，但基于 Android Wi-Fi Direct 单接口设计，预期大多数手机同样不支持 P2P-P2P 并发。

---

## 5. 关键发现与风险

| ID | 发现 | 影响 | 建议 |
|---|---|---|---|
| F1 | 一加 11 通话会阻塞 P2P Group 创建 | 中 | 架构层面不影响；使用场景多为非通话状态 |
| F2 | 华为/荣耀设备 P2P 接口未分配 IPv6 地址 | **高** | 原架构假设「IPv6 link-local 通信」可能不成立，需回退到 IPv4 私有地址（192.168.49.x） |
| F3 | 目标机型不支持 P2P-P2P 并发 | **高** | 原架构假设「AP-STA 并发实现多跳」不成立，需改为 **单跳直连** 或 **时分复用** 方案 |
| F4 | Wi-Fi 开关必须打开且未连接路由器才能稳定建组 | 中 | 引导用户关闭普通 Wi-Fi 或确保未连接路由器 |
| F5 | 部分 OEM 限制 `adb shell pm grant` | 低 | 测试阶段需手动授权；Release 版本正常弹窗申请 |

---

## 6. 对架构的影响

根据本次实测，原架构中的两个关键假设需要重新审视：

1. **IPv6 link-local 通信**
   - 当前证据：至少华为/荣耀设备不分配 IPv6 地址给 P2P Client 接口。
   - 建议：改为使用 **IPv4 私有地址（192.168.49.0/24）** 进行 UDP Mesh 通信，或同时支持 IPv4/IPv6 双栈。

2. **AP-STA 并发多跳**
   - 当前证据：目标测试机不支持同时作为 GO 和 Client。
   - 建议：
     - **方案 A（推荐 MVP）**：退化为 **单跳直连**，2-5 人小队通过一台 GO + 多台 Client 直连，覆盖 30m 范围。
     - **方案 B（后续研究）**：时分复用角色切换，节点轮流当 GO/Client 转发，但延迟和稳定性风险大。
     - **方案 C（换机型）**：寻找明确支持 Wi-Fi Direct AP-STA 并发的设备（如部分 Pixel、Samsung）继续验证，但会限制用户机型。

---

## 7. 下一步行动

1. **与 PM/架构师确认**：是否接受 MVP 阶段退化为单跳直连（方案 A）。
2. **修改架构文档**：将 IPv6 link-local 改为 IPv4 私有地址方案，或在文档中说明双栈兼容。
3. **继续 M1-T3/T4**：
   - T3 改为验证 IPv4 UDP 双向通信。
   - T4 验证 Opus 编解码延迟。
4. **准备第三台设备**：如仍想验证 P2P-P2P 并发，需第三台 Android 12+ 设备。

---

## 8. 相关代码修改

- `app/src/main/AndroidManifest.xml`：`WifiDirectTestActivity` 临时设置 `android:exported="true"`，便于 adb 直接启动测试页面。
- `WifiDirectTestActivity.kt`：添加 `@SuppressLint("MissingPermission")` 通过 Lint，实际运行时已动态申请权限。

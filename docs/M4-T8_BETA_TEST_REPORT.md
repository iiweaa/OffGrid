# M4-T8 Beta 版本测试报告

> 测试时间：2026-06-28  
> 应用版本：`main` 分支 debug APK（commit `04efdb9` 及之后，BC-8 修复后为最新构建）  
> 测试人员：`pro-android-system-test`（试官）协同用户执行  
> 目标设备：一加 11（`62978a15`）、华为 Mate 30 Pro 5G（`2KE0219B02018194`）

---

## 1. 测试环境

| 项目 | 一加 11 | 华为 Mate 30 Pro 5G |
|------|---------------------|------------------------|
| 型号 | PHB110 | LIO-AN00 |
| 系统 | Android 14 | HarmonyOS / Android 12 |
| 电量起始 | 36% | 100% |
| 连接方式 | USB 调试 | USB 调试 |
| 网络 | 移动数据与 Wi-Fi 关闭 | 移动数据与 Wi-Fi 关闭 |

---

## 2. 执行摘要

本次 Beta 测试共执行 7 个用例（BC-1 ~ BC-3, BC-5 ~ BC-8）。BC-4「省电模式通话」已随 M4-T6 从产品需求中删除。BC-8 在首轮测试中发现蓝牙耳机已连接但音频未走蓝牙的问题，经定位后修复并复测通过。其余用例均一次通过。测试过程中未出现 ANR、Crash 或阻塞性连接失败。

---

## 3. 测试结果

| 用例 | 结果 | 缺陷 | 备注 |
|------|------|------|------|
| BC-1 基础通话（GO + Client） | ✅ 通过 | 无 | 双向语音清晰，Neighbors 显示对端 |
| BC-2 角色互换（Client + GO） | ✅ 通过 | 无 | 华为 GO、一加 Client，连接正常 |
| BC-3 锁屏/后台 5 分钟 | ✅ 通过 | 无 | 锁屏后语音未中断，前台服务存活 |
| BC-5 长时间稳定性 15 分钟 | ✅ 通过 | 无 | 无 ANR/Crash/断连 |
| BC-6 应用被杀后重连 | ✅ 通过 | 无 | 重开后可重新发现并连接 |
| BC-7 权限边界 | ✅ 通过 | 无 | 权限被拒提示正确，不崩溃 |
| BC-8 蓝牙耳机外设 | ✅ 通过 | 已修复，见第 4 节 | 修复后蓝牙播放/采集与拔出切换均正常 |

---

## 4. 问题记录与修复

### 4.1 问题：蓝牙耳机已连接，但通话音频未走蓝牙

**发现用例**：BC-8  
**现象**：蓝牙耳机与一加 11 已配对并连接，但建立通话后声音仍从扬声器播放，未切换到蓝牙耳机。

**根因分析**：
- `AudioEngine.initAudio()` 在初始化时强制调用 `setCommunicationDevice(speaker)` / `setSpeakerphoneOn(true)`，将音频输出锁定在扬声器；
- `AudioRouter` 虽然监听了音频设备变化并会重新路由，但其逻辑仅在「当前通信设备已是耳机」时保持耳机，否则回退到扬声器；
- 由于 `AudioEngine` 已先把通信设备设成扬声器，`AudioRouter` 未能主动选择已连接的蓝牙耳机。

**修复方案**：
- `AudioEngine` 不再强制设置扬声器，仅将 `AudioManager.mode` 设为 `MODE_IN_COMMUNICATION`；
- `AudioRouter.route()` 改为优先从 `availableCommunicationDevices` 中选择有线/蓝牙/USB 耳机，无耳机时才回退到扬声器；
- 对于 Android S 以下设备，在检测到有线或蓝牙音频设备时不强制打开扬声器。

**修改文件**：
- `app/src/main/java/com/offgrid/app/audio/AudioEngine.kt`
- `app/src/main/java/com/offgrid/app/audio/router/AudioRouter.kt`

**复测结果**：修复后 BC-8 通过，蓝牙耳机可正常播放与采集，拔出耳机后自动回退到扬声器。

---

## 5. 日志与截图

- 原始 logcat：`logs/m4t8-manual/logcat-oneplus.log`、`logs/m4t8-manual/logcat-huawei.log`
- 各用例电量与截图：`logs/m4t8-manual/bc1/` ~ `bc8/`

---

## 6. 结论与下一步

- 本次 Beta 测试 **通过**，无 P0 / P1 缺陷；
- 已修复 1 个 P2 级音频路由问题（蓝牙耳机未生效）；
- 建议在 M5 户外实测前，在更多机型上重复 BC-1 / BC-8，进一步验证蓝牙耳机的兼容性；
- M4 阶段全部任务已完成（M4-T6 省电模式已按产品决策从产品需求中删除）；M5 户外实测已取消，下一步进入 M5-T3 Bug 修复与 v1.0 Release 准备。

---

*报告由 `pro-android-system-test` 整理。*

# pro-android-system-test 项目级规则

> 本文件由 `pro-android-system-test` 维护，记录 OffGrid 项目在 Android 真机测试方面的环境、约束与执行入口。

---

## 当前任务

- **M4-T5-VERIFY**：已完成（`docs/M4-T5_VERIFICATION.md`）
- ~~**M5-T2**：户外实测执行~~（已取消）

---

## 测试环境

| 设备 | 序列号 | 角色 | 系统版本 |
|------|--------|------|----------|
| 一加 11 | `62978a15` | 主/副均可 | Android 14 |
| 华为 Mate 30 Pro 5G | `2KE0219B02018194` | 主/副均可 | HarmonyOS / Android 12 |

---

## 前置条件

1. 两台设备已通过 USB 连接并开启开发者模式 + USB 调试；
2. 设备已关闭移动数据与 Wi-Fi，仅保留 Wi-Fi Direct；
3. 屏幕常亮或设置足够长休眠时间；
4. 已授予 OffGrid 所需权限：位置、附近设备、录音、通知。

> 注：ADB 直接 `pm grant` 在两台设备上均受限，首次安装后需由测试人员在设备上手动授予权限。

---

## 常用命令

```bash
# 确认设备在线
adb devices -l

# 安装/重装 APK
adb -s 62978a15 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 2KE0219B02018194 install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb -s 62978a15 shell am start -n com.offgrid.app/.MainActivity
adb -s 2KE0219B02018194 shell am start -n com.offgrid.app/.MainActivity

# 抓取 logcat
adb -s 62978a15 logcat -v threadtime -T 0 > logs/m4t5_oneplus.log &
adb -s 2KE0219B02018194 logcat -v threadtime -T 0 > logs/m4t5_huawei.log &

# 读取电量
adb -s 62978a15 shell dumpsys battery | grep level
adb -s 2KE0219B02018194 shell dumpsys battery | grep level
```

---

## 测试脚本

- `scripts/run_m4t5_verification.sh`：M4-T5 网络验证执行脚本

脚本职责：
- 检查设备在线状态；
- 安装/更新 APK；
- 清理并启动 logcat 捕获；
- 按验证文档输出逐步操作提示；
- 在关键节点自动记录电量、截图、进程状态；
- 收集日志到 `logs/` 目录。

---

## 输出物

每次验证完成后，必须在对应验证文档中回填：

- 测试日期、固件/应用版本、环境说明；
- 各用例结果（通过/失败/阻塞）；
- 失败用例的 logcat 片段、截图、复现步骤。

---

## 相关文档

- M4-T5 验证方案：`docs/M4-T5_VERIFICATION.md`
- M5-T1 户外实测方案：`docs/M5-T1_OUTDOOR_TEST_PLAN.md`
- 任务指派：`.skill/TASKS.md`
- 进度跟踪：`docs/PROGRESS_TRACKING.md`

# M5-T3-2 Opus 原生库替换回归测试报告

> 任务：验证将 `com.github.martoreto:opuscodec` 替换为自编译 Xiph libopus + JNI 后，核心 Beta 用例仍通过。
> 测试日期：2026-06-28
> 执行人：`pro-android-system-test`
> 报告路径：`docs/M5-T3_REGRESSION_REPORT.md`

---

## 1. 测试环境

| 项目 | 设备 A | 设备 B |
|------|--------|--------|
| 型号 | 一加 11（PHB110） | 华为 Mate 30 Pro 5G（LIO-AN00） |
| 序列号 | `62978a15` | `2KE0219B02018194` |
| 系统 | Android 14 | HarmonyOS / Android 12 |
| ABI | `arm64-v8a` | `arm64-v8a` |

- 应用版本：`app-debug.apk`（M5-T3-2 替换后的最新构建）
- 网络：关闭移动数据与 Wi-Fi，一台设备作为 Wi-Fi Direct Group Owner，其余作为 Client 接入
- 权限：位置、附近设备、录音、通知已授予
- 电量：起始 ≥ 30%

---

## 2. 被测变更

- 移除依赖 `com.github.martoreto:opuscodec:v1.2.1.2`
- 新增自编译 Xiph libopus v1.4 + JNI：`libopus_jni.so`
- Kotlin 封装类：`com.offgrid.app.audio.opus.OpusEncoder` / `OpusDecoder`
- 迁移调用方：`AudioEngine.kt`、`OpusLatencyTestActivity.kt`
- ABI 过滤：`arm64-v8a`、`armeabi-v7a`

---

## 3. 测试用例与结果

| 用例 | 结果 | 缺陷等级 | 备注 |
|------|------|----------|------|
| BC-1 基础通话（GO + Client） | ✅ 通过 | — | 一加作为 Group Owner（热点），华为作为 Client 接入，双向语音清晰，Neighbors 显示对端 |
| BC-5 长时间稳定性 | ✅ 通过 | — | 15 分钟通话，无 ANR / Crash / 断连 |
| BC-8 音频外设 | ✅ 通过 | — | 蓝牙耳机播放/采集与拔出回退均正常 |

> 测试执行过程中未出现 P0 / P1 缺陷。

---

## 4. 日志与证据

- 原始 logcat：`logs/m5t3-regression-20260628-140600/`
  - `logcat-62978a15.txt`
  - `logcat-2KE0219B02018194.txt`
- 电量 / Wi-Fi 状态：`battery-*-{init,final}.txt`、`wifi-*-{init,final}.txt`
- 截图：`screenshot-*-{init,final}.png`

### 4.1 异常扫描

对 logcat 进行关键字扫描（`AndroidRuntime`、`FATAL`、`ANR`、`com.offgrid.app` 相关 Exception）：

- 未发现 OffGrid 进程 Crash 或 ANR。
- 未发现与 `libopus_jni.so` 相关的 native crash 或解码/编码错误。
- 发现少量系统侧日志（如一加相机 HAL 的 `APS_ALOG Fatal Err`、华为 Wi-Fi 扫描日志），与 OffGrid 应用无关。

---

## 5. 结论

- 自编译 libopus + JNI 方案在一加 11 / 华为 Mate 30 Pro 5G 两台 arm64 设备上通过 BC-1 / BC-5 / BC-8 回归测试。
- 替换后未引入新的稳定性或音频质量问题。
- `opuscodec` 许可风险已消除，APK 已包含 `arm64-v8a` 原生库。

---

## 6. 后续建议

1. 如后续需要进一步压缩包体或降低功耗，可评估 libopus 编译选项：
   - `OPUS_FIXED_POINT`（纯定点，部分平台更省电）
   - ARM NEON 优化（已默认启用）
   - 仅构建需要的应用模式
2. 在更多机型 / 更低 Android 版本上复测 BC-1 / BC-8，验证兼容性。
3. 将本报告链接更新到 `docs/M5-T3_OPUS_REPLACEMENT.md` 与 `docs/PROGRESS_TRACKING.md`。

---

## 7. 参考

- `docs/M5-T3_OPUS_REPLACEMENT.md`
- `docs/M5-T3_LICENSE_COMPLIANCE.md`
- `docs/M4-T8_BETA_TEST.md`
- `docs/M4-T8_BETA_TEST_REPORT.md`

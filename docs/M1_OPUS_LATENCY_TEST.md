# M1-T4 Opus 编解码集成与端到端延迟测试

## 目标

验证 Android 端 Opus 编解码库的可集成性，并测量「麦克风采集 → Opus 编码 → Opus 解码 → 扬声器播放」的端到端延迟是否满足 MVP 目标（< 200 ms）。

## 选型结论

| 候选库 | 结果 | 说明 |
|--------|------|------|
| `com.github.martoreto:opuscodec:v1.2.1.2` | **已集成** | JitPack 可达，API 简单（`OpusEncoder`/`OpusDecoder`），无需自行编译 JNI |
| `io.element.android:opusencoder` | 不适用 | 仅编码器，无法解码 |
| `kopus` / `android-opus-codec` | 未采用 | Maven Central 路径不可达或 GitHub 下载超时 |
| `rapidopus` | 未采用 | Maven Central 无可用版本 |

> 注：`opuscodec` 在 GitHub 仓库未声明 LICENSE 文件，MVP 阶段先用于技术验证，正式产品发布前需确认许可合规。

## 集成变更

- `settings.gradle.kts`：在依赖解析仓库中加入 `https://jitpack.io`
- `app/build.gradle.kts`：添加 `implementation("com.github.martoreto:opuscodec:v1.2.1.2")`
- `gradle.properties`：启用 `android.enableJetifier=true` 以兼容库内遗留 support-annotations
- `AndroidManifest.xml`：添加 `RECORD_AUDIO`、`MODIFY_AUDIO_SETTINGS`
- 新增 `OpusLatencyTestActivity.kt`：实时采集/编码/解码/播放回环，并统计延迟

## 测试方法

1. 使用 `AudioRecord` 以 16 kHz、单声道、16-bit 采集 20 ms 帧（320 samples）。
2. 每帧调用 `OpusEncoder.encode()` 编码，再调用 `OpusDecoder.decode()` 解码。
3. 解码后的 PCM 通过 `AudioTrack`（MODE_STREAM，语音属性）立即播放。
4. 记录每帧的读取耗时、编码耗时、解码耗时、写入耗时及总延迟。
5. 线程优先级设置为 `THREAD_PRIORITY_URGENT_AUDIO`。

## 测试设备

| 设备 | 序列号 | Android 版本 | 结果 |
|------|--------|--------------|------|
| 一加 11 | `62978a15` | 14 | 通过，平均延迟约 20 ms |
| 华为/荣耀 | `2KE0219B02018194` | 12 | 通过，平均延迟约 20 ms |

真机麦克风/扬声器直连测试（未使用耳机，存在轻微回声）。

> 注：华为/荣耀设备的权限弹窗由系统权限控制器处理，UI dump 无法读取 Compose 内部文本，但 logcat 中的 `OpusLatency` 日志确认编解码与延迟正常。

## 测试结果

### 纯编解码基准

```
Synthetic codec benchmark: 274.7 ms for 1000 frames (0.275 ms/frame)
```

纯 Opus 编解码耗时约 **0.28 ms/帧**，远低于 20 ms 帧长。

### 端到端延迟（一加 11）

在约 60 秒连续运行后，Activity 界面显示：

```
Avg 20.0 ms (codec 2.0 ms)
read 0.4 write 17.5 ms
Range 0.4 - 165.6 ms
```

- **平均端到端延迟**：约 **20 ms**
- **编解码耗时**：约 **2 ms**
- **读取/写入 I/O 耗时**：读取约 0.4 ms，写入平均约 17.5 ms（`AudioTrack` 流式写入会在缓冲区满时阻塞）
- **最大延迟**：约 **166 ms**

### 端到端延迟（华为/荣耀）

logcat 连续采样显示：

```
frame=560 read=17992us enc=1860us dec=329us write=71us latency=20509us
frame=580 read=18426us enc=980us dec=207us write=54us latency=19875us
...
frame=1160 read=19472us enc=439us dec=88us write=28us latency=20109us
```

- **平均端到端延迟**：约 **20 ms**
- **编解码耗时**：约 **1–3 ms**
- **读取/写入 I/O 耗时**：读取约 **18 ms**，写入 < 0.1 ms（与一加 11 的分布相反，体现不同厂商音频 HAL 的调度差异）
- **最大延迟**：约 **29 ms**

### 结论

两台目标机型端到端平均延迟均 **< 200 ms**，满足 M1-T4 验收标准。最大延迟偶有 130–180 ms 的尖峰（主要出现在一加 11 的 `AudioTrack.write()` 阻塞），仍在 200 ms 阈值内。

## 已知问题与后续优化

1. **arm64 原生库缺失**：`opuscodec` AAR 未提供 `arm64-v8a` 的 `libsenz.so`，当前依赖设备的 32-bit 兼容模式运行。长期建议替换为带 arm64 原生库的方案（如自行编译 libopus + JNI）。
2. **写入阻塞尖峰**：`AudioTrack` 流式写入偶发 100+ ms 阻塞。后续可尝试：
   - 使用更大的播放缓冲 + 非阻塞写入；
   - 或采用 AAudio/OpenSL ES 低延迟音频路径。
3. **许可合规**：需确认 `opuscodec` 上游许可，避免产品发布风险。

## 相关文件

- `app/src/main/java/com/offgrid/app/OpusLatencyTestActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`

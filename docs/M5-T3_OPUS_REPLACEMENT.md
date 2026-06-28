# M5-T3-2 arm64 Opus 原生库替换方案

> 任务 ID：M5-T3-2  
> 负责人：`pro-android-app` / `pro-c-cpp`  
> 依赖：M5-T3-1（许可合规审查报告）  
> 目标：将 `com.github.martoreto:opuscodec` 替换为许可证清晰、支持 `arm64-v8a` 的 Opus 方案。

---

## 1. 结论

**推荐方案：自编译 libopus + JNI**

- 许可证：Xiph Opus 为 BSD-3-Clause，与 MIT 项目完全兼容；
- ABI：同时构建 `arm64-v8a` 与 `armeabi-v7a`；
- 工作量：中等（约 1~2 天），主要改动集中在 `AudioEngine.kt`、`build.gradle.kts`、新增 JNI 封装；
- 风险：低，API 稳定，已有成熟参考实现。

---

## 2. 当前 `opuscodec` 使用分析

### 2.1 依赖声明

```kotlin
// app/build.gradle.kts
implementation("com.github.martoreto:opuscodec:v1.2.1.2")
```

### 2.2 Kotlin 层调用点

文件：`app/src/main/java/com/offgrid/app/audio/AudioEngine.kt`

| 调用 | 当前 API（`com.score.rahasak.utils`） | 替换后 API（自定义 JNI） |
|------|--------------------------------------|--------------------------|
| 创建编码器 | `OpusEncoder()` | `OpusEncoder()` |
| 初始化编码器 | `init(SAMPLE_RATE, CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP)` | `init(SAMPLE_RATE, CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP)` |
| 设置码率 | `setBitrate(bitrate)` | `setBitrate(bitrate)` |
| 设置复杂度 | `setComplexity(complexity)` | `setComplexity(complexity)` |
| 编码 | `encode(pcmShorts, FRAME_SAMPLES, opusOut)` | `encode(pcmShorts, FRAME_SAMPLES, opusOut)` |
| 创建解码器 | `OpusDecoder()` | `OpusDecoder()` |
| 初始化解码器 | `init(SAMPLE_RATE, CHANNELS)` | `init(SAMPLE_RATE, CHANNELS)` |
| 解码 | `decode(frame, outShorts, FRAME_SAMPLES)` | `decode(frame, outShorts, FRAME_SAMPLES)` |
| 释放句柄 | `close()` | `release()`（并保留 `close()` 别名兼容旧调用） |
| 句柄校验 | 反射检查 `address` 字段 | 包装类内部直接持有 native 指针 |

### 2.3 关键约束

- 采样率：16 kHz
- 声道：单声道
- 帧长：20 ms（320 samples）
- 应用类型：`OPUS_APPLICATION_VOIP`
- 码率：24 kbps（正常）/ 12 kbps（省电模式，已删除但代码保留）
- 复杂度：5（正常）/ 3（省电模式）

---

## 3. 替换方案设计

### 3.1 目录结构

```
app/
├── build.gradle.kts              # 添加 externalNativeBuild、ndk 版本、ABI 过滤
├── src/
│   └── main/
│       ├── cpp/                  # 新增
│       │   ├── CMakeLists.txt
│       │   ├── opus/             # libopus 源码子模块或预下载源码
│       │   │   └── ...
│       │   └── opus_jni.cpp      # JNI 封装
│       └── java/com/offgrid/app/audio/
│           ├── AudioEngine.kt    # 替换 OpusEncoder/OpusDecoder 导入与调用
│           └── opus/
│               ├── OpusEncoder.kt
│               └── OpusDecoder.kt
```

### 3.2 JNI 封装 API

```cpp
// opus_jni.cpp
JNIEXPORT jlong JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeInit(
    JNIEnv*, jobject, jint sampleRate, jint channels, jint application);

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeSetBitrate(
    JNIEnv*, jobject, jlong handle, jint bitrate);

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeSetComplexity(
    JNIEnv*, jobject, jlong handle, jint complexity);

JNIEXPORT jint JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeEncode(
    JNIEnv*, jobject, jlong handle, jshortArray pcm, jint frameSize, jbyteArray out);

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeDestroy(
    JNIEnv*, jobject, jlong handle);

// Decoder 对应 init / decode / destroy
```

### 3.3 Kotlin 包装类

```kotlin
package com.offgrid.app.audio.opus

class OpusEncoder {
    private var handle: Long = 0

    fun init(sampleRate: Int, channels: Int, application: Int): Boolean {
        handle = nativeInit(sampleRate, channels, application)
        return handle != 0L
    }

    fun setBitrate(bitrate: Int) = nativeSetBitrate(handle, bitrate)
    fun setComplexity(complexity: Int) = nativeSetComplexity(handle, complexity)
    fun encode(pcm: ShortArray, frameSize: Int, out: ByteArray): Int =
        nativeEncode(handle, pcm, frameSize, out)

    fun release() { if (handle != 0L) { nativeDestroy(handle); handle = 0 } }
    fun close() = release()

    private external fun nativeInit(...): Long
    // ...
}
```

### 3.4 CMake 配置要点

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("offgrid_opus")

# 引入 libopus 源码（建议使用子模块或预下载）
add_subdirectory(opus)

add_library(opus_jni SHARED opus_jni.cpp)
target_link_libraries(opus_jni opus)
```

### 3.5 `build.gradle.kts` 改动

```kotlin
android {
    // ...
    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags("")
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs.pickFirsts += listOf("**/libopus_jni.so")
    }
}

dependencies {
    // 移除
    // implementation("com.github.martoreto:opuscodec:v1.2.1.2")
}
```

---

## 4. 迁移步骤

1. **环境准备**：安装 Android NDK（25.x / 26.x）与 CMake 3.22+；
2. **源码准备**：下载 Xiph libopus v1.4 源码到 `app/src/main/cpp/opus`；
3. **JNI 实现**：编写 `opus_jni.cpp` 与 Kotlin 包装类；
4. **构建配置**：更新 `build.gradle.kts` 与 `CMakeLists.txt`；
5. **替换调用**：在 `AudioEngine.kt` 与 `OpusLatencyTestActivity.kt` 中替换 `com.score.rahasak.utils` 导入与 API；
6. **清理旧依赖**：移除 `opuscodec` 依赖及 `support-annotations` 传递依赖；
7. **构建验证**：执行 `./gradlew clean build`；
8. **真机回归**：在一加 11 / 华为 Mate 30 Pro 5G 上执行 BC-1 / BC-5 / BC-8 核心用例。

---

## 5. 实施状态

- [x] 环境准备：NDK `25.1.8937393` / `26.1.10909125`、CMake `3.22.1` 可用；
- [x] libopus v1.4 源码已解压到 `app/src/main/cpp/opus/`；
- [x] `opus_jni.cpp`、`OpusEncoder.kt`、`OpusDecoder.kt` 已完成；
- [x] `app/build.gradle.kts` 已启用 CMake 并移除 `opuscodec` 依赖；
- [x] `AudioEngine.kt` 与 `OpusLatencyTestActivity.kt` 已迁移到新 JNI 封装；
- [x] `./gradlew clean build` 通过，APK 同时包含 `arm64-v8a` 与 `armeabi-v7a` 的 `libopus_jni.so`；
- [x] 真机 BC-1 / BC-5 / BC-8 回归通过（报告见 `docs/M5-T3_REGRESSION_REPORT.md`）；
- [x] 更新 `README.md` / `BUILD.md` / `DEVELOPER_GUIDE.md` / `ARCHITECTURE.md` 中 Opus 依赖与构建说明。

## 6. 原生库产物（验证）

```
app/build/intermediates/merged_native_libs/debug/out/lib/arm64-v8a/libopus_jni.so   1.3M
app/build/intermediates/merged_native_libs/debug/out/lib/armeabi-v7a/libopus_jni.so 1.1M
app/build/intermediates/merged_native_libs/release/out/lib/arm64-v8a/libopus_jni.so  1.6M
app/build/intermediates/merged_native_libs/release/out/lib/armeabi-v7a/libopus_jni.so 1.2M
```

## 7. 风险与应对

| 风险 | 概率 | 影响 | 应对策略 |
|------|------|------|----------|
| libopus 编译失败或 ABI 不兼容 | 中 | 高 | 使用稳定版本（1.4/1.5），先在 `arm64-v8a` 上验证 |
| 替换后音质/延迟劣化 | 低 | 高 | 保持相同参数（16kHz、VOIP、24kbps），执行 BC-1/BC-5/BC-8 回归 |
| NDK/CMake 环境缺失阻塞 | 中 | 中 | 由环境管理员安装 NDK 26.x + CMake 3.22+ |
| 省电模式代码引用 BITRATE_POWER_SAVING 等常量 | 低 | 低 | 保留常量但移除 UI 开关；封装类仍支持动态码率 |

---

## 8. 验收标准

- [x] `./gradlew clean build` 通过；
- [x] APK 包含 `arm64-v8a/libopus_jni.so` 与 `armeabi-v7a/libopus_jni.so`；
- [x] `AudioEngine.kt` 与 `OpusLatencyTestActivity.kt` 不再引用 `com.score.rahasak.utils`；
- [x] `app/build.gradle.kts` 不再依赖 `opuscodec`；
- [x] 真机 BC-1 / BC-5 / BC-8 回归通过；
- [x] 更新 `README.md`、`BUILD.md`、`DEVELOPER_GUIDE.md`、`ARCHITECTURE.md` 中关于 Opus 依赖与构建说明。

---

## 9. 下一步

1. 将 `docs/M5-T3_REGRESSION_REPORT.md` 链接同步到 `docs/PROGRESS_TRACKING.md`；
2. 根据发布计划决定是否进一步调优 libopus 编译选项（如 `OPUS_FIXED_POINT`、NEON 等）；
3. 后续在更多机型上复测 BC-1 / BC-8，验证 libopus JNI 兼容性。

---

## 10. 参考

- `docs/M5-T3_LICENSE_COMPLIANCE.md`
- `docs/M5-T3_RELEASE_POLISH_PLAN.md`
- `docs/M1_OPUS_LATENCY_TEST.md`
- `docs/M2_VOICE_PIPELINE.md`
- `app/src/main/java/com/offgrid/app/audio/AudioEngine.kt`
- Xiph Opus 源码：`https://gitlab.xiph.org/xiph/opus`

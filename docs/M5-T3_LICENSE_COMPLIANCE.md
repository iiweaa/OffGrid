# M5-T3-1 Opus 依赖许可合规审查报告

> 任务 ID：M5-T3-1  
> 负责人：`pro-general-pm`  
> 审查日期：2026-06-28  
> 结论：**`com.github.martoreto:opuscodec` 存在许可风险，建议替换**

---

## 1. 审查范围

本次审查覆盖 `app/build.gradle.kts` 中声明的所有运行时依赖，重点评估其与 OffGrid（MIT 协议）发布时的兼容性。

---

## 2. 依赖许可清单

| 依赖 | 版本 | 许可证 | 与 MIT 兼容性 | 风险等级 | 说明 |
|------|------|--------|---------------|----------|------|
| `androidx.core:core-ktx` | 1.12.0 | Apache-2.0 | ✅ 兼容 | 低 | Android Jetpack 官方库 |
| `androidx.lifecycle:lifecycle-*` | 2.6.2 | Apache-2.0 | ✅ 兼容 | 低 | Android Jetpack 官方库 |
| `androidx.activity:activity-compose` | 1.8.0 | Apache-2.0 | ✅ 兼容 | 低 | Android Jetpack 官方库 |
| `androidx.navigation:navigation-compose` | 2.7.5 | Apache-2.0 | ✅ 兼容 | 低 | Android Jetpack 官方库 |
| `androidx.compose:*` (BOM 2023.08.00) | 2023.08.00 | Apache-2.0 | ✅ 兼容 | 低 | Android Compose 官方库 |
| `com.github.martoreto:opuscodec` | v1.2.1.2 | **未声明** | ⚠️ **不兼容/不确定** | **高** | 仓库无 LICENSE 文件，POM 无 `<licenses>` 节点 |
| `com.android.support:support-annotations` | 27.0.2（传递依赖） | Apache-2.0 | ✅ 兼容 | 低 | `opuscodec` 的传递依赖 |
| `junit:junit` | 4.13.2 | EPL-1.0 | ✅ 兼容（仅测试） | 低 | 仅测试依赖，不参与发布 |
| `androidx.test.ext:junit` | 1.1.5 | Apache-2.0 | ✅ 兼容（仅测试） | 低 | 仅测试依赖 |
| `androidx.test.espresso:espresso-core` | 3.5.1 | Apache-2.0 | ✅ 兼容（仅测试） | 低 | 仅测试依赖 |

---

## 3. 高风险依赖详细分析

### 3.1 `com.github.martoreto:opuscodec:v1.2.1.2`

**问题描述**

- GitHub 仓库 `martoreto/opuscodec` 未包含 `LICENSE` 文件；
- JitPack POM（`opuscodec-v1.2.1.2.pom`）未声明 `<licenses>` 节点；
- README 未提及任何许可条款；
- 该仓库自 2017 年后基本未更新，联系作者获得授权的可能性较低。

**法律风险**

根据 GitHub 默认规则与多数司法管辖区解释：

> 没有许可证的代码默认受版权保护，**他人无权复制、分发或创建衍生作品**。

OffGrid 计划以 MIT 协议在 GitHub 发布 APK 与源代码。将 `opuscodec` AAR 打包进 APK 并在仓库中引用，可能构成未经授权的分发/改编，存在被主张侵权的风险。

**技术风险**

- 该库未提供 `arm64-v8a` 原生库，当前依赖设备 32-bit 兼容模式运行（见 `docs/M1_OPUS_LATENCY_TEST.md` 已知问题）；
- 长期维护与更新不可预期。

---

## 4. 可行的应对策略

| 策略 | 说明 | 风险 | 建议 |
|------|------|------|------|
| **A. 替换为自编译 libopus + JNI（推荐）** | 使用 Xiph 官方 Opus（BSD-3-Clause），自行编写 Android JNI 封装 | 实施工作量中等；可完全控制许可证与 ABI | **首选** |
| **B. 使用其他有明确许可证的 Android Opus 封装库** | 寻找并评估社区中许可证清晰的封装方案 | 需要重新评估 API、稳定性、arm64 支持 | 备选 |
| **C. 保留 `opuscodec` 并添加风险说明** | 在 README 中声明该依赖许可证不明 | 仍可能构成侵权，无法根本消除风险 | 不推荐 |
| **D. 联系作者获取授权** | 向 `martoreto` 发送邮件/PR 请求添加 MIT/Apache 许可证 | 作者多年未活跃，成功概率低 | 可并行尝试，但不应阻塞发布 |

---

## 5. 结论

1. **除 `opuscodec` 外，所有依赖许可证均与 MIT 项目兼容。**
2. **`com.github.martoreto:opuscodec` 存在显著的许可合规风险**，不建议在 v1.0 Release 中继续使用。
3. **推荐策略 A**：替换为自编译 libopus + JNI，同时解决许可证与 arm64 原生库缺失问题。
4. 若 M5-T3-2 评估后证明替换工作量过大，可退而求其次选择策略 B；策略 C 仅作为临时记录，不应作为最终发布方案。

---

## 6. 下一步

- 触发 **M5-T3-2**：`pro-android-app` / `pro-c-cpp` 基于本报告进行 Opus 替换方案评估与实施；
- 输出 `docs/M5-T3_OPUS_REPLACEMENT.md`；
- 替换完成后执行回归测试（至少覆盖 Beta 核心用例 BC-1 / BC-5 / BC-8）。

---

## 7. 参考

- `app/build.gradle.kts`
- `docs/M1_OPUS_LATENCY_TEST.md`
- `docs/M2_VOICE_PIPELINE.md`
- `docs/M5-T3_RELEASE_POLISH_PLAN.md`
- JitPack POM：`https://jitpack.io/com/github/martoreto/opuscodec/v1.2.1.2/opuscodec-v1.2.1.2.pom`
- GitHub 仓库：`https://github.com/martoreto/opuscodec`（无 LICENSE 文件）

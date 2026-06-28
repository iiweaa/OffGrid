# OffGrid v1.0.0 Release Draft

## 发布状态

- **Tag**：`v1.0.0` 已推送至 `origin/main`（commit `d7bf5cc`）
- **APK**：`app/build/outputs/apk/release/app-release.apk`
- **APK SHA256**：`b58b79dcd80cdf636f3e3a0b6c436f285f939ead4459e2559c03210d2b6f8278`
- **APK 大小**：约 36 MB
- **签名**：v2 签名已验证
- **GitHub Release 页面**：待创建（缺少 API Token）

## 版本范围（已冻结）

v1.0.0 包含以下已完成内容：

- Wi-Fi Direct Group Owner / Client 手动组网
- 全双工语音群聊（Opus 16 kHz 单声道 24 kbps）
- 队友相对方位与距离共享
- 有线/蓝牙耳机自动路由与按键控制
- 前台服务后台保活
- 自编译 Xiph libopus v1.4 + JNI，支持 arm64-v8a / armeabi-v7a
- MIT 开源、BUILD.md / DEVELOPER_GUIDE.md / ARCHITECTURE.md 等文档

**明确不包含在 v1.0**：

- 多跳中继 / 去中心化 mesh
- 户外实测
- 省电模式
- PRD v1.2 中提出的「星型单跳」需求变更（归入 v1.1 规划）

## 发布步骤（待执行）

1. 在 GitHub 创建 Release：`https://github.com/iiweaa/OffGrid/releases/new?tag=v1.0.0`
2. Title：`OffGrid v1.0.0`
3. 将 `CHANGELOG.md` 内容贴入 Release notes
4. 上传 `app/build/outputs/apk/release/app-release.apk`
5. 确认 Release 发布

## 校验命令

```bash
# 验证签名
$ANDROID_SDK/build-tools/34.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# 校验 SHA256
sha256sum app/build/outputs/apk/release/app-release.apk
```

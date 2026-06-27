# 贡献指南

感谢你对 OffGrid 的兴趣！本仓库欢迎 Issue、Pull Request 和户外实测数据。

## 开发环境

- JDK 17
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK API 34 / build-tools 34.0.0
- 至少两台 Android 12+ 真机（用于 Wi-Fi Direct 与语音测试）

## 构建

```bash
./gradlew assembleDebug
```

## 提交 Issue

提交前请先搜索现有 Issue。我们提供了多种模板：

- 🐛 Bug 报告
- ✨ 功能请求
- 📱 设备兼容性报告
- 🏔️ 户外测试报告

## 提交 Pull Request

1. Fork 本仓库并从 `main` 切出分支。
2. 一个 PR 聚焦一个变更点，避免大包大揽。
3. 本地通过 `./gradlew test` 与 `./gradlew assembleDebug`。
4. 如修改了用户可见行为，同步更新 `docs/` 与 `README.md`。
5. 填写 PR 模板，说明测试机型与 Android 版本。

## 代码风格

- Kotlin 官方风格
- 新增公开 API 需添加简短 KDoc
- 避免在主线程执行阻塞 I/O 或长时间初始化

## 测试

- 单元测试：`./gradlew test`
- 真机测试：优先在目标机型（一加 11、华为/荣耀等）验证 Wi-Fi Direct 建组与语音通话

## 许可

通过提交 PR，你同意你的贡献在 [MIT License](./LICENSE) 下发布。

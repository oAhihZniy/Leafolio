# 一页 · Leafolio

一页是一款 Android 日记应用，采用本地优先设计。无需账号或网络即可记录日常；日记和照片保存在设备上，并可通过完整备份或通用导出带走。

## 当前版本

- 版本：`0.2.0`（`versionCode` 2）
- 平台：Android 8.0 及以上
- 状态：首版功能开发与当前设备验收已完成；生产签名和面向所有机型的发布验证尚未完成。
- 发布：[`v0.2.0 测试版`](https://github.com/oAhihZniy/Leafolio/releases/tag/v0.2.0)

## 主要功能

- 日记时间流、草稿、自动保存、编辑撤销与重做
- 日期、心情、标签、收藏、置顶、日历回看与组合搜索
- 照片导入、排序、查看、分享及照片质量设置
- 回收站、完整备份与恢复、按全部/日期范围/指定记录导出
- 深浅主题、字号、应用锁、隐私遮挡和可选提醒

数据默认只保存在本机，不提供云同步；本地数据和备份不加密。换机前请在应用内创建完整备份，并将备份文件保存到其他位置。

## 开发

工程使用 Kotlin、Jetpack Compose、Room 和 DataStore，最低 Android API 26，编译/目标 API 36。无需 Android Studio，可用 Windows PowerShell 和 Android 命令行工具开发。

准备环境和工具位置见 [DEVELOPMENT.md](DEVELOPMENT.md)。载入本机工具环境后：

```powershell
. .\scripts\Enter-Dev.ps1
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

APK 输出到 `app\build\outputs\apk\debug\app-debug.apk`。该文件是调试签名构建，仅用于开发和测试，不用于正式商店发布。更多测试与设备验收记录见 [ACCEPTANCE.md](ACCEPTANCE.md)，功能和设计基线见 [REQUIREMENTS.md](REQUIREMENTS.md)，阶段进度见 [PROGRESS.md](PROGRESS.md)。

## 仓库说明

- 应用源码、数据库迁移和自动化测试位于 `app/`。
- Android 工具链、Gradle 缓存、构建产物、本机签名文件和真机验收临时文件不提交。
- 用户个人日记数据库、备份文件和照片不属于本仓库内容。
- 仓库尚未指定开源许可证；公开可见不代表授予复制、修改或再分发许可。

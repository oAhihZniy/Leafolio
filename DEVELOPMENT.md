# 开发环境

Leafolio 使用 Windows PowerShell、JDK 17、Android SDK 命令行工具和 Gradle，不要求安装 Android Studio。

## 工具版本

| 工具 | 版本 |
| --- | --- |
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin / Compose 插件 | 2.2.21 |
| Compose BOM | 2025.10.01 |
| Android 编译/目标 SDK | API 36 |
| Android 最低版本 | API 26（Android 8.0） |
| Android Build Tools | 35.0.0 |

## 安装工具

在仓库根目录运行：

```powershell
.\scripts\Setup-Dev.ps1
```

安装脚本默认把 SDK、Gradle 和缓存放在仓库下被 Git 忽略的 `.local/tools`。如果 `.local/tools-root.txt` 存在，脚本会优先使用其中的本机工具目录；该配置文件不会提交到仓库。如果仓库位于系统盘，或希望将工具放到其他位置，请显式指定不在系统盘的工具目录：

```powershell
.\scripts\Setup-Dev.ps1 -ToolsRoot '<工具目录>' -SourceJdk '<已有的 JDK 17 目录>'
```

若当前环境已通过 `JAVA_HOME` 或 `PATH` 找到 JDK 17，可省略 `-SourceJdk`。安装脚本会下载 Android 命令行工具及所需 SDK，并校验 Gradle 分发包的 SHA-256。

脚本只为当前 PowerShell 进程设置工具路径和临时目录，不修改系统级 PATH。需要避免系统盘写入时，请将工具目录放在其他可写磁盘，并留意 Windows 自身的系统日志、分页文件等不受项目脚本控制。

## 构建与测试

每次打开新的 PowerShell 后载入工具环境：

```powershell
. .\scripts\Enter-Dev.ps1
java -version
gradle --version
adb version
```

运行单元测试、构建调试 APK 和 Lint：

```powershell
gradle.bat --no-daemon --console=plain testDebugUnitTest assembleDebug lintDebug
```

或使用构建脚本：

```powershell
.\scripts\Build-App.ps1
```

调试 APK 输出到 `app\build\outputs\apk\debug\app-debug.apk`。构建并安装到已授权的 Android 设备：

```powershell
.\scripts\Build-App.ps1 -Install
```

环境烟雾测试脚本为 `scripts\Test-Dev.ps1`，只验证基础工具链，不启动产品界面。Android 真机需要启用开发者选项和 USB 调试，并在设备上授权当前电脑。

## 签名与发布

当前版本为 `0.2.0`（`versionCode` 2）。本地构建和 GitHub Release 中提供的是 debug 签名 APK，仅供试用；正式发布前需要配置受保护的 release 签名、重新构建并执行发布验证。不要提交签名密钥或本地密钥配置。

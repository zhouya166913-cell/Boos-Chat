# Flutter App 项目说明

当前目录是企业管理助手的正式 Flutter App 工程：

```text
boss-chat-app
```

技术栈：

```text
Flutter + Dart
```

当前先以 Android / iOS 为主，鸿蒙后续再单独适配。

## 当前已经具备

- `android/`、`ios/`、`web/` 平台目录
- 登录页
- 登录成功后的占位首页
- 与后端 `/api/auth/login`、`/api/auth/me`、`/api/auth/logout` 的联调代码
- Flutter Web 调试端口 `9999`

## 当前开发环境状态

当前机器已经安装：

- Flutter 3.41.9 stable
- Android Studio
- Android SDK 36.1.0
- Android SDK Build-Tools 35.0.0
- Chrome 调试环境

当前这台机器上的 Android SDK 本地路径：

```text
C:\Users\Lenovo\AppData\Local\Android\Sdk
```

> 这是当前电脑的本地环境路径，换机器后需要按新电脑上的 Android SDK 实际安装位置调整。

`flutter doctor` 中仅剩 Windows 桌面端所需的 Visual Studio 未安装。  
这不会影响当前 Android / Web 调试；如果后续确实要开发 Windows 桌面端，再补装 Visual Studio 与 “Desktop development with C++” 工作负载即可。

## 依赖安装

```powershell
cd D:\LanTu\life-cycle-management-system\bossChat\boss-chat-app
flutter pub get
```

如果当前终端暂时还识别不到 `flutter` 命令，可以直接使用：

```powershell
& "D:\flutter\flutter_windows_3.41.9-stable\flutter\bin\flutter.bat" pub get
```

## Flutter Web 调试

```powershell
flutter run -d chrome --web-port=9999 --dart-define=API_BASE_URL=http://localhost:9090/api
```

默认访问：

```text
http://localhost:9999
```

> `9999` 只是 Flutter Web 调试端口。真正安装到手机上的原生 App，本身没有“启动在 9999 端口”这回事。

## Android 调试

更完整的 Android Studio 启动、模拟器创建、设备选择与联调说明，统一见：

```text
..\02-启动与联调\Flutter App 安卓启动与联调说明.md
```

### 连接真机或启动模拟器后

```powershell
flutter devices
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:9090/api
```

说明：

- Android 模拟器访问电脑本机服务时，通常使用 `10.0.2.2`
- Android 真机调试时，应把接口地址改成电脑在局域网中的可访问地址

### Android Studio 运行配置

如果在 Android Studio 中打开 `lib/main.dart` 后，右上角仍然是：

```text
Add Configuration
```

且运行按钮是灰色，通常表示 IDE 还没有生成 Flutter 运行配置。处理顺序：

1. 确认 Flutter / Dart 插件已安装并启用
2. 确认 Flutter SDK 路径：

```text
D:\flutter\flutter_windows_3.41.9-stable\flutter
```

3. 如仍未自动生成配置，手动新增 Flutter 配置：

```text
Name: boss-chat-app
Dart entrypoint: lib/main.dart
Working directory: D:\LanTu\life-cycle-management-system\bossChat\boss-chat-app
```

如果 Android Studio 顶部设备下拉框里只有 `Windows / Chrome / Edge`，或点击 `Restart Flutter Daemon` 后仍显示 `<no devices>`，说明当前还没有可用的 Android 模拟器或 Android 真机。  
这类问题也请直接参考独立文档：

```text
..\02-启动与联调\Flutter App 安卓启动与联调说明.md
```

如果 Android 构建阶段报出与 Gradle 压缩包或 `Build-Tools 35.0.0` 相关的 ZIP 错误，也统一参考上述安卓联调文档中的“Android 构建依赖说明”和“常见问题”。

## iOS 调试说明

Flutter 代码可以继续在当前项目中维护，但真正的 iOS 编译、签名与上架需要 macOS + Xcode 环境。

## 默认登录账号

```text
账号：admin
密码：Admin@123
```

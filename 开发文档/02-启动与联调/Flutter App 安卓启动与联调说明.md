# Flutter App 安卓启动与联调说明

本文档专门说明 `boss-chat-app` 在 Android Studio 中如何以 Android 方式启动、如何创建模拟器，以及如何与本机后端联调。

适用场景：

- 使用 Android Studio 开发 Flutter App
- 使用 Android 模拟器或 Android 真机运行 App
- 需要调用本机启动的后端服务 `http://localhost:9090`

---

## 一、当前本机环境

当前开发环境已具备：

- Flutter 3.41.9 stable
- Android Studio
- Android SDK 36.1.0
- Android SDK Build-Tools 35.0.0

本机 Android SDK 路径：

```text
C:\Users\Lenovo\AppData\Local\Android\Sdk
```

本机 Flutter SDK 路径：

```text
D:\flutter\flutter_windows_3.41.9-stable\flutter
```

本机 Dart SDK 路径：

```text
D:\flutter\flutter_windows_3.41.9-stable\flutter\bin\cache\dart-sdk
```

---

## 二、首次打开项目时的准备

在 Android Studio 中打开：

```text
D:\LanTu\life-cycle-management-system\bossChat\boss-chat-app
```

第一次进入项目后，先在底部终端执行：

```powershell
flutter pub get
```

如果 Android Studio 提示：

```text
Dart SDK is not configured
```

则进入 Dart 设置，把 SDK 路径指向：

```text
D:\flutter\flutter_windows_3.41.9-stable\flutter\bin\cache\dart-sdk
```

如果右上角仍显示：

```text
Add Configuration
```

并且运行按钮是灰色，可手动新建一个 Flutter 运行配置：

```text
Name: boss-chat-app
Dart entrypoint: lib/main.dart
Working directory: D:\LanTu\life-cycle-management-system\bossChat\boss-chat-app
```

---

## 三、创建 Android 模拟器

### 1. 打开 Device Manager

在 Android Studio 中打开：

```text
View -> Tool Windows -> Device Manager
```

然后选择：

```text
Add Device / Create Virtual Device
```

### 2. 推荐设备

当前开发阶段可直接选择：

```text
Pixel 8
```

### 3. 推荐系统镜像

日常开发优先选择稳定镜像，例如：

```text
Google Play Intel x86_64 Atom System Image
API 36.0
```

不建议把下面这种镜像作为第一个日常开发模拟器：

```text
Pre-Release 16 KB Page Size ...
```

原因：

- 普通稳定镜像更适合日常启动、页面调试和登录联调
- `Pre-Release` 镜像更适合后续专门做新版本兼容性验证

### 4. 左下角两个复选框的含义

```text
Show system images with SDK extensions
```

表示显示带 SDK 扩展能力的系统镜像。普通开发阶段一般不用勾选。

```text
Show unsupported system images
```

表示把当前不推荐或不适配的系统镜像也列出来。首次创建模拟器时一般也不用勾选。

---

## 四、正确选择运行设备

当前 `boss-chat-app` 已具备：

```text
android/
ios/
web/
```

但当前并没有：

```text
windows/
```

因此：

- 调试 App 时，应选择 Android 模拟器或 Android 真机
- 不要把 `Windows (desktop)` 当成当前 App 的正常运行目标
- `Restart Flutter Daemon` 不是设备，它只是重启 Flutter 设备发现服务

如果设备下拉框中只有：

```text
Windows (desktop)
Chrome (web)
Edge (web)
```

说明当前还没有可用的 Android 模拟器或 Android 真机。

可用以下命令辅助检查：

```powershell
flutter devices
flutter emulators
adb devices
```

---

## 五、启动 Android App

### 方式 A：在 Android Studio 中启动

1. 先在 Device Manager 中启动刚创建的 Android 模拟器
2. 回到 Flutter 项目顶部设备下拉框
3. 选择 Android 模拟器设备
4. 打开：

```text
lib/main.dart
```

5. 点击绿色运行按钮

### 方式 B：在终端中启动

Android 模拟器访问本机后端时，应使用：

```text
http://10.0.2.2:9090/api
```

因此可执行：

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:9090/api
```

---

## 六、Android 构建依赖说明

当前项目首次构建 Android 调试包时，会自动检查并使用：

```text
Gradle 8.14
Android SDK Build-Tools 35.0.0
Android SDK Platform 36
NDK 28.2.13676358
```

其中 `Android SDK Build-Tools 35.0.0` 需要在 Android Studio 中确认已安装：

```text
File -> Settings -> Languages & Frameworks -> Android SDK -> SDK Tools
```

勾选右下角：

```text
Show Package Details
```

然后确认：

```text
Android SDK Build-Tools -> 35.0.0
```

已经安装。

说明：

- `35.0.0` 可以和 `36.1.0`、`37.0.0` 等其他版本共存
- 不需要为了安装 `35.0.0` 卸载其他 Build-Tools 版本

---

## 七、为什么 Android 模拟器不能直接用 localhost

在 Android 模拟器中：

```text
localhost
```

表示的是“模拟器自己”，不是你的电脑。

如果 App 需要访问电脑上运行的后端服务，应使用：

```text
10.0.2.2
```

所以本项目 Android 模拟器联调后端时，应使用：

```text
http://10.0.2.2:9090/api
```

如果改为 Android 真机调试，则需要把接口地址改成电脑在局域网中的可访问 IP。

---

## 八、常见问题

| 现象 | 原因 | 处理方式 |
| --- | --- | --- |
| 选择 `Windows (desktop)` 后启动失败 | 当前项目没有 Windows 工程，且本机未配置 Windows 桌面开发环境 | 选择 Android 模拟器或 Android 真机 |
| 顶部显示 `<no devices>` | 当前没有可用 Android 设备，或 IDE 设备列表尚未刷新 | 先创建并启动模拟器，再检查 `flutter devices` |
| 只有 `Restart Flutter Daemon` | 它不是设备，只是重启服务的命令 | 先创建模拟器或连接真机 |
| 模拟器已经打开，但点击 `Open Android Emulator` 又报错 | 同一个 AVD 已经在运行，又重复启动了一次 | 直接选择设备列表里的 `Pixel 8 (mobile)`，不要再次启动同一个 AVD |
| 右上角只有 `Add Configuration` | Flutter 运行配置尚未生成 | 手动新增 Flutter 配置 |
| 顶部提示 `Dart SDK is not configured` | Dart SDK 路径未设置 | 配置 Dart SDK 路径 |
| 构建时报 `zip END header not found` | Gradle 下载包损坏或未下完整 | 删除对应损坏缓存后重新下载 |
| 构建时报 `Archive is not a ZIP archive`，并指向 `Build-Tools 35.0.0` | Android SDK Build-Tools 安装包损坏或未安装完整 | 在 SDK Manager 中确认并重新安装 `35.0.0` |
| App 能启动但登录请求失败 | Android 模拟器访问后端时仍使用了 `localhost` | 改用 `http://10.0.2.2:9090/api` |

---

## 九、当前阶段推荐做法

如果只是临时看页面，可以使用 Flutter Web：

```powershell
flutter run -d chrome --web-port=9999 --dart-define=API_BASE_URL=http://localhost:9090/api
```

如果是正式做 App 开发，则建议：

1. 使用 Android 模拟器或真机
2. 选择稳定版 Google Play 镜像
3. 使用 `10.0.2.2` 联调本机后端
4. 不把 Windows 桌面目标当作当前 App 的默认启动方式

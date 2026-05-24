# Windows 服务器部署环境要求

本文档说明：如果后续把当前项目部署到 Windows 服务器上，服务器需要具备哪些**运行环境**，以及哪些工具只在**构建或发布阶段**需要。

## 一、先明确部署对象

当前四个项目中，真正需要在服务器上长期运行的内容是：

| 内容 | 是否在服务器常驻运行 | 说明 |
| --- | --- | --- |
| MySQL 数据库 | 是 | 存储正式业务数据 |
| 后端服务 `boss-chat-server` | 是 | 提供统一 API |
| 管理系统 `boss-chat-web` | 是 | 通常以静态资源方式部署，由 Web 服务器托管 |
| Flutter App `boss-chat-app` | 否 | 服务器不运行 App，本地或 CI 构建后发布安装包 |
| 小程序 `boss-chat-miniapp` | 否 | 服务器不运行小程序，本地或 CI 构建后上传平台 |

> 也就是说，生产服务器不需要安装 Android Studio、Flutter SDK、微信开发者工具，除非你决定在这台服务器上直接做构建工作。

---

## 二、服务器必须具备的运行环境

### 1. Windows 操作系统

建议：

```text
Windows Server 2025
```

如果只是早期测试，也可以继续使用仍受支持的 Windows Server 2022；但截至 2026 年 5 月，Windows Server 2025 已是当前 LTSC 版本，更适合作为新部署的默认选择。

### 2. Java 运行环境

后端当前基于：

```text
Spring Boot 3.3.5
Java 17 LTS
```

因此服务器必须安装：

```text
JDK 17 LTS
```

推荐使用长期支持版本，并在服务器上设置好：

```text
JAVA_HOME
PATH
```

Spring Boot 3.x 使用 Java 17 作为最低基线；本项目 `pom.xml` 当前也明确指定了 `java.version=17`。 citeturn1search1

### 3. MySQL

当前项目数据库版本基线为：

```text
MySQL 8.0.46
```

服务器必须准备：

- MySQL Server 8.0.46
- 正式数据库账号
- 数据库备份策略
- 防火墙仅开放必要访问

在 Windows 上安装 MySQL 8.0 时，还需要系统具备：

```text
Microsoft Visual C++ 2019 Redistributable
```

这是 MySQL 官方对 Windows 平台的要求。 citeturn0search6

### 4. Web 服务器 / 反向代理

管理系统前端最终会构建为静态文件，因此服务器还需要一个 Web 服务组件来：

- 托管管理系统静态资源
- 反向代理后端接口
- 统一处理 HTTPS
- 后续支持域名、缓存、压缩、限流等

Windows 下可选方案：

| 方案 | 建议 |
| --- | --- |
| IIS | 更适合作为 Windows 生产环境默认方案 |
| Nginx for Windows | 可用，但官方仍把 Windows 版本视为 beta，不建议作为高并发正式环境首选 |

当前阶段更推荐：

```text
IIS
```

如果后续你更倾向统一 Linux / Windows 的代理配置，也可以继续评估 Nginx；只是要知道 Nginx 官方文档明确提示 Windows 版本在性能、扩展性和服务化方面仍有限制。

### 5. HTTPS 证书

正式对外服务时应准备：

- 域名
- SSL/TLS 证书
- 80 / 443 端口放行

推荐最终让用户访问：

```text
https://你的域名
```

而不是直接暴露：

```text
http://服务器IP:9090
```

### 6. 防火墙与端口

建议：

| 端口 | 是否建议公网开放 | 用途 |
| --- | --- | --- |
| 80 | 是 | HTTP，通常用于跳转 HTTPS |
| 443 | 是 | HTTPS 正式入口 |
| 3306 | 否 | MySQL，建议只允许本机或内网访问 |
| 9090 | 否 | 后端服务端口，建议只由 Nginx 反向代理访问 |

---

## 三、部署时推荐的软件清单

### 生产运行必需

| 软件 | 建议版本 | 是否必须 |
| --- | --- | --- |
| Windows Server | 2025 | 是 |
| JDK | 17 LTS | 是 |
| MySQL Server | 8.0.46（当前项目基线使用 8.0 系列） | 是 |
| IIS | 随 Windows Server 2025 启用 | 是 |
| SSL 证书 | 有效证书 | 正式环境必需 |

### 构建阶段才需要

| 软件 | 建议版本 | 是否必须装在生产服务器 | 用途 |
| --- | --- | --- | --- |
| Maven | 3.9.15 | 否 | 构建后端 jar |
| Node.js | 24.14.1 LTS | 否 | 构建管理系统前端、小程序 H5 产物 |
| npm | 随 Node.js 24.14.1 LTS 安装 | 否 | 安装前端依赖 |
| Flutter SDK | 3.41.9 stable | 否 | 构建 App |
| Android SDK | 36.1.0 | 否 | 构建或调试 Android App |
| 微信开发者工具 | 当前官方稳定版 | 否 | 预览与上传小程序 |

> 更推荐的方式是：在本地或 CI 环境完成构建，只把 `jar` 包和前端 `dist` 文件上传到服务器。Node.js 生产环境应优先选择仍处于 LTS 生命周期内的版本；Node.js 官方也建议生产环境使用 Active LTS 或 Maintenance LTS 发布线。 citeturn0search1turn0search2

---

## 四、当前项目部署时至少需要准备的内容

### 1. 后端

需要上传：

```text
boss-chat-server-*.jar
```

需要配置：

- 数据库地址
- 数据库账号密码
- 生产环境跨域域名
- 正式管理员初始化账号
- 日志目录

当前后端支持通过环境变量覆盖数据库连接：

```powershell
$env:DB_HOST="..."
$env:DB_PORT="3306"
$env:DB_NAME="..."
$env:DB_USERNAME="..."
$env:DB_PASSWORD="..."
```

### 2. 管理系统

需要先构建：

```powershell
npm run build
```

然后把生成的静态文件部署到 Nginx 或 IIS。

### 3. 数据库

需要准备：

- 正式库
- 正式账号
- 初始化脚本
- Flyway 迁移执行策略
- 自动备份策略

### 4. 域名与访问路径

建议后续采用：

```text
https://admin.example.com      -> 管理系统
https://api.example.com        -> 后端 API
```

或者：

```text
https://example.com            -> 管理系统
https://example.com/api        -> 后端 API
```

二者都可以，后续要结合域名规划和跨域策略决定。

---

## 五、Windows 部署时的额外注意点

1. Windows 服务托管  
   后端 jar 最好注册为系统服务，而不是靠手工开一个命令行窗口长期运行。

2. MySQL 服务权限  
   确认数据库数据目录、备份目录有合适权限。

3. 路径与编码  
   避免把部署目录放在过深或带复杂中文/空格的路径下。

4. 日志轮转  
   需要提前设计日志保留周期，避免磁盘被写满。

5. 防火墙  
   只开放真正需要对外访问的端口。

---

## 六、当前项目不建议在生产服务器上安装的东西

除非你有明确理由，否则生产服务器上不建议安装：

- Android Studio
- Flutter SDK
- 微信开发者工具
- VS Code
- IDEA
- 浏览器调试环境

这些都属于开发或构建工具，不属于生产运行必需环境。

---

## 七、部署前检查清单

- [ ] Windows Server 已安装并完成安全更新
- [ ] JDK 17 LTS 已安装
- [ ] MySQL 8.0.46 已安装并能正常启动
- [ ] Microsoft Visual C++ 2019 Redistributable 已安装
- [ ] IIS 已启用
- [ ] 域名已解析
- [ ] HTTPS 证书已准备
- [ ] 防火墙规则已配置
- [ ] 后端环境变量已准备
- [ ] 数据库备份策略已确认
- [ ] 前端静态文件部署目录已确认
- [ ] 后端 jar 的启动方式已确定

---

## 八、当前阶段结论

如果只看“让当前基础模板正式上线运行”所需的最小集合，Windows 服务器至少要有：

```text
Windows Server
JDK 17 LTS
MySQL 8.0.46
IIS
SSL 证书
```

而：

```text
Node.js
Maven
Flutter SDK
Android Studio
微信开发者工具
```

都不是生产服务器长期运行当前系统的硬性依赖。

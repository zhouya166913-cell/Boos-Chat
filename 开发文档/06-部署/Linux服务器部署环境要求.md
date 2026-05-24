# Linux 服务器部署环境要求

本文档说明：如果后续把当前项目部署到 Linux 服务器上，服务器需要具备哪些**运行环境**，以及哪些工具只在**构建或发布阶段**需要。

## 一、先明确部署对象

当前四个项目中，真正需要在服务器上长期运行的内容是：

| 内容 | 是否在服务器常驻运行 | 说明 |
| --- | --- | --- |
| MySQL 数据库 | 是 | 存储正式业务数据 |
| 后端服务 `boss-chat-server` | 是 | 提供统一 API |
| 管理系统 `boss-chat-web` | 是 | 通常以静态资源方式部署，由 Web 服务器托管 |
| Flutter App `boss-chat-app` | 否 | 服务器不运行 App，本地或 CI 构建后发布安装包 |
| 小程序 `boss-chat-miniapp` | 否 | 服务器不运行小程序，本地或 CI 构建后上传平台 |

> 生产服务器的目标是稳定运行服务，不是承担全部开发工作。因此默认不需要在生产服务器安装 Flutter SDK、Android Studio、微信开发者工具。

---

## 二、推荐 Linux 发行版

建议优先选择长期支持版本，例如：

```text
Ubuntu Server 26.04 LTS
```

或：

```text
Ubuntu Server 24.04 LTS
```

如果你更偏好传统企业服务器生态，也可以选择：

```text
Rocky Linux 9.x
```

截至 2026 年 5 月，Ubuntu 26.04 LTS 已是当前 LTS 版本；如果你更看重“已经被大量项目验证过一段时间”的成熟度，Ubuntu 24.04 LTS 仍然是很稳妥的选择。Ubuntu 官方当前仍同时提供 26.04 LTS 与 24.04 LTS；Rocky Linux 9 系列也仍在持续更新。

---

## 三、服务器必须具备的运行环境

### 1. Java 运行环境

后端当前基于：

```text
Spring Boot 3.3.5
Java 17 LTS
```

因此服务器必须安装：

```text
JDK 17 LTS
```

Spring Boot 3.x 使用 Java 17 作为最低基线；本项目 `pom.xml` 当前也明确指定了 `java.version=17`。 citeturn1search1

### 2. MySQL

当前项目数据库版本基线为：

```text
MySQL 8.0.46
```

服务器必须准备：

- MySQL Server 8.0.46
- 正式数据库账号
- 数据目录规划
- 自动备份策略
- 只允许本机或内网访问的数据库安全策略

### 3. Web 服务器 / 反向代理

管理系统前端最终会构建成静态文件，因此 Linux 服务器还需要：

```text
Nginx 1.30.1 stable
```

它负责：

- 托管管理系统静态资源
- 反向代理后端服务
- 统一处理 HTTPS
- 后续支持 gzip、缓存、限流、访问日志等

### 4. HTTPS 证书

正式对外服务时应准备：

- 域名
- SSL/TLS 证书
- 80 / 443 端口放行

推荐最终让用户访问：

```text
https://你的域名
```

### 5. 防火墙与端口

建议：

| 端口 | 是否建议公网开放 | 用途 |
| --- | --- | --- |
| 22 | 视情况 | SSH 管理 |
| 80 | 是 | HTTP，通常用于跳转 HTTPS |
| 443 | 是 | HTTPS 正式入口 |
| 3306 | 否 | MySQL，建议只允许本机或内网访问 |
| 9090 | 否 | 后端服务端口，建议只由 Nginx 访问 |

---

## 四、部署时推荐的软件清单

### 生产运行必需

| 软件 | 建议版本 | 是否必须 |
| --- | --- | --- |
| Linux 发行版 | Ubuntu 26.04 LTS / Ubuntu 24.04 LTS / Rocky Linux 9.x | 是 |
| JDK | 17 LTS | 是 |
| MySQL Server | 8.0.46（当前项目基线使用 8.0 系列） | 是 |
| Nginx | 1.30.1 stable | 是 |
| SSL 证书 | 有效证书 | 正式环境必需 |

### 构建阶段才需要

| 软件 | 建议版本 | 是否必须装在生产服务器 | 用途 |
| --- | --- | --- | --- |
| Maven | 3.9.15 | 否 | 构建后端 jar |
| Node.js | 24.14.1 LTS | 否 | 构建管理系统、小程序 H5 产物 |
| npm | 随 Node.js 24.14.1 LTS 安装 | 否 | 安装前端依赖 |
| Flutter SDK | 3.41.9 stable | 否 | 构建 App |
| Android SDK | 36.1.0 | 否 | 构建 Android App |
| 微信开发者工具 | 当前官方稳定版 | 否 | 上传小程序 |

> 更推荐在本地或 CI 环境完成构建，把 `jar` 和前端静态文件部署到服务器。Node.js 生产环境应优先选择仍处于 LTS 生命周期内的版本；Node.js 官方也建议生产应用只使用 Active LTS 或 Maintenance LTS 发布线。 citeturn0search1turn0search2

---

## 五、当前项目部署时至少需要准备的内容

### 1. 后端服务

需要上传：

```text
boss-chat-server-*.jar
```

需要配置：

- 数据库地址
- 数据库账号密码
- 正式环境跨域域名
- 正式管理员初始化账号
- 日志路径

当前后端支持通过环境变量覆盖数据库连接：

```bash
export DB_HOST="..."
export DB_PORT="3306"
export DB_NAME="..."
export DB_USERNAME="..."
export DB_PASSWORD="..."
```

### 2. 管理系统前端

需要先构建：

```bash
npm run build
```

然后把构建产物部署到 Nginx 静态目录。

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

最终要结合域名规划、跨域策略与证书申请方式决定。

---

## 六、Linux 部署时的额外建议

### 1. 使用 systemd 托管后端

不要依赖手工命令长期运行：

```bash
java -jar ...
```

正式环境应使用：

```text
systemd service
```

统一管理：

- 开机自启
- 异常重启
- 日志输出
- 进程状态

### 2. 建议单独规划目录

可按类似结构规划：

```text
/opt/boss-chat/
├─ app/
├─ logs/
└─ backup/
```

### 3. 数据库与应用权限分离

建议：

- 应用使用专门数据库账号
- 不直接使用 root
- 不把数据库端口直接暴露公网

### 4. 日志与备份

至少要准备：

- 后端日志保留策略
- Nginx 访问日志保留策略
- MySQL 定时备份
- 备份恢复演练

### 5. 监控

后续正式环境建议补充：

- CPU / 内存 / 磁盘监控
- 后端存活探测
- MySQL 连接与容量监控
- 证书到期提醒

---

## 七、当前项目不建议在生产服务器上安装的东西

除非你明确要让服务器承担构建任务，否则生产服务器上不建议安装：

- Android Studio
- Flutter SDK
- 微信开发者工具
- IDEA
- VS Code
- 浏览器调试环境

这些属于开发或发布链路，不属于服务运行必需环境。

---

## 八、部署前检查清单

- [ ] Linux 发行版已确定
- [ ] JDK 17 LTS 已安装
- [ ] MySQL 8.0.46 已安装并能启动
- [ ] Nginx 1.30.1 stable 已安装
- [ ] 域名已解析
- [ ] HTTPS 证书已准备
- [ ] 防火墙规则已配置
- [ ] 后端环境变量已准备
- [ ] 数据库备份策略已确认
- [ ] systemd 服务文件已准备
- [ ] 前端静态目录已确认
- [ ] 日志目录和权限已确认

---

## 九、当前阶段结论

如果只看“让当前基础模板正式上线运行”所需的最小集合，Linux 服务器至少要有：

```text
Linux
JDK 17 LTS
MySQL 8.0.46
Nginx 1.30.1 stable
SSL 证书
```

而：

```text
Node.js
Maven
Flutter SDK
Android SDK
微信开发者工具
```

都不是生产服务器长期运行当前系统的硬性依赖。

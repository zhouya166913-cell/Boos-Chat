# Windows 服务器部署操作手册

本文档用于指导当前基础模板在 Windows 服务器上的第一版部署。  
当前建议场景是：

- 生产服务器运行后端、数据库和管理系统静态站点
- Flutter App 与小程序仍在本地或 CI 环境中构建、发布
- Windows 服务器优先采用 IIS 托管管理系统静态资源

---

## 一、版本基线

### 1. 生产运行环境

| 组件 | 建议版本 |
| --- | --- |
| 操作系统 | Windows Server 2025 |
| Java | JDK 17 LTS |
| 数据库 | MySQL Server 8.0.46 |
| Web 服务 | IIS（随 Windows Server 启用） |
| HTTPS | 有效 SSL/TLS 证书 |

### 2. 构建环境

| 组件 | 建议版本 | 是否必须装在生产服务器 |
| --- | --- | --- |
| Maven | 3.9.15 | 否 |
| Node.js | 24.14.1 LTS | 否 |
| npm | 随 Node.js 24.14.1 LTS 安装 | 否 |
| Flutter SDK | 3.41.9 stable | 否 |
| Android SDK | 36.1.0 | 否 |

> 建议在本地或 CI 中完成构建，再把 jar 包和前端静态文件上传到服务器。

---

## 二、部署前准备

### 1. 建议目录结构

```text
C:\boss-chat\
├─ app\        # 后端 jar
├─ web\        # 管理系统静态文件
├─ logs\       # 后端日志
├─ backup\     # 数据库备份
└─ config\     # 生产环境配置说明或脚本
```

### 2. 必要软件

服务器至少安装：

- JDK 17 LTS
- MySQL Server 8.0.46
- Microsoft Visual C++ 2019 Redistributable
- IIS
- SSL 证书

### 3. 推荐端口规划

| 端口 | 用途 | 是否建议公网开放 |
| --- | --- | --- |
| 80 | HTTP | 是 |
| 443 | HTTPS | 是 |
| 3306 | MySQL | 否 |
| 9090 | 后端服务 | 否 |

---

## 三、数据库准备

### 1. 创建正式数据库

建议正式环境不要直接沿用开发库名，可使用：

```text
boss_chat
```

### 2. 创建数据库账号

建议为应用创建专用账号，不直接使用 `root`。

示例：

```sql
CREATE DATABASE boss_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'boss_chat_app'@'localhost' IDENTIFIED BY '请替换为强密码';
GRANT ALL PRIVILEGES ON boss_chat.* TO 'boss_chat_app'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 迁移脚本

后端首次启动后，Flyway 会自动执行：

```text
src/main/resources/db/migration
```

中的版本化迁移脚本。

---

## 四、构建发布物

### 1. 构建后端 jar

在开发机或 CI 中执行：

```powershell
cd D:\LanTu\life-cycle-management-system\bossChat\boss-chat-server
mvn clean package -DskipTests
```

构建完成后上传：

```text
target\boss-chat-server-0.1.0.jar
```

到：

```text
C:\boss-chat\app\
```

### 2. 构建管理系统

```powershell
cd D:\LanTu\life-cycle-management-system\bossChat\boss-chat-web
npm install
npm run build
```

上传：

```text
dist\
```

中的静态文件到：

```text
C:\boss-chat\web\
```

---

## 五、配置后端

### 1. 建议使用环境变量

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="boss_chat"
$env:DB_USERNAME="boss_chat_app"
$env:DB_PASSWORD="请替换为强密码"
```

### 2. 需要额外确认的配置

- 生产环境数据库连接
- 正式管理员初始账号
- CORS 白名单域名
- 日志路径
- 是否允许 Swagger 在生产环境开放

---

## 六、启动后端

### 1. 首次手动验证

```powershell
cd C:\boss-chat\app
java -jar .\boss-chat-server-0.1.0.jar
```

### 2. 验证成功标志

控制台出现：

```text
Tomcat started on port 9090
Started BossChatApplication
```

### 3. 正式环境建议

正式部署时不要依赖手工打开命令行窗口长期运行。  
应在后续固定一种 Windows 服务托管方案，例如：

- WinSW
- NSSM
- 企业内部统一的 Windows 服务包装工具

当前模板阶段先完成“应用可稳定运行”的基础部署，再在正式上线前补齐服务化托管方案。

---

## 七、部署管理系统到 IIS

### 1. 启用 IIS

通过“添加角色和功能”启用：

```text
Web Server (IIS)
```

### 2. 创建站点

站点目录指向：

```text
C:\boss-chat\web
```

### 3. 配置前端路由回退

如果管理系统后续使用 history 路由，需要额外配置回退到：

```text
index.html
```

### 4. 配置 HTTPS

绑定域名和证书，正式访问建议统一走：

```text
https://你的域名
```

---

## 八、上线前验证

### 1. 后端

```text
http://127.0.0.1:9090/swagger-ui.html
```

确认：

- 后端可访问
- 登录接口可用
- Flyway 已完成迁移

### 2. 管理系统

确认：

- 首页可打开
- 登录页面可用
- 能正常请求后端

### 3. 数据库

确认正式库中已生成：

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- `sys_oauth_account`
- `sys_login_record`
- `flyway_schema_history`

---

## 九、Windows 部署后的待办

正式上线前，还需要继续补齐：

1. 后端注册为 Windows 服务
2. 数据库自动备份
3. 日志轮转与归档
4. HTTPS 证书续期
5. Swagger 生产环境开放策略
6. 防火墙白名单与远程管理策略

---

## 十、当前阶段结论

Windows 服务器要运行当前系统，最低需要：

```text
Windows Server 2025
JDK 17 LTS
MySQL Server 8.0.46
IIS
SSL 证书
```

构建阶段建议使用：

```text
Maven 3.9.15
Node.js 24.14.1 LTS
Flutter 3.41.9 stable
Android SDK 36.1.0
```

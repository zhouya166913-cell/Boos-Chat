# Linux 服务器部署操作手册

本文档用于指导当前基础模板在 Linux 服务器上的第一版部署。  
当前建议场景是：

- 生产服务器运行后端、数据库和管理系统静态站点
- Flutter App 与小程序仍在本地或 CI 环境中构建、发布
- Linux 服务器采用 `systemd + Nginx` 管理后端与前端访问

---

## 一、版本基线

### 1. 生产运行环境

| 组件 | 建议版本 |
| --- | --- |
| 操作系统 | Ubuntu Server 26.04 LTS / Ubuntu Server 24.04 LTS |
| Java | JDK 17 LTS |
| 数据库 | MySQL Server 8.0.46 |
| Web 服务 | Nginx 1.30.1 stable |
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
/opt/boss-chat/
├─ app/        # 后端 jar
├─ web/        # 管理系统静态文件
├─ logs/       # 后端日志
├─ backup/     # 数据库备份
└─ config/     # 生产环境配置说明或脚本
```

### 2. 必要软件

服务器至少安装：

- JDK 17 LTS
- MySQL Server 8.0.46
- Nginx 1.30.1 stable
- SSL 证书

### 3. 推荐端口规划

| 端口 | 用途 | 是否建议公网开放 |
| --- | --- | --- |
| 22 | SSH 管理 | 视情况 |
| 80 | HTTP | 是 |
| 443 | HTTPS | 是 |
| 3306 | MySQL | 否 |
| 9090 | 后端服务 | 否 |

---

## 三、数据库准备

### 1. 创建正式数据库

建议正式环境使用：

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

```bash
cd /path/to/boss-chat-server
mvn clean package -DskipTests
```

构建完成后上传：

```text
target/boss-chat-server-0.1.0.jar
```

到：

```text
/opt/boss-chat/app/
```

### 2. 构建管理系统

```bash
cd /path/to/boss-chat-web
npm install
npm run build
```

上传：

```text
dist/
```

中的静态文件到：

```text
/opt/boss-chat/web/
```

---

## 五、配置后端

### 1. 建议使用环境变量

```bash
export DB_HOST="localhost"
export DB_PORT="3306"
export DB_NAME="boss_chat"
export DB_USERNAME="boss_chat_app"
export DB_PASSWORD="请替换为强密码"
```

### 2. 需要额外确认的配置

- 生产环境数据库连接
- 正式管理员初始账号
- CORS 白名单域名
- 日志路径
- 是否允许 Swagger 在生产环境开放

---

## 六、使用 systemd 托管后端

### 1. 创建服务文件

```bash
sudo nano /etc/systemd/system/boss-chat.service
```

示例：

```ini
[Unit]
Description=Boss Chat Backend Service
After=network.target mysql.service

[Service]
User=bosschat
WorkingDirectory=/opt/boss-chat/app
Environment=DB_HOST=localhost
Environment=DB_PORT=3306
Environment=DB_NAME=boss_chat
Environment=DB_USERNAME=boss_chat_app
Environment=DB_PASSWORD=请替换为强密码
ExecStart=/usr/bin/java -jar /opt/boss-chat/app/boss-chat-server-0.1.0.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### 2. 启动并设置开机自启

```bash
sudo systemctl daemon-reload
sudo systemctl enable boss-chat
sudo systemctl start boss-chat
sudo systemctl status boss-chat
```

### 3. 查看日志

```bash
journalctl -u boss-chat -f
```

---

## 七、配置 Nginx

### 1. 管理系统静态站点

示例：

```nginx
server {
    listen 80;
    server_name admin.example.com;

    root /opt/boss-chat/web;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 2. 后端反向代理

如果采用独立 API 域名：

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. 检查并重载

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 八、HTTPS 配置

正式上线建议统一改为：

```text
https://admin.example.com
https://api.example.com
```

证书方案可以按你后续购买的云厂商、证书服务商或自动续期方案决定。  
在正式域名确定后，再补一份专门的证书与 Nginx 配置文档会更合适。

---

## 九、上线前验证

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

## 十、Linux 部署后的待办

正式上线前，还需要继续补齐：

1. MySQL 自动备份
2. Nginx 与后端日志轮转
3. HTTPS 证书续期
4. Swagger 生产环境开放策略
5. 防火墙与 SSH 安全策略
6. 监控与告警

---

## 十一、当前阶段结论

Linux 服务器要运行当前系统，最低需要：

```text
Ubuntu Server 26.04 LTS 或 Ubuntu Server 24.04 LTS
JDK 17 LTS
MySQL Server 8.0.46
Nginx 1.30.1 stable
SSL 证书
```

构建阶段建议使用：

```text
Maven 3.9.15
Node.js 24.14.1 LTS
Flutter 3.41.9 stable
Android SDK 36.1.0
```

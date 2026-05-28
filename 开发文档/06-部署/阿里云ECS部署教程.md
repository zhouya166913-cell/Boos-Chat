# 阿里云 ECS 部署教程

本文档用于把当前 `bossChat` 项目部署到阿里云 ECS。当前建议采用一台云服务器先跑通测试版本：

- 后端：`boss-chat-server`，Spring Boot，端口 `9090`
- 管理后台：`boss-chat-web`，Vue 构建后的静态文件，由 Nginx 托管
- 调查问卷静态页：放在后端服务中，由 `/survey/enterprise-diagnosis.html` 对外访问
- 数据库：MySQL 8.0
- 对外入口：Nginx 统一暴露 `80/443`，不要直接暴露 `9090` 和 `3306`

推荐最终访问方式：

```text
https://你的域名/                         管理后台
https://你的域名/api/...                  后端接口
https://你的域名/survey/enterprise-diagnosis.html  客户填写问卷页面
```

---

## 1. 购买阿里云 ECS

### 1.1 推荐配置

测试版本可以先用：

```text
实例规格：2 核 4G 起步
系统盘：40G 起步
操作系统：Ubuntu 22.04 LTS 或 Ubuntu 24.04 LTS
带宽：1M 到 5M，按实际访问量调整
地域：尽量选择离客户近的地域
```

如果后续图片、附件、AI 调用量上来，建议升级到：

```text
4 核 8G
80G 以上系统盘
单独使用 OSS/COS 保存附件和图片
```

### 1.2 安全组放行

阿里云控制台进入 ECS 安全组，放行：

| 端口 | 用途 | 是否必须 |
| --- | --- | --- |
| 22 | SSH 登录服务器 | 必须 |
| 80 | HTTP 访问和证书申请 | 必须 |
| 443 | HTTPS 正式访问 | 正式环境必须 |

不要对公网放行：

| 端口 | 原因 |
| --- | --- |
| 3306 | MySQL 不应该暴露到公网 |
| 9090 | 后端只允许 Nginx 在本机反向代理 |

---

## 2. 登录服务器并安装环境

以下命令以 Ubuntu 为例。

### 2.1 更新系统

```bash
sudo apt update
sudo apt upgrade -y
```

### 2.2 安装 Java、MySQL、Nginx

```bash
sudo apt install -y openjdk-17-jdk mysql-server nginx unzip curl
```

检查版本：

```bash
java -version
mysql --version
nginx -v
```

后端项目要求 Java 17，当前 `pom.xml` 中配置的是：

```text
java.version = 17
Spring Boot = 3.3.5
```

---

## 3. 创建部署目录和运行用户

```bash
sudo useradd -r -s /usr/sbin/nologin bosschat || true
sudo mkdir -p /opt/boss-chat/app
sudo mkdir -p /opt/boss-chat/web
sudo mkdir -p /opt/boss-chat/logs
sudo mkdir -p /opt/boss-chat/backup
sudo mkdir -p /opt/boss-chat/app/uploads
sudo chown -R bosschat:bosschat /opt/boss-chat
```

目录说明：

```text
/opt/boss-chat/app      后端 jar、生产配置、上传目录
/opt/boss-chat/web      管理后台静态文件
/opt/boss-chat/logs     日志目录
/opt/boss-chat/backup   数据库备份目录
```

---

## 4. 初始化 MySQL

登录 MySQL：

```bash
sudo mysql
```

创建生产库和应用账号：

```sql
CREATE DATABASE boss_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'boss_chat_app'@'localhost' IDENTIFIED BY '请替换成强密码';

GRANT ALL PRIVILEGES ON boss_chat.* TO 'boss_chat_app'@'localhost';

FLUSH PRIVILEGES;
```

退出：

```sql
exit;
```

测试连接：

```bash
mysql -u boss_chat_app -p boss_chat
```

说明：

- 不要让应用使用 MySQL `root` 账号。
- 不要在阿里云安全组开放 `3306`。
- 首次启动后端时，Flyway 会自动执行 `src/main/resources/db/migration` 下的数据库迁移脚本。

---

## 5. 本地打包项目

建议在本地电脑或 CI 上打包，不建议在生产服务器上安装完整开发环境。

### 5.1 打包后端

在本地项目目录执行：

```bash
cd boss-chat-server
mvn clean package -DskipTests
```

产物位置：

```text
boss-chat-server/target/boss-chat-server-0.1.0.jar
```

### 5.2 打包管理后台

管理后台默认请求 `/api`，配合 Nginx 同域部署即可。

```bash
cd boss-chat-web
npm install
npm run build
```

产物位置：

```text
boss-chat-web/dist/
```

---

## 6. 上传部署产物

把后端 jar 上传到服务器：

```bash
scp boss-chat-server/target/boss-chat-server-0.1.0.jar root@你的服务器公网IP:/opt/boss-chat/app/
```

把前端静态文件上传到服务器：

```bash
scp -r boss-chat-web/dist/* root@你的服务器公网IP:/opt/boss-chat/web/
```

修正权限：

```bash
sudo chown -R bosschat:bosschat /opt/boss-chat
```

---

## 7. 配置生产环境

后端 `application.yml` 已经支持读取同目录下的 `application-local.yml`：

```yaml
spring:
  config:
    import: optional:file:./application-local.yml
```

因此在服务器创建：

```bash
sudo nano /opt/boss-chat/app/application-local.yml
```

示例配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/boss_chat?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: boss_chat_app
    password: 请替换成数据库强密码

app:
  bootstrap:
    username: admin
    password: 请替换成正式管理员密码
    display-name: Admin

  model-seed:
    zhipu-api-key: 请填写智谱APIKey
    kimi-api-key: 请填写KimiAPIKey
    qwen-api-key: 请填写通义千问或DashScopeKey
    openai-api-key:

  agent:
    workspace-root: /opt/boss-chat/app
    allowed-roots: /opt/boss-chat/app
```

注意：

- API Key 不要写进前端代码。
- 生产管理员密码一定要改掉，不要继续使用开发默认密码。
- 图片存储、腾讯 COS、阿里云 OSS 等配置可以部署后在“图片存储管理”页面继续维护。
- 如果生产环境使用同域名部署，通常不需要额外配置 CORS。

---

## 8. 使用 systemd 托管后端

创建服务文件：

```bash
sudo nano /etc/systemd/system/boss-chat.service
```

写入：

```ini
[Unit]
Description=Boss Chat Backend Service
After=network.target mysql.service

[Service]
User=bosschat
Group=bosschat
WorkingDirectory=/opt/boss-chat/app
ExecStart=/usr/bin/java -jar /opt/boss-chat/app/boss-chat-server-0.1.0.jar
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable boss-chat
sudo systemctl start boss-chat
sudo systemctl status boss-chat
```

查看日志：

```bash
journalctl -u boss-chat -f
```

本机测试后端：

```bash
curl http://127.0.0.1:9090/api/auth/me
```

如果返回未登录或认证相关信息，说明后端已经起来了。首次启动时也可以查看日志确认 Flyway 是否迁移成功。

---

## 9. 配置 Nginx

创建站点配置：

```bash
sudo nano /etc/nginx/sites-available/boss-chat.conf
```

如果你暂时只有 IP，还没有域名，可以先把 `server_name` 写成 `_`。

```nginx
server {
    listen 80;
    server_name 你的域名或_;

    client_max_body_size 100m;

    root /opt/boss-chat/web;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }

    location /survey/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/boss-chat.conf /etc/nginx/sites-enabled/boss-chat.conf
sudo nginx -t
sudo systemctl reload nginx
```

如果默认站点占用了入口，可以删除默认配置：

```bash
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

---

## 10. 配置域名和 HTTPS

### 10.1 域名解析

在域名 DNS 中添加：

```text
记录类型：A
主机记录：@
记录值：ECS 公网 IP
```

如果要使用 `www`：

```text
记录类型：A
主机记录：www
记录值：ECS 公网 IP
```

### 10.2 申请 HTTPS 证书

可以使用阿里云 SSL 证书，也可以使用其他证书。证书配置完成后，Nginx 增加 `443 ssl` 站点，并把 `80` 跳转到 `443`。

示例结构：

```nginx
server {
    listen 80;
    server_name 你的域名;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name 你的域名;

    ssl_certificate /etc/nginx/ssl/你的证书.pem;
    ssl_certificate_key /etc/nginx/ssl/你的私钥.key;

    client_max_body_size 100m;

    root /opt/boss-chat/web;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }

    location /survey/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }
}
```

重新加载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 11. 验证部署结果

### 11.1 管理后台

打开：

```text
https://你的域名/
```

登录管理员账号，确认可以进入：

- AI 对话
- 智能体管理
- 模型管理
- 图片存储
- 调查记录

### 11.2 调查问卷

打开：

```text
https://你的域名/survey/enterprise-diagnosis.html
```

测试流程：

1. 填写姓名、手机号、公司、公司人数、产品服务和问卷内容。
2. 点击“提交并生成诊断”。
3. 页面跳转到结果页。
4. 观察 AI 是否开始生成诊断。
5. 回到管理后台“调查记录”，确认能看到刚才的记录和 AI 结果。

### 11.3 后端日志

```bash
journalctl -u boss-chat -f
```

重点观察：

- 数据库是否连接成功
- Flyway 是否迁移成功
- AI 调用是否成功
- 附件上传是否有异常

---

## 12. 每次更新项目怎么发版

### 12.1 后端更新

本地重新打包：

```bash
cd boss-chat-server
mvn clean package -DskipTests
```

上传 jar：

```bash
scp target/boss-chat-server-0.1.0.jar root@你的服务器公网IP:/opt/boss-chat/app/
```

重启服务：

```bash
sudo systemctl restart boss-chat
sudo systemctl status boss-chat
```

### 12.2 前端更新

本地重新构建：

```bash
cd boss-chat-web
npm run build
```

上传静态文件：

```bash
scp -r dist/* root@你的服务器公网IP:/opt/boss-chat/web/
```

前端静态文件不需要重启后端，必要时刷新浏览器缓存。

### 12.3 数据库迁移

后端启动时 Flyway 自动执行新增迁移脚本。发版前要确认：

```text
boss-chat-server/src/main/resources/db/migration
```

中的脚本已经提交并随 jar 打进去。

---

## 13. 常见问题

### 13.1 访问后台空白或接口 404

检查 Nginx 是否把 `/api/` 代理到了后端：

```bash
sudo nginx -t
sudo systemctl status nginx
curl http://127.0.0.1:9090/api/auth/me
```

### 13.2 问卷页面打不开

问卷静态页在后端服务中，不在 Vue 前端中。需要确认 Nginx 有：

```nginx
location /survey/ {
    proxy_pass http://127.0.0.1:9090;
}
```

然后访问：

```text
https://你的域名/survey/enterprise-diagnosis.html
```

### 13.3 AI 结果流式输出卡住

确认 Nginx 关闭代理缓冲：

```nginx
proxy_buffering off;
proxy_cache off;
proxy_read_timeout 3600s;
```

流式接口走 `/api/public/surveys/{publicId}/stream` 和部分 AI 对话接口，如果代理缓冲没关，浏览器可能等内容生成完才一次性看到结果。

### 13.4 上传附件失败

检查：

```bash
sudo ls -la /opt/boss-chat/app/uploads
sudo chown -R bosschat:bosschat /opt/boss-chat/app/uploads
```

如果走腾讯 COS 或阿里云 OSS，检查后台“图片存储管理”中的：

- Bucket
- 地域
- 访问域名
- SecretId / SecretKey 或 AccessKey
- 是否设为默认存储

### 13.5 端口被占用

```bash
sudo lsof -i:9090
sudo lsof -i:80
sudo lsof -i:443
```

后端端口被占用后，先确认是不是旧服务还在跑：

```bash
sudo systemctl status boss-chat
```

---

## 14. 上线前检查清单

- 阿里云安全组只开放 `22/80/443`
- MySQL `3306` 没有公网开放
- 后端 `9090` 没有公网开放
- 管理员默认密码已修改
- `application-local.yml` 中的 API Key 没有提交到代码仓库
- Nginx 已配置 `/api/` 和 `/survey/`
- `/survey/enterprise-diagnosis.html` 可访问
- 调查问卷提交后能生成结果
- 管理后台“调查记录”能查看提交记录
- AI 对话能正常回复
- 图片/附件上传策略已确认
- MySQL 已设置定期备份

---

## 15. 使用 CI 一键部署

可以使用 CI 做一键部署。当前项目最推荐的方式是：

```text
代码仓库推送 main 分支
        ↓
CI 自动构建后端 jar
        ↓
CI 自动构建前端 dist
        ↓
CI 通过 SSH 上传到阿里云 ECS
        ↓
CI 重启 systemd 后端服务
        ↓
Nginx 继续托管前端和反向代理后端
```

这种方式不需要在服务器上安装 Maven、Node.js 和 npm，服务器只负责运行服务，更干净，也更稳定。

### 15.1 CI 方案选择

可以任选一种：

| 方案 | 适合情况 |
| --- | --- |
| GitHub Actions | 代码放在 GitHub，配置简单 |
| Gitee Go / Gitee 流水线 | 代码放在 Gitee，国内访问更稳定 |
| 阿里云云效 Flow | 想和阿里云体系集成 |
| Jenkins | 公司已有 Jenkins 或想完全自建 |

测试版本建议先用 GitHub Actions 或 Gitee 流水线，逻辑最简单。

### 15.2 服务器侧提前准备

服务器只需要按前面的章节准备好：

```text
/opt/boss-chat/app
/opt/boss-chat/web
/opt/boss-chat/app/application-local.yml
/etc/systemd/system/boss-chat.service
Nginx 站点配置
```

并确认手动部署已经跑通过一次：

```bash
sudo systemctl status boss-chat
curl http://127.0.0.1:9090/api/auth/me
```

第一次一定建议手动跑通，后面再接 CI。这样 CI 出问题时容易判断是流水线问题，还是服务器配置问题。

### 15.3 准备 SSH 部署账号

推荐创建专门的部署账号，不要一直用 root：

```bash
sudo adduser deploy
sudo usermod -aG sudo deploy
```

给部署账号创建 SSH 目录：

```bash
sudo mkdir -p /home/deploy/.ssh
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
```

在本地生成一组专门给 CI 用的 SSH Key：

```bash
ssh-keygen -t ed25519 -C "boss-chat-ci" -f boss-chat-ci
```

把公钥 `boss-chat-ci.pub` 内容追加到服务器：

```bash
sudo nano /home/deploy/.ssh/authorized_keys
sudo chown deploy:deploy /home/deploy/.ssh/authorized_keys
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

测试：

```bash
ssh -i boss-chat-ci deploy@你的服务器公网IP
```

### 15.4 允许 deploy 重启服务

CI 需要重启 `boss-chat` 服务，可以给 deploy 用户配置免密执行指定命令。

```bash
sudo visudo
```

添加：

```text
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart boss-chat, /bin/systemctl status boss-chat, /bin/systemctl reload nginx
```

如果你的系统 `systemctl` 路径不是 `/bin/systemctl`，先查一下：

```bash
which systemctl
```

### 15.5 CI 需要配置的密钥

在 CI 平台的 Secrets / Variables 中配置：

| 名称 | 内容 |
| --- | --- |
| `ALIYUN_HOST` | ECS 公网 IP 或域名 |
| `ALIYUN_USER` | SSH 用户，例如 `deploy` |
| `ALIYUN_SSH_KEY` | 私钥内容，也就是 `boss-chat-ci` 文件内容 |
| `ALIYUN_APP_DIR` | `/opt/boss-chat` |

不要把数据库密码、模型 API Key 写进 CI。它们应该放在服务器的：

```text
/opt/boss-chat/app/application-local.yml
```

CI 只负责上传程序，不负责携带生产密钥。

### 15.6 GitHub Actions 示例

如果使用 GitHub Actions，可以创建：

```text
.github/workflows/deploy-aliyun.yml
```

内容示例：

```yaml
name: Deploy to Aliyun ECS

on:
  workflow_dispatch:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Build backend
        working-directory: boss-chat-server
        run: mvn clean package -DskipTests

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: boss-chat-web/package-lock.json

      - name: Build web
        working-directory: boss-chat-web
        run: |
          npm ci
          npm run build

      - name: Prepare SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.ALIYUN_SSH_KEY }}" > ~/.ssh/aliyun_key
          chmod 600 ~/.ssh/aliyun_key
          ssh-keyscan -H "${{ secrets.ALIYUN_HOST }}" >> ~/.ssh/known_hosts

      - name: Upload backend jar
        run: |
          scp -i ~/.ssh/aliyun_key boss-chat-server/target/boss-chat-server-0.1.0.jar \
            ${{ secrets.ALIYUN_USER }}@${{ secrets.ALIYUN_HOST }}:${{ secrets.ALIYUN_APP_DIR }}/app/boss-chat-server-0.1.0.jar

      - name: Upload web dist
        run: |
          rsync -avz --delete -e "ssh -i ~/.ssh/aliyun_key" \
            boss-chat-web/dist/ \
            ${{ secrets.ALIYUN_USER }}@${{ secrets.ALIYUN_HOST }}:${{ secrets.ALIYUN_APP_DIR }}/web/

      - name: Restart backend
        run: |
          ssh -i ~/.ssh/aliyun_key ${{ secrets.ALIYUN_USER }}@${{ secrets.ALIYUN_HOST }} \
            "sudo systemctl restart boss-chat && sudo systemctl status boss-chat --no-pager"
```

说明：

- `workflow_dispatch` 表示可以在 GitHub 页面手动点按钮部署。
- `push main` 表示推送到 `main` 分支后自动部署。
- 如果不想每次 push 都自动部署，可以删除 `push` 部分，只保留手动部署。
- `rsync --delete` 会让服务器前端目录和本次构建结果保持一致。

### 15.7 Gitee 流水线思路

如果使用 Gitee，流程也是一样：

```text
拉取代码
安装 JDK 17
mvn clean package -DskipTests
安装 Node.js
npm ci
npm run build
通过 SSH 上传 jar 和 dist
执行 sudo systemctl restart boss-chat
```

Gitee 的变量也按下面几个配置即可：

```text
ALIYUN_HOST
ALIYUN_USER
ALIYUN_SSH_KEY
ALIYUN_APP_DIR
```

脚本逻辑可以直接参考 GitHub Actions 里的命令。

### 15.8 CI 部署后的验证命令

可以在 CI 最后加一段健康检查：

```bash
curl -I http://你的域名/
curl -I http://你的域名/survey/enterprise-diagnosis.html
curl -I http://你的域名/api/auth/me
```

如果已经启用 HTTPS，就改成：

```bash
curl -I https://你的域名/
curl -I https://你的域名/survey/enterprise-diagnosis.html
curl -I https://你的域名/api/auth/me
```

### 15.9 一键部署注意事项

- 不要把 `application-local.yml` 由 CI 覆盖掉，里面有生产数据库和 API Key。
- 不要把生产 API Key 写入仓库。
- 数据库迁移由后端启动时 Flyway 自动执行，部署前要确保迁移脚本正确。
- 第一次部署建议手动执行，确认服务器、数据库、Nginx、systemd 全部没问题后，再接 CI。
- 如果流式输出在生产环境变成一次性返回，优先检查 Nginx 是否配置了 `proxy_buffering off`。
## 2026-05-28 实测更新

当前测试服务器已采用 Alibaba Cloud Linux 4 + Jenkins 本机部署方式跑通，不再是单纯手工上传 jar 的流程。公网入口统一走 Nginx `80`，Jenkins 临时管理入口为 `9999`，后端 `9090` 和 MySQL `3306` 不对公网开放。

当前实测 URL、Nginx、Jenkins、健康检查与安全组收口说明见：[当前服务器部署 URL 与联调说明](./当前服务器部署URL与联调说明.md)。

---

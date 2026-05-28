# Jenkins 本机部署完整复刻流程

这份文档用于在另一台阿里云服务器上复刻当前部署方式。

当前推荐方案：

```text
Jenkins 安装在业务服务器本机
Jenkins 从 GitHub 拉代码
Jenkins 在本机打包后端和前端
Jenkins 直接部署到 /opt/boss-chat
Jenkins 重启 boss-chat 服务
```

这种方式不需要 SSH 私钥，不需要配置远程服务器 IP，适合单台服务器部署测试版或初版上线。

## 0. 当前执行进度（2026-05-28）

本节是接力用进度记录。当前不要从头重装 Jenkins，先按这里确认进度。

已经完成：

- 已选择“Jenkins 安装在业务服务器本机”的部署方式。
- 阿里云 ECS 使用 4 核 8G，系统为 Alibaba Cloud Linux 4 LTS。
- Jenkins 已安装成功，版本 `2.555.2`。
- Jenkins 服务已启动成功，曾确认 `active (running)`，默认端口 `8080`。
- 项目仓库已准备本机部署版 `Jenkinsfile`。
- 项目已推送到 GitHub：

```text
https://github.com/zhouya166913-cell/Boos-Chat.git
```

插件处理进度：

- 首次推荐插件安装时有部分失败，这是正常网络问题，不必重装 Jenkins。
- 后续已在插件管理里看到 Git、Pipeline、Credentials Binding 等插件方向。
- `SSH Build Agents plugin` 和 `SSH Agent Plugin` 不是同一个东西：
  - `SSH Build Agents plugin` 用于远程构建节点。
  - `SSH Agent Plugin` 用于 Pipeline 使用 SSH 凭据。
  - 当前本机部署不依赖 SSH Agent，后续远程部署才需要重点配置。

下一步先不要新建 Pipeline，先在服务器执行环境检查：

```bash
java -version
git --version
mvn -version
node -v
npm -v
curl --version
nginx -v
mysql --version
systemctl status jenkins --no-pager
```

如果这些工具缺失，再从本文第 9 步继续补装。确认工具都正常后，再继续准备 `/opt/boss-chat`、`application-local.yml`、`boss-chat.service`、Nginx 和 Jenkins Pipeline。

本机部署不需要在 Jenkins 里配置服务器 IP，也不需要 SSH 私钥。敏感配置只放服务器本地：

```text
/opt/boss-chat/config/application-local.yml
```

## 1. 服务器建议配置

推荐：

```text
系统：Alibaba Cloud Linux 4 / CentOS / Rocky / AlmaLinux
CPU：4 核
内存：8G
系统盘：40G 或以上
带宽：按需
```

2 核 4G 也能跑，但 Jenkins 构建时可能会比较卡。

## 2. 阿里云安全组放行端口

至少放行：

```text
22    SSH 登录
80    网站访问
443   HTTPS，后续配置证书时使用
8080  Jenkins 初始化和管理页面
9090  后端服务端口，如果你需要外部直接访问后端
```

正式上线后，建议 Jenkins 不长期暴露 8080，可以后续用 Nginx 反代并加登录限制。

## 3. 安装 Jenkins 之前先装 Java

Jenkins 2.555.2 LTS 建议使用 Java 21。

```bash
sudo yum install -y wget fontconfig git tar gzip openssh-clients
sudo yum install -y java-21-openjdk java-21-openjdk-devel
java -version
```

看到 `openjdk 21` 即可。

## 4. 安装 Jenkins LTS

添加 Jenkins LTS 源：

```bash
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
```

安装：

```bash
sudo yum upgrade -y
sudo yum install -y jenkins
```

如果安装包下载失败，可以手动下载 rpm 后本地安装：

```bash
wget -c https://get.jenkins.io/redhat-stable/jenkins-2.555.2-1.1.noarch.rpm
sudo yum localinstall -y jenkins-2.555.2-1.1.noarch.rpm
```

启动 Jenkins：

```bash
sudo systemctl daemon-reload
sudo systemctl enable jenkins
sudo systemctl start jenkins
sudo systemctl status jenkins
```

看到：

```text
Active: active (running)
```

说明 Jenkins 启动成功。

## 5. 获取 Jenkins 初始密码

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

复制输出的密码，然后浏览器访问：

```text
http://服务器公网IP:8080
```

粘贴初始密码进入 Jenkins。

## 6. 初始化 Jenkins

插件安装页面如果失败，不要一直重试，可以先点：

```text
继续
```

进入 Jenkins 后再手动配置插件源和插件。

创建管理员账号后进入首页。

## 7. 修改 Jenkins 插件源

进入：

```text
系统管理 -> 插件管理 -> Advanced settings
```

把升级站点 URL 从：

```text
https://updates.jenkins.io/update-center.json
```

改成：

```text
https://mirrors.tuna.tsinghua.edu.cn/jenkins/updates/update-center.json
```

保存后重启 Jenkins：

```bash
sudo systemctl restart jenkins
```

等待 20 到 40 秒后刷新 Jenkins 页面。

## 8. 安装并确认 Jenkins 插件

进入：

```text
系统管理 -> 插件管理
```

需要确认这些插件已安装并启用：

```text
Git
Pipeline
Credentials Binding
SSH Agent
```

本机部署版不依赖 SSH Agent，但保留它没有影响，后续如果升级为远程部署可以继续使用。

在 `Installed plugins` 中分别搜索：

```text
git
pipeline
credentials binding
ssh agent
```

确认右侧是启用状态。

Pipeline 至少应包含：

```text
Pipeline: Declarative
Pipeline: SCM Step
Pipeline: Groovy
Pipeline: Basic Steps
```

## 9. 安装构建工具

Jenkins 构建项目需要：

```text
Git
Maven
Node.js
npm
tar
curl
Java
```

安装基础工具：

```bash
sudo yum install -y git maven tar gzip curl
```

检查：

```bash
git --version
mvn -v
java -version
curl --version
```

安装 Node.js 22。可以使用 NodeSource：

```bash
curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo bash -
sudo yum install -y nodejs
```

检查：

```bash
node -v
npm -v
```

建议看到 Node.js 22.x。

## 10. 准备部署目录

执行：

```bash
sudo mkdir -p /opt/boss-chat/app /opt/boss-chat/web /opt/boss-chat/backup /opt/boss-chat/config /opt/boss-chat/uploads
sudo chown -R jenkins:jenkins /opt/boss-chat
```

目录用途：

```text
/opt/boss-chat/app      后端 jar
/opt/boss-chat/web      前端静态文件
/opt/boss-chat/backup   旧 jar 备份
/opt/boss-chat/config   后端本地配置
/opt/boss-chat/uploads  本地上传文件和图片
```

## 11. 准备后端本地配置

建议配置文件放在：

```text
/opt/boss-chat/config/application-local.yml
```

这个文件要包含数据库、AI Key、OSS/COS 等本地配置。

注意：

```text
不要把 application-local.yml 提交到 GitHub
不要把 API Key 写进 Jenkinsfile
不要把密钥写进公开仓库
```

## 12. 配置 boss-chat systemd 服务

创建服务文件：

```bash
sudo vi /etc/systemd/system/boss-chat.service
```

写入：

```ini
[Unit]
Description=Boss Chat Server
After=network.target mysql.service

[Service]
Type=simple
WorkingDirectory=/opt/boss-chat
ExecStart=/usr/bin/java -jar /opt/boss-chat/app/boss-chat-server-0.1.0.jar --spring.profiles.active=local --spring.config.additional-location=file:/opt/boss-chat/config/application-local.yml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

保存后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable boss-chat
```

第一次 jar 还没部署前，启动失败是正常的，等 Jenkins 第一次构建完成后再启动。

## 13. 允许 Jenkins 重启 boss-chat 服务

Jenkins 需要执行：

```bash
sudo systemctl restart boss-chat
sudo systemctl status boss-chat --no-pager
```

先确认 systemctl 路径：

```bash
which systemctl
```

如果输出：

```text
/usr/bin/systemctl
```

则执行：

```bash
sudo visudo -f /etc/sudoers.d/boss-chat-jenkins
```

写入：

```text
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart boss-chat, /usr/bin/systemctl status boss-chat
```

保存后检查：

```bash
sudo cat /etc/sudoers.d/boss-chat-jenkins
```

如果 `which systemctl` 输出不是 `/usr/bin/systemctl`，要把上面的路径替换成实际路径。

## 14. 配置 Nginx

前端目录：

```text
/opt/boss-chat/web
```

后端默认端口：

```text
9090
```

Nginx 应该至少代理：

```text
/        -> /opt/boss-chat/web
/api/    -> http://127.0.0.1:9090
/survey/ -> http://127.0.0.1:9090
```

当前问卷静态页放在后端 jar 中，不在 Vue 前端构建目录中，所以 `/survey/` 必须走后端代理。推荐配置：

```nginx
server {
    listen 80;
    server_name _;

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

配置完成后检查：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 15. 创建 Jenkins Pipeline 任务

Jenkins 首页点击：

```text
新建任务
```

填写：

```text
任务名称：boss-chat-deploy
类型：Pipeline
```

进入任务配置页，找到 Pipeline 区域：

```text
Definition：Pipeline script from SCM
SCM：Git
Repository URL：https://github.com/zhouya166913-cell/Boos-Chat.git
Branch Specifier：*/main
Script Path：Jenkinsfile
```

保存。

## 16. 第一次构建

进入任务页面，点击：

```text
Build with Parameters
```

参数保持默认：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `DEPLOY_DIR` | `/opt/boss-chat` | 本机部署目录 |
| `SERVICE_NAME` | `boss-chat` | systemd 服务名 |
| `HEALTH_URL` | `http://127.0.0.1:9090/api/health` | Jenkins 重启服务后的本机健康检查地址 |

点击构建。

构建成功后，Jenkins 会自动：

```text
拉取代码
打包后端
打包前端
备份旧 jar
部署新 jar
部署前端文件
重启 boss-chat
访问 /api/health 确认服务可用
输出服务状态
```

## 17. 验证访问

访问：

```text
http://服务器公网IP/
http://服务器公网IP/api/health
http://服务器公网IP/survey/enterprise-diagnosis.html
```

也可以检查后端：

```bash
sudo systemctl status boss-chat --no-pager
journalctl -u boss-chat -n 100 --no-pager
```

## 18. 常见问题

### Jenkins 插件安装失败

先修改插件源为清华镜像，再重启 Jenkins。

### 找不到 mvn

执行：

```bash
sudo yum install -y maven
mvn -v
```

### 找不到 npm 或 node

安装 Node.js 22：

```bash
curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo bash -
sudo yum install -y nodejs
node -v
npm -v
```

### Jenkins 无法写入 /opt/boss-chat

执行：

```bash
sudo chown -R jenkins:jenkins /opt/boss-chat
```

### sudo systemctl 需要密码

检查：

```bash
sudo cat /etc/sudoers.d/boss-chat-jenkins
```

确认里面是：

```text
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart boss-chat, /usr/bin/systemctl status boss-chat
```

### 后端启动失败

查看日志：

```bash
journalctl -u boss-chat -n 100 --no-pager
```

重点看：

```text
数据库连接是否正确
端口 9090 是否被占用
application-local.yml 是否存在
AI Key 是否配置
Flyway 数据库迁移是否失败
```

### 前端访问 404

检查：

```bash
ls -la /opt/boss-chat/web
```

确认里面有前端打包后的文件。

再检查 Nginx：

```bash
sudo nginx -t
sudo systemctl status nginx --no-pager
```

## 19. 日常更新流程

以后更新代码只需要：

```text
本地修改代码
git push 到 GitHub
打开 Jenkins
进入 boss-chat-deploy
点击 Build with Parameters
点击 Build
等待构建完成
```

不需要手动登录服务器拉代码、打包、复制文件。

## 20. 当前方案与远程部署方案的区别

当前本机部署版：

```text
Jenkins 和项目在同一台服务器
不需要 SSH 凭据
最简单，适合单服务器
```

远程部署版：

```text
Jenkins 单独一台机器
项目部署到另一台服务器
需要 SSH 私钥和远程上传
适合多环境、多服务器
```

当前阶段建议继续使用本机部署版。

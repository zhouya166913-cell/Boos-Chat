# Jenkins 本机一键部署教程

这份教程适用于当前最简单的部署方式：

```text
GitHub 仓库 -> Jenkins 拉代码 -> 本机打包 -> 本机部署 -> 重启 boss-chat 服务
```

当前你的 Jenkins 和项目都在同一台阿里云 ECS 上，所以不需要配置 SSH 私钥，也不需要配置远程服务器 IP。

## 1. 当前项目已经准备好的文件

项目根目录已经有：

```text
Jenkinsfile
```

Jenkins 会按这个文件自动执行：

```text
拉取 GitHub 代码
打包后端 boss-chat-server
打包前端 boss-chat-web
复制 jar 到 /opt/boss-chat/app
复制前端 dist 到 /opt/boss-chat/web
重启 boss-chat 服务
```

Jenkins 不会提交或覆盖这些敏感配置：

```text
数据库密码
AI API Key
OSS/COS 密钥
用户上传文件
运行日志
```

这些内容应保留在服务器本地配置文件中。

## 2. 服务器目录准备

在阿里云服务器执行：

```bash
sudo mkdir -p /opt/boss-chat/app /opt/boss-chat/web /opt/boss-chat/backup /opt/boss-chat/config
sudo chown -R jenkins:jenkins /opt/boss-chat
```

推荐把后端本地配置放在：

```text
/opt/boss-chat/config/application-local.yml
```

这个文件不要提交到 GitHub。

## 3. Jenkins 需要的插件

进入：

```text
系统管理 -> 插件管理 -> Installed plugins
```

确认这些插件已经启用：

```text
Git
Pipeline
SSH Agent
Credentials Binding
```

本机部署版不依赖 SSH Agent，但保留它也没问题，后面如果要升级成远程部署可以继续用。

## 4. Jenkins 机器需要的构建工具

Jenkins 所在服务器需要能执行：

```bash
git --version
java -version
mvn -v
node -v
npm -v
tar --version
```

当前项目建议：

```text
Java 17 或 21
Maven 3.8+
Node.js 22
```

Alibaba Cloud Linux 可以按需安装：

```bash
sudo yum install -y git maven tar gzip
```

如果 Node.js 版本太低，建议安装 Node.js 22。

## 5. 允许 Jenkins 重启后端服务

Jenkins 最后会执行：

```bash
sudo systemctl restart boss-chat
sudo systemctl status boss-chat --no-pager
```

所以要给 `jenkins` 用户配置免密执行这两个命令。

先查看 systemctl 路径：

```bash
which systemctl
```

常见结果是：

```text
/usr/bin/systemctl
```

创建 sudo 授权文件：

```bash
sudo visudo -f /etc/sudoers.d/boss-chat-jenkins
```

写入下面内容。如果你的 `systemctl` 路径不是 `/usr/bin/systemctl`，要替换成实际路径：

```text
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart boss-chat, /usr/bin/systemctl status boss-chat
```

保存后检查：

```bash
sudo cat /etc/sudoers.d/boss-chat-jenkins
```

## 6. 准备 systemd 服务

后端服务名建议固定为：

```text
boss-chat
```

服务文件路径：

```text
/etc/systemd/system/boss-chat.service
```

示例：

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

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable boss-chat
```

第一次 jar 还没部署前，启动失败是正常的。等 Jenkins 构建成功后再看状态。

## 7. 创建 Jenkins Pipeline 任务

Jenkins 首页点击：

```text
新建任务
```

填写：

```text
任务名称：boss-chat-deploy
类型：Pipeline
```

进入配置页，找到 Pipeline 区域：

```text
Definition：Pipeline script from SCM
SCM：Git
Repository URL：https://github.com/zhouya166913-cell/Boos-Chat.git
Branch Specifier：*/main
Script Path：Jenkinsfile
```

保存。

## 8. 第一次构建

进入任务页面，点击：

```text
Build with Parameters
```

参数保持默认即可：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `DEPLOY_DIR` | `/opt/boss-chat` | 本机部署目录 |
| `SERVICE_NAME` | `boss-chat` | systemd 服务名 |

点击构建。

成功后访问：

```text
http://服务器公网IP/
http://服务器公网IP/survey/enterprise-diagnosis.html
```

## 9. 常见问题

### 1. 找不到 mvn、node 或 npm

说明 Jenkins 所在服务器缺少构建工具。

检查：

```bash
mvn -v
node -v
npm -v
```

### 2. 无法写入 /opt/boss-chat

执行：

```bash
sudo chown -R jenkins:jenkins /opt/boss-chat
```

### 3. sudo systemctl 需要密码

说明第 5 步 sudo 授权没配好。

检查：

```bash
sudo cat /etc/sudoers.d/boss-chat-jenkins
```

### 4. 后端重启失败

查看日志：

```bash
journalctl -u boss-chat -n 100 --no-pager
```

重点看：

```text
数据库连接
端口占用
application-local.yml
AI Key 配置
Flyway 数据库迁移
```

### 5. 前端访问 404

检查 Nginx 是否指向：

```text
/opt/boss-chat/web
```

并确认 Jenkins 构建日志里前端打包成功。

## 10. 后续维护方式

以后你的日常流程是：

```text
本地修改代码
git push 到 GitHub
打开 Jenkins
点击 boss-chat-deploy 构建
等待部署完成
```

如果以后要拆成多台服务器，再把 `Jenkinsfile` 升级成 SSH 远程部署版即可。

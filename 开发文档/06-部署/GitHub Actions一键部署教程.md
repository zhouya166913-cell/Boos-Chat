# GitHub Actions 一键部署教程

这份教程是给第一次使用 CI 的版本。目标是：你把代码推到 GitHub 后，在 GitHub 页面点一个按钮，系统自动打包后端、打包前端，并上传到阿里云服务器完成部署。

当前 CI 采用“手动触发”，不会因为你误提交代码就自动更新线上服务。跑通以后，再考虑改成 push 到 main 自动部署。

## 一、CI 会做什么

流程是：

```text
GitHub 拉取代码
安装 Java 17
打包 boss-chat-server 后端 jar
安装 Node.js 22
打包 boss-chat-web 前端 dist
通过 SSH 上传到阿里云服务器
替换 /opt/boss-chat/app 里的 jar
替换 /opt/boss-chat/web 里的前端文件
重启 boss-chat 后端服务
```

CI 不会上传这些内容：

```text
application-local.yml
数据库密码
AI API Key
用户上传文件
运行日志
```

这些敏感配置继续放在服务器本地。

## 二、服务器先准备好一次

第一次接 CI 前，建议你先让服务器手动部署跑通一次。服务器上至少要有：

```text
/opt/boss-chat/app
/opt/boss-chat/web
/opt/boss-chat/backup
/opt/boss-chat/app/application-local.yml
/etc/systemd/system/boss-chat.service
Nginx 配置
MySQL 数据库
```

确认后端服务名叫：

```text
boss-chat
```

可以在服务器执行：

```bash
sudo systemctl status boss-chat
```

如果这个命令能看到服务状态，说明 CI 后续可以重启它。

## 三、创建部署用户

推荐新建一个专门部署用的用户，不要长期用 root。

在服务器执行：

```bash
sudo adduser deploy
sudo usermod -aG sudo deploy
```

让 deploy 用户可以写入项目目录：

```bash
sudo chown -R deploy:deploy /opt/boss-chat
```

如果你后端服务使用 `bosschat` 用户运行，也可以后续再细化权限。测试阶段先保证 CI 能写入部署目录最重要。

## 四、创建 CI 专用 SSH Key

在你自己的电脑上执行：

```bash
ssh-keygen -t ed25519 -C "boss-chat-ci" -f boss-chat-ci
```

执行后会生成两个文件：

```text
boss-chat-ci       私钥，放到 GitHub Secrets
boss-chat-ci.pub   公钥，放到服务器
```

把公钥内容添加到服务器：

```bash
sudo mkdir -p /home/deploy/.ssh
sudo nano /home/deploy/.ssh/authorized_keys
```

把 `boss-chat-ci.pub` 里的内容复制进去，保存后执行：

```bash
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

在本地测试能否登录：

```bash
ssh -i boss-chat-ci deploy@你的服务器公网IP
```

能登录，说明 SSH Key 没问题。

## 五、允许 CI 重启后端服务

CI 最后一步需要执行：

```bash
sudo systemctl restart boss-chat
```

所以要给 deploy 用户免密执行这个命令。

在服务器执行：

```bash
sudo visudo
```

在文件底部添加：

```text
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart boss-chat, /bin/systemctl status boss-chat
```

如果你的服务器上 `systemctl` 路径不是 `/bin/systemctl`，先执行：

```bash
which systemctl
```

然后把上面的路径改成实际路径。

测试：

```bash
sudo -u deploy sudo systemctl status boss-chat
```

如果不要求输入密码，就说明配置成功。

## 六、在 GitHub 配置 Secrets

进入 GitHub 仓库：

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

新增下面这些：

| 名称 | 示例 | 说明 |
| --- | --- | --- |
| `ALIYUN_HOST` | `你的服务器公网IP` | 阿里云服务器 IP 或域名 |
| `ALIYUN_USER` | `deploy` | SSH 登录用户 |
| `ALIYUN_SSH_KEY` | `boss-chat-ci` 文件里的全部内容 | 注意是私钥，不是 `.pub` 公钥 |
| `ALIYUN_APP_DIR` | `/opt/boss-chat` | 服务器部署目录 |
| `ALIYUN_SSH_PORT` | `22` | 可选，不填默认 22 |

最容易填错的是 `ALIYUN_SSH_KEY`。它要复制私钥文件完整内容，通常长这样：

```text
-----BEGIN OPENSSH PRIVATE KEY-----
中间很多行
-----END OPENSSH PRIVATE KEY-----
```

## 七、运行一键部署

我已经在仓库里新增了：

```text
.github/workflows/deploy-aliyun.yml
```

推送到 GitHub 后，打开仓库页面：

```text
Actions -> Deploy to Aliyun ECS -> Run workflow
```

点击运行后，它会自动执行：

```text
Build backend
Build admin web
Upload release files
Activate release
Restart backend
```

如果全部变绿，就说明部署成功。

## 八、验证线上是否正常

部署成功后，打开：

```text
http://你的服务器公网IP/
http://你的服务器公网IP/survey/enterprise-diagnosis.html
```

如果配置了域名和 HTTPS，就用：

```text
https://你的域名/
https://你的域名/survey/enterprise-diagnosis.html
```

服务器上也可以看日志：

```bash
journalctl -u boss-chat -f
```

## 九、常见失败原因

### 1. Permission denied

一般是 SSH Key 没配好，检查：

```text
ALIYUN_SSH_KEY 是否填的是私钥
服务器 authorized_keys 是否放的是公钥
deploy 用户是否存在
```

### 2. 没有权限写入 /opt/boss-chat

在服务器执行：

```bash
sudo chown -R deploy:deploy /opt/boss-chat
```

### 3. sudo systemctl restart 要求输入密码

说明 `visudo` 没配好，重新检查第五步。

### 4. 前端打开还是旧页面

浏览器强制刷新，或者检查 CI 是否成功替换：

```bash
ls -la /opt/boss-chat/web
```

### 5. 后端启动失败

看日志：

```bash
journalctl -u boss-chat -n 100 --no-pager
```

重点看数据库连接、端口占用、配置文件路径和 API Key 配置。

## 十、后续可以升级成自动部署

现在是手动点击部署。等你熟悉后，可以把 `.github/workflows/deploy-aliyun.yml` 改成：

```yaml
on:
  workflow_dispatch:
  push:
    branches:
      - main
```

这样每次推送到 `main` 分支，都会自动部署到服务器。

测试期我不建议一开始就这样做。先手动点按钮，更可控。

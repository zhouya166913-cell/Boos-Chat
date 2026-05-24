# Jenkins 一键部署教程

可以使用 Jenkins 部署本项目。Jenkins 和 GitHub Actions 一样，都是 CI/CD 工具。区别是：

| 工具 | 适合情况 |
| --- | --- |
| GitHub Actions | 代码在 GitHub，想少维护一台 CI 机器 |
| Jenkins | 想自己控制部署机器、部署流程、插件和权限 |

如果你之前已经在轻量服务器上拉源码运行过项目，那么 Jenkins 的理解方式可以很简单：

```text
以前：你登录服务器，手动拉代码、手动编译、手动重启
现在：Jenkins 替你做这些事，你只需要点一次“构建”
```

## 一、当前项目已经准备好的文件

项目根目录已经新增：

```text
Jenkinsfile
```

Jenkins 会按这个文件执行：

```text
拉取代码
打包后端 boss-chat-server
打包前端 boss-chat-web
上传 jar 到 /opt/boss-chat/app
上传 dist 到 /opt/boss-chat/web
重启 boss-chat 服务
```

Jenkins 不会上传：

```text
application-local.yml
数据库密码
AI API Key
用户上传文件
日志
```

这些仍然保留在服务器本地。

## 二、服务器先准备好

Jenkins 部署前，阿里云服务器需要先手动跑通过一次。服务器上应该有：

```text
/opt/boss-chat/app
/opt/boss-chat/web
/opt/boss-chat/backup
/opt/boss-chat/app/application-local.yml
/etc/systemd/system/boss-chat.service
Nginx
MySQL
```

确认后端服务可以被 systemd 管理：

```bash
sudo systemctl status boss-chat
```

确认 Nginx 已代理：

```text
/           管理后台前端
/api/       后端接口
/survey/    调查问卷静态页
```

## 三、安装 Jenkins

如果你想简单试，可以把 Jenkins 装在同一台阿里云服务器上。正式一点的话，Jenkins 可以单独放一台机器。

Ubuntu 示例：

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk curl gnupg

curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install -y jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins
```

打开：

```text
http://服务器公网IP:8080
```

首次密码查看：

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

阿里云安全组需要临时放行：

```text
8080
```

正式使用建议给 Jenkins 配 Nginx 和 HTTPS，不要长期裸露 8080。

## 四、Jenkins 需要安装的工具

Jenkins 机器需要能执行：

```bash
java -version
mvn -v
node -v
npm -v
tar --version
ssh -V
scp
```

Ubuntu 安装示例：

```bash
sudo apt install -y openjdk-17-jdk maven nodejs npm tar openssh-client
```

如果 Node.js 版本太低，推荐安装 Node.js 22：

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

## 五、安装 Jenkins 插件

进入 Jenkins：

```text
Manage Jenkins -> Plugins
```

确认安装：

```text
Git
Pipeline
SSH Agent
```

`SSH Agent` 插件很重要，Jenkinsfile 会用它读取 SSH 私钥。

## 六、准备 SSH 部署用户

推荐服务器上新建部署用户：

```bash
sudo adduser deploy
sudo usermod -aG sudo deploy
sudo chown -R deploy:deploy /opt/boss-chat
```

在 Jenkins 机器上生成 SSH Key：

```bash
ssh-keygen -t ed25519 -C "boss-chat-jenkins" -f boss-chat-jenkins
```

得到：

```text
boss-chat-jenkins       私钥，放进 Jenkins
boss-chat-jenkins.pub   公钥，放进服务器
```

把公钥加入服务器：

```bash
sudo mkdir -p /home/deploy/.ssh
sudo nano /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

测试：

```bash
ssh -i boss-chat-jenkins deploy@服务器公网IP
```

## 七、允许 deploy 重启服务

Jenkins 最后会执行：

```bash
sudo systemctl restart boss-chat
sudo systemctl status boss-chat
```

所以需要免密授权：

```bash
sudo visudo
```

添加：

```text
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart boss-chat, /bin/systemctl status boss-chat
```

如果你的 `systemctl` 不是 `/bin/systemctl`：

```bash
which systemctl
```

然后把路径改成实际路径。

## 八、在 Jenkins 添加 SSH 凭据

进入：

```text
Manage Jenkins -> Credentials -> System -> Global credentials -> Add Credentials
```

选择：

```text
Kind: SSH Username with private key
ID: boss-chat-aliyun-ssh
Username: deploy
Private Key: 复制 boss-chat-jenkins 私钥完整内容
```

注意：`ID` 必须是：

```text
boss-chat-aliyun-ssh
```

因为 Jenkinsfile 里写的是这个 ID。

## 九、创建 Jenkins 任务

新建任务：

```text
New Item -> Pipeline
```

名称可以叫：

```text
boss-chat-deploy
```

配置：

```text
Pipeline -> Definition -> Pipeline script from SCM
SCM -> Git
Repository URL -> https://github.com/zhouya166913-cell/Boos-Chat.git
Branch Specifier -> */main
Script Path -> Jenkinsfile
```

保存。

## 十、第一次构建

点击：

```text
Build with Parameters
```

填写：

| 参数 | 示例 |
| --- | --- |
| `DEPLOY_HOST` | 你的服务器公网 IP |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_PORT` | `22` |
| `DEPLOY_DIR` | `/opt/boss-chat` |
| `SERVICE_NAME` | `boss-chat` |

然后点击构建。

成功后，访问：

```text
http://你的服务器公网IP/
http://你的服务器公网IP/survey/enterprise-diagnosis.html
```

## 十一、常见失败原因

### 1. 找不到 mvn / npm

说明 Jenkins 机器没装 Maven 或 Node.js。

检查：

```bash
mvn -v
node -v
npm -v
```

### 2. Permission denied

说明 SSH Key 或部署用户没配好。

检查：

```text
Jenkins Credentials ID 是否是 boss-chat-aliyun-ssh
私钥是否填完整
服务器 authorized_keys 是否放了公钥
DEPLOY_USER 是否是 deploy
```

### 3. 无法写入 /opt/boss-chat

服务器执行：

```bash
sudo chown -R deploy:deploy /opt/boss-chat
```

### 4. sudo systemctl 要密码

说明 `visudo` 没配好。重新检查第七步。

### 5. 后端重启失败

服务器查看日志：

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

## 十二、Jenkins 和 GitHub Actions 选哪个

如果你只是想最快上线，GitHub Actions 更省事。

如果你想自己掌控部署流程、以后加测试环境、正式环境、回滚按钮、定时任务，Jenkins 更灵活。

当前项目两套都已经可以保留：

```text
.github/workflows/deploy-aliyun.yml   GitHub Actions 部署
Jenkinsfile                           Jenkins 部署
```

你可以先试 Jenkins，跑通后再决定最终保留哪套。

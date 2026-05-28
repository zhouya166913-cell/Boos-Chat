# 当前服务器部署 URL 与联调说明

更新时间：2026-05-28

本文记录当前阿里云 ECS 测试服务器的实际部署入口、反向代理规则、Jenkins 状态，以及本地前端开发时如何切换本地后端和服务器后端。

## 一、当前服务器信息

```text
公网 IP：8.162.26.228
系统：Alibaba Cloud Linux 4
部署方式：Jenkins 本机部署
Jenkins 版本：2.555.2
后端服务名：boss-chat
后端内网端口：9090
Jenkins 端口：9999
Web 入口端口：80
```

当前部署目录：

```text
/opt/boss-chat/app      后端 jar
/opt/boss-chat/web      前端管理系统静态文件
/opt/boss-chat/backup   旧 jar 备份
/opt/boss-chat/config   服务器本地配置
/opt/boss-chat/uploads  上传文件目录
```

服务器敏感配置只放在：

```text
/opt/boss-chat/config/application-local.yml
```

不要把数据库密码、API Key、生产管理员密码写入 GitHub。

## 二、当前 URL 清单

| 模块 | URL | 说明 |
| --- | --- | --- |
| Jenkins | `http://8.162.26.228:9999/` | 构建与部署入口 |
| 前端管理系统 | `http://8.162.26.228/` | Vue 管理后台 |
| 前端登录页 | `http://8.162.26.228/login` | 管理后台登录页 |
| 后端健康检查 | `http://8.162.26.228/api/health` | Nginx 转发到后端 |
| Swagger | `http://8.162.26.228/swagger-ui.html` | 开发阶段接口文档入口 |
| 问卷调查页 | `http://8.162.26.228/survey/enterprise-diagnosis.html` | 客户填写问卷页面 |
| 后端本机直连 | `http://127.0.0.1:9090/` | 只在服务器本机访问 |
| 后端本机健康检查 | `http://127.0.0.1:9090/api/health` | Jenkins 部署验收使用 |

不要在阿里云安全组开放 `9090` 和 `3306` 到公网。

## 三、Nginx 反向代理规则

当前 Nginx 统一暴露 `80`，后端服务只监听服务器本机 `9090`。

当前实际规则：

```text
/                         -> /opt/boss-chat/web 前端静态文件
/api/...                  -> http://127.0.0.1:9090/api/...
/survey/...               -> http://127.0.0.1:9090/survey/...
/swagger-ui.html          -> http://127.0.0.1:9090/swagger-ui.html
/swagger-ui/...           -> http://127.0.0.1:9090/swagger-ui/...
/v3/api-docs              -> http://127.0.0.1:9090/v3/api-docs
```

Nginx 配置文件：

```text
/etc/nginx/nginx.conf
```

修改后验证：

```bash
nginx -t && systemctl reload nginx
```

## 四、Jenkins 部署任务

Jenkins 任务名：

```text
boss-chat-deploy
```

Jenkins Pipeline 配置：

```text
Definition：Pipeline script from SCM
SCM：Git
Repository URL：https://github.com/zhouya166913-cell/Boos-Chat.git
Credentials：无
Branches to build：*/main
Script Path：Jenkinsfile
```

Jenkins 当前端口已经从 `8080` 改为 `9999`。

systemd override 文件：

```text
/etc/systemd/system/jenkins.service.d/override.conf
```

当前关键环境：

```text
JENKINS_PORT=9999
JAVA_HOME=/usr/lib/jvm/java-21-alibaba-dragonwell-21.0.10.0.10-1.1.alnx4.x86_64
PATH 包含 Java 21 bin 目录
```

Jenkins 部署完成后会访问：

```text
http://127.0.0.1:9090/api/health
```

只有健康检查返回 `status: UP`，Jenkins 才认为部署成功。

## 五、本地前端如何切换后端接口

前端项目：

```text
boss-chat-web
```

前端代码里统一请求：

```text
/api
```

不要在 `src/api/http.ts`、`src/api/chat.ts`、`src/api/workbench.ts` 里来回注释地址。

当前开发环境配置文件：

```text
boss-chat-web/.env.development
```

默认内容：

```env
VITE_API_BASE_URL=/api

# Local backend
VITE_API_PROXY_TARGET=http://localhost:9090

# Server backend
# VITE_API_PROXY_TARGET=http://8.162.26.228
```

如果要从本地前端直接测试服务器后端，把注释切换成：

```env
VITE_API_BASE_URL=/api

# Local backend
# VITE_API_PROXY_TARGET=http://localhost:9090

# Server backend
VITE_API_PROXY_TARGET=http://8.162.26.228
```

切换后必须重启 Vite：

```bash
npm run dev
```

也可以在本机创建私有覆盖文件：

```text
boss-chat-web/.env.development.local
```

该文件已被 `.gitignore` 忽略，不会提交到 GitHub。

## 六、部署后验收命令

服务器执行：

```bash
systemctl status boss-chat --no-pager
ss -lntp | grep -E ':80|:9090|:9999' || true

curl -fsS http://127.0.0.1:9090/api/health
curl -fsS http://127.0.0.1/api/health

curl -I http://127.0.0.1/
curl -I http://127.0.0.1/login
curl -I http://127.0.0.1/survey/enterprise-diagnosis.html
curl -I http://127.0.0.1/swagger-ui.html
```

公网浏览器验收：

```text
http://8.162.26.228/
http://8.162.26.228/login
http://8.162.26.228/api/health
http://8.162.26.228/swagger-ui.html
http://8.162.26.228/survey/enterprise-diagnosis.html
http://8.162.26.228:9999/
```

## 七、安全组收口建议

开发测试期至少应收口：

```text
80    允许 0.0.0.0/0
443   申请 HTTPS 后允许 0.0.0.0/0
22    只允许当前办公公网 IP/32
9999  只允许当前办公公网 IP/32
```

建议删除或不要开放：

```text
8080  Jenkins 已改到 9999，不再使用
3389  Linux 服务器不需要 RDP
9090  后端只允许 Nginx 本机转发
3306  MySQL 不允许公网访问
```

Swagger 会暴露接口结构，只建议开发阶段临时开放。正式上线后应增加 IP 限制、登录限制，或关闭公网访问。

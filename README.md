# Boos Chat

企业 AI 管理助手项目，包含后端服务、管理后台、App、小程序和项目文档。

## 项目结构

```text
boss-chat-server   Spring Boot 后端服务
boss-chat-web      Vue 管理后台
boss-chat-app      Flutter App
boss-chat-miniapp  uni-app 小程序
开发文档             启动、架构、数据库、部署等说明
```

## 本地开发

后端：

```bash
cd boss-chat-server
mvn spring-boot:run
```

管理后台：

```bash
cd boss-chat-web
npm install
npm run dev
```

默认访问：

```text
后端：http://localhost:9090
管理后台：http://localhost:9000
```

## 部署说明

阿里云 ECS 部署教程见：

```text
开发文档/06-部署/阿里云ECS部署教程.md
```


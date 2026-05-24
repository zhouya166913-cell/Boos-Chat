# 智谱模型配置与 API Key 更换说明

本文档用于说明当前项目如何配置智谱模型，以及以后从“个人开发 Key”切换为“公司 Key”时应该怎么改。

当前适用项目：

```text
boss-chat-server
```

当前默认模型配置：

```text
provider=zhipu
baseUrl=https://open.bigmodel.cn/api/paas/v4
model=glm-4-flash-250414
```

---

## 一、当前项目里的配置分层

当前后端把智谱配置拆成两类：

### 1. 可公开的普通配置

这些值写在正式配置文件里：

```text
boss-chat-server/src/main/resources/application.yml
```

当前包括：

```yaml
app:
  llm:
    provider: ${LLM_PROVIDER:zhipu}
    base-url: ${LLM_BASE_URL:https://open.bigmodel.cn/api/paas/v4}
    api-key: ${LLM_API_KEY:}
    model: ${LLM_MODEL:glm-4-flash-250414}
```

其中：

- `provider`
- `base-url`
- `model`

都属于普通配置，可以进入代码仓库。

### 2. 不能公开的敏感配置

真正的 `API Key` 不应写死在正式配置文件里。

当前开发机支持两种方式提供 Key：

1. 本地文件：

```text
boss-chat-server/application-local.yml
```

2. 环境变量：

```powershell
$env:LLM_API_KEY="你的智谱 API Key"
```

当前项目启动时会自动读取：

```yaml
spring:
  config:
    import: optional:file:./application-local.yml
```

也就是说：

- 有 `application-local.yml` 时，优先直接读取本机文件
- 没有本地文件时，也可以继续用环境变量

---

## 二、当前开发阶段怎么使用

当前开发阶段，最方便的方式是使用本地文件：

```text
boss-chat-server/application-local.yml
```

内容示例：

```yaml
app:
  llm:
    api-key: "你的智谱 API Key"
```

这个文件已经加入：

```text
boss-chat-server/.gitignore
```

因此它不会被正常提交进代码仓库。

这样做的好处是：

- 本机开发时不用每次手动设置环境变量
- 以后更换 Key 时只改一个地方
- 不会把 Key 混进正式配置和代码里

---

## 三、以后改成公司的 Key 应该怎么做

如果以后要把当前个人 Key 改成公司 Key，按下面做即可。

### 方式 A：继续使用本地文件

适合：

- 你自己的开发机
- 公司内部测试机
- 暂时还没有统一部署平台的时候

操作：

1. 打开：

```text
boss-chat-server/application-local.yml
```

2. 把旧 Key 替换成新的公司 Key：

```yaml
app:
  llm:
    api-key: "新的公司 API Key"
```

3. 重启后端服务。

这就是最简单的更换方式。

### 方式 B：改为环境变量

适合：

- 服务器
- CI/CD
- Docker
- 未来正式上线环境

Windows PowerShell 示例：

```powershell
$env:LLM_API_KEY="新的公司 API Key"
mvn spring-boot:run
```

Linux 示例：

```bash
export LLM_API_KEY="新的公司 API Key"
java -jar boss-chat-server.jar
```

如果以后进入正式部署阶段，更推荐这种方式。

---

## 四、如果以后不只是换 Key，还要换模型怎么办

当前以下三个值都支持通过环境变量覆盖：

```text
LLM_PROVIDER
LLM_BASE_URL
LLM_MODEL
```

例如以后公司决定：

- 仍然用智谱，但换一个模型
- 切到别的兼容模型服务
- 临时测试不同模型

都可以只改配置，不必改业务代码。

示例：

```powershell
$env:LLM_PROVIDER="zhipu"
$env:LLM_BASE_URL="https://open.bigmodel.cn/api/paas/v4"
$env:LLM_MODEL="glm-4-flash-250414"
$env:LLM_API_KEY="新的公司 API Key"
mvn spring-boot:run
```

---

## 五、最推荐的长期做法

建议按阶段区分：

| 阶段 | 推荐方式 |
| --- | --- |
| 当前个人开发 | `application-local.yml` |
| 公司内部测试 | `application-local.yml` 或服务器环境变量 |
| 正式上线 | 服务器环境变量 / 部署平台密钥管理 |

长期建议：

1. `application.yml` 只放非敏感默认值
2. `API Key` 永远不要提交到仓库
3. 正式环境尽量不要依赖开发机上的本地文件
4. 更换公司 Key 时，先更新配置，再重启服务，再做一次真实对话验证

---

## 六、更换 Key 后如何验证是否成功

更换完成后，建议按下面顺序验证：

1. 启动后端
2. 登录管理系统
3. 进入仪表盘首页
4. 选择一个智能体
5. 发送一句简单问题
6. 确认能收到真实回复

如果后端报错：

- `模型服务尚未配置`

通常说明：

- `API Key`
- `baseUrl`
- `model`

三者中至少有一个没配上。

如果出现第三方接口报错，则优先检查：

- 新 Key 是否有效
- 模型名是否仍可用
- 当前账号是否有调用权限

---

## 七、当前项目中的相关文件

| 文件 | 用途 |
| --- | --- |
| `boss-chat-server/src/main/resources/application.yml` | 正式默认配置 |
| `boss-chat-server/application-local.yml` | 本机敏感配置 |
| `boss-chat-server/.gitignore` | 确保本地 Key 文件不提交 |
| `boss-chat-server/src/main/java/com/zhiyinhui/bosschat/common/config/LlmProperties.java` | 读取模型配置 |
| `boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/LlmChatService.java` | 实际调用模型接口 |

---

## 八、给未来自己的最短答案

如果你以后只想快速知道“公司 Key 到底改哪里”，看这一段就够：

### 本机开发

改：

```text
boss-chat-server/application-local.yml
```

### 服务器 / 正式环境

改：

```text
LLM_API_KEY
```

### 改完以后

重启后端，再去管理系统首页真实发一条消息确认。

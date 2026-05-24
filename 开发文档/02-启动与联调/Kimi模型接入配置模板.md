# Kimi 模型接入配置模板

本文档用于准备接入 Kimi / Moonshot 模型。

当前文档是模板版。等拿到 Kimi 官方文档地址、API Key、可用模型名后，再补齐真实参数并开始代码接入。

---

## 一、接入目标

当前目标不是把 Kimi 当成“独立智能体平台”，而是把 Kimi 作为新的模型供应商接入本系统。

在本项目中的定位：

```text
Kimi / Moonshot = 模型供应商
Kimi 模型 = 大脑
本系统 = 统一赋能平台
AI 助手 / 智能体 = 模型 + 提示词 + 记忆 + 知识库 + 工作流 + 工具
```

接入后，管理员可以在系统中创建或修改 AI 助手，使其底层模型使用 Kimi。

---

## 二、待确认官方参数

等待用户提供或根据官方文档确认：

| 项 | 当前状态 |
| --- | --- |
| 官方文档地址 | 待提供 |
| API Base URL | 待确认 |
| API Key | 待提供，本地配置，不写入正式代码 |
| Chat Completions 路径 | 待确认 |
| 鉴权 Header | 待确认 |
| 可用模型名 | 待确认 |
| 是否支持 SSE 流式输出 | 待确认 |
| 是否支持工具调用 / function calling | 待确认 |
| 是否支持文件、视觉、多模态 | 待确认 |
| 付费模型调用限制 | 用户已说明有付费额度，可调用付费接口，仍需确认接口限流与价格 |

---

## 三、预计本地配置方式

开发阶段建议仍然使用后端根目录的本地配置文件：

```text
boss-chat-server/application-local.yml
```

该文件应继续保持在 `.gitignore` 中，不提交到代码仓库。

模板：

```yaml
app:
  llm:
    provider: kimi
    base-url: 待填
    api-key: 待填
    model: 待填
```

如果后续做多供应商管理，不建议继续只用全局 `app.llm`，而应迁移到数据库表：

```text
ai_model_provider
ai_model
```

但当前阶段可先用配置方式跑通链路。

---

## 四、预计后端改造点

当前后端已经有：

```text
LlmProperties
LlmChatService
AiAgent.modelProvider
AiAgent.modelName
```

短期接入 Kimi 可优先复用这些能力。

预计需要检查：

1. Kimi 是否兼容 OpenAI Chat Completions 格式
2. 请求路径是否仍是：

```text
/chat/completions
```

3. 请求体字段是否兼容：

```json
{
  "model": "待填",
  "messages": [],
  "temperature": 0.35,
  "max_tokens": 4096,
  "stream": true
}
```

4. 流式响应是否使用：

```text
data: {...}
data: [DONE]
```

5. 工具调用返回结构是否兼容当前 `LlmToolResponse`

如果完全兼容，改动会很小；如果不兼容，需要给 Kimi 增加单独 adapter。

---

## 五、预计前端改造点

短期可以先在“智能体管理”中手动填写：

```text
modelProvider = kimi
modelName = 待填
```

长期建议新增：

- 模型供应商管理
- 模型管理
- AI 助手选择模型时，从模型列表中选择，而不是手动输入字符串

---

## 六、建议第一轮验证任务

等 API Key 和模型名确认后，建议按以下顺序验证：

### 1. 普通同步问答

目标：确认 Kimi 基础调用可用。

测试问题：

```text
请用一句话介绍你自己。
```

### 2. 流式输出

目标：确认前端流式展示正常。

测试问题：

```text
请分三点说明企业为什么需要 AI 赋能。
```

### 3. 长上下文能力

目标：验证 Kimi 的长上下文优势。

测试内容：

```text
粘贴较长业务材料，让模型总结结构和关键问题。
```

### 4. 增强型助手能力

目标：确认 Kimi 可配合本系统知识库 / 记忆 / 工作流。

方式：

- 开启知识库
- 开启工作流
- 不开启工具调用
- 观察是否能按系统注入内容回答

### 5. 工具型智能体能力

目标：如果 Kimi 支持工具调用，验证能否走当前工具链。

方式：

- 开启工具调用
- 让它读取工作区目录
- 让它读取普通文件或 `.xlsx`
- 验证人工确认流程是否正常

---

## 七、安全要求

API Key 不允许写入：

```text
src/main/resources/application.yml
Git 提交记录
开发文档
前端代码
```

推荐方式：

```text
application-local.yml
环境变量
后续数据库加密存储
```

如果用户临时在对话中提供 Key，开发阶段可以用于本机配置，但正式上线前应替换为公司 Key，并清理可能泄露的历史记录。

---

## 八、待补充内容

拿到官方文档后，需要补齐：

- 官方文档地址
- Base URL
- 鉴权方式
- 模型列表
- 免费 / 付费模型说明
- 上下文长度
- 工具调用格式
- 流式输出格式
- 错误码
- 限流规则
- 价格或计费单位

---

## 九、当前状态

```text
状态：等待官方文档地址与 API Key
下一步：根据官方文档确认接口格式，再决定是否复用现有 LlmChatService 或新增 Kimi Adapter
```


---

## 2026-05-19 代码落地状态

- 已新增 Kimi 供应商种子：`kimi / Kimi / Moonshot`
- 已设置 Base URL：`https://api.moonshot.cn/v1`
- 已新增模型种子：`kimi-k2.6`
- 已新增模型管理页面：`/models`
- 已支持通过数据库 API Key 池统一管理 Kimi Key
- 已通过前端构建和后端编译检查

# 模型兼容模式与 OpenAI 接入说明

本文记录 2026-05-22 新增的模型兼容机制，以及 OpenAI `gpt-5.5` 接入方式。

## 一、为什么需要兼容模式

不同模型厂商虽然都可能提供类 OpenAI 的 `/chat/completions` 接口，但请求参数并不完全一致。

本项目把冲突分成两类处理：

| 类型 | 示例 | 处理方式 |
| --- | --- | --- |
| 能力冲突 | 模型不支持工具调用，但当前场景需要工具调用 | 通过模型能力字段拦截，例如 `supports_tools = 0` 时不进入工具调用流程 |
| 参数冲突 | GPT-5 系列不接受旧的 `temperature` / `max_tokens` 组合 | 通过 `compatibility_profile` 自动调整请求体 |

因此后续新增模型时，不建议为了某个模型到处写特殊判断，而应优先补充或复用兼容模式。

## 二、数据库字段

新增字段：

```text
ai_model.compatibility_profile
```

迁移脚本：

```text
boss-chat-server/src/main/resources/db/migration/V16__model_compatibility_profile.sql
```

当前内置值：

| 值 | 适用模型 | 说明 |
| --- | --- | --- |
| 空字符串 | 默认 OpenAI Chat Completions 兼容请求 | 不做额外参数改写 |
| `openai_gpt5` | OpenAI GPT-5 系列，例如 `gpt-5.5` | 使用 GPT-5 系列参数兼容规则 |
| `kimi_k2` | Kimi K2 系列，例如 `kimi-k2.6` | 使用 Kimi K2 参数兼容规则 |

## 三、自动推断规则

模型保存时，如果前端没有手动选择兼容模式，后端会尝试自动推断：

| 供应商 | 模型名规则 | 自动兼容模式 |
| --- | --- | --- |
| `openai` | `gpt-5*` | `openai_gpt5` |
| `kimi` | `kimi-k2*` | `kimi_k2` |

对应代码：

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AiModelManagementService.java
```

管理端也提供了手动选择入口：

```text
Model Management -> 模型编辑 -> Compatibility
```

## 四、OpenAI GPT-5 系列规则

当模型命中 `openai_gpt5` 时，后端请求体会自动调整：

```text
移除 temperature
max_tokens -> max_completion_tokens
默认补充 reasoning_effort = medium
```

这样可以避免 GPT-5 系列因为旧参数不兼容而返回调用失败。

对应代码：

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/LlmChatService.java
```

## 五、Kimi K2 系列规则

当模型命中 `kimi_k2` 时，后端会继续使用 Kimi K2 的兼容逻辑，避免把不兼容的参数或思考配置带入请求。

## 六、OpenAI 供应商与模型预置

新增供应商：

```text
provider_code: openai
provider_name: OpenAI
base_url: https://api.openai.com/v1
auth_type: bearer
```

新增模型：

```text
model_name: gpt-5.5
display_name: GPT-5.5
model_type: chat
api_path: /chat/completions
compatibility_profile: openai_gpt5
```

迁移脚本：

```text
boss-chat-server/src/main/resources/db/migration/V15__seed_openai_gpt55.sql
```

种子配置支持环境变量：

```text
MODEL_SEED_OPENAI_API_KEY
OPENAI_API_KEY
```

注意：API Key 不应写入文档、前端代码或提交记录。开发期可以使用后端本地忽略文件 `boss-chat-server/application-local.yml`，正式环境建议使用环境变量或后续加密配置。

## 七、错误信息处理

为方便排查厂商调用失败，后端现在会尽量解析厂商返回的错误体，并把核心错误信息透出到前端。

覆盖场景：

```text
普通模型调用失败
流式模型调用失败
工具模型调用失败
```

常见定位方式：

| 前端错误 | 优先检查 |
| --- | --- |
| 模型服务调用失败 | Base URL、API Path、API Key、模型名、请求参数 |
| 模型流式调用失败 | 模型是否支持 stream、SSE 响应格式 |
| 模型工具调用失败 | 模型 `supports_tools`、兼容模式、工具调用格式 |

## 八、后续新增模型的建议流程

1. 在模型管理中新增供应商或复用已有供应商。
2. 新增模型，填写真实 `model_name` 和 `api_path`。
3. 勾选该模型真实支持的能力，不支持工具调用就关闭 `Tools`。
4. 如果是已知系列，选择对应 `Compatibility`；如果留空，后端会按规则自动推断。
5. 先用普通对话测试，再测试流式，最后测试工具调用。
6. 如果厂商返回参数不兼容错误，优先新增兼容模式，不要把特殊逻辑散落在业务流程里。

## 九、本次验证

已执行：

```text
cd boss-chat-server
mvn -q -DskipTests compile

cd boss-chat-web
npm run build
```

结果：后端编译通过，前端构建通过。

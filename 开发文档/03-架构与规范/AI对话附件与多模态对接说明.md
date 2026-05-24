# AI 对话附件与多模态对接说明

更新时间：2026-05-22

本文记录管理端 AI 对话中“上传图片、Excel、文档、视频等资源并随消息发送给 AI”的第一版实现方式，供后续继续接入文件解析和更多多模态模型时参考。

---

## 一、当前第一版能力

已完成：

- AI 对话输入框新增“上传附件”按钮。
- 后端新增附件上传接口：

```text
POST /api/chat/attachments
Content-Type: multipart/form-data
field: file
```

- 聊天发送接口新增 `attachments` 字段：

```text
POST /api/chat/messages
POST /api/chat/messages/stream
```

- 支持上传并随消息发送：
  - 图片：`.jpg`、`.jpeg`、`.png`、`.webp`、`.gif`
  - Excel：`.xlsx`
  - 文档：`.pdf`、`.doc`、`.docx`、`.txt`、`.md`
  - 视频：`.mp4`、`.mov`、`.webm`
- `.xlsx` 上传后会在后端读取前 50 行内容，并作为文本上下文传给模型。
- 图片会在模型请求中作为 `image_url` 放入 Chat Completions 多模态 `content` 数组。若图片已上传到 OSS，则直接使用公网 HTTPS URL；本地兜底模式下仍会转成 `data:image/...;base64,...`。
- 文档和视频第一版先保存本地，并在上下文中提示“已上传，当前版本暂不解析正文 / 视频内容”。

---

## 二、当前存储策略

当前阶段资源边界已调整为：

- 用户上传图片：优先上传到阿里云 OSS，作为用户资源和图片编辑源图。
- 用户上传 Excel / PDF / Word / 文本 / 视频：继续保存到本地，后续再按解析和权限需求决定是否迁移 OSS。
- AI 厂商生成 / 编辑返回的图片：先直接展示厂商 URL，不再自动下载到本地或 OSS；用户确认要保存或继续编辑时，再转为用户资源。

阿里云 OSS / 腾讯云 COS 通过管理端“图片存储管理”维护，并将对应配置设置为默认存储。配置会保存到 `ai_image_storage_config`，AccessKey / SecretKey 会加密保存。

推荐配置：

```text
存储类型：阿里云 OSS
Endpoint：https://oss-cn-wuhan-lr.aliyuncs.com
地域：cn-wuhan-lr
Bucket：lantu-boss-chat
访问域名：https://lantu-boss-chat.oss-cn-wuhan-lr.aliyuncs.com
保存前缀：chat-images
默认存储：开启
```

腾讯云 COS 测试配置：

```text
存储类型：腾讯云 COS
地域：ap-guangzhou
Bucket：lantu-boss-chat-1314624174
访问域名：https://lantu-boss-chat-1314624174.cos.ap-guangzhou.myqcloud.com
保存前缀：chat-images
默认存储：开启
```

图片上传到 OSS 的目录约定：

```text
chat-images/{yyyyMMdd}/{userId}/{uuid}.{ext}
```

OSS 未启用时，图片附件仍保存到后端本地目录：

附件保存到后端本地目录：

```text
boss-chat-server/uploads/chat-attachments/{yyyyMMdd}/{userId}/{attachmentId}.{ext}
```

后端通过静态资源映射暴露访问路径：

```text
/api/uploads/**
```

说明：

- OSS 图片 URL 主要用于前端预览、跨设备访问和云端模型读取。
- 本地兜底图片传给云端视觉模型时，后端会读取本地图片并转成 base64 data URL，避免本地 `localhost` 图片无法被模型厂商访问。
- AI 厂商返回的生成图 / 编辑图可能有有效期，当前产品策略是不自动沉淀；用户需要保存或继续编辑时，再转为用户资源。

目录约定：

```text
uploads/chat-attachments/{yyyyMMdd}/{userId}/
uploads/ai-images/{yyyyMMdd}/{userId}/
```

---

## 三、请求数据结构

前端上传附件后，后端返回：

```json
{
  "attachmentId": "uuid",
  "fileName": "demo.xlsx",
  "fileType": "excel",
  "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "size": 12345,
  "url": "https://lantu-boss-chat.oss-cn-wuhan-lr.aliyuncs.com/chat-images/20260522/1/uuid.png",
  "localPath": "",
  "summary": "Excel 文件摘要..."
}
```

聊天发送时把这些对象放入：

```json
{
  "sceneId": 1,
  "agentId": 1,
  "conversationId": 10,
  "content": "请分析这个表格",
  "attachments": []
}
```

---

## 四、模型调用方式

普通文本模型仍使用原来的 messages 格式：

```json
{
  "role": "user",
  "content": "用户问题"
}
```

当本轮消息包含图片附件时，`LlmChatService` 会把最后一条用户消息改成多模态格式：

```json
{
  "role": "user",
  "content": [
    {
      "type": "text",
      "text": "用户问题和附件说明"
    },
    {
      "type": "image_url",
      "image_url": {
            "url": "https://lantu-boss-chat.oss-cn-wuhan-lr.aliyuncs.com/chat-images/20260522/1/uuid.png"
      }
    }
  ]
}
```

这要求当前 AI 绑定的模型支持视觉输入，例如当前预置的：

```text
智谱多模态工作台 / glm-5v-turbo
```

如果后续模型厂商要求不同字段格式，应在 `LlmChatService` 里按供应商做 payload adapter，而不要把差异扩散到前端。

---

## 五、当前限制

当前第一版只是把主链路跑通，仍有这些限制：

- 阿里云 OSS / 腾讯云 COS 当前只接管用户上传图片；其他附件仍在本地。
- `.xlsx` 支持基础 XML 读取，不支持 `.xls`。
- PDF / Word / 视频只保存文件，不解析正文或画面。
- 附件信息暂未单独建表，当前由上传接口返回后随消息请求携带。
- 历史消息只保存用户可见的附件名称，不保存附件结构化元数据。

---

## 六、后续建议

下一步可以按优先级继续做：

1. 新增 `ai_chat_attachment` 表，把附件和 `conversation_id`、`message_id` 绑定，避免只依赖前端回传。
2. PDF / Word 增加正文解析，解析结果进入附件摘要。
3. 视频增加抽帧或视频理解模型适配。
4. 前端消息气泡改成附件卡片展示，历史消息也能打开附件。
5. 对不同模型厂商增加多模态 payload adapter，避免只兼容 OpenAI 风格接口。
6. 增加“保存到素材 / 继续编辑”动作，把用户选中的 AI 厂商临时图转存为用户资源。

---

## 七、图片编辑试接入：Qwen Image Edit

当前已新增 `edit_image` 工具，用于测试“基于上一张图继续修改”的链路。

### 接入模型

```text
供应商：通义千问 / DashScope
供应商编码：qwen
Base URL：https://dashscope.aliyuncs.com/api/v1
模型：qwen-image-2.0-pro
模型类型：image_edit
接口路径：/services/aigc/multimodal-generation/generation
官方文档：https://www.alibabacloud.com/help/en/model-studio/qwen-image-edit-api
```

新增迁移：

```text
boss-chat-server/src/main/resources/db/migration/V14__seed_qwen_image_edit.sql
```

启动时 `AiModelBootstrap` 也会补齐供应商、模型和初始化 Key。

### API Key 配置

本地测试可以配置任意一个环境变量：

```text
MODEL_SEED_QWEN_API_KEY=你的 DashScope API Key
DASHSCOPE_API_KEY=你的 DashScope API Key
```

也可以在模型管理页里，找到 `Qwen Image Edit Pro` 后手动新增或编辑 API Key。

### 工具行为

`edit_image` 参数：

```json
{
  "instruction": "保持人物脸部和构图不变，把头发改成红色卷发",
  "sourceImageUrl": "",
  "size": "1024*1024",
  "negativePrompt": "低清晰度、五官变形、额外手指"
}
```

说明：

- `instruction` 必填。
- `sourceImageUrl` 可选；为空时后端会自动取当前会话最近一张成功生成的图片。
- 当前第一版主要用于“上一张图继续修改”的测试。
- Qwen 返回的图片 URL 有有效期；当前后端先直接返回厂商 URL。若用户需要保存或继续编辑，应转存为用户资源后再作为源图使用。

### 相关代码

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AiImageEditService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AgentToolRegistry.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AutonomousAgentService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/bootstrap/AiModelBootstrap.java
```

---

## 八、验证记录

本次验证：

```text
后端：mvn -q -DskipTests compile
前端：npm run build
```

两项均通过。前端构建仍有已有的 Vite 大 chunk 提示和 `@vueuse/core` 注释提示，不影响本功能。

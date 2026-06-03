# AI项目进度与接力说明

本文档不是普通项目说明书，而是给后续新对话中的 AI 快速接手本项目使用的长期维护文档。

使用方式建议：

1. 先让 AI 自行分析当前代码结构
2. 再让 AI 阅读本文件
3. 最后根据当前任务，再补读对应专项文档

本文件的目标是让新的 AI 在最短时间内理解：

- 这个项目现在做到哪里了
- 哪些技术决策已经定下来了
- 哪些目录是主线，哪些只是参考
- 哪些问题已经排查过，不要重复走弯路
- 下一步最适合继续推进什么

---

## 一、项目当前定位

当前仓库已经从旧原型过渡到新的四项目结构，目标是建设一个长期维护的正式项目，而不是一次性 demo。

当前主线项目为：

```text
boss-chat-server      # 后端服务
boss-chat-web         # 管理系统
boss-chat-app         # Flutter App
boss-chat-miniapp     # uni-app 小程序
```

当前明确保留但不再作为主线继续扩展的参考目录：

```text
minimax-token-chat-demo
MinimaxAndroidChat
tools
```

这些参考目录可以用于借鉴思路，但后续新需求、新代码、新文档应优先落在四个主线项目中。

---

## 二、当前技术栈与端口约定

### 1. 技术栈

| 端 | 技术栈 |
| --- | --- |
| 后端 | Java Spring Boot + MySQL + Flyway + MyBatis-Plus + Sa-Token + Swagger |
| 管理系统 | Vue 3 + TypeScript + Element Plus |
| App | Flutter + Dart |
| 小程序 | Vue 3 + uni-app |

### 2. 端口约定

| 项目 | 端口 | 说明 |
| --- | --- | --- |
| 后端服务 | `9090` | 真正提供接口服务 |
| 管理系统 | `9000` | Vite 开发端口 |
| App | `9999` | 仅 Flutter Web 调试端口 |
| 小程序 | `9900` | 仅 uni-app H5 预览端口 |

重要说明：

- Flutter 原生 App 本身没有固定 HTTP 监听端口
- 微信 / 支付宝小程序也不是“运行在 9900 端口”
- `9999` 和 `9900` 都只是本地开发联调用的预览端口

---

## 三、当前已经完成的内容

### 1. 架构层

- 已完成前后端分离的四项目结构收敛
- 已把文档从根目录收拢到 `开发文档/`
- 已明确参考项目与主线项目边界
- 四个主线项目的基础模板均已实际跑通

### 2. 后端层

- 已建立正式认证与权限基础模型
- 已启用 Flyway 脚本化迁移
- 已从测试表切换到正式第一版权限模型
- 已接入 Swagger
- 已完成登录闭环
- 已进入真实项目第一阶段，用户模型新增手机号、微信号、QQ 号预留字段
- 已新增智能体、会话、消息、用量记录基础表
- 已预置 `super_admin` 与 `user` 两类角色
- 已预置一个最高权限超级管理员账号用于当前阶段联调
- 已完成智能体历史会话与继续对话基础能力
- 已完成智能体真流式回复接口
- 已完成工作区智能体第一版：记忆、知识库、工作流、工具调用、本地文件与命令能力
- 已新增课程期数与签到主线：课程期数、学员列表、每期专属问卷二维码、按期归档调查记录、数据看板痛点汇总

当前核心表包括：

```text
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
sys_oauth_account
sys_login_record
ai_agent
ai_conversation
ai_message
ai_usage_record
course_phase
course_student
survey_record
flyway_schema_history
```

### 3. 前端管理系统

- 已建立管理系统基础模板
- 已接通登录页与后端登录接口
- 已建立基础后台页面骨架
- 已配置与后端的本地联调代理
- 管理系统模板已可正常启动
- 已切换为真实管理端第一版：用户管理使用真实接口，智能体对话已拆为独立页面
- 已新增智能体管理页面
- 智能体对话页已支持历史会话列表、新建会话、继续对话与真流式回复
- 已开始第一轮目录规范化整理：登录与智能体对话模块已按“页面 / 逻辑 / 组件 / 样式”拆分
- 已新增“智能体工作台”，可执行本地工作区任务并查看工具调用结果
- 已新增 `/checkins` 签到列表：可创建课程期数、维护学员、复制每期问卷链接、保存每期二维码、查看本期数据看板
- 已升级 `/survey-records` 调查记录：支持按期数筛选、复制当前期数问卷链接、查看当前期数数据看板

本次发布重点：

- 后端会通过 Flyway 执行 `V30__course_phases_students_and_survey_links.sql`，新增课程期数和学员签到相关表结构。
- 发布后优先检查 `/checkins` 是否能创建期数、生成二维码、添加学员。
- 再使用带 `?phase=...` 的问卷链接提交一次问卷，确认 `/survey-records` 能按期数筛选并生成数据看板。

### 4. Flutter App

- 已补齐为正式 Flutter 工程
- 已具备 `android/`、`ios/`、`web/` 目录
- 已接通登录页与后端登录接口
- 已完成 Flutter Web 调试链路
- 已梳理 Android Studio、模拟器、设备选择和接口地址问题
- Flutter App 已在 Android 模拟器中成功跑通

### 5. 小程序

- 已建立 uni-app 小程序模板
- 已接通登录页与后端登录接口
- 已明确微信开发者工具导入的是编译产物目录，而不是源码目录
- 小程序模板已可按微信小程序流程正常构建与预览

---

## 四、当前认证与权限决策

当前已经明确采用：

```text
Sa-Token + 本地账号密码登录 + RBAC
```

并且已经明确以下设计原则：

- 权限归属 `sys_user`
- 角色与权限走标准 RBAC 关系
- 微信 / QQ 等第三方登录未来通过 `sys_oauth_account` 绑定到本地用户
- 无论用户通过账号密码、微信还是 QQ 登录，最终都应落到同一个 `sys_user`

这意味着：

- 第三方登录只是“身份来源变化”
- 权限体系不挂在微信、QQ 账号上
- 权限体系始终挂在系统用户本体上

这是当前项目的核心长期决策，后续 AI 不应轻易改动。

---

## 五、当前重要运行约定

### 1. 后端

- 默认开发端口：`9090`
- Swagger 地址：`http://localhost:9090/swagger-ui.html`
- Swagger 不带 `/api` 前缀
- 当前登录接口：`POST /api/auth/login`
- 需要登录的接口可通过 Swagger 的 `Authorize` 按钮注入令牌
- 当前令牌请求头名称是：

```text
satoken
```

不是：

```text
Authorization: Bearer ...
```

### 2. Android 模拟器联调

Android 模拟器访问本机后端时，不应使用：

```text
http://localhost:9090/api
```

而应使用：

```text
http://10.0.2.2:9090/api
```

### 3. Flutter 设备选择

当前 Flutter App 默认开发目标应理解为：

- Android 模拟器 / Android 真机
- iOS 模拟器 / iPhone 真机
- Chrome 仅作为临时 Web 调试

不要把：

```text
Windows (desktop)
```

当成当前项目 App 的默认运行目标。

---

## 六、已经踩过并确认的问题

### 1. Swagger 路径误解

曾出现把 Swagger 写成：

```text
http://localhost:9090/api/swagger-ui/...
```

这是错误的。  
正确入口是：

```text
http://localhost:9090/swagger-ui.html
```

### 2. Flutter Android Studio 灰色运行按钮

曾出现：

- `Add Configuration`
- `Dart SDK is not configured`
- 运行按钮灰色

已确认处理方式为：

- 安装并启用 Flutter / Dart 插件
- 配置 Flutter SDK 路径
- 配置 Dart SDK 路径
- 必要时手工新建 Flutter 运行配置

### 3. Android 模拟器能打开，但又报错

曾出现：

```text
Running multiple emulators with the same AVD is an experimental feature
```

这不是模拟器损坏，而是：

- 同一个 AVD 已经启动
- 又额外点击了一次 `Open Android Emulator`

正确做法是：

- 如果模拟器已经打开，就直接选择设备列表中的：

```text
Pixel 8 (mobile)
```

- 不要重复启动同一个 AVD

### 4. Android 模拟器图形/Vulkan 警告

曾出现显卡驱动版本不足、回退兼容模式的警告。  
当前结论是：

- 这类警告不一定阻塞模拟器启动
- 如果模拟器已正常打开，可暂时不作为主阻塞处理
- 真正卡顿或黑屏时，再单独处理显卡驱动与图形模式

### 5. Android 构建依赖与损坏下载包

曾出现：

```text
zip END header not found
Archive is not a ZIP archive
```

当前已确认：

- `zip END header not found` 曾由损坏的 `gradle-8.14-all.zip` 缓存导致
- `Archive is not a ZIP archive` 曾由 `Android SDK Build-Tools 35.0.0` 下载包损坏或安装不完整导致
- 这类问题优先按“依赖下载损坏”处理，不要先怀疑业务代码
- 当前 Android 构建依赖里需要确认 `Build-Tools 35.0.0`

### 6. uni-app 小程序命令误解

曾出现直接执行：

```powershell
npm run dev
```

却希望它直接“打开微信小程序”的理解偏差。

当前已明确：

- `npm run dev` 当前默认等价于 `npm run dev:mp-weixin`
- 它的作用是编译微信小程序产物
- 微信开发者工具导入的是：

```text
dist/dev/mp-weixin
```

不是源码根目录

---

## 七、当前文档体系

当前业务文档已经集中在：

```text
开发文档/
```

建议新 AI 先看：

1. `开发文档/文档导航.md`
2. `开发文档/01-总览/新架构重构工作区说明.md`
3. 本文档

再根据任务补看：

- 启动问题：`02-启动与联调`
- 权限问题：`03-架构与规范`
- 数据库问题：`04-数据库`
- 具体项目说明：`05-项目说明`
- 服务器部署：`06-部署`

---

## 八、最近关键决策

| 日期 | 决策 | 原因 |
| --- | --- | --- |
| 2026-05-16 | App 最终采用 `Flutter`，小程序采用 `uni-app` | App 与小程序后续目标不同，Flutter 更适合独立 App，uni-app 更适合多端小程序 |
| 2026-05-16 | 后端保留单服务架构，三个前端共用同一套接口 | 当前阶段先把业务主线跑通，避免过早拆分 |
| 2026-05-16 | 权限采用 `RBAC`，第三方登录绑定到 `sys_user` | 保证微信 / QQ 登录接入后仍复用统一权限体系 |
| 2026-05-18 | 文档统一收拢到 `开发文档/` | 避免根目录散落大量 Markdown，方便长期维护 |
| 2026-05-18 | 新增独立的 Android 启动说明与部署文档 | 这两块已经形成独立知识域，继续塞进总说明会降低可维护性 |
| 2026-05-18 | 当前阶段先只推进后端与管理系统，App / 小程序暂缓 | 老板当前最关心先把可对话的智能体和后台基础能力跑起来 |
| 2026-05-18 | 智能体先按两个 Agent 落地，统一先接智谱 | 两个 Agent 更容易看出差异；当前优先验证真实调用链路，而不是过早做复杂模型编排 |
| 2026-05-18 | 当前默认模型先使用 `glm-4-flash-250414` | 同为官方免费模型，当前联调时比 `glm-4.7-flash` 更稳定，先优先保证老板可测 |
| 2026-05-18 | 智能体对话从仪表盘拆为独立侧边栏页面 | 对话能力已经成为真实业务主入口，独立页面更利于后续扩展历史、流式、命名和删除能力 |
| 2026-05-18 | 智能体对话改为真流式返回 | 用户消息先上屏，AI 回复逐段展示，降低等待感，更贴近真实聊天产品体验 |
| 2026-05-18 | 智谱 API Key 当前阶段放在后端根目录本地配置文件 `application-local.yml` 中 | 兼顾开发期省事与后续更换便利；该文件已加入忽略列表，不进入主代码 |
| 2026-05-18 | 四个正式项目统一采用“业务模块优先、模块内部再分层”的组织方式 | 让后续功能增长时仍能按业务快速定位代码，减少交接成本 |
| 2026-05-18 | 智能体系统先落地为“工作区智能体”，再逐步演进为独立本地代理 | 先验证记忆、知识库、工作流、工具和本地能力的主链路，同时避免一开始放开整机权限 |
| 2026-05-19 | 项目方向升级为“多模型统一接入 + 后台赋能管理” | 大模型是大脑，系统通过记忆、知识库、工作流、工具和权限给模型赋能，最终形成不同类型的 AI 助手或智能体 |

维护要求：

- 如果后续出现会长期影响架构、技术路线或协作方式的决定，应继续追加到本表
- 临时排查结论不要混入这里，临时问题应放在“已确认问题”或对应专项文档中

---

## 九、当前阻塞与待确认事项

### 当前阻塞

当前没有阻塞主线开发的硬性问题。

### 仍需后续确认的事项

| 事项 | 当前状态 | 后续影响 |
| --- | --- | --- |
| 生产环境是否长期开放 Swagger | 尚未决定 | 影响安全策略与部署配置 |
| 生产环境域名拆分方式 | 尚未决定 | 影响 CORS、Nginx / IIS 配置与证书规划 |
| Windows 生产环境最终使用 IIS 还是统一改用 Nginx | 当前默认 IIS | 影响 Windows 部署手册后续演进 |
| App 后续是否需要鸿蒙单独适配 | 已暂缓 | 影响移动端长期技术规划 |

---

## 十、当前已明确但尚未完成的主线任务

后续最适合继续推进的方向：

1. 完善用户管理新增 / 编辑能力
2. 智能体会话命名 / 删除等基础管理能力
3. 角色与权限管理
4. 微信 / QQ 第三方登录绑定
5. 用量与后续付费相关能力

当前阶段最稳妥的推进方式是：

- 继续以管理系统为第一主线
- 后端优先把用户、角色、权限的真实管理接口补齐
- App 和小程序先保留登录模板和基础联调能力

---

## 十一、对新 AI 的接手要求

新 AI 接手本项目时，应优先遵守以下约束：

1. 不要把参考项目当成主线项目进行直接扩展
2. 不要把临时测试表重新带回主线数据库设计
3. 不要把权限重新挂到第三方登录账号上
4. 不要把 Flutter Web 调试端口误认为 App 正式运行端口
5. 不要把 Swagger 路径误写成 `/api/swagger-ui/...`
6. 不要默认把 Android 模拟器接口地址写成 `localhost`
7. 不要轻易改动当前已经确定的四项目结构

---

## 十二、新 AI 的推荐接手流程

如果是新的对话窗口，建议按以下顺序进入项目：

1. 先自行扫描代码结构与实际文件
2. 阅读 `开发文档/文档导航.md`
3. 阅读 `开发文档/01-总览/新架构重构工作区说明.md`
4. 阅读本文档
5. 再根据本次任务补读专项文档
6. 开始修改前，先核对本文档中的“最近关键决策”和“当前阻塞与待确认事项”

这样做的目的不是替代代码分析，而是避免：

- 重复做已经做过的判断
- 推翻已经确定的长期决策
- 忽略项目当前真正的推进方向

---

## 十三、建议长期维护方式

每当项目推进一个阶段，建议补充更新以下内容：

- 当前已完成的功能
- 新增的关键技术决策
- 已确认的坑与处理结论
- 下一步优先级
- 当前阻塞与待确认事项
- 文档目录是否发生变化

建议把本文件当作：

```text
AI 接手说明 + 项目阶段进度摘要
```

来维护，而不是当作普通需求清单。

---

## 十四、推荐更新模板

后续每次阶段性更新时，建议至少补齐下面这些字段：

```text
更新时间：

本阶段完成：
- 

新增关键决策：
- 

已确认问题 / 处理结论：
- 

当前阻塞：
- 无 / 具体问题

仍需确认：
- 

下一步优先级：
1. 
2. 
3. 

文档同步情况：
- 已更新：
- 仍待更新：
```

如果只是一次很小的改动，不一定每项都要写满；但只要涉及架构、协作方式、部署方式或后续 AI 容易误判的内容，就应该更新本文档。

---

## 十五、阶段记录

### 2026-05-18

本阶段完成：

- 完成四项目基础模板
- 四个主线项目基础模板均已实际跑通
- 完成登录闭环
- 完成文档体系第一次集中整理
- 完成 Windows / Linux 部署环境与部署操作文档
- 打通 Flutter Android 模拟器基础链路
- 真实项目第一阶段开始：补齐用户预留字段、两类基础角色、智能体基础数据模型
- 管理系统切换到真实接口，新增用户管理列表、智能体管理、独立智能体对话页面
- 已验证后端 Flyway 从 `v1` 正常迁移到 `v2`
- 智能体对话页已支持历史会话、新建会话、继续对话与真流式回复
- 已新增《项目目录与文件组织规范》和《第一轮目录整理任务清单》
- 管理系统已完成第一轮示范性拆分：登录页与智能体对话模块已按职责拆开
- 智能体系统第一版已落地：新增长期记忆、知识库、工作流、工具执行记录和智能体工作台

新增关键决策：

- 新增本文档，作为后续 AI 接力入口
- 文档主入口固定为 `开发文档/文档导航.md`
- 当前阶段暂不推进 App 与小程序，先集中后端与管理系统
- 智能体第一版先统一使用智谱，两个 Agent 通过不同提示词区分
- 当前默认模型先使用 `glm-4-flash-250414`
- 当前只保留 `super_admin` 与 `user` 两类角色
- 当前开发期智谱 Key 通过本地忽略文件自动加载，不写入正式配置
- 智能体对话从仪表盘拆为独立页面，并采用真流式回复
- 四个正式项目后续统一按“业务模块优先、模块内部再分层”整理目录
- 真正智能体能力先以“工作区智能体”形式落地，再继续演进到独立本地代理

已确认问题 / 处理结论：

- Swagger 正确入口不带 `/api`
- 智能体对话早期“发送失败但刷新后成功”由前端 10 秒超时过短导致，已改为流式链路并延长等待
- 局域网访问时，前端需要监听所有网卡，后端 CORS 需要放行对应来源模式
- Android 模拟器访问本机后端使用 `10.0.2.2`
- 同一 AVD 已启动时不要再次点击 `Open Android Emulator`
- Android 首次构建需要确认 `Build-Tools 35.0.0`
- ZIP 类错误优先排查 Gradle / SDK 下载包是否损坏

当前阻塞：

- 无

仍需确认：

- 生产环境域名方案
- Swagger 是否在生产环境开放
- Windows 正式部署最终是否长期采用 IIS

下一步优先级：

1. 用户管理补新增 / 编辑能力
2. 智能体补会话命名 / 删除等基础管理能力
3. 在老板试用后决定是否切回更新的免费模型

文档同步情况：

- 已更新：总览、启动、安卓联调、部署、AI 接力说明
- 仍待更新：后续功能模块落地后继续补充阶段记录

---

## 十九、2026-05-19 第二版实现进度：模型统一管理与智能体管理收敛

本轮根据用户重新明确的第二版需求，完成了第一版智能体工作台后的结构收敛。

### 本阶段完成

- 本地开发库已清空并重新执行 Flyway V1-V4。
- 后端已完成第二版核心表初始化：
  - `ai_model_provider`
  - `ai_model`
  - `ai_model_api_key`
  - `ai_agent`
  - `ai_memory`
  - `ai_knowledge_document`
  - `ai_workflow`
- 默认超级管理员账号继续保留：

```text
账号：admin
密码：Admin@123
```

- 模型管理已支持：
  - 供应商管理
  - 模型管理
  - API Key 池管理
- 默认初始化两个供应商：
  - 智谱
  - Kimi / Moonshot
- 默认初始化三个模型：
  - `glm-4.7`
  - `glm-4.5-flash`
  - `kimi-k2.6`
- 默认初始化三个 AI 配置：
  - AI获客操盘手：面向企业咨询和 AI 赋能企业业务，负责获客定位、渠道打法、内容选题、销售话术和成交路径
  - 企业AI赋能顾问：面向客户企业现状诊断，负责 AI 赋能方案、咨询交付路径和流程改造建议
  - 企业AI落地执行助手：面向各类型企业，负责把 AI 获客和企业赋能方案落成诊断报告、方案文档、流程清单、表格资料、内容脚本和交付材料
- 智能体工作台不再作为主入口继续扩展，相关能力收敛到：

```text
智能体管理：配置能力
AI 对话：真实使用能力
```

- 智能体管理页已支持在编辑 AI 时维护：
  - 基础配置
  - 模型绑定
  - API Key 绑定
  - 长期记忆
  - 知识库
  - 工作流
  - 本地工具开关

### 新增关键决策

- 不再纠结外部平台提供的是“大模型”还是“智能体”，本项目按“能力”定义产品形态。
- 外部厂商主要提供模型能力；真正的智能体能力由本系统通过记忆、知识库、工作流、工具和权限统一赋能。
- 模型管理负责接入外部模型；智能体管理负责给模型赋能；AI 对话负责真实使用。
- 一个供应商下可以配置多个模型；每个模型下面维护自己的 API Key，方便后续按具体模型切换免费额度、付费额度或公司 Key。
- 模型管理负责维护“供应商 -> 模型 -> 模型 API Key”，智能体管理只通过下拉选择供应商和模型 API，不再重复手填厂家、模型名和 Key。

### 验证结果

- 前端执行 `npm run build`：通过。
- 后端执行 `mvn -q clean compile`：通过。
- 后端从空库执行 Flyway 迁移：通过。
- 数据初始化结果：

| 数据 | 数量 |
| --- | ---: |
| 用户 | 1 |
| 模型供应商 | 2 |
| 模型 | 3 |
| API Key | 3 |
| AI 配置 / 智能体 | 3 |

当前模型与 Key 初始化关系：

```text
智谱 AI / GLM-4.7        -> 智谱 GLM-4.7 Key
智谱 AI / GLM-4.5-Flash -> 智谱 GLM-4.5-Flash Key
Kimi / Moonshot / Kimi K2.6 -> Kimi K2.6 Key
```

### 已确认问题 / 处理结论

- Windows PowerShell 管道写入中文时可能导致文件中文乱码，后续涉及中文长文本时优先使用 `apply_patch` 或确认 UTF-8 写入方式。
- `spring-boot:run` 如果出现旧 class 缺失问题，先执行 `mvn clean compile` 清理旧产物。
- 当前 API Key 仅应保存在 `boss-chat-server/application-local.yml`，该文件已被忽略，不应写入正式代码或文档。

### 下一步优先级

1. 继续在 AI 对话中验证三个默认 AI 配置的真实调用效果。
2. 根据老板试用反馈，补充企业咨询场景的长期记忆、知识库和工作流模板。
3. 增加 AI 会话删除、重命名、分类等基础管理能力。
4. 后续如进入多人或生产环境，新增迁移必须继续使用 `V5__xxx.sql`，不要再直接修改已执行迁移。

### 2026-05-19

本阶段完成：

- AI 对话入口已恢复同时支持普通大模型与工具型智能体，并在会话列表中标记来源：`大模型` / `智能体`
- 工具型智能体已接入 AI 对话页，支持聊天记录、上下文、工作流选择、工具调用、人工确认与手动停止
- 智能体工作台 UI 已从临时测试页优化为后台配置管理页
- 智能体工作台右侧新增“智能体能力配置”区域，按 Tab 管理：长期记忆、知识库、工作流
- 长期记忆、知识库、工作流均已支持查看已创建内容、启用 / 停用、编辑、删除
- 后端补齐长期记忆、知识库、工作流的 `PUT` / `DELETE` 维护接口
- 后端对长期记忆键、工作流编码增加重复校验，避免唯一约束冲突直接暴露为 500
- 已支持 `.xlsx` Excel 文件读取工具 `read_excel`，用于读取工作簿和前若干行内容

新增关键决策：

- `AI 对话` 是老板真实体验智能体能力的主要入口
- `智能体工作台` 更偏后台管理与调试：执行本地工作区任务，同时维护记忆、知识库、工作流
- 普通大模型对话暂时保留，但需要在会话记录中明确标记，避免与真正工具型智能体混淆
- 记忆、知识库、工作流不再视为测试内容，而是智能体正式能力的一部分

已确认问题 / 处理结论：

- 智能体如果需要访问未授权目录或执行敏感工具，应先向用户请求确认
- `run_command` 始终属于敏感工具，即使路径看起来安全，也需要人工确认
- `.xlsx` 不应按普通文本读取，应通过 `read_excel` 工具读取
- 工作台当前上下文仍为浏览器本地上下文，不是数据库级多设备会话

当前阻塞：

- 无

仍需确认：

- 正式产品中普通用户是否也能使用本地工具型智能体，还是仅超级管理员可用
- 后续是否需要把工作台上下文持久化到数据库
- 是否需要增加 diff 预览、文件修改回滚、命令白名单等更强安全能力

下一步优先级：

1. 在 AI 对话中继续打磨工具型智能体的真实使用体验
2. 增加会话命名 / 删除 / 分类等基础管理能力
3. 根据老板试用结果补充业务型知识库与工作流模板
4. 继续完善智能体安全边界，例如 diff 预览、回滚、命令白名单

文档同步情况：

- 已更新：AI 接力说明、智能体系统第一版说明、前端管理系统项目说明、后端项目说明、SQL 脚本使用说明
- 仍待更新：如果后续智能体安全策略继续细化，需要单独新增《智能体本地工具安全说明》

---

## 十六、当前维护时间点

本文档当前反映的项目阶段：

```text
2026-05-19
```

对应状态：

- 四项目基础模板已完成
- 四项目基础模板均已实际跑通
- 登录闭环已打通
- 文档体系已完成第一轮整理
- 部署环境与部署操作文档已补齐
- Flutter Android 模拟器联调链路已基本打通
- 项目已进入真实业务第一阶段，当前主线聚焦后端与管理系统
- 智能体对话已独立成页，并完成真实流式交互
- AI 对话已支持普通大模型与工具型智能体，并在会话列表中标记来源
- 智能体工作台已支持长期记忆、知识库、工作流的查看、新增、编辑、删除

---

## 十七、2026-05-19 多模型统一管理与二次自检

本轮完成多模型统一管理第一版，并对改动做了二次复查。

已完成：

- 后端新增 `ai_model_provider`、`ai_model`、`ai_model_api_key` 三张表。
- `ai_agent` 新增 `model_id` 和 `api_key_id`，用于绑定统一管理中的模型和 API Key。
- 新增 `/api/admin/model-management/**` 后端管理接口。
- 前端新增 `/models` 模型管理页面。
- 智能体管理页支持选择统一模型和 API Key。
- 能力形态改为：基础模型助手、增强型助手、工具型智能体。
- 非工具型模型也可以注入长期记忆、知识库、工作流上下文。
- 工具型智能体使用数据库模型和 API Key 时，消息记录与用量记录会保存实际 provider / model。
- 当工具型智能体绑定的模型没有标记支持工具调用时，后端会给出明确错误。

二次自检结果：

- 后端执行 `mvn -q -DskipTests compile`，通过。
- 前端执行 `npm run build`，通过。
- 已修复新增前端页面中文文案被写成连续问号乱码的编码问题。
- 已检查敏感 Key，Kimi Key 未写入代码或文档。
- 当前只有 `boss-chat-server/application-local.yml` 中保留开发阶段智谱 Key，该文件已在 `boss-chat-server/.gitignore` 中忽略。

说明：前端构建仍有 Vite chunk 体积偏大的 warning，这是打包优化提示，不影响当前功能运行。

---

## 十八、2026-05-19 第二版数据库重置记录

为进入第二版实现阶段，已按用户确认清空并重建本地开发数据库：

```text
数据库：boss_chat_dev
字符集：utf8mb4
排序规则：utf8mb4_unicode_ci
当前表数量：0
```

执行策略：

- 不是逐表删除，而是整库 `DROP DATABASE` 后重新 `CREATE DATABASE`
- 这样可以彻底清除旧表、旧外键、旧索引和 `flyway_schema_history`
- 后续重新启动后端时，Flyway 会从 V1 开始重新执行迁移脚本

本次重置意味着以下开发阶段数据已清空：

- 用户、角色、权限
- 登录记录
- AI 会话与消息
- 长期记忆
- 知识库
- 工作流
- 模型供应商、模型、API Key 池
- 工具执行记录与用量记录

第二版接下来需要重新梳理：

1. 数据库表是否继续沿用 V1-V4，还是合并重写为更清晰的第二版迁移。
2. “模型供应商 / 模型 / API Key / AI 配置 / 能力赋能”是否需要进一步抽象。
3. AI 对话与智能体工作台是否继续保留两个入口，还是统一成一个主入口。
4. 本地工具权限、安全审批、文件修改确认是否需要在数据库中正式建模。
5. 普通用户和超级管理员的 AI 能力边界如何区分。

---

## 十九、2026-05-19 AI 对话上下文清空语义修正

本轮修复 AI 对话页“清空上下文后，重新点击左侧会话记录又恢复旧上下文”的问题。

问题原因：

- 原实现只清空了前端当前页面中的 `messages` 和 `conversationId`
- 后端会话仍然保持 `active` 状态
- 用户再次点击左侧历史会话时，前端会重新请求后端会话详情，所以旧消息又被加载回来

修正后的语义：

- 清空上下文 = 结束当前会话的上下文使用，并开启一个新的干净会话
- 后端会把当前会话状态标记为 `cleared`
- 已清空的会话不再出现在可继续会话列表中，也不会再作为上下文重新加载
- 当前没有物理删除消息数据，后续如需做“回收站 / 审计 / 历史归档”，仍有扩展空间

新增接口：

```text
POST /api/chat/conversations/{conversationId}/clear-context
```

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 前端执行 `npm run build`，通过。

---

## 二十一、2026-05-20 图片生成能力配置基础结构

本轮开始为 AI 增加“图片生成能力”的基础配置能力。

设计原则：

- 对话模型是“大脑”，负责理解用户需求、整理图片提示词、决定是否需要生成图片。
- 图片生成模型是“工具”，由平台统一管理并按能力配置绑定给某个 AI。
- 因此 Kimi、智谱 GLM 等对话模型本身即使不是图片模型，也可以通过开启图片生成工具来获得图片生成能力。
- 图片结果不直接塞入模型上下文，只在上下文中保留短摘要和图片编号，避免污染多轮对话质量。

已完成：

- `ai_model` 增加 `model_type` 字段，用于区分：

```text
chat              对话模型
image_generation 图片生成模型
embedding         向量模型，暂时预留
```

- `ai_agent` 增加图片生成能力配置字段：

```text
image_generation_enabled 是否启用图片生成能力
image_model_id           绑定的图片生成模型
image_api_key_id         绑定的图片生成 API Key
image_storage_strategy   图片存储策略：local / object_storage / oss / cos / qiniu / s3
```

- 新增 `ai_generated_image` 表，用于保存图片生成记录、图片地址、本地路径、图床地址和上下文摘要。
- 模型管理支持维护图片生成模型。
- 智能体管理弹框右侧新增“图片生成能力”配置卡片。
- 图片生成模型与对话模型已解耦：智能体仍选择一个对话模型，同时可以额外绑定图片生成模型 API。
- 初始化增加智谱 `GLM-Image` 图片生成模型记录，并复用智谱 Key 作为开发阶段图片生成 Key。

图片存储说明：

- 第一版可以先使用本地存储，便于开发联调。
- 正式环境建议切换成图床或对象存储，例如阿里 OSS、腾讯 COS、七牛云或 S3。
- 后续只需要替换图片存储实现，不影响 AI 对话和工具调用逻辑。

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 前端执行 `npm run build`，通过。
- 后端执行 `mvn -q spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"`，Flyway 已成功迁移到 V5。

---

## 二十二、2026-05-20 图片存储 / 图床管理模块

本轮新增图片存储管理模块，用于后续统一维护本地存储、图床和对象存储配置。

设计原因：

- 图片生成能力不应该写死本地目录或某一个图床。
- 后续可能同时存在本地开发存储、阿里 OSS、腾讯 COS、七牛云、S3、自有图床等多个存储配置。
- AI 图片生成能力应该绑定“具体图片存储配置”，而不是只绑定 `local/oss/cos` 这种粗粒度类型。

已完成：

- 新增数据库表 `ai_image_storage_config`。
- 新增默认配置：

```text
编码：local_dev
名称：本地开发存储
类型：local
目录：uploads/ai-images
```

- `ai_agent` 新增 `image_storage_config_id`，用于绑定具体图片存储配置。
- 后端新增图片存储管理接口：

```text
GET  /api/admin/image-storage
POST /api/admin/image-storage
PUT  /api/admin/image-storage/{storageId}
```

- 图片存储配置中的 AccessKeyId / AccessKeySecret 会加密保存，接口只返回脱敏值。
- 前端新增菜单：`图片存储`。
- 前端新增页面：图片存储管理。
- 智能体图片生成 Tab 中的“图片存储”改为选择具体存储配置，而不是选择静态存储类型。

当前存储类型约定：

```text
local           本地存储
object_storage  图床 / 对象存储
oss             阿里 OSS
cos             腾讯 COS
qiniu           七牛云
s3              S3 兼容存储
custom          自定义
```

后续图片生成落地策略：

- 如果选择本地存储，生成图片保存到 `boss-chat-server/uploads/ai-images`。
- 如果选择图床或对象存储，后端先接收生成图片，再上传到对应存储，并保存最终访问地址。
- 删除会话仍然只做软删除，不直接删除图片资源；图片资源清理应做成独立功能或定时任务。

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 前端执行 `npm run build`，通过。
- 后端执行 `mvn -q spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"`，Flyway 已成功迁移到 V6。

---

## 二十、2026-05-19 AI 会话记录编辑与软删除

本轮补齐 AI 对话左侧会话记录的基础管理能力。

已完成：

- 左侧会话卡片新增操作入口。
- 支持修改会话标题，方便用户把自动生成标题改成更容易识别的业务名称。
- 支持删除会话。
- 删除会话不是物理删除，而是软删除：后端把 `ai_conversation.status` 标记为 `deleted`。
- 清空上下文继续使用独立状态：`cleared`。
- 历史会话列表只展示 `active` 状态，所以 `cleared` 和 `deleted` 都不会在页面中继续显示。

新增接口：

```text
PUT    /api/chat/conversations/{conversationId}/title
DELETE /api/chat/conversations/{conversationId}
```

当前会话状态约定：

```text
active   正常可见、可继续对话
cleared  已清空上下文，不再作为可继续会话加载
deleted  用户删除，页面不可见，但数据库保留
```

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 前端执行 `npm run build`，通过。

---

## 二十三、2026-05-20 图片存储扩展配置与空值展示优化

本轮继续完善图片存储 / 图床管理模块，主要解决不同图床参数不完全一致的问题，以及页面空字段展示不够清晰的问题。

设计结论：

- 图床和对象存储的核心字段可以统一，例如 Endpoint、Region、Bucket、访问域名、保存目录、AccessKeyId、AccessKeySecret。
- 但不同服务商仍可能存在特殊参数，例如相册 ID、上传路径策略、签名方式、接口版本、自定义 Header 等。
- 因此数据库保留通用字段，同时新增扩展配置字段，用于保存特殊服务商参数。

已完成：

- `ai_image_storage_config` 新增字段：

```text
extra_config_json  扩展配置 JSON，用于保存特殊图床或对象存储参数
```

- 后端图片存储接口已支持读取和保存 `extraConfigJson`。
- 前端图片存储表单新增“扩展配置”文本域。
- 图片存储列表中未填写的字段统一显示为 `—`，避免空白造成误解。
- AccessKeyId / AccessKeySecret 仍然加密保存，列表只展示脱敏值；没有填写时同样显示 `—`。

扩展配置示例：

```json
{
  "albumId": "xxx",
  "pathStyleAccess": true,
  "customHeader": "value"
}
```

当前建议：

- 本地开发存储可以只填写名称、编码、类型和保存目录。
- OSS / COS / 七牛 / S3 这类对象存储优先使用通用字段。
- 遇到特殊图床或非标准接口时，再把额外参数放到 `extra_config_json`。

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 前端执行 `npm run build`，通过。
- 后端执行 `mvn -q spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"`，Flyway 已成功迁移到 V7。

---

## 二十四、2026-05-20 Postimages 图床配置预置

本轮根据 Postimages 账号资料，新增一条 Postimages 图片存储配置，方便后续接入图片上传能力。

已完成：

- 新增 Flyway 迁移：`V8__postimages_storage_config.sql`。
- 新增图片存储配置：

```text
编码：postimages_zhouya
名称：Postimages 个人图床
类型：object_storage
Endpoint：https://api.postimage.org/1/upload
访问域名：https://i.postimg.cc
空间/账号：zhouya166913
状态：停用
```

- 扩展配置中记录 Postimages 个人主页和后续 API 接入信息：

```json
{
  "profileUrl": "https://postimg.cc/user/zhouya166913",
  "profileUser": "zhouya166913",
  "directImageDomain": "https://i.postimg.cc",
  "apiKeyUrl": "https://postimages.org/login/api",
  "apiStatus": "pending_api_key",
  "resize": "0",
  "expire": "0"
}
```

注意：

- 当前配置先保持停用，因为还没有拿到 Postimages API Key。
- 等拿到 API Key 后，再把 API Key 写入密钥字段并启用。
- 如果 Postimages 只有单个 API Token，后续可以把它放入 AccessKeySecret，AccessKeyId 留空；也可以进一步把前端表单按存储类型优化成“Token”展示。
- Postimages 更适合开发验证或轻量图床，不建议作为企业正式生产图片资产的唯一存储。

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 后端执行 `mvn -q spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"`，Flyway 已成功迁移到 V8。

---

## 二十五、2026-05-20 AI 对话接入图片生成工具调用

本轮修复“智能体已开启图片生成能力，但对话时仍只输出文本流程图”的问题。

问题原因：

- 之前已经完成了图片生成的配置层：模型管理、智能体配置、图片生成 API、图片存储配置。
- 但 AI 对话执行链路中还没有把图片生成注册成可调用工具。
- 因此模型虽然知道用户想要流程图，但只能用文本 / Markdown 字符图回答，不会真正调用图片模型。

已完成：

- 新增 `generate_image` 工具。
- 当智能体开启图片生成能力后，对话接口会进入工具调用链路。
- 如果用户明确要求生成图片、画图、配图、海报、流程图、示意图或视觉素材，系统提示词会要求模型调用 `generate_image`。
- `generate_image` 会调用当前智能体绑定的图片生成模型 API。
- 当前已按 OpenAI 兼容风格请求：

```text
POST {provider.baseUrl}/images/generations
Authorization: Bearer {apiKey}
```

- 已新增后端实体与 Mapper：

```text
AiGeneratedImage
AiGeneratedImageMapper
AiImageGenerationService
```

- 生成记录会写入 `ai_generated_image` 表。
- 图片生成成功后，工具会返回 Markdown 图片链接。
- 如果模型最终回答没有带上图片 Markdown，后端会自动把图片补到最终回答末尾。
- 前端 AI 对话窗口已支持识别并展示 Markdown 图片：

```text
![AI生成图片](https://...)
```

当前限制：

- 目前优先展示图片模型返回的图片 URL。
- 本地落盘、Postimages 上传、OSS/COS/Qiniu/S3 上传还没有正式实现上传适配器。
- Postimages 当前仍建议作为轻量验证，不建议作为生产唯一图片资产存储。

本次验证：

- 后端执行 `mvn -q clean compile`，通过。
- 前端执行 `npm run build`，通过。
- 后端执行 `mvn -q spring-boot:run "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"`，应用启动成功，Flyway 当前为 V8。


---

## 二十六、2026-05-20 场景管理与单聊 / 团队会话模型

本次把 AI 对话从“只选择一个 AI”升级为“先选择业务场景，再选择场景内 AI”。这个设计是为了支持老板后续真正想要的能力：面对不同企业、不同业务场景，可以配置一组专属 AI 助手。

### 核心概念

- **单聊场景**：每个 AI 拥有自己的独立会话和上下文，适合分别调试某个助手。
- **团队场景**：同一个场景内多个 AI 共用一个会话上下文，类似一个 AI 团队在同一会议室里协作。
- **图片生成能力仍然属于 AI 助手能力**，不是场景能力。

因此：

| 场景模式 | 上下文规则 | 图片生成规则 |
| --- | --- | --- |
| single | 选中哪个 AI，就使用该 AI 的独立上下文 | 选中的 AI 开启图片生成后，可在单聊中生成图片 |
| team | 场景内 AI 共用同一份上下文 | 用户切换到带图片生成能力的 AI 后，可基于团队上下文生成图片 |

### 后端变更

- 新增数据表：
  - `ai_scene`：AI 场景。
  - `ai_scene_agent`：场景和 AI 助手的关联关系。
- `ai_conversation` 新增：
  - `scene_id`：所属场景。
  - `chat_mode`：会话模式，`single` 或 `team`。
- 新增接口：
  - `GET /api/chat/scenes`：查询对话可用场景。
  - `GET /api/admin/scenes`：管理端查询场景。
  - `POST /api/admin/scenes`：新增场景。
  - `PUT /api/admin/scenes/{sceneId}`：修改场景。
- 对话接口已带上 `sceneId` 和 `agentId`，后端会校验当前 AI 是否属于当前场景。

### 前端变更

- 新增“场景管理”菜单。
- AI 对话页顶部增加场景选择。
- 场景切换后，只展示当前场景下可用的 AI。
- 单聊模式切换 AI 会进入该 AI 的独立上下文。
- 团队模式切换 AI 不会清空会话，只决定下一次由哪个 AI 回复。
- 会话列表会展示“团队 / 工具 / 图片 / 模型”标签。

### 图片生成验收规则

- 单聊场景中，只有当前选中的 AI 开启图片生成时，才应生成图片。
- 团队场景中，可以把带图片生成能力的 AI 加入场景；当用户在团队会话中切换到该 AI 后，它应能根据共享上下文生成图片。
- 图片生成结果仍然绑定到当前 `conversation_id`，避免图片和会话记录脱节。

### 验证结果

- 后端通过：`mvn -q clean compile`
- 前端通过：`npm run build`

### 后续提醒

1. 如果未来接入真正图床，需要把图片生成后的文件上传逻辑接入图片存储适配器。
2. 如果老板要做“AI 团队开会”，当前团队场景已经具备基础模型：共享上下文 + 切换发言 AI。
3. 后续可以再加“@某个 AI 发言”或“让多个 AI 依次回答”的会议增强能力。

---

## 二十七、2026-05-20 旧会话接力进度与后端日志问题结论

本节记录来自会话 `019e3901-1f7a-7681-a348-22b42d847b71` 的接力信息。该会话在继续排查请求问题时上下文空间耗尽，因此后续 AI 接手时应先读本节，避免重复排查。

### 旧会话已完成 / 已确认

- 已确认当前四个主线项目仍是：`boss-chat-server`、`boss-chat-web`、`boss-chat-app`、`boss-chat-miniapp`。
- 已确认参考目录 `MinimaxAndroidChat`、`minimax-token-chat-demo` 和 `tools` 只作为参考，不作为主线继续扩展。
- 已完成并验证过多模型管理、图片生成能力、图片存储配置、Postimages 预置配置、AI 对话图片生成工具调用、场景管理、单聊 / 团队会话模型等阶段性能力。
- 已修复过管理端部分中文显示乱码问题，前端执行过 `npm run build` 并通过。
- 已确认端口含义：`9000` 是管理端 Vite 开发服务，`9090` 才是真正后端服务。
- 已确认 `boss-chat-server/backend-run.log` 是 2026-05-16 的旧日志；如果后端从 IntelliJ IDEA 启动，实时日志主要在 IDEA Run 控制台里，本仓库文件中未必能读到最新输出。

### 2026-05-20 后端日志问题

用户在旧会话中贴出的关键日志为：

```text
org.springframework.web.servlet.resource.NoResourceFoundException:
No static resource api/chat/workflows.
```

结论：

- 这不是模型调用失败，也不是数据库迁移失败。
- 这也不是 `9000` / `9090` 端口理解错误导致的日志来源问题。
- 根因是前端当前会请求 `GET /api/chat/workflows?agentId=...`，但后端 `AiChatController` 目前没有暴露 `GET /api/chat/workflows`。
- Spring MVC 找不到 Controller 映射后，把该路径交给静态资源处理器，最后抛出 `NoResourceFoundException`，所以日志里才会出现 “No static resource api/chat/workflows”。

当前代码对应关系：

```text
前端：boss-chat-web/src/api/chat.ts
  listChatWorkflows() -> GET /chat/workflows

后端：boss-chat-server/.../AiChatController.java
  已有 GET /api/chat/scenes
  已有 GET /api/chat/agents
  已有 POST /api/chat/messages
  已有 POST /api/chat/messages/stream
  目前没有 GET /api/chat/workflows

后端已有管理端工作流接口：
  GET /api/admin/agents/{agentId}/workflows
```

后续处理建议：

1. 优先在后端新增一个聊天端只读接口：`GET /api/chat/workflows?agentId=...`，内部复用 `AiWorkflowService.listByAgent(agentId)`，并按聊天入口权限做登录校验。
2. 不建议直接让 AI 对话页调用 `/api/admin/agents/{agentId}/workflows`，因为那是管理端接口，权限语义更重，容易让普通聊天入口被管理员接口绑死。
3. `GlobalExceptionHandler` 后续可以单独处理 `NoResourceFoundException`，返回更明确的 404，避免这类前后端路径不一致被误判为 500 服务端异常。

### 当前接力状态

到本节更新为止，文档已记录旧会话遗留问题，但尚未修改代码。下一步如果要继续推进，应先补齐 `GET /api/chat/workflows`，再重新验证 AI 对话页场景选择、AI 选择、工作流下拉框和发送消息链路。

---

## 二十八、2026-05-21 模型接口路径动态配置

本次根据模型管理页测试反馈，把“模型调用接口”从代码硬编码改为模型配置项。目标是让管理员在模型管理中正确填写各厂家的接口路径后，当前 AI 调用可以直接按配置走对应接口。

### 已完成代码变更

- `ai_model` 新增字段：
  - `api_path`：模型接口路径，例如 `/chat/completions`、`/images/generations`、`/zrag/retrieval/retrieve`。
  - `billing_type`：计费标识，`free` / `paid` / `unknown`。
  - `official_doc_url`：官方 API 文档地址。
- 新增迁移：`boss-chat-server/src/main/resources/db/migration/V11__dynamic_model_endpoint_config.sql`。
- 后端模型管理接口已支持创建、更新、返回上述字段。
- 新增模型删除接口：`DELETE /api/admin/model-management/models/{modelId}`。
- 删除模型前会检查：
  - 该模型下是否还有 API Key。
  - 是否仍被智能体的对话模型或图片模型引用。
  - 是否已有图片生成记录。
- `LlmChatService` 不再硬编码 `/chat/completions`，会按当前模型的 `api_path` 拼接供应商 `base_url`。
- `AiImageGenerationService` 不再硬编码 `/images/generations`，会按图片模型的 `api_path` 拼接调用地址。
- 前端模型管理详情页：
  - 模型卡片展示“接口：...”。
  - 模型编辑弹窗新增“接口路径 / 计费类型 / 官方文档”。
  - 模型卡片新增“删除模型”操作。
  - 免费 / 付费标签和官方文档链接优先使用后端配置，静态映射只作为旧数据兜底。

### 使用说明

供应商的 `base_url` 只维护域名和版本前缀，例如：

```text
https://open.bigmodel.cn/api/paas/v4
https://api.moonshot.cn/v1
```

模型的 `api_path` 维护具体接口路径，例如：

```text
/chat/completions
/images/generations
/zrag/retrieval/retrieve
```

最终调用地址由后端运行时拼接：

```text
base_url + api_path
```

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`

---


## 三十八、2026-05-22 OpenAI GPT-5.5 对话模型接入

本次在现有“模型厂家 -> 模型 -> API Key”管理体系中新增 OpenAI 厂家和 GPT-5.5 对话模型。

### 本阶段完成

- 新增厂家：`openai`
- Base URL：`https://api.openai.com/v1`
- 新增模型：

```text
模型 ID：gpt-5.5
显示名称：GPT-5.5
模型类型：chat
接口路径：/chat/completions
官方文档：https://developers.openai.com/api/docs/models/gpt-5.5/
上下文窗口：1050000
支持能力：stream / tools / vision
```

- 新增迁移：

```text
boss-chat-server/src/main/resources/db/migration/V15__seed_openai_gpt55.sql
```

- 新增初始化 Key 配置项：

```text
MODEL_SEED_OPENAI_API_KEY
OPENAI_API_KEY
```

任一环境变量可作为初始化 OpenAI API Key；也可以在模型管理页手动新增 Key，后端会加密保存。

### 调用兼容

- 当前后端仍复用 Chat Completions 抽象。
- OpenAI GPT-5 系列调用时会把 `max_tokens` 转为 `max_completion_tokens`。
- OpenAI GPT-5 系列默认附带 `reasoning_effort=medium`。
- 官方推荐长期迁移到 Responses API；这次先做兼容接入，后续可单独做 OpenAI Responses 调用适配层。

### 图片资源策略提醒

- AI 厂商生成或编辑得到的图片 URL 不再自动下载到本地或对象存储。
- 用户上传图片会进入当前启用的图片存储配置，可选本地、阿里云 OSS、腾讯云 COS。
- 如果用户要继续编辑某张 AI 图，可以由用户手动下载后作为用户资源重新上传。

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`
- 前端通过：`npm run build`

---

## 三十七、2026-05-22 阿里云 OSS 用户图片资源接入

本次根据最新产品判断，调整图片资源策略：AI 厂商生成 / 编辑返回的图片先直接展示厂商 URL，不再自动下载到本地；阿里云 OSS 优先用于保存用户上传图片，以及后续用户主动保存或继续编辑所需的图片资源。

### 本阶段完成

- 新增阿里云 OSS SDK 依赖。
- 恢复“图片存储管理”配置入口，支持新增 / 编辑本地存储、阿里云 OSS 和腾讯云 COS。
- 阿里云 OSS 配置保存到 `ai_image_storage_config`，AccessKey 使用现有 `ApiKeyCryptoService` 加密保存。
- 上传图片时会读取默认启用的图片存储配置；默认配置为 `oss` 时上传到阿里云 OSS，默认配置为 `cos` 时上传到腾讯云 COS，否则继续本地保存兜底。
- 新增 `AliyunOssStorageService`，上传图片时自动设置：
  - `Content-Type`
  - `Content-Disposition: inline`
  - `Cache-Control`
- 新增 `TencentCosStorageService`，使用腾讯云 COS Java SDK 上传图片。
- `ChatAttachmentService` 已改为：图片附件在 OSS 启用时上传到 OSS；OSS 未启用时继续本地保存兜底。
- `LlmChatService` 已支持直接把 OSS 图片 HTTPS URL 作为多模态 `image_url` 传给视觉模型。
- `AiImageGenerationService` / `AiImageEditService` 已改为直接返回厂商图片 URL，并在 `ai_generated_image.object_url` 中记录该 URL，不再自动保存到 `uploads/ai-images`。

### 新增关键决策

- OSS 不做 AI 试生成图片的全量备份，避免把大量临时图沉淀为系统资源。
- OSS 当前优先服务“用户上传图片 / 用户确认保存图片 / 图片编辑源图”。
- 后续应新增“保存到素材 / 继续编辑”动作，将用户选中的 AI 临时图转存为用户资源。

### 当前限制

- 厂商返回图片 URL 可能过期，历史会话图片不保证长期可回放。
- 用户如果要长期保留 AI 图片，需要后续通过“保存到素材”显式转存。
- 当前只完成用户上传图片的 OSS 接入，Excel / PDF / Word / 视频仍走本地保存。
- 腾讯 COS 测试 Bucket 信息：

```text
Bucket：lantu-boss-chat-1314624174
地域：ap-guangzhou
访问域名：https://lantu-boss-chat-1314624174.cos.ap-guangzhou.myqcloud.com
权限：公有读私有写
```

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`

---

## 三十六、2026-05-21 当前项目进度总览

本节用于快速判断当前项目真实进度，避免后续只看到零散功能点。

### 当前已完成的主线能力

- 后台基础权限、用户、AI 场景、AI 管理、模型管理、图片存储管理页面已具备基础管理能力。
- AI 对话已支持单聊场景和团队场景，单聊按 AI 独立上下文，团队按场景共享上下文。
- AI 对话已支持 SSE 流式回复、工具调用步骤展示和敏感工具审批。
- 会话列表操作已收敛为三个点菜单，AI 回复 Markdown 已做前端渲染优化。
- 模型管理已支持供应商、模型、API Key 的新增、编辑、删除，并支持动态 `api_path`。
- 模型卡片已支持免费/付费、官方文档链接、模型能力标签。
- 已预置智谱、Kimi、通义 Qwen 等供应商和模型。
- 已预置 `GLM-5V-Turbo` 多模态对话模型，并绑定到“智谱多模态工作台”。
- 已接入 `GLM-Image` 图片生成工具。
- 已试接入 `Qwen Image Edit` 图片编辑工具 `edit_image`。
- 用户上传附件已支持图片、`.xlsx`、PDF、Word、文本和视频本地保存。
- 图片附件会以 base64 data URL 传给支持视觉的对话模型。
- AI 生成图片和编辑图片返回资源会下载到本地 `uploads/ai-images`，前端展示本地 `/api/uploads/...` 链接。
- `GET /api/chat/workflows` 已补齐，后端不再因为该路径缺失刷 `No static resource api/chat/workflows`。

### 当前可测试路径

1. 在模型管理中确认智谱对话模型、智谱图片生成模型、Qwen 图片编辑模型均有可用 API Key。
2. 在 AI 对话中选择“智谱多模态工作台”。
3. 测试普通对话、上传图片分析、上传 `.xlsx` 分析。
4. 测试“生成一张图片”，应调用 `generate_image`。
5. 在同一会话继续说“把上一张图改成红色卷发”，应优先调用 `edit_image`。
6. 测试后可清理：

```text
uploads/chat-attachments
uploads/ai-images
```

### 当前风险和待验证点

- 本机尚未配置 Qwen / DashScope API Key，因此 `edit_image` 已完成编译验证，但还需要真实 Key 做厂商联调。
- Qwen 图片编辑当前是全局工具模型，尚未做成每个 AI 独立绑定的配置项。
- 历史消息仍只保存附件名称，附件结构化元数据尚未入库。
- PDF / Word / 视频当前只保存文件，尚未做正文解析、抽帧或视频理解。
- 多模态工具型智能体对图片附件的处理仍需继续增强，尤其是“工具链路里真正把上传图片交给视觉模型分析”。
- 前端构建存在已有大 chunk 警告和 `@vueuse/core` 注释警告，不影响当前功能，但后续可优化。

### 下一步建议

- 第一优先：配置 Qwen / DashScope API Key，真实测试 `edit_image`。
- 第二优先：把 AI 回复图片、本地附件做成历史消息可回显的附件卡片。
- 第三优先：新增附件表，绑定 `conversation_id`、`message_id`、本地路径、资源类型和摘要。
- 第四优先：把“图片编辑模型”独立加入 AI 管理页配置，避免长期使用全局默认模型。

### 本轮收口验证

- 已同步更新过期文档：`开发文档/05-项目说明/后端项目说明.md`
- 后端通过：`mvn -q -DskipTests compile`
- 前端通过：`npm run build`
- 前端仍只有已有大 chunk 和 `@vueuse/core` 注释警告。
- 前端通过：`npm run build`

前端构建仍有已有的 Vite 大 chunk 警告和 `@vueuse/core` Rollup 注释警告，不影响本次功能。

---

## 二十九、2026-05-21 智谱最新多模态模型预置

本次根据模型管理页测试诉求，给智谱新增一个最新多模态模型预置，并结合当前“企业 AI 管理助手 / AI 获客与落地交付”的项目背景创建默认 AI 助手。

### 新增模型

```text
供应商：智谱 AI
模型名：glm-5v-turbo
显示名：智谱多模态工作台
类型：chat
接口路径：/chat/completions
上下文：200000
能力：流式、工具调用、视觉输入
官方文档：https://docs.bigmodel.cn/cn/guide/models/vlm/glm-5v-turbo
```

对应文件：

```text
boss-chat-server/src/main/resources/db/migration/V12__seed_zhipu_glm5v_turbo.sql
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/bootstrap/AiModelBootstrap.java
```

### 新增默认 AI

```text
agent_code：multimodal_workspace
名称：智谱多模态工作台
绑定模型：zhipu / glm-5v-turbo
```

定位：识别图片、截图、视频、文件和图文混合材料中的业务信息，并把材料转成企业 AI 方案、诊断结论、流程清单、话术或下一步验证动作。

对应文件：

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/bootstrap/AiAgentBootstrap.java
```

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`
- 前端通过：`npm run build`

前端构建仍只有已有的 Vite 大 chunk 和 Rollup 注释警告。

---

## 三十、2026-05-21 AI 对话附件上传与多模态输入第一版

本次根据“多模态模型如何上传图片、视频、Excel 等资源”的反馈，打通了管理端 AI 对话附件链路。

### 本阶段完成

- AI 对话输入框新增“上传附件”按钮。
- 后端新增接口：

```text
POST /api/chat/attachments
```

- 对话发送接口 `POST /api/chat/messages` 和流式接口 `POST /api/chat/messages/stream` 已支持 `attachments` 字段。
- 图片附件第一版保存到本地，并在模型调用时转换成 `data:image/...;base64,...` 多模态输入，解决本地图片 URL 不能被云端模型访问的问题。
- `.xlsx` 文件上传后会在后端读取前 50 行内容，作为附件摘要和系统上下文传给 AI。
- PDF / Word / 文本 / 视频第一版先保存为本地附件，并在上下文中说明当前版本暂不解析正文或视频画面。
- 前端发送消息时会把附件名称展示在用户消息中，便于确认本轮消息带了哪些资源。

### 相关代码

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/controller/AiChatController.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/ChatAttachmentService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AiChatService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/LlmChatService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/common/config/WebConfig.java
boss-chat-web/src/api/chat.ts
boss-chat-web/src/modules/chat/components/ChatComposer.vue
boss-chat-web/src/modules/chat/composables/useChatWorkspace.ts
boss-chat-web/src/modules/chat/styles/chat.css
```

### 新增对接文档

已新增专项文档：

```text
开发文档/03-架构与规范/AI对话附件与多模态对接说明.md
```

该文档记录了上传接口、请求结构、模型多模态 payload、当前限制和后续建议。

### 当前限制

- 图片暂未真正上传到图床或对象存储；当前先用本地存储和 base64 data URL 保证本地联调可用。
- `.xls` 暂不支持，需要另存为 `.xlsx`。
- PDF / Word / 视频当前只保存文件，尚未做正文解析、视频抽帧或视频理解。
- 附件暂未单独建表，历史消息只保存附件名称，不保存完整附件元数据。

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`
- 前端通过：`npm run build`

前端构建仍只有已有的 Vite 大 chunk 和 Rollup 注释提示，不影响本次功能。

---

## 三十一、2026-05-21 图片存储收敛为本地测试

本次根据最新反馈，暂时删除 Postimages / OSS / COS 等外部图片存储方向，当前阶段只保留本地存储测试。

### 本阶段完成

- 新增迁移：

```text
boss-chat-server/src/main/resources/db/migration/V13__local_only_image_storage.sql
```

- 迁移会把 `local_dev` 设置为启用和默认，并删除非本地图片存储配置。
- 如果已有 AI 绑定了非本地图片存储配置，会自动改回 `local_dev`。
- 图片存储管理页已收敛为“本地存储”配置，不再展示 Postimages / OSS / COS / 七牛 / S3 等选项。
- 对接文档已同步说明：当前只使用本地存储，外部图床和对象存储后续再评估。

### 当前结论

- 图片生成结果和对话附件都先走本地目录。
- 多模态图片输入仍使用后端读取本地文件并转 base64 data URL 的方式调用模型。
- 不再把 Postimages 作为当前任务分支继续推进。

---

## 三十二、2026-05-21 修复 AI 对话工作流接口缺失

本次根据后端日志：

```text
NoResourceFoundException: No static resource api/chat/workflows.
```

确认根因是前端 AI 对话页会请求：

```text
GET /api/chat/workflows?agentId=...
```

但后端此前只提供了管理端工作流接口：

```text
GET /api/admin/agents/{agentId}/workflows
```

聊天入口没有对应的 `/api/chat/workflows` Controller 方法，请求落空后被 Spring MVC 当作静态资源路径处理，最终进入全局异常并刷出“未处理的服务端异常”。

### 本阶段完成

- 新增聊天侧只读工作流接口：

```text
GET /api/chat/workflows?agentId={agentId}
```

- 接口会校验登录态和 AI 是否启用，然后返回该 AI 下已启用的工作流。
- 保持权限边界：聊天页只读取可用工作流，管理页继续负责工作流增删改。
- 全局异常处理新增 `NoResourceFoundException` 的 404 返回，避免不存在的接口或静态资源路径继续被记录成未处理 500。

### 相关代码

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/controller/AiChatController.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/common/exception/GlobalExceptionHandler.java
```

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`
- 本次未修改前端代码，无需重新构建前端。

---

## 三十三、2026-05-21 图片生成后续修改失败排查

本次根据聊天页现象排查：

```text
先生成一张人像成功，再发送“换成红色发头发”后页面只显示“回复失败，请稍后再试。”
```

### 结论

当前 `generate_image` 工具是文生图能力，只能根据提示词生成新图片；它还不是“读取上一张图片并做像素级编辑”的图片编辑接口。用户说“把上一张图改成/换成/继续调整”时，当前合理行为应是结合上下文重新生成一张满足修改要求的变体，而不是承诺对原图做局部编辑。

页面之前还有一个体验问题：SSE 返回真实错误时，聊天气泡只显示统一的“回复失败，请稍后再试”，导致看不到后端真实原因。

### 本阶段完成

- 前端 SSE `error` 事件会解析真实错误文本。
- 聊天气泡会展示 `回复失败：具体错误原因`，便于定位是模型调用、图片模型调用、工具调用还是接口配置问题。
- 后端工具描述已明确 `generate_image` 是文生图，不是像素级图片编辑接口。
- 智能体系统提示已要求：遇到“把上一张图改成/换成/继续调整”时，结合上下文重新生成一张完整变体，并说明这是重新生成版本。

### 相关代码

```text
boss-chat-web/src/api/chat.ts
boss-chat-web/src/modules/chat/composables/useChatWorkspace.ts
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AgentToolRegistry.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AutonomousAgentService.java
```

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`
- 前端通过：`npm run build`

前端构建仍只有已有的大 chunk 和 `@vueuse/core` 注释警告。

---

## 三十四、2026-05-21 试接入 Qwen Image Edit 图片编辑

本次根据“哪个大模型支持图片编辑，先试试”的反馈，新增通义 Qwen 图片编辑链路第一版。

### 本阶段完成

- 新增供应商预置：

```text
qwen / 通义千问 / DashScope
https://dashscope.aliyuncs.com/api/v1
```

- 新增图片编辑模型预置：

```text
qwen-image-2.0-pro
模型类型：image_edit
接口路径：/services/aigc/multimodal-generation/generation
官方文档：https://www.alibabacloud.com/help/en/model-studio/qwen-image-edit-api
```

- 新增迁移：

```text
boss-chat-server/src/main/resources/db/migration/V14__seed_qwen_image_edit.sql
```

- 新增后端服务：

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AiImageEditService.java
```

- 新增智能体工具：`edit_image`
- 用户说“把上一张图改成/换成/继续调整”时，系统提示会优先调用 `edit_image`。
- `sourceImageUrl` 为空时，后端会自动取当前会话最近一张成功生成的图片作为源图。
- 新增配置项：

```text
MODEL_SEED_QWEN_API_KEY
DASHSCOPE_API_KEY
```

任一环境变量可作为初始化 Qwen API Key；也可以在模型管理页手动新增 Key。

### 当前限制

- 本机当前未配置 Qwen / DashScope API Key，因此只完成编译验证，尚未真实调用厂商接口。
- Qwen 返回的图片 URL 有有效期；当前已改为后端立即下载到本地 `uploads/ai-images` 后再返回前端。
- 当前第一版是全局图片编辑模型，尚未在 AI 管理页增加“图片编辑模型”独立绑定项。

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`

---

## 三十五、2026-05-21 测试阶段资源统一落本地

本次根据“为了方便测试，用户资源和 AI 回复资源都暂时放入本地”的反馈，收敛资源保存策略。

### 本阶段完成

- 新增本地 AI 图片保存服务：

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/LocalAiImageStorageService.java
```

- 用户上传附件继续保存到：

```text
uploads/chat-attachments/{yyyyMMdd}/{userId}/
```

- AI 图片生成和图片编辑返回的厂商临时 URL，会立即下载到：

```text
uploads/ai-images/{yyyyMMdd}/{userId}/
```

- 聊天消息中的 Markdown 图片链接返回本地 `/api/uploads/ai-images/...`，不再直接暴露厂商临时链接。
- `ai_generated_image.source_url` 保留厂商原始 URL，`object_url` 保存本地访问 URL，`local_path` 保存本地文件路径。

### 相关代码

```text
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AiImageGenerationService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/AiImageEditService.java
boss-chat-server/src/main/java/com/zhiyinhui/bosschat/ai/service/LocalAiImageStorageService.java
```

### 验证结果

- 后端通过：`mvn -q -DskipTests compile`
---

## 三十九、2026-05-22 图片存储策略最新覆盖说明

本节覆盖前文“测试阶段资源统一落本地”的临时策略。

当前结论：

- AI 厂商生成 / 编辑返回的图片 URL：直接展示，不再默认下载到本地或图床。
- 图床只用于保存用户上传图片、聊天附件图片、后续保存为素材的图片、后续继续编辑需要持久化的源图。
- 非图片附件仍按当前附件逻辑处理。
- 图片存储配置继续走数据库表 `ai_image_storage_config`，不是直接写死在 `application-local.yml`。

已支持图床：

```text
local
aliyun_oss
tencent_cos
```

专项文档：

```text
开发文档/03-架构与规范/图片存储策略说明.md
```

---

## 四十、2026-05-22 模型兼容模式最新覆盖说明

本节补充 OpenAI GPT-5.5 接入后的参数兼容策略，用于后续新增有冲突的模型时复用。

本阶段完成：

- 新增 OpenAI 供应商和 `gpt-5.5` 对话模型。
- 新增迁移 `V15__seed_openai_gpt55.sql`。
- 新增迁移 `V16__model_compatibility_profile.sql`。
- 新增模型字段 `ai_model.compatibility_profile`。
- 模型管理页新增 Compatibility 字段，可自动推断或手动选择。

当前内置兼容模式：

| 兼容模式 | 适用模型 | 作用 |
| --- | --- | --- |
| `openai_gpt5` | OpenAI GPT-5 系列 | 移除 `temperature`，把 `max_tokens` 改为 `max_completion_tokens`，默认补 `reasoning_effort=medium` |
| `kimi_k2` | Kimi K2 系列 | 套用 Kimi K2 参数兼容逻辑 |

自动推断规则：

```text
openai + gpt-5* -> openai_gpt5
kimi + kimi-k2* -> kimi_k2
```

后续新增模型时，需要区分两类冲突：

| 类型 | 处理方式 |
| --- | --- |
| 能力冲突 | 用 `supports_tools`、`supports_stream`、`supports_vision` 控制，模型不支持的能力不要开启 |
| 参数冲突 | 用 `compatibility_profile` 控制，避免在业务代码里散落模型特判 |

专项文档：

```text
开发文档/03-架构与规范/模型兼容模式与OpenAI接入说明.md
```

验证结果：

- 后端通过：`mvn -q -DskipTests compile`
- 前端通过：`npm run build`

---

## 四十一、2026-05-28 当前接力快照与部署进度

本节用于新会话快速接手。当前项目已经从“AI 对话管理后台”推进到“问卷诊断 + 图片存储 + Jenkins 部署准备”的测试版收口阶段。

### 当前项目主线

- 后端：`boss-chat-server`，Spring Boot 3.3.5，端口默认 `9090`，数据库 `boss_chat_dev`，Flyway 已有 26 个迁移。
- 前端管理端：`boss-chat-web`，用于登录、AI 对话、智能体管理、模型管理、图片存储、调查记录等后台功能。
- 静态问卷页：已放入后端静态资源，不依赖 Vue 项目，客户可直接访问：

```text
/survey/enterprise-diagnosis.html
```

- 问卷结果页：后端静态资源：

```text
/survey/result.html
```

- 当前部署目标：阿里云 ECS 单机部署，Jenkins、后端、前端、Nginx、MySQL 都在同一台服务器上。

### 已完成的重要功能

- 图片存储配置已支持数据库管理，类型包含：

```text
local
aliyun_oss
tencent_cos
```

- 当前图片存储策略：
  - AI 厂商生成或编辑返回的图片 URL 直接展示，不默认下载。
  - 用户上传图片、后续编辑源图、需要保存的素材图片走图床或本地存储。
- AI 模型管理已支持能力标记和兼容模式：
  - `supports_tools`
  - `supports_stream`
  - `supports_vision`
  - `compatibility_profile`
- 调查问卷闭环已完成：
  - 销售可把统一问卷链接发到群里。
  - 客户填写姓名、手机号、公司、人数、业绩和需求选项。
  - 提交后保存数据库记录。
  - 提交后进入结果页，并调用 AI 生成一次性诊断报告。
  - 管理端侧边栏已新增 `调查记录`，可查看列表和详情。
- 企业需求诊断场景已固定为两个 Kimi K2.6 智能体协作：
  - 第一个 AI：整理问卷、识别痛点、生成规划提示词。
  - 第二个 AI：输出正式诊断结论、落地方案和行动建议。
  - 该场景用于固定业务流程，不建议在普通操作中随意替换模型。
- 问卷页体验已优化：
  - 手机号校验提示改成面向客户的文案：`请输入正确的手机号`。
  - 提交等待时有加载状态，不再是空白等待。
  - AI 结果页移除了“重新填写”入口。
  - AI 结果会清理 Markdown 中的 `#`、`*`、表格分隔符等不适合客户阅读的符号。
  - 结果按段落、序号、重点字段展示，痛点、需求和关键解决方案会重点加粗。
- 管理端调查详情也做了清理展示，避免直接暴露模型原始 Markdown 符号。

### 当前本地启动状态

2026-05-27 本地后端曾启动失败，核心错误是：

```text
Access denied for user 'root'@'localhost' (using password: NO)
```

结论：不是业务代码问题，而是本地运行没有读取到数据库密码。已经在本机忽略文件中补了本地 datasource 配置：

```text
boss-chat-server/application-local.yml
```

注意：

- 这个文件是本地运行配置，不要提交到 Git。
- 新环境如果再次出现 `using password: NO`，优先检查 `application-local.yml` 是否包含 datasource 用户名和密码。
- 文档和仓库中不要写真实数据库密码、AI Key、OSS/COS Secret。

本地验证结果：

- 后端可连接 MySQL。
- Flyway 可验证 26 个迁移。
- Tomcat 可启动到 `9090`。
- 验证用进程已停止，不占用端口。

### Jenkins 部署当前进度

用户已经在一台 4 核 8G 阿里云 ECS 上开始部署 Jenkins。

当前已知状态：

- 服务器系统：Alibaba Cloud Linux 4 LTS 64 位。
- Jenkins 安装成功，版本 `2.555.2`。
- Jenkins 服务已能启动，状态曾显示 `active (running)`。
- Jenkins 默认端口：`8080`。
- 当前推荐部署方式：本机部署，不走远程 SSH。

已确认或已安装的 Jenkins 插件方向：

```text
Git
Pipeline
Credentials Binding
SSH Agent
```

注意区分：

- `SSH Build Agents plugin` 是让 Jenkins 连接远程构建节点。
- `SSH Agent Plugin` 是 Pipeline 中使用 SSH 凭据的插件。
- 当前本机部署版不依赖 SSH Agent；即使没配 SSH 私钥，也可以继续做本机部署。

当前代码仓库已经具备：

```text
Jenkinsfile
```

该 Jenkinsfile 当前按本机部署思路执行：

```text
拉取 GitHub 代码
打包后端 boss-chat-server
打包前端 boss-chat-web
复制 jar 到 /opt/boss-chat/app
复制前端 dist 到 /opt/boss-chat/web
重启 boss-chat systemd 服务
访问 http://127.0.0.1:9090/api/health 做本机健康检查
```

2026-05-28 已补齐公开健康检查接口：

```text
GET /api/health
```

该接口不需要登录，用于 Jenkins、Nginx 和人工部署验收确认后端进程已经启动。它不是业务接口，不替代登录、问卷提交或 AI 调用测试。

### Jenkins 下一步

新会话接手时，不要从安装 Jenkins 重新开始。下一步应先检查服务器构建环境：

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

如果缺工具，再按部署文档补装。随后继续：

1. 准备服务器目录：

```bash
sudo mkdir -p /opt/boss-chat/app /opt/boss-chat/web /opt/boss-chat/backup /opt/boss-chat/config /opt/boss-chat/uploads
sudo chown -R jenkins:jenkins /opt/boss-chat
```

2. 准备服务器本地配置：

```text
/opt/boss-chat/config/application-local.yml
```

该文件只放服务器本地，包含数据库连接、AI Key、OSS/COS 配置等敏感信息，不进 GitHub。

3. 创建或确认 systemd 服务：

```text
/etc/systemd/system/boss-chat.service
```

4. 配置 Jenkins 用户免密执行：

```text
systemctl restart boss-chat
systemctl status boss-chat --no-pager
```

5. 配置 Nginx 反向代理。
6. Jenkins 新建 Pipeline：

```text
任务名：boss-chat-deploy
仓库：https://github.com/zhouya166913-cell/Boos-Chat.git
分支：main
脚本路径：Jenkinsfile
```

7. 点击构建并验证：

```text
http://服务器公网IP/
http://服务器公网IP/api/health
http://服务器公网IP/survey/enterprise-diagnosis.html
```

### 新会话优先阅读文档

```text
开发文档/06-部署/Jenkins本机部署完整复刻流程.md
开发文档/06-部署/Jenkins一键部署教程.md
开发文档/06-部署/阿里云ECS部署教程.md
开发文档/03-架构与规范/图片存储策略说明.md
开发文档/03-架构与规范/模型兼容模式与OpenAI接入说明.md
```

### 接手注意事项

- 不要把真实 API Key、数据库密码、OSS/COS Secret 写入文档或提交到 GitHub。
- 如果 Jenkins 插件安装失败，优先换清华 Jenkins update-center，然后重启 Jenkins 再装。
- 如果只是单台服务器，优先走“Jenkins 本机部署”，维护成本最低。
- 如果后端启动报数据库密码为空，优先检查运行配置，不要误改业务代码。
- 调查问卷页面现在属于后端静态资源，部署时必须确认后端 jar 能正常提供 `/survey/enterprise-diagnosis.html`。
## 2026-05-28 部署接力更新

- 当前阿里云 ECS 测试服务器公网 IP 为 `8.162.26.228`。
- Jenkins 已安装并切换到 `9999` 端口，入口为 `http://8.162.26.228:9999/`。
- Jenkins 任务名为 `boss-chat-deploy`，使用 `Pipeline script from SCM`，仓库为 `https://github.com/zhouya166913-cell/Boos-Chat.git`，分支为 `*/main`，脚本路径为 `Jenkinsfile`。
- 后端服务由 systemd 托管，服务名为 `boss-chat`，后端只在服务器本机监听 `9090`，不要对公网开放 `9090`。
- Nginx 统一暴露 `80`：`/` 指向前端管理系统，`/api/` 转发后端 API，`/survey/` 转发问卷静态页，`/swagger-ui.html`、`/swagger-ui/`、`/v3/api-docs` 转发 Swagger。
- 当前关键公网 URL：管理系统 `http://8.162.26.228/`，健康检查 `http://8.162.26.228/api/health`，Swagger `http://8.162.26.228/swagger-ui.html`，问卷 `http://8.162.26.228/survey/enterprise-diagnosis.html`。
- 服务器敏感配置只放在 `/opt/boss-chat/config/application-local.yml`，不要提交数据库密码和 API Key。
- 前端本地开发统一请求 `/api`，通过 `boss-chat-web/.env.development` 中的 `VITE_API_PROXY_TARGET` 在本地后端 `http://localhost:9090` 和服务器后端 `http://8.162.26.228` 之间切换。
- 详细说明见：[当前服务器部署 URL 与联调说明](../06-部署/当前服务器部署URL与联调说明.md)。

## 2026-05-29 上线前实测与代码接力更新

本轮目标是把系统调整为“上线后空白业务系统”：只保留登录、账号和权限基础数据，智能体、场景、模型、API Key、图片存储、问卷记录等业务配置都由管理员进入系统后手动创建。

已完成代码调整：

- 删除后端默认 AI 业务种子启动类，避免新环境自动写入智能体、模型、场景、工作流等演示数据。
- 新增 Flyway 迁移 `V28__prelaunch_blank_business_data.sql`，清空 AI 业务配置、图片存储配置、对话记录、问卷记录等业务数据，并重置自增 ID；不删除登录和权限相关数据。
- `application.yml` 中不再写死默认智谱模型地址、默认模型或模型种子 Key；服务器真实配置继续放在 `/opt/boss-chat/config/application-local.yml`。
- 模型管理改为管理员手动配置：供应商可选择常见厂商并自动填充官方文档地址；模型接口支持填写完整官方调用地址，避免供应商前缀和模型路径混淆。
- Kimi K2 系列兼容规则已处理：请求中关闭 thinking，避免普通对话和问卷生成只返回 reasoning 内容。
- 图片存储管理改为单一“新增”入口，按阿里云 OSS、腾讯云 COS、其他存储切换表单；保存前可进行真实验证，验证会上传临时文件、访问公开 URL，并尝试删除临时文件。
- 图片存储密钥 ID 和密钥 Secret 在管理端按用户要求明文展示，后续正式权限体系完善后再按角色限制入口。
- 问卷结果页修复流式生成体验：loading 图标只创建一次，流式输出时只更新报告正文，避免 spinner 被反复重建造成视觉卡顿。
- 前端 `.env.example` 增加 `VITE_SURVEY_PUBLIC_URL` 示例，本地默认指向 `http://localhost:9090/survey/enterprise-diagnosis.html`，服务器示例指向 `http://8.162.26.228/survey/enterprise-diagnosis.html`。

本轮本地验证：

```text
后端：mvn.cmd -DskipTests compile 通过
前端：npm.cmd run build 通过
人工流程：登录、模型管理、图片存储真实验证、问卷提交和流式结果页已完成上线前实测
```

接手注意：

- 部署后第一次进入系统时，模型、API Key、智能体、场景、工作流、图片存储都应为空，需要按实际业务逐项配置。
- 问卷 AI 需要两个智能体：一个负责企业需求诊断分析，一个负责落地方案文本生成；两者可以绑定同一个 Kimi K2.6 模型和 Key，但职责要分开。
- 如果问卷结果页显示模型调用失败，先到“模型管理”确认供应商、模型完整接口、API Key、智能体绑定关系是否正确。
- 不要把本地 `.env.development.local`、服务器 `application-local.yml`、真实数据库密码、真实 AI Key、OSS/COS Secret 提交进 Git。

## 2026-06-03 课程管理闭环更新

本次把原来的“签到列表”和“调查记录”合并为“课程管理”，入口为 `/courses`，旧入口 `/checkins` 和 `/survey-records` 会自动跳转到课程管理。

业务流程调整为：

1. 管理员先创建课程期数，例如“第十三期AI运营操盘手”。
2. 系统为每一期生成专属调查问卷链接和二维码，链接中包含 `phase` 参数。
3. 管理员在当前期数中录入学员，姓名必填；手机号、身份证号、是否新学员和备注可补充维护。
4. 学员通过二维码进入公开问卷时，需要填写姓名，并在“我是新学员 / 我是老学员”中二选一；手机号和身份证号可选填。
5. 签到准入只校验姓名是否匹配当前期数已录入学员；手机号和身份证号作为本次问卷资料保存，不参与准入拦截。
6. 姓名匹配成功后，进入原调查问卷页面；姓名、手机号、身份证号和新老学员选择会自动带入，不再让学员重复填写基础身份信息。
7. 学员提交问卷后，继续走原来的 AI 诊断分析流程，记录会归属到对应课程期数。
8. 课程管理页可以在同一页面查看当前期数的学员名单、调查问卷记录、二维码和数据看板。
9. 数据看板新增“课程分析”按钮，会调用 `survey_solution_planner` 智能体汇总本期所有学员的痛点，生成给老师备课用的课程思路。

新增后端接口：

```text
GET  /api/public/course-phases/{phaseCode}
POST /api/public/course-phases/{phaseCode}/check-in
POST /api/admin/course-phases/{phaseId}/course-analysis
```

新增数据库迁移：

```text
boss-chat-server/src/main/resources/db/migration/V31__course_survey_checkin_and_analysis.sql
```

该迁移为 `survey_record` 增加 `id_card` 字段，并增加期数+姓名查询索引，用于问卷签到校验和课程看板统计。

发布后验证重点：

- 创建一期课程，确认二维码和问卷链接都带 `phase` 参数。
- 录入学员后，用错误姓名访问问卷，必须被拦截；手机号和身份证号可留空，也不参与签到准入校验。
- 用正确学员姓名选择“新学员/老学员”后，确认能进入问卷；手机号和身份证号填或不填都不影响准入。
- 提交问卷后，课程管理页的“调查问卷记录”和“数据看板”能看到对应记录。
- 数据看板点击“课程分析”，应能根据本期学员痛点生成备课建议。

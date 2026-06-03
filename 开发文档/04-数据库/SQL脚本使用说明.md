# SQL 脚本使用说明

本文档说明当前项目中 SQL 脚本的正确使用方式。

先给结论：

> 不是所有 SQL 都需要你复制到 MySQL 客户端里手动执行。  
> 当前项目中，通常只有“创建数据库”这一步需要先完成；后续“建表、改表、加索引”等数据库结构变更，应该交给 Flyway 在后端启动时自动执行。

---

## 一、当前项目里有两类 SQL 脚本

### 1. 数据库创建脚本

位置：

```text
boss-chat-server/database/create_database.sql
```

用途：

- 创建数据库本身
- 当前默认创建：

```text
boss_chat_dev
```

这类脚本一般只在第一次搭建本地环境时手动执行一次。

### 2. Flyway 迁移脚本

位置：

```text
boss-chat-server/src/main/resources/db/migration/
```

当前正式脚本：

```text
V1__create_system_auth_tables.sql
V2__real_project_phase1.sql
V3__agentic_workspace.sql
V4__multi_model_management.sql
...
V30__course_phases_students_and_survey_links.sql
V31__course_student_identity_fields.sql
V32__allow_blank_course_student_phone.sql
V33__course_analysis_history_and_survey_student_type.sql
```

用途：

- 创建表
- 修改表
- 新增字段
- 新增索引
- 做数据库结构版本升级

这类脚本不需要你复制到 MySQL 客户端里手动执行。

正确方式是：

1. 把脚本放进 `db/migration`
2. 启动后端
3. Flyway 自动按版本顺序执行
4. 执行结果自动记录到：

```text
flyway_schema_history
```

---

## 二、为什么不建议手动执行迁移脚本

如果你手动执行了某个迁移 SQL：

- Flyway 不知道你已经执行过
- `flyway_schema_history` 中没有记录
- 后端下次启动时，Flyway 可能还会再次尝试执行
- 多人协作时，也没人能准确确认数据库当前处于哪个版本

这样会让数据库状态变得不可追踪。

---

## 三、当前正式认证模型

目前后端已经从测试表切换为第一版正式认证结构：

```text
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
sys_oauth_account
sys_login_record
```

它们分别承担：

| 表 | 作用 |
| --- | --- |
| `sys_user` | 系统内的真实用户 |
| `sys_role` | 角色 |
| `sys_permission` | 权限点 |
| `sys_user_role` | 用户与角色关系 |
| `sys_role_permission` | 角色与权限关系 |
| `sys_oauth_account` | 微信 / QQ 等第三方账号与系统用户的绑定 |
| `sys_login_record` | 登录日志 |

---

## 三补充、当前智能体相关表

截至 2026-05-19，智能体第一版已经通过 `V2` 与 `V3` 迁移脚本补齐以下表：

```text
ai_agent
ai_conversation
ai_message
ai_usage_record
ai_memory
ai_knowledge_document
ai_workflow
ai_tool_execution
```

其中：

| 表 | 作用 |
| --- | --- |
| `ai_agent` | 智能体 / AI 配置 |
| `ai_conversation` | AI 对话会话 |
| `ai_message` | AI 对话消息 |
| `ai_usage_record` | 模型调用用量记录 |
| `ai_memory` | 当前用户的长期记忆 |
| `ai_knowledge_document` | 知识库文本内容 |
| `ai_workflow` | 智能体工作流定义 |
| `ai_tool_execution` | 工具调用执行记录 |

注意：

- 后续新增字段或表，继续新增下一个版本号的迁移脚本，不要直接改已执行过的历史脚本。
- 如果本地数据库已经执行过旧脚本，修改旧脚本不会自动生效，必须通过新迁移脚本前进。

---

## 三再补充、当前课程与问卷相关表

截至 2026-06-03，课程期数、学员名单、问卷记录和课程分析历史已经通过 `V30` 到 `V33` 迁移脚本纳入正式结构：

```text
course_phase
course_student
course_analysis_history
survey_record
```

其中：

| 表 | 作用 |
| --- | --- |
| `course_phase` | 课程期数，例如第十三期 AI 运营操盘手 |
| `course_student` | 每一期的学员名单，用于问卷进入前按姓名校验 |
| `survey_record` | 学员提交的调查问卷记录和 AI 诊断结果 |
| `course_analysis_history` | 数据看板中课程分析的历史记录 |

当前规则：

- 问卷提交会保存手机号、身份证号、新老学员类型到 `survey_record`。
- 如果姓名匹配到当前期数学员，系统会同步这些身份信息到 `course_student`。
- 手机号和身份证号不是必填字段；当前进入问卷只校验姓名。
- 调查记录支持按当前期数单条删除或全部删除。

---

## 四、正确的日常使用方式

### 场景 1：第一次创建数据库

手动执行：

```text
boss-chat-server/database/create_database.sql
```

你可以在：

- MySQL Workbench
- Navicat
- DBeaver
- DataGrip
- MySQL 命令行

中执行。

### 场景 2：新增一个迁移脚本

例如以后新增会话表：

```text
V2__create_sys_session_table.sql
```

你只需要：

1. 把脚本放进 `db/migration`
2. 启动后端
3. 看控制台中 Flyway 是否执行成功

如果成功，会看到类似：

```text
Migrating schema `boss_chat_dev` to version "2 - create sys session table"
Successfully applied 1 migration
```

### 场景 3：已经执行过的迁移脚本需要调整

不推荐直接修改旧脚本。

正确方式是继续往前新增版本，例如：

```text
V3__add_last_login_ip_to_sys_user.sql
```

原因：

- 已执行脚本应视为历史记录
- 数据库升级应该通过新脚本向前推进
- 这样每一次结构变更都可追踪

---

## 五、以后看到 SQL 时怎么判断该怎么用

| SQL 类型 | 是否手动执行 | 说明 |
| --- | --- | --- |
| 创建数据库脚本 | 一般手动执行一次 | 例如 `create_database.sql` |
| Flyway 迁移脚本 | 不手动执行 | 放入 `db/migration` 后随后端启动自动执行 |
| 临时查询 SQL | 手动执行 | 例如查数据、排错、验证结果 |
| 数据修复脚本 | 视情况而定 | 如果属于正式结构或正式数据迁移，优先也做成版本化脚本 |

---

## 六、一句话记忆

你可以把规则简单记成：

> 创建数据库，手动一次；  
> 数据库升级，交给 Flyway；  
> 查数和排错，才去客户端手动执行 SQL。
---

## 2026-05-19 第二版数据库重置

本地开发数据库已为第二版清空并重建：

```sql
DROP DATABASE IF EXISTS boss_chat_dev;
CREATE DATABASE boss_chat_dev
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

当前状态：

```text
boss_chat_dev 已存在
已重新执行 Flyway V1-V4
```

后续说明：

- 启动后端后，Flyway 会重新从 V1 执行迁移。
- 如果第二版决定重写表结构，可以在清库状态下调整迁移脚本。
- 如果保留历史迁移，需要确认 V1-V4 是否符合第二版长期结构。

---

## 2026-05-19 第二版迁移验证结果

第二版迁移脚本已从空库重新执行并验证通过。

当前默认初始化数据：

| 数据 | 数量 | 说明 |
| --- | ---: | --- |
| `sys_user` | 1 | 默认超级管理员 `admin` |
| `ai_model_provider` | 2 | 智谱、Kimi |
| `ai_model` | 3 | `glm-4.7`、`glm-4.5-flash`、`kimi-k2.6` |
| `ai_model_api_key` | 3 | 每个默认模型各绑定一个开发期 Key，通过 `application-local.yml` 注入 |
| `ai_agent` | 3 | AI获客操盘手、企业AI赋能顾问、企业AI落地执行助手 |

第二版核心关系：

```text
模型供应商 ai_model_provider
  └─ 模型 ai_model
      └─ 模型 API Key ai_model_api_key
          └─ AI 配置 / 智能体 ai_agent
              ├─ 长期记忆 ai_memory
              ├─ 知识库 ai_knowledge_document
              ├─ 工作流 ai_workflow
              └─ 对话与消息 ai_conversation / ai_message
```

当前关键约束：

- `ai_model_provider` 只表示厂家入口，例如智谱、Kimi。
- `ai_model` 表示厂家下面具体可调用的模型。
- `ai_model_api_key` 同时保存 `provider_id` 与 `model_id`，实际业务上以 `model_id` 为主，表示这个 Key 属于哪个具体模型。
- `ai_agent` 只需要选择 `model_id` 与 `api_key_id`，不应再让管理员重复手填厂家编码、模型名称和 Key。

注意：

- 开发期可以清空本地库重跑迁移。
- 一旦进入多人协作或生产环境，不应再修改已执行过的迁移脚本，应继续新增 `V5__xxx.sql`。

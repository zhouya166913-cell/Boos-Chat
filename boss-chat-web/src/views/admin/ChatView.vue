<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import ChatComposer from "../../modules/chat/components/ChatComposer.vue";
import ChatMessageList from "../../modules/chat/components/ChatMessageList.vue";
import ConversationSidebar from "../../modules/chat/components/ConversationSidebar.vue";
import { useChatWorkspace } from "../../modules/chat/composables/useChatWorkspace";

const {
  scenes,
  selectedSceneId,
  selectedScene,
  selectedAgentId,
  selectedAgent,
  availableAgents,
  selectedWorkflowCode,
  workflows,
  conversationId,
  conversations,
  messages,
  showContextPanel,
  input,
  attachments,
  uploadingAttachment,
  sending,
  loadingScenes,
  loadingConversations,
  loadingMessages,
  isTeamMode,
  headerTitle,
  headerDescription,
  openConversation,
  startNewConversation,
  renameConversation,
  removeConversation,
  handleSend,
  handleAttachmentUpload,
  removeAttachment,
  stopCurrentResponse,
  agentAbilityLabel
} = useChatWorkspace();
const messageListRef = ref<InstanceType<typeof ChatMessageList>>();

watch(
  () => messages.value.map((message) => `${message.role}:${message.agentName || ""}:${message.content}`).join("|"),
  async () => {
    await nextTick();
    await messageListRef.value?.scrollToBottom();
  }
);
</script>

<template>
  <section class="chat-page">
    <el-card shadow="never" class="panel-card chat-panel">
      <template #header>
        <div class="chat-header">
          <div>
            <strong>{{ headerTitle }}</strong>
            <small>{{ headerDescription }}</small>
            <div v-if="selectedScene" class="chat-header-meta">
              <el-tooltip
                :content="isTeamMode ? '团队场景：多个 AI 共用同一段上下文' : '单聊场景：每个 AI 使用独立上下文'"
                placement="bottom-start"
              >
                <span class="chat-mode-pill" :class="isTeamMode ? 'team' : 'single'">
                  {{ isTeamMode ? "团队共享上下文" : "单聊独立上下文" }}
                </span>
              </el-tooltip>
              <span v-if="selectedAgent?.imageGenerationEnabled === 1" class="chat-ability-pill">
                可生成图片
              </span>
            </div>
          </div>
          <div class="chat-header-actions">
            <el-button @click="showContextPanel = true">上下文记录</el-button>
            <el-select
              v-model="selectedSceneId"
              v-loading="loadingScenes"
              placeholder="选择场景"
              style="width: 220px"
            >
              <el-option
                v-for="scene in scenes"
                :key="scene.id"
                :label="scene.sceneName"
                :value="scene.id"
              >
                <span>{{ scene.sceneName }}</span>
                <small class="chat-option-hint">{{ scene.chatMode === "team" ? "团队" : "单聊" }}</small>
              </el-option>
            </el-select>
            <el-select v-model="selectedAgentId" placeholder="选择 AI" style="width: 220px">
              <el-option
                v-for="agent in availableAgents"
                :key="agent.agentId"
                :label="agent.agentName"
                :value="agent.agentId"
              >
                <span>{{ agent.agentName }}</span>
                <small class="chat-option-hint">{{ agentAbilityLabel(agent) }}</small>
              </el-option>
            </el-select>
            <el-select v-model="selectedWorkflowCode" clearable placeholder="选择工作流" style="width: 220px">
              <el-option
                v-for="workflow in workflows"
                :key="workflow.id"
                :label="workflow.workflowName"
                :value="workflow.workflowCode"
              />
            </el-select>
          </div>
        </div>
      </template>

      <div class="chat-workspace">
        <ConversationSidebar
          :conversations="conversations"
          :agents="availableAgents"
          :selected-conversation-id="conversationId"
          :loading="loadingConversations"
          :scene-mode="selectedScene?.chatMode || 'single'"
          @new="startNewConversation"
          @select="openConversation"
          @rename="renameConversation"
          @remove="removeConversation"
        />

        <ChatMessageList
          ref="messageListRef"
          :messages="messages"
          :selected-agent-name="selectedAgent?.agentName"
          :loading="loadingMessages"
        >
          <ChatComposer
            v-model="input"
            :sending="sending"
            :attachments="attachments"
            :uploading="uploadingAttachment"
            @send="handleSend"
            @stop="stopCurrentResponse"
            @upload="handleAttachmentUpload"
            @remove-attachment="removeAttachment"
          />
        </ChatMessageList>
      </div>
    </el-card>

    <el-drawer v-model="showContextPanel" title="当前上下文记录" size="420px">
      <div class="chat-context-summary">
        <p>
          当前会话会把以下消息作为上下文传给 AI。
          {{ isTeamMode ? "团队场景会共享给场景中的所有 AI。" : "单聊场景只会传给当前 AI。" }}
        </p>
        <el-tag type="info">当前 {{ messages.length }} 条消息</el-tag>
      </div>

      <el-empty v-if="messages.length === 0" description="当前还没有上下文记录" />
      <div v-else class="chat-context-list">
        <article
          v-for="(message, index) in messages"
          :key="`${message.role}-${index}`"
          class="chat-context-item"
          :class="message.role"
        >
          <strong>{{ message.role === "user" ? "你" : message.agentName || selectedAgent?.agentName || "AI" }}</strong>
          <p>{{ message.content || (message.waiting ? "正在生成..." : "暂无内容") }}</p>
          <small v-if="message.toolSteps?.length">包含 {{ message.toolSteps.length }} 次工具调用</small>
        </article>
      </div>

      <template #footer>
        <el-button @click="showContextPanel = false">关闭</el-button>
      </template>
    </el-drawer>
  </section>
</template>

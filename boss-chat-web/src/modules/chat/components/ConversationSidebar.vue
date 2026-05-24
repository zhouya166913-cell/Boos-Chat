<script setup lang="ts">
import type { ChatSceneAgent, Conversation } from "../../../api/chat";

const props = defineProps<{
  conversations: Conversation[];
  agents: ChatSceneAgent[];
  selectedConversationId?: number;
  loading: boolean;
  sceneMode: "single" | "team";
}>();

const emit = defineEmits<{
  new: [];
  select: [conversation: Conversation];
  rename: [conversation: Conversation];
  remove: [conversation: Conversation];
}>();

function getConversationAgent(conversation: Conversation) {
  return props.agents.find((agent) => agent.agentId === conversation.agentId);
}

function getConversationAgentName(conversation: Conversation) {
  if (props.sceneMode === "team") return "团队会话";
  return getConversationAgent(conversation)?.agentName || "未知 AI";
}

function getConversationMode(conversation: Conversation) {
  if (conversation.chatMode === "team") return "团队";
  const agent = getConversationAgent(conversation);
  if (agent?.toolsEnabled === 1) return "工具";
  if (agent?.imageGenerationEnabled === 1) return "图片";
  return "模型";
}

function modeClass(conversation: Conversation) {
  const mode = getConversationMode(conversation);
  if (mode === "团队") return "team";
  if (mode === "工具") return "agent";
  if (mode === "图片") return "image";
  return "model";
}

function handleConversationCommand(command: string | number | object, conversation: Conversation) {
  if (command === "rename") {
    emit("rename", conversation);
    return;
  }
  if (command === "remove") {
    emit("remove", conversation);
  }
}
</script>

<template>
  <aside class="conversation-sidebar">
    <el-button type="primary" plain @click="$emit('new')">新建对话</el-button>

    <div v-loading="loading" class="conversation-list">
      <button
        v-for="conversation in conversations"
        :key="conversation.id"
        type="button"
        class="conversation-item"
        :class="{ active: conversation.id === selectedConversationId }"
        @click="$emit('select', conversation)"
      >
        <span class="conversation-item-heading">
          <strong>{{ conversation.title }}</strong>
          <el-dropdown trigger="click" @command="handleConversationCommand($event, conversation)">
            <span class="conversation-action" title="更多操作" aria-label="更多操作" @click.stop>...</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">修改标题</el-dropdown-item>
                <el-dropdown-item command="remove" class="conversation-danger-action">删除会话</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </span>
        <em>{{ getConversationAgentName(conversation) }}</em>
        <small>
          <span>{{ conversation.updateTime?.replace("T", " ") }}</span>
          <span class="conversation-mode-badge" :class="modeClass(conversation)">
            {{ getConversationMode(conversation) }}
          </span>
        </small>
      </button>

      <el-empty
        v-if="!loading && conversations.length === 0"
        description="还没有历史对话"
        :image-size="72"
      />
    </div>
  </aside>
</template>

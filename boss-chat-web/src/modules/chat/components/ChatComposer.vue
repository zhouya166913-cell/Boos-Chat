<script setup lang="ts">
import { ref } from "vue";
import type { ChatAttachment } from "../../../api/chat";

const props = defineProps<{
  modelValue: string;
  sending: boolean;
  attachments: ChatAttachment[];
  uploading: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: string];
  send: [];
  stop: [];
  upload: [file: File];
  "remove-attachment": [attachmentId: string];
}>();

const fileInputRef = ref<HTMLInputElement>();

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== "Enter" || event.shiftKey || event.isComposing) {
    return;
  }
  event.preventDefault();
  emit("send");
}

function openFilePicker() {
  fileInputRef.value?.click();
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  files.forEach((file) => emit("upload", file));
  input.value = "";
}

function attachmentLabel(fileType: ChatAttachment["fileType"]) {
  const labels: Record<ChatAttachment["fileType"], string> = {
    image: "图片",
    excel: "Excel",
    video: "视频",
    document: "文档",
    file: "文件"
  };
  return labels[fileType] || "文件";
}
</script>

<template>
  <div class="chat-composer">
    <div v-if="props.attachments.length" class="chat-attachment-list">
      <span v-for="attachment in props.attachments" :key="attachment.attachmentId" class="chat-attachment-chip">
        <span>{{ attachmentLabel(attachment.fileType) }}</span>
        <strong>{{ attachment.fileName }}</strong>
        <button
          type="button"
          :disabled="props.sending"
          aria-label="移除附件"
          @click="$emit('remove-attachment', attachment.attachmentId)"
        >
          ×
        </button>
      </span>
    </div>

    <div class="chat-composer-row">
      <input
        ref="fileInputRef"
        class="chat-file-input"
        type="file"
        multiple
        accept="image/*,.xlsx,.pdf,.doc,.docx,.txt,.md,video/*"
        @change="handleFileChange"
      />
      <el-button :loading="props.uploading" :disabled="props.sending" @click="openFilePicker">上传附件</el-button>
      <el-input
        :model-value="props.modelValue"
        type="textarea"
        :rows="3"
        placeholder="输入你想测试的问题，Enter 发送，Shift + Enter 换行"
        @update:model-value="$emit('update:modelValue', $event)"
        @keydown="handleKeydown"
      />
      <el-button type="primary" :loading="props.sending" @click="$emit('send')">发送</el-button>
      <el-button v-if="props.sending" type="danger" plain @click="$emit('stop')">停止 AI</el-button>
    </div>
  </div>
</template>

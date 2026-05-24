<script setup lang="ts">
import { nextTick, ref } from "vue";
import type { DisplayChatMessage } from "../composables/useChatWorkspace";

const props = defineProps<{
  messages: DisplayChatMessage[];
  selectedAgentName?: string;
  loading: boolean;
}>();

const messageListRef = ref<HTMLElement>();

interface MessagePart {
  type: "text" | "image";
  content: string;
  alt?: string;
}

async function scrollToBottom() {
  await nextTick();
  const element = messageListRef.value;
  if (element) {
    element.scrollTop = element.scrollHeight;
  }
}

defineExpose({ scrollToBottom });

function messageSpeaker(message: DisplayChatMessage) {
  if (message.role === "user") return "你";
  return message.agentName || props.selectedAgentName || "AI";
}

function renderMessageParts(content: string): MessagePart[] {
  const parts: MessagePart[] = [];
  const pattern = /!\[([^\]]*)]\((https?:\/\/[^\s)]+)\)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: "text", content: content.slice(lastIndex, match.index) });
    }
    parts.push({
      type: "image",
      content: match[2],
      alt: match[1] || "AI 生成图片"
    });
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < content.length) {
    parts.push({ type: "text", content: content.slice(lastIndex) });
  }

  return parts.length ? parts : [{ type: "text", content }];
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function renderInlineMarkdown(value: string) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*\*([^*]+)\*/g, "<strong>$1</strong>")
    .replace(/\*([^*\n]{2,40})\*/g, "<strong>$1</strong>")
    .replace(/\*/g, "");
}

function renderMarkdownText(content: string, role: DisplayChatMessage["role"]) {
  if (role === "user") {
    return escapeHtml(content);
  }

  const lines = normalizeAssistantMarkdown(content).split("\n");
  const output: string[] = [];
  let listType: "ul" | "ol" | null = null;

  function closeList() {
    if (listType) {
      output.push(`</${listType}>`);
      listType = null;
    }
  }

  function openList(type: "ul" | "ol") {
    if (listType !== type) {
      closeList();
      output.push(`<${type}>`);
      listType = type;
    }
  }

  for (let index = 0; index < lines.length; index++) {
    const rawLine = lines[index];
    const line = rawLine.trim();
    if (!line) {
      closeList();
      continue;
    }

    if (isMarkdownTableRow(line)) {
      closeList();
      const tableRows: string[] = [];
      while (index < lines.length && isMarkdownTableRow(lines[index].trim())) {
        tableRows.push(lines[index].trim());
        index++;
      }
      index--;
      output.push(renderMarkdownTable(tableRows));
      continue;
    }

    const heading = /^(#{1,4})\s*(.+)$/.exec(line);
    if (heading) {
      closeList();
      const level = Math.min(heading[1].length + 2, 5);
      output.push(`<h${level}>${renderInlineMarkdown(heading[2])}</h${level}>`);
      continue;
    }

    const unorderedItem = /^[-*]\s+(.+)$/.exec(line);
    if (unorderedItem) {
      openList("ul");
      output.push(`<li>${renderInlineMarkdown(unorderedItem[1])}</li>`);
      continue;
    }

    const orderedItem = /^\d+[.)]\s+(.+)$/.exec(line);
    if (orderedItem) {
      openList("ol");
      output.push(`<li>${renderInlineMarkdown(orderedItem[1])}</li>`);
      continue;
    }

    closeList();
    output.push(`<p>${renderInlineMarkdown(line)}</p>`);
  }

  closeList();
  return output.join("");
}

function normalizeAssistantMarkdown(content: string) {
  return content
    .replace(/\r\n/g, "\n")
    .trim()
    .replace(/^\s*[-–—]{2,}\s*$/gm, "")
    .replace(/^\s*[-*]\s*$/gm, "")
    .replace(/\s*(#{2,4})\s*/g, "\n\n$1 ")
    .replace(/(#{2,4}\s+[^\n]*?)(\d+[.、]\s+)/g, "$1\n$2")
    .replace(/([。；;])\s*(\d+[.、]\s+)/g, "$1\n$2")
    .replace(/([^\n])([-*]\s+)/g, "$1\n$2")
    .replace(/([^\n])(\|[^|\n]+(?:\|[^|\n]+)+\|?)/g, "$1\n$2")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function isMarkdownTableRow(line: string) {
  if (!line.includes("|")) return false;
  const cells = splitTableRow(line);
  return cells.length >= 2;
}

function isMarkdownTableSeparator(line: string) {
  return splitTableRow(line).every((cell) => /^:?-{3,}:?$/.test(cell.trim()));
}

function splitTableRow(line: string) {
  return line
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|")
    .map((cell) => cell.trim())
    .filter((cell) => cell.length > 0);
}

function renderMarkdownTable(rows: string[]) {
  const cleanRows = rows.filter((row) => !isMarkdownTableSeparator(row)).map(splitTableRow);
  if (cleanRows.length === 0) return "";
  const [header, ...body] = cleanRows;
  const headerHtml = header.map((cell) => `<th>${renderInlineMarkdown(cell)}</th>`).join("");
  const bodyHtml = body
    .map((row) => `<tr>${row.map((cell) => `<td>${renderInlineMarkdown(cell)}</td>`).join("")}</tr>`)
    .join("");
  return `<div class="chat-markdown-table-wrap"><table><thead><tr>${headerHtml}</tr></thead><tbody>${bodyHtml}</tbody></table></div>`;
}
</script>

<template>
  <div v-loading="loading" class="chat-main">
    <div ref="messageListRef" class="chat-messages">
      <el-empty v-if="messages.length === 0" description="选择场景和 AI 后开始第一轮真实对话" />

      <article
        v-for="(message, index) in messages"
        :key="`${message.role}-${index}`"
        class="chat-message"
        :class="message.role"
      >
        <strong class="chat-message-speaker">{{ messageSpeaker(message) }}</strong>
        <div class="chat-message-content">
          <span v-if="message.streaming && !message.content" class="typing-indicator" aria-label="AI 正在输入">
            <i></i>
            <i></i>
            <i></i>
          </span>
          <template v-else>
            <template
              v-for="(part, partIndex) in renderMessageParts(message.content)"
              :key="`${part.type}-${partIndex}`"
            >
              <span
                v-if="part.type === 'text'"
                class="chat-message-text"
                v-html="renderMarkdownText(part.content, message.role)"
              ></span>
              <a
                v-else
                class="chat-generated-image-link"
                :href="part.content"
                target="_blank"
                rel="noopener noreferrer"
              >
                <img class="chat-generated-image" :src="part.content" :alt="part.alt" />
              </a>
            </template>
            <span v-if="message.streaming" class="typing-cursor"></span>
          </template>
        </div>
        <div v-if="message.toolSteps?.length" class="chat-tool-steps">
          <div
            v-for="(step, stepIndex) in message.toolSteps"
            :key="`${step.toolName}-${stepIndex}`"
            class="chat-tool-step"
            :class="step.status"
          >
            <strong>{{ step.status === "approval_required" ? "等待确认" : "工具" }}：{{ step.toolName }}</strong>
            <pre>{{ step.resultSummary }}</pre>
          </div>
        </div>
      </article>
    </div>

    <slot />
  </div>
</template>

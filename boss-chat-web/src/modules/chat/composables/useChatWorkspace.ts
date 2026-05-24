import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  approveChatToolExecution,
  deleteConversation,
  getConversationDetail,
  listChatScenes,
  listChatWorkflows,
  listConversations,
  rejectChatToolExecution,
  renameConversationTitle,
  streamChatMessage,
  uploadChatAttachment,
  type ChatAttachment,
  type ChatMessage,
  type ChatScene,
  type ChatSceneAgent,
  type Conversation
} from "../../../api/chat";
import type { AgentToolApproval, AgentToolStep, WorkflowItem } from "../../../api/workbench";

export interface DisplayChatMessage extends ChatMessage {
  streaming?: boolean;
  waiting?: boolean;
  toolSteps?: AgentToolStep[];
}

export function useChatWorkspace() {
  const scenes = ref<ChatScene[]>([]);
  const selectedSceneId = ref<number>();
  const selectedAgentId = ref<number>();
  const selectedWorkflowCode = ref("");
  const workflows = ref<WorkflowItem[]>([]);
  const conversationId = ref<number>();
  const conversations = ref<Conversation[]>([]);
  const messages = ref<DisplayChatMessage[]>([]);
  const input = ref("");
  const attachments = ref<ChatAttachment[]>([]);
  const uploadingAttachment = ref(false);
  const sending = ref(false);
  const loadingScenes = ref(false);
  const loadingConversations = ref(false);
  const loadingMessages = ref(false);
  const showContextPanel = ref(false);
  let abortController: AbortController | undefined;

  const selectedScene = computed(() => scenes.value.find((scene) => scene.id === selectedSceneId.value));
  const availableAgents = computed(() => selectedScene.value?.agents.filter((agent) => agent.enabled === 1) || []);
  const selectedAgent = computed(() => availableAgents.value.find((agent) => agent.agentId === selectedAgentId.value));
  const isTeamMode = computed(() => selectedScene.value?.chatMode === "team");
  const headerTitle = computed(() => selectedScene.value?.sceneName || "请选择场景");
  const headerDescription = computed(() => {
    const scene = selectedScene.value;
    if (!scene) return "选择一个业务场景后，再选择对应的 AI 助手";
    const modeText = scene.chatMode === "team" ? "团队场景：多个 AI 共用上下文" : "单聊场景：每个 AI 独立上下文";
    return `${scene.description || "暂无说明"} · ${modeText}`;
  });

  async function loadScenes() {
    loadingScenes.value = true;
    try {
      scenes.value = await listChatScenes();
      if (!selectedSceneId.value || !scenes.value.some((scene) => scene.id === selectedSceneId.value)) {
        selectedSceneId.value = scenes.value[0]?.id;
      }
      ensureSelectedAgent();
      await Promise.all([loadWorkflows(), loadConversations()]);
      if (scenes.value.length === 0) {
        ElMessage.warning("还没有可用场景，请先在场景管理中创建场景");
      }
    } catch (error: any) {
      if (error?.response?.status !== 401) {
        ElMessage.error(error?.response?.data?.message || "加载场景失败");
      }
    } finally {
      loadingScenes.value = false;
    }
  }

  function ensureSelectedAgent() {
    if (!availableAgents.value.length) {
      selectedAgentId.value = undefined;
      return;
    }
    if (!selectedAgentId.value || !availableAgents.value.some((agent) => agent.agentId === selectedAgentId.value)) {
      selectedAgentId.value = availableAgents.value[0].agentId;
    }
  }

  async function loadWorkflows() {
    if (!selectedAgentId.value) {
      workflows.value = [];
      selectedWorkflowCode.value = "";
      return;
    }
    try {
      workflows.value = await listChatWorkflows(selectedAgentId.value);
      if (!workflows.value.some((workflow) => workflow.workflowCode === selectedWorkflowCode.value)) {
        selectedWorkflowCode.value = workflows.value[0]?.workflowCode || "";
      }
    } catch {
      workflows.value = [];
      selectedWorkflowCode.value = "";
    }
  }

  async function loadConversations() {
    if (!selectedScene.value) return;
    loadingConversations.value = true;
    try {
      conversations.value = await listConversations({
        sceneId: selectedScene.value.id,
        chatMode: selectedScene.value.chatMode,
        agentId: selectedScene.value.chatMode === "single" ? selectedAgentId.value : undefined
      });
    } catch (error: any) {
      if (error?.response?.status !== 401) {
        ElMessage.error(error?.response?.data?.message || "加载会话失败");
      }
    } finally {
      loadingConversations.value = false;
    }
  }

  async function openConversation(target: Conversation) {
    conversationId.value = target.id;
    loadingMessages.value = true;
    try {
      const detail = await getConversationDetail(target.id);
      messages.value = detail.messages;
      if (!isTeamMode.value && detail.conversation.agentId) {
        selectedAgentId.value = detail.conversation.agentId;
      }
    } catch (error: any) {
      if (error?.response?.status !== 401) {
        ElMessage.error(error?.response?.data?.message || "加载对话失败");
      }
    } finally {
      loadingMessages.value = false;
    }
  }

  function startNewConversation() {
    conversationId.value = undefined;
    messages.value = [];
    input.value = "";
    attachments.value = [];
  }

  async function renameConversation(conversation: Conversation) {
    try {
      const { value } = await ElMessageBox.prompt("请输入新的会话标题", "修改标题", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        inputValue: conversation.title,
        inputPlaceholder: "请输入会话标题",
        inputValidator(value) {
          const title = value.trim();
          if (!title) return "标题不能为空";
          if (title.length > 100) return "标题不能超过 100 个字符";
          return true;
        }
      });
      const title = value.trim();
      if (title === conversation.title) return;

      await renameConversationTitle(conversation.id, title);
      conversation.title = title;
      conversation.updateTime = new Date().toISOString().slice(0, 19);
      conversations.value = [...conversations.value].sort(
        (left, right) => new Date(right.updateTime).getTime() - new Date(left.updateTime).getTime()
      );
      ElMessage.success("标题已更新");
    } catch (error: any) {
      if (error === "cancel" || error === "close") return;
      if (error?.response?.status !== 401) {
        ElMessage.error(error?.response?.data?.message || "修改标题失败");
      }
    }
  }

  async function removeConversation(conversation: Conversation) {
    if (sending.value) {
      ElMessage.warning("AI 正在回复时暂时不能删除会话");
      return;
    }

    try {
      await ElMessageBox.confirm(
        `确定删除「${conversation.title}」吗？删除后页面不再展示，但数据库会保留记录。`,
        "删除会话",
        {
          type: "warning",
          confirmButtonText: "删除",
          cancelButtonText: "取消",
          confirmButtonClass: "el-button--danger"
        }
      );

      await deleteConversation(conversation.id);
      conversations.value = conversations.value.filter((item) => item.id !== conversation.id);
      if (conversationId.value === conversation.id) {
        startNewConversation();
      }
      ElMessage.success("会话已删除");
    } catch (error: any) {
      if (error === "cancel" || error === "close") return;
      if (error?.response?.status !== 401) {
        ElMessage.error(error?.response?.data?.message || "删除会话失败");
      }
    }
  }

  async function handleSend() {
    const content = input.value.trim();
    const sendingAttachments = [...attachments.value];
    if (!selectedScene.value) return ElMessage.warning("请选择场景");
    if (!selectedAgentId.value || !selectedAgent.value) return ElMessage.warning("请选择 AI");
    if ((!content && sendingAttachments.length === 0) || sending.value || uploadingAttachment.value) return;

    const assistantMessage = reactive<DisplayChatMessage>({
      role: "assistant",
      content: "",
      agentId: selectedAgent.value.agentId,
      agentName: selectedAgent.value.agentName,
      streaming: true,
      waiting: true,
      toolSteps: []
    });
    let pendingText = "";
    let renderedText = "";
    let streamFinished = false;
    let typingTimer: ReturnType<typeof setTimeout> | undefined;
    let resolveTypingFinished: (() => void) | undefined;
    const typingFinished = new Promise<void>((resolve) => {
      resolveTypingFinished = resolve;
    });

    function scheduleTyping() {
      if (typingTimer) return;

      const renderNext = () => {
        typingTimer = undefined;

        if (!pendingText) {
          if (streamFinished) {
            assistantMessage.streaming = false;
            assistantMessage.waiting = false;
            resolveTypingFinished?.();
          }
          return;
        }

        assistantMessage.waiting = false;
        const batchSize = getTypingBatchSize(pendingText.length);
        const nextText = pendingText.slice(0, batchSize);
        pendingText = pendingText.slice(batchSize);
        renderedText += nextText;
        assistantMessage.content = renderedText;

        typingTimer = setTimeout(renderNext, getTypingDelay(nextText, pendingText.length));
      };

      typingTimer = setTimeout(renderNext, assistantMessage.waiting ? 220 : 0);
    }

    messages.value.push({ role: "user", content: buildDisplayUserContent(content, sendingAttachments) }, assistantMessage);
    input.value = "";
    attachments.value = [];
    sending.value = true;
    abortController = new AbortController();

    try {
      await streamChatMessage(
        {
          sceneId: selectedScene.value.id,
          agentId: selectedAgentId.value,
          conversationId: conversationId.value,
          content,
          attachments: sendingAttachments,
          workflowCode: selectedWorkflowCode.value || undefined
        },
        {
          onMeta(meta) {
            conversationId.value = meta.conversationId;
          },
          onDelta(delta) {
            pendingText += delta;
            scheduleTyping();
          },
          onStep(step) {
            assistantMessage.waiting = false;
            assistantMessage.toolSteps?.push(step);
          },
          onApprovalRequired(approval) {
            handleApprovalRequired(approval);
          },
          onDone(response) {
            streamFinished = true;
            assistantMessage.agentId = response.assistantMessage.agentId ?? assistantMessage.agentId;
            assistantMessage.agentName = response.assistantMessage.agentName || assistantMessage.agentName;
            const finalContent = response.assistantMessage.content || "";
            if (finalContent.startsWith(renderedText)) {
              pendingText = finalContent.slice(renderedText.length);
            } else if (!pendingText) {
              pendingText = finalContent;
              renderedText = "";
              assistantMessage.content = "";
            }
            scheduleTyping();
          }
        },
        abortController.signal
      );
      await typingFinished;
      await loadConversations();
    } catch (error: any) {
      if (typingTimer) clearTimeout(typingTimer);
      assistantMessage.streaming = false;
      assistantMessage.waiting = false;
      if (error?.name === "AbortError") {
        assistantMessage.content = assistantMessage.content || "已停止本次回复";
        ElMessage.info("已停止 AI");
        return;
      }
      const message = error?.message || "请求失败";
      if (!assistantMessage.content) {
        assistantMessage.content = `回复失败：${message}`;
      }
      ElMessage.error(message);
    } finally {
      sending.value = false;
      abortController = undefined;
    }
  }

  async function handleApprovalRequired(approval: AgentToolApproval) {
    try {
      await ElMessageBox.confirm(
        `AI 准备调用工具：${approval.toolName}\n\n参数：${approval.argumentsJson}\n\n是否允许执行？`,
        "工具调用确认",
        {
          confirmButtonText: "允许执行",
          cancelButtonText: "拒绝",
          type: "warning",
          dangerouslyUseHTMLString: false
        }
      );
      await approveChatToolExecution(approval.approvalId);
      ElMessage.success("已允许执行");
    } catch {
      try {
        await rejectChatToolExecution(approval.approvalId);
      } catch {
        // 工具审批可能已经过期，这里不阻断用户继续对话。
      }
      ElMessage.info("已拒绝执行");
    }
  }

  function stopCurrentResponse() {
    abortController?.abort();
    sending.value = false;
  }

  async function handleAttachmentUpload(file: File) {
    if (sending.value) {
      ElMessage.warning("AI 正在回复，稍后再上传附件");
      return;
    }
    uploadingAttachment.value = true;
    try {
      const attachment = await uploadChatAttachment(file);
      attachments.value = [...attachments.value, attachment];
      ElMessage.success("附件已上传");
    } catch (error: any) {
      ElMessage.error(error?.response?.data?.message || error?.message || "附件上传失败");
    } finally {
      uploadingAttachment.value = false;
    }
  }

  function removeAttachment(attachmentId: string) {
    attachments.value = attachments.value.filter((attachment) => attachment.attachmentId !== attachmentId);
  }

  function agentAbilityLabel(agent?: ChatSceneAgent) {
    if (!agent) return "AI";
    if (agent.toolsEnabled === 1) return "工具";
    if (agent.imageGenerationEnabled === 1) return "图片";
    return "模型";
  }

  watch(selectedSceneId, async () => {
    ensureSelectedAgent();
    startNewConversation();
    await loadWorkflows();
    await loadConversations();
  });

  watch(selectedAgentId, async (_newValue, oldValue) => {
    if (!selectedScene.value || _newValue === oldValue) return;
    await loadWorkflows();
    if (!isTeamMode.value) {
      startNewConversation();
      await loadConversations();
    }
  });

  onMounted(loadScenes);

  return {
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
  };
}

function buildDisplayUserContent(content: string, attachments: ChatAttachment[]) {
  if (attachments.length === 0) return content;
  const attachmentLines = attachments.map((attachment) => `- ${attachment.fileName}（${attachmentLabel(attachment.fileType)}）`);
  return [content, "上传附件：", ...attachmentLines].filter(Boolean).join("\n");
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

function getTypingBatchSize(bufferLength: number) {
  if (bufferLength > 180) return 2;
  return 1;
}

function getTypingDelay(renderedChunk: string, bufferLength: number) {
  const lastCharacter = renderedChunk.at(-1) || "";

  if (/[。！？!]/.test(lastCharacter)) return 165;
  if (/[，、；：,;:]/.test(lastCharacter)) return 105;
  if (bufferLength > 180) return 24;
  if (bufferLength > 80) return 34;
  return 42 + Math.floor(Math.random() * 18);
}

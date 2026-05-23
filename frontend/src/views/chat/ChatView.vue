<template>
  <div class="chat-page">
    <aside class="conversation-panel">
      <div class="panel-head">
        <div>
          <h2>站内消息</h2>
          <p>买家和卖家可以在这里实时沟通</p>
        </div>
        <el-button text @click="refreshConversations">刷新</el-button>
      </div>

      <div v-if="loadingConversations" class="panel-loading">
        <el-skeleton :rows="6" animated />
      </div>

      <el-empty v-else-if="!conversations.length" description="暂无会话" />

      <div v-else class="conversation-list">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="conversation-item"
          :class="{ active: activeConversationId === item.id }"
          @click="selectConversation(item)"
        >
          <div class="conversation-top">
            <strong>{{ item.other?.nickname || "未知用户" }}</strong>
            <span>{{ formatTime(item.lastMessageTime) }}</span>
          </div>
          <div class="conversation-source">{{ formatSource(item) }}</div>
          <div class="conversation-bottom">
            <span class="conversation-preview">{{ item.lastMessageContent || "点击开始聊天" }}</span>
            <el-badge v-if="item.unreadCount" :value="item.unreadCount" />
          </div>
        </button>
      </div>
    </aside>

    <section class="chat-panel">
      <div v-if="!activeConversation" class="chat-placeholder">
        <el-empty description="请选择一个会话" />
      </div>

      <template v-else>
        <header class="chat-head">
          <div>
            <h3>{{ activeConversation.other?.nickname || "未知用户" }}</h3>
            <p>{{ formatSource(activeConversation) }}</p>
          </div>
        </header>

        <div v-if="bargainRequests.length" class="trade-panel">
          <article v-for="request in bargainRequests" :key="request.id" class="trade-card">
            <div class="trade-copy">
              <span>{{ request.statusName || request.status }}</span>
              <strong>{{ request.buyerName || "买家" }} 出价 ¥{{ Number(request.proposedPrice || 0).toFixed(2) }}</strong>
              <small>{{ request.productName || activeConversation.sourceTitle }}</small>
            </div>
            <div class="trade-actions">
              <template v-if="request.status === 'PENDING' && isSellerForBargain(request)">
                <el-button
                  size="small"
                  type="primary"
                  :loading="actionLoadingKey === `confirm-${request.id}`"
                  @click="handleConfirmBargain(request)"
                >
                  同意并生成订单
                </el-button>
                <el-button
                  size="small"
                  :loading="actionLoadingKey === `reject-${request.id}`"
                  @click="handleRejectBargain(request)"
                >
                  拒绝
                </el-button>
              </template>
              <span v-else-if="request.status === 'PENDING'" class="trade-note">等待卖家处理</span>
              <el-button v-else-if="request.orderId" size="small" type="success" plain @click="router.push('/secondhand/orders')">
                查看订单
              </el-button>
              <span v-else class="trade-note">{{ request.statusName || "已结束" }}</span>
            </div>
          </article>
        </div>

        <div v-loading="loadingMessages" class="message-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="{ self: Number(message.senderUserId) === Number(currentUserId) }"
          >
            <div class="message-bubble">
              <div class="message-author">{{ message.sender?.nickname || "用户" }}</div>
              <div class="message-content">{{ message.content }}</div>
              <div class="message-time">{{ formatTime(message.createTime, true) }}</div>
            </div>
          </div>
        </div>

        <footer class="chat-composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            resize="none"
            placeholder="输入消息"
            @keydown="handleComposerKeydown"
          />
          <div class="composer-actions">
            <span class="composer-tip">{{ sendStatus }}</span>
            <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
          </div>
        </footer>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  createChatConversationApi,
  listChatConversationsApi,
  listChatMessagesApi,
  sendChatMessageApi,
} from "@/api/chat";
import {
  confirmBargainApi,
  listBargainRequestsApi,
  rejectBargainApi,
} from "@/api/secondhand";
import {
  onRealtimeEvent,
  startRealtimeClient,
} from "@/realtime/realtimeClient";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const conversations = ref([]);
const messages = ref([]);
const bargainRequests = ref([]);
const activeConversationId = ref(null);
const draft = ref("");
const sendStatus = ref("消息会自动同步给对方");
const actionLoadingKey = ref("");

const currentUserId = computed(() => userStore.userInfo?.id);
const chatRoutePath = computed(() =>
  route.path.startsWith("/merchant") ? "/merchant/messages" : "/messages",
);
const activeConversation = computed(() =>
  conversations.value.find((item) => Number(item.id) === Number(activeConversationId.value)) || null,
);

let unsubscribeRealtime = null;
let chatPollTimer = null;
let syncingChat = false;
const MOCK_STORE_KEY = "segroup8_mock_store_v1";

onMounted(async () => {
  startRealtimeClient();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
  window.addEventListener("storage", handleStorageEvent);
  await bootstrap();
  chatPollTimer = window.setInterval(syncActiveChat, 4000);
});

onUnmounted(() => {
  if (typeof unsubscribeRealtime === "function") {
    unsubscribeRealtime();
  }
  if (chatPollTimer) {
    window.clearInterval(chatPollTimer);
  }
  window.removeEventListener("storage", handleStorageEvent);
});

watch(
  () => route.query.conversationId,
  async (conversationId) => {
    if (!conversationId) {
      return;
    }
    const found = conversations.value.find((item) => Number(item.id) === Number(conversationId));
    if (found && Number(activeConversationId.value) !== Number(found.id)) {
      await selectConversation(found);
    }
  },
);

async function bootstrap() {
  await refreshConversations();
  const conversationId = route.query.conversationId;
  if (conversationId) {
    const found = conversations.value.find((item) => Number(item.id) === Number(conversationId));
    if (found) {
      await selectConversation(found);
      return;
    }
  }
  const participantId = route.query.participantId;
  if (participantId) {
    await ensureConversation();
    return;
  }
  if (conversations.value.length) {
    await selectConversation(conversations.value[0]);
  }
}

async function refreshConversations(options = {}) {
  const silent = options.silent === true;
  if (!silent) {
    loadingConversations.value = true;
  }
  try {
    const activeId = activeConversationId.value;
    const result = await listChatConversationsApi();
    const next = result.data || [];
    if (collectionSignature(conversations.value) !== collectionSignature(next)) {
      conversations.value = next;
    }
    if (activeId && !next.some((item) => Number(item.id) === Number(activeId))) {
      activeConversationId.value = null;
      messages.value = [];
      bargainRequests.value = [];
    }
  } finally {
    if (!silent) {
      loadingConversations.value = false;
    }
  }
}

async function ensureConversation() {
  const participantId = Number(route.query.participantId);
  if (!participantId) {
    return;
  }
  const initialMessage = String(route.query.initialMessage || "");
  const result = await createChatConversationApi({
    targetUserId: participantId,
    sourceType: route.query.sourceType || "DIRECT",
    sourceId: route.query.sourceId ? Number(route.query.sourceId) : null,
  });
  const conversation = result.data;
  const existingIndex = conversations.value.findIndex((item) => Number(item.id) === Number(conversation.id));
  if (existingIndex >= 0) {
    conversations.value.splice(existingIndex, 1, conversation);
  } else {
    conversations.value.unshift(conversation);
  }
  await router.replace({ path: chatRoutePath.value, query: { conversationId: conversation.id } });
  await selectConversation(conversation);
  if (initialMessage && !draft.value.trim()) {
    draft.value = initialMessage;
  }
}

async function selectConversation(conversation) {
  if (!conversation?.id) {
    return;
  }
  activeConversationId.value = conversation.id;
  await loadMessages(conversation.id);
  await loadBargainRequests();
  const currentQueryId = String(route.query.conversationId || "");
  if (currentQueryId !== String(conversation.id)) {
    router.replace({ path: chatRoutePath.value, query: { conversationId: conversation.id } });
  }
}

async function loadMessages(conversationId, options = {}) {
  const silent = options.silent === true;
  if (!silent) {
    loadingMessages.value = true;
  }
  try {
    const before = messages.value;
    const result = await listChatMessagesApi(conversationId);
    const next = result.data || [];
    const changed = collectionSignature(before) !== collectionSignature(next);
    if (changed) {
      messages.value = next;
    }
    clearConversationUnread(conversationId);
    if (!silent || changed) {
      await scrollToBottom();
    }
  } finally {
    if (!silent) {
      loadingMessages.value = false;
    }
  }
}

async function loadBargainRequests(options = {}) {
  const silent = options.silent === true;
  const conversation = activeConversation.value;
  if (!conversation || String(conversation.sourceType || "").toUpperCase() !== "SECONDHAND" || !conversation.sourceId) {
    if (bargainRequests.value.length) {
      bargainRequests.value = [];
    }
    return;
  }
  try {
    const result = await listBargainRequestsApi({
      productId: conversation.sourceId,
      counterpartUserId: conversation.other?.id,
    });
    const next = result.data?.records || result.data || [];
    if (collectionSignature(bargainRequests.value) !== collectionSignature(next)) {
      bargainRequests.value = next;
    }
  } catch {
    if (!silent && bargainRequests.value.length) {
      bargainRequests.value = [];
    }
  }
}

async function sendMessage() {
  const content = draft.value.trim();
  if (!content || !activeConversation.value) {
    return;
  }
  sending.value = true;
  try {
    const conversationId = activeConversation.value.id;
    const result = await sendChatMessageApi(conversationId, { content });
    appendIncomingMessage(result.data);
    draft.value = "";
    sendStatus.value = "已发送，正在自动同步";
    await refreshConversations({ silent: true });
    await loadMessages(conversationId, { silent: true });
    await scrollToBottom();
  } finally {
    sending.value = false;
  }
}

async function syncActiveChat() {
  if (syncingChat || sending.value) {
    return;
  }
  syncingChat = true;
  try {
    const activeId = activeConversationId.value;
    await refreshConversations({ silent: true });
    if (activeId && conversations.value.some((item) => Number(item.id) === Number(activeId))) {
      await loadMessages(activeId, { silent: true });
      await loadBargainRequests({ silent: true });
      return;
    }
    if (!activeId && conversations.value.length) {
      await selectConversation(conversations.value[0]);
    }
  } finally {
    syncingChat = false;
  }
}

function isSellerForBargain(request) {
  return Number(request?.sellerUserId) === Number(currentUserId.value);
}

async function handleConfirmBargain(request) {
  actionLoadingKey.value = `confirm-${request.id}`;
  try {
    const result = await confirmBargainApi({
      negotiationId: request.id,
      confirmedPrice: request.proposedPrice,
      createOrder: true,
    });
    ElMessage.success("已同意议价，并生成二手订单");
    await loadBargainRequests();
    await loadMessages(activeConversation.value.id, { silent: true });
    updateBargainInList(result.data);
  } finally {
    actionLoadingKey.value = "";
  }
}

async function handleRejectBargain(request) {
  actionLoadingKey.value = `reject-${request.id}`;
  try {
    await rejectBargainApi(request.id);
    ElMessage.success("已拒绝议价");
    await loadBargainRequests();
    await loadMessages(activeConversation.value.id, { silent: true });
  } finally {
    actionLoadingKey.value = "";
  }
}

function updateBargainInList(next) {
  if (!next?.id) {
    return;
  }
  const index = bargainRequests.value.findIndex((item) => Number(item.id) === Number(next.id));
  if (index >= 0) {
    bargainRequests.value.splice(index, 1, next);
  }
}

function collectionSignature(records) {
  return (records || []).map((item) => [
    item.id,
    item.proposedPrice,
    item.confirmedPrice,
    item.status,
    item.statusName,
    item.orderId,
    item.lastMessageContent,
    item.lastMessageTime,
    item.unreadCount,
    item.content,
    item.createTime,
  ].join("|")).join(";");
}

function handleComposerKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
    event.preventDefault();
    sendMessage();
  }
}

function handleRealtimeEvent(event) {
  const detail = event?.detail;
  if (!detail) {
    return;
  }
  if (detail.eventType === "CONNECTED") {
    sendStatus.value = "消息会自动同步给对方";
    return;
  }
  if (detail.eventType === "PONG") {
    sendStatus.value = "消息会自动同步给对方";
    return;
  }
  if (detail.eventType === "CHAT_MESSAGE" && detail.payload) {
    sendStatus.value = "收到新消息";
    appendIncomingMessage(detail.payload);
    return;
  }
  if (detail.eventType === "CHAT_ERROR") {
    ElMessage.error(detail.payload?.message || "消息发送失败");
  }
}

function appendIncomingMessage(message) {
  if (!message?.conversationId) {
    return;
  }
  updateConversationFromMessage(message);
  if (Number(message.conversationId) === Number(activeConversationId.value)) {
    const exists = messages.value.some((item) => Number(item.id) === Number(message.id));
    if (!exists) {
      messages.value.push(message);
      scrollToBottom();
    }
    clearConversationUnread(message.conversationId);
    return;
  }
  const target = conversations.value.find((item) => Number(item.id) === Number(message.conversationId));
  if (target) {
    target.unreadCount = Number(target.unreadCount || 0) + 1;
  }
}

function updateConversationFromMessage(message) {
  const conversation = conversations.value.find((item) => Number(item.id) === Number(message.conversationId));
  if (!conversation) {
    refreshConversations();
    return;
  }
  conversation.lastMessageContent = message.content;
  conversation.lastMessageTime = message.createTime;
  const index = conversations.value.findIndex((item) => Number(item.id) === Number(message.conversationId));
  if (index > 0) {
    const [current] = conversations.value.splice(index, 1);
    conversations.value.unshift(current);
  }
}

function clearConversationUnread(conversationId) {
  const conversation = conversations.value.find((item) => Number(item.id) === Number(conversationId));
  if (conversation) {
    conversation.unreadCount = 0;
  }
}

function formatSource(conversation) {
  const type = String(conversation?.sourceType || "DIRECT").toUpperCase();
  if (type === "PRODUCT") {
    return `商品咨询：${conversation?.sourceTitle || "商品"}`;
  }
  if (type === "SECONDHAND") {
    return `二手商品咨询：${conversation?.sourceTitle || "商品"}`;
  }
  if (type === "BARGAIN") {
    return `议价沟通：${conversation?.sourceTitle || "商品"}`;
  }
  return conversation?.sourceTitle || "站内私聊";
}

function handleStorageEvent(event) {
  if (event.key === MOCK_STORE_KEY) {
    syncActiveChat();
  }
}

function formatTime(value, withTime = false) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return withTime
    ? date.toLocaleString("zh-CN", { hour12: false })
    : date.toLocaleString("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });
}

async function scrollToBottom() {
  await nextTick();
  const container = document.querySelector(".message-list");
  if (container) {
    container.scrollTop = container.scrollHeight;
  }
}
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  min-height: calc(100vh - 120px);
}

.conversation-panel,
.chat-panel {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  overflow: hidden;
}

.panel-head,
.chat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line-soft);
  background: linear-gradient(135deg, #edf7f3 0%, #f1f0fb 100%);
}

.panel-head h2,
.chat-head h3 {
  margin: 0;
  font-size: 20px;
}

.panel-head p,
.chat-head p {
  margin: 6px 0 0;
  color: #718096;
  font-size: 13px;
}

.panel-loading {
  padding: 18px;
}

.conversation-list {
  display: flex;
  flex-direction: column;
}

.conversation-item {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--line-soft);
  background: transparent;
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  background: #edf7f3;
}

.conversation-top,
.conversation-bottom {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.conversation-top span,
.conversation-source {
  color: #718096;
  font-size: 12px;
}

.conversation-source {
  margin: 6px 0 8px;
}

.conversation-preview {
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  display: flex;
  flex-direction: column;
}

.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.trade-panel {
  display: grid;
  gap: 10px;
  border-bottom: 1px solid #eef2f7;
  background: #fbfdff;
  padding: 12px 18px;
}

.trade-card {
  border: 1px solid rgba(137, 199, 255, 0.32);
  border-radius: 12px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 100%);
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.trade-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.trade-copy span {
  width: fit-content;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--brand-primary);
  padding: 3px 9px;
  font-size: 12px;
  font-weight: 900;
}

.trade-copy strong,
.trade-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trade-copy small,
.trade-note {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.trade-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 0 0 auto;
}

.message-list {
  flex: 1;
  min-height: 420px;
  max-height: calc(100vh - 320px);
  overflow-y: auto;
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(82, 170, 94, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #fefefe 100%);
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row.self {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(72%, 560px);
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 16px;
  padding: 12px 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.message-row.self .message-bubble {
  background: linear-gradient(135deg, #dff7e3 0%, #eefaf2 100%);
}

.message-author {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.message-content {
  color: #0f172a;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.chat-composer {
  border-top: 1px solid #eef2f7;
  padding: 16px 18px 18px;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.composer-tip {
  color: #718096;
  font-size: 12px;
}

@media (max-width: 960px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .trade-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .message-bubble {
    max-width: 88%;
  }
}
</style>

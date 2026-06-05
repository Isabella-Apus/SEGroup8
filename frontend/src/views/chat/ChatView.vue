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
            <span class="conversation-preview">{{ formatMessagePreview(item.lastMessageContent) }}</span>
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
          <div class="chat-head-actions">
            <el-button type="warning" plain :loading="blockLoading" :disabled="isCurrentUserBlocked" @click="handleBlockUser">
              {{ isCurrentUserBlocked ? "已拉黑" : "拉黑" }}
            </el-button>
            <el-button type="danger" plain @click="goToReport">举报</el-button>
          </div>
        </header>

        <div v-loading="loadingMessages" class="message-list">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="{ self: Number(message.senderUserId) === Number(currentUserId) }"
          >
            <div class="message-bubble">
              <div class="message-author">{{ message.sender?.nickname || "用户" }}</div>
              <div class="message-content">
                <template v-if="getMessagePayload(message).type === 'bargain'">
                  <article class="bargain-message-card">
                    <div class="bargain-card-head">
                      <span>{{ getMessagePayload(message).bargain.statusLabel }}</span>
                      <strong>{{ getMessagePayload(message).bargain.title }}</strong>
                    </div>
                    <div class="bargain-card-body">
                      <div class="bargain-info-row product-row">
                        <small>商品</small>
                        <b>{{ getMessagePayload(message).bargain.productName || activeConversation.sourceTitle || "二手商品" }}</b>
                      </div>
                      <div v-if="getMessagePayload(message).bargain.proposedPrice" class="bargain-info-row price-row">
                        <small>买家出价</small>
                        <b>¥{{ Number(getMessagePayload(message).bargain.proposedPrice || 0).toFixed(2) }}</b>
                      </div>
                      <div v-if="getMessagePayload(message).bargain.confirmedPrice" class="bargain-info-row price-row">
                        <small>确认价格</small>
                        <b>¥{{ Number(getMessagePayload(message).bargain.confirmedPrice || 0).toFixed(2) }}</b>
                      </div>
                      <div v-if="getMessagePayload(message).bargain.effectiveUntil" class="bargain-info-row">
                        <small>有效期至</small>
                        <b>{{ formatTime(getMessagePayload(message).bargain.effectiveUntil, true) }}</b>
                      </div>
                    </div>
                    <div class="bargain-card-actions">
                      <template v-if="canHandleBargain(getMessagePayload(message).bargain) && isSellerForBargain(getMessagePayload(message).bargain)">
                        <el-button
                          size="small"
                          type="primary"
                          :loading="actionLoadingKey === `confirm-${getMessagePayload(message).bargain.id}`"
                          :disabled="isBargainActionPending(getMessagePayload(message).bargain)"
                          @click="handleConfirmBargain(getMessagePayload(message).bargain)"
                        >
                          同意并生成订单
                        </el-button>
                        <el-button
                          size="small"
                          :loading="actionLoadingKey === `reject-${getMessagePayload(message).bargain.id}`"
                          :disabled="isBargainActionPending(getMessagePayload(message).bargain)"
                          @click="handleRejectBargain(getMessagePayload(message).bargain)"
                        >
                          拒绝
                        </el-button>
                      </template>
                      <el-button v-else-if="getMessagePayload(message).bargain.orderId && getMessagePayload(message).bargain.bargainKind !== 'APPLY'" size="small" type="success" plain @click="goBargainOrder(getMessagePayload(message).bargain)">
                        {{ isBuyerForBargain(getMessagePayload(message).bargain) ? "去支付订单" : "查看订单" }}
                      </el-button>
                      <span v-else class="trade-note">{{ getMessagePayload(message).bargain.actionHint }}</span>
                    </div>
                  </article>
                </template>
                <template v-else-if="getMessagePayload(message).type === 'image'">
                  <el-image
                    class="message-media message-image"
                    :src="getMessagePayload(message).url"
                    :preview-src-list="[getMessagePayload(message).url]"
                    :initial-index="0"
                    fit="cover"
                    preview-teleported
                  />
                  <div v-if="getMessagePayload(message).caption" class="message-caption">
                    {{ getMessagePayload(message).caption }}
                  </div>
                </template>
                <template v-else-if="getMessagePayload(message).type === 'video'">
                  <video
                    class="message-media message-video"
                    :src="getMessagePayload(message).url"
                    controls
                    preload="metadata"
                  />
                  <div v-if="getMessagePayload(message).caption" class="message-caption">
                    {{ getMessagePayload(message).caption }}
                  </div>
                </template>
                <template v-else>
                  {{ getMessagePayload(message).text }}
                </template>
              </div>
              <div class="message-time">{{ formatTime(message.createTime, true) }}</div>
            </div>
          </div>
        </div>

        <footer class="chat-composer">
          <input
            ref="mediaInputRef"
            class="media-input"
            type="file"
            accept="image/*,video/*"
            @change="handleMediaSelected"
          />
          <el-input
            v-model="draft"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            resize="none"
            :disabled="isCurrentUserBlocked"
            :placeholder="isCurrentUserBlocked ? '已拉黑该用户，无法继续发送消息' : '输入消息'"
            @keydown="handleComposerKeydown"
          />
          <div class="composer-actions">
            <span class="composer-tip">{{ sendStatus }}</span>
            <div class="composer-buttons">
              <el-button :disabled="sending || isCurrentUserBlocked" @click="openMediaPicker">上传图片/视频</el-button>
              <el-button type="primary" :loading="sending" :disabled="isCurrentUserBlocked" @click="sendMessage">发送</el-button>
            </div>
          </div>
        </footer>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
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
  blockUserApi,
  isBlockingApi,
} from "@/api/credit";
import { uploadMediaApi } from "@/api/upload";
import {
  onRealtimeEvent,
  startRealtimeClient,
} from "@/realtime/realtimeClient";
import { useUserStore } from "@/stores/user";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const blockLoading = ref(false);
const conversations = ref([]);
const messages = ref([]);
const bargainRequests = ref([]);
const activeConversationId = ref(null);
const draft = ref("");
const mediaInputRef = ref(null);
const blockedUserIds = ref(new Set());
const sendStatus = ref("消息会自动同步给对方");
const actionLoadingKey = ref("");

const currentUserId = computed(() => userStore.userInfo?.id);
const chatRoutePath = computed(() =>
  route.path.startsWith("/merchant") ? "/merchant/messages" : "/messages",
);
const activeConversation = computed(() =>
  conversations.value.find((item) => Number(item.id) === Number(activeConversationId.value)) || null,
);
const activeOtherUserId = computed(() => {
  const other = activeConversation.value?.other || {};
  return other.userId || other.id || null;
});
const isCurrentUserBlocked = computed(() =>
  activeOtherUserId.value ? blockedUserIds.value.has(Number(activeOtherUserId.value)) : false,
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
  await loadBlockStatus();
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
  if (!content || !activeConversation.value || isCurrentUserBlocked.value) {
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

function openMediaPicker() {
  if (!activeConversation.value || sending.value || isCurrentUserBlocked.value) {
    return;
  }
  mediaInputRef.value?.click();
}

async function handleMediaSelected(event) {
  const file = event.target?.files?.[0];
  if (event.target) {
    event.target.value = "";
  }
  if (!file || !activeConversation.value || isCurrentUserBlocked.value) {
    return;
  }
  const type = getMediaType(file);
  if (!type) {
    ElMessage.warning("仅支持上传图片或视频");
    return;
  }
  sending.value = true;
  try {
    const conversationId = activeConversation.value.id;
    const uploadResult = await uploadMediaApi(file);
    const uploaded = uploadResult.data || {};
    const content = createMediaMessageContent({
      type,
      url: uploaded.url,
      filename: uploaded.filename || file.name,
      contentType: uploaded.contentType || file.type,
    });
    const result = await sendChatMessageApi(conversationId, { content });
    appendIncomingMessage(result.data);
    sendStatus.value = type === "image" ? "图片已发送" : "视频已发送";
    await refreshConversations({ silent: true });
    await loadMessages(conversationId, { silent: true });
    await scrollToBottom();
  } finally {
    sending.value = false;
  }
}

function goToReport() {
  const other = activeConversation.value?.other || {};
  const reportedId = other.userId || other.id;
  if (!reportedId) {
    ElMessage.warning("无法识别当前对话用户");
    return;
  }
  router.push({
    path: "/credit",
    query: {
      reportUserId: reportedId,
      reportUserName: other.nickname || `用户 ${reportedId}`,
      reportContext: inferReportContext(activeConversation.value),
      fromConversationId: activeConversation.value.id,
    },
  });
}

async function loadBlockStatus() {
  const targetUserId = activeOtherUserId.value;
  if (!targetUserId) {
    return;
  }
  try {
    const result = await isBlockingApi(targetUserId);
    const isBlocked = Boolean(result.data);
    setBlockedUser(targetUserId, isBlocked);
  } catch {
    setBlockedUser(targetUserId, false);
  }
}

async function handleBlockUser() {
  const targetUserId = activeOtherUserId.value;
  const targetName = activeConversation.value?.other?.nickname || `用户 ${targetUserId}`;
  if (!targetUserId || blockLoading.value || isCurrentUserBlocked.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认拉黑 ${targetName}？拉黑后对方将无法继续向你发送消息。`,
      "拉黑用户",
      {
        type: "warning",
        confirmButtonText: "确认拉黑",
        cancelButtonText: "取消",
      },
    );
  } catch {
    return;
  }
  blockLoading.value = true;
  try {
    await blockUserApi(targetUserId);
    setBlockedUser(targetUserId, true);
    ElMessage.success("已拉黑该用户");
  } finally {
    blockLoading.value = false;
  }
}

function setBlockedUser(userId, blocked) {
  const next = new Set(blockedUserIds.value);
  if (blocked) {
    next.add(Number(userId));
  } else {
    next.delete(Number(userId));
  }
  blockedUserIds.value = next;
}

function inferReportContext(conversation) {
  const sourceType = String(conversation?.sourceType || "").toUpperCase();
  if (sourceType === "PRODUCT") {
    return "SHOP";
  }
  if (sourceType === "SECONDHAND") {
    return route.path.startsWith("/merchant") ? "SH_SELLER" : "SH_BUYER";
  }
  return route.path.startsWith("/merchant") ? "SH_SELLER" : "SHOP";
}

function getMediaType(file) {
  const contentType = String(file?.type || "").toLowerCase();
  if (contentType.startsWith("image/")) {
    return "image";
  }
  if (contentType.startsWith("video/")) {
    return "video";
  }
  return "";
}

function createMediaMessageContent(payload) {
  return JSON.stringify({
    messageKind: "media",
    type: payload.type,
    url: payload.url,
    filename: payload.filename || "",
    contentType: payload.contentType || "",
  });
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

function isBuyerForBargain(request) {
  return Number(request?.buyerUserId) === Number(currentUserId.value);
}

function goBargainOrder(request) {
  if (request?.orderId) {
    const query = isSellerForBargain(request) ? { from: "seller", scope: "secondhand" } : {};
    router.push({ path: `/secondhand/orders/${request.orderId}`, query });
    return;
  }
  router.push({ path: "/order", query: { type: "SECONDHAND", tab: "PENDING_PAY" } });
}

function canHandleBargain(request) {
  const status = String(request?.status || "").toUpperCase();
  return status === "PENDING" || status === "APPLIED";
}

function isBargainActionPending(request) {
  return actionLoadingKey.value === `confirm-${request?.id}` || actionLoadingKey.value === `reject-${request?.id}`;
}

async function handleConfirmBargain(request) {
  if (!canHandleBargain(request) || isBargainActionPending(request) || actionLoadingKey.value) {
    return;
  }
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
  if (!canHandleBargain(request) || isBargainActionPending(request) || actionLoadingKey.value) {
    return;
  }
  actionLoadingKey.value = `reject-${request.id}`;
  try {
    const result = await rejectBargainApi(request.id);
    ElMessage.success("已拒绝议价");
    await loadBargainRequests();
    await loadMessages(activeConversation.value.id, { silent: true });
    updateBargainInList(result.data);
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

function getMessagePayload(message) {
  const payload = parseMessageContent(message?.content);
  if (payload.type === "bargain") {
    return {
      ...payload,
      bargain: decorateBargainPayload(mergeLatestBargain(payload.bargain)),
    };
  }
  return payload;
}

function formatMessagePreview(content) {
  const payload = parseMessageContent(content);
  if (payload.type === "bargain") {
    return payload.bargain?.preview || "[议价消息]";
  }
  if (payload.type === "image") {
    return "[图片]";
  }
  if (payload.type === "video") {
    return "[视频]";
  }
  return payload.text || "点击开始聊天";
}

function parseMessageContent(content) {
  const text = String(content || "");
  if (!text) {
    return { type: "text", text: "" };
  }
  const bargainPayload = parseBargainMessage(text);
  if (bargainPayload) {
    return { type: "bargain", bargain: decorateBargainPayload(bargainPayload) };
  }
  try {
    const parsed = JSON.parse(text);
    if (parsed && typeof parsed === "object" && parsed.url) {
      const type = normalizeMediaType(parsed.type, parsed.contentType, parsed.url);
      if (type) {
        return {
          type,
          url: toAssetUrl(parsed.url),
          caption: parsed.caption || "",
          filename: parsed.filename || "",
        };
      }
    }
  } catch {
    // Plain text messages are expected for existing chat history.
  }
  const type = normalizeMediaType("", "", text);
  if (type) {
    return { type, url: toAssetUrl(text), caption: "", filename: "" };
  }
  return { type: "text", text };
}

function parseBargainMessage(text) {
  const matched = String(text || "").match(/^\[(BARGAIN_APPLY|BARGAIN_CONFIRM|BARGAIN_REJECT)](\{[\s\S]*})$/);
  if (!matched) {
    return null;
  }
  try {
    const data = JSON.parse(matched[2]);
    const kind = matched[1].replace("BARGAIN_", "");
    return {
      ...data,
      id: data.negotiationId,
      status: kind === "APPLY" ? "APPLIED" : (kind === "CONFIRM" ? "CONFIRMED" : "REJECTED"),
      bargainKind: kind,
    };
  } catch {
    return null;
  }
}

function mergeLatestBargain(bargain) {
  if (!bargain?.id) {
    return bargain;
  }
  const latest = bargainRequests.value.find((item) => Number(item.id) === Number(bargain.id));
  const processed = findProcessedBargainMessage(bargain.id);
  if (!latest && !processed) {
    return bargain;
  }
  const mergedLatest = processed || latest || {};
  return {
    ...bargain,
    ...mergedLatest,
    id: mergedLatest.id || bargain.id,
    negotiationId: mergedLatest.id || bargain.negotiationId,
    bargainKind: bargain.bargainKind,
  };
}

function findProcessedBargainMessage(bargainId) {
  for (let index = messages.value.length - 1; index >= 0; index -= 1) {
    const payload = parseBargainMessage(messages.value[index]?.content);
    if (
      payload
      && Number(payload.id) === Number(bargainId)
      && ["CONFIRM", "REJECT"].includes(payload.bargainKind)
    ) {
      return payload;
    }
  }
  return null;
}

function decorateBargainPayload(bargain) {
  const status = String(bargain?.status || "").toUpperCase();
  const proposedPrice = bargain?.proposedPrice;
  const confirmedPrice = bargain?.confirmedPrice;
  let statusLabel = bargain?.statusName || bargain?.status || "议价";
  let title = "买家发起议价";
  let actionHint = "等待卖家处理";
  if (status === "CONFIRMED" || status === "USED") {
    statusLabel = status === "USED" ? "已生成订单" : "已确认";
    title = "卖家已同意议价";
    actionHint = "议价已确认";
  } else if (status === "REJECTED") {
    statusLabel = "已拒绝";
    title = "卖家已拒绝议价";
    actionHint = "议价已拒绝";
  } else if (status === "PENDING" || status === "APPLIED") {
    statusLabel = "待处理";
    title = "买家发起议价";
    actionHint = "等待卖家处理";
  }
  const priceText = confirmedPrice || proposedPrice;
  return {
    ...bargain,
    statusLabel,
    title,
    actionHint,
    preview: priceText ? `${title} ¥${Number(priceText || 0).toFixed(2)}` : title,
  };
}

function normalizeMediaType(type, contentType, url) {
  const normalizedType = String(type || "").toLowerCase();
  if (normalizedType === "image" || normalizedType === "video") {
    return normalizedType;
  }
  const normalizedContentType = String(contentType || "").toLowerCase();
  if (normalizedContentType.startsWith("image/")) {
    return "image";
  }
  if (normalizedContentType.startsWith("video/")) {
    return "video";
  }
  const normalizedUrl = String(url || "").split("?")[0].toLowerCase();
  if (/\.(png|jpe?g|gif|webp|bmp|svg)$/.test(normalizedUrl)) {
    return "image";
  }
  if (/\.(mp4|webm|ogg|mov|m4v)$/.test(normalizedUrl)) {
    return "video";
  }
  return "";
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

.chat-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
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

.bargain-message-card {
  position: relative;
  border: 1px solid rgba(95, 176, 210, 0.24);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 253, 252, 0.96)),
    linear-gradient(135deg, rgba(95, 230, 189, 0.16), rgba(105, 185, 255, 0.14));
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  overflow: hidden;
  min-width: min(390px, 100%);
  white-space: normal;
}

.bargain-message-card::before {
  content: "";
  display: block;
  height: 4px;
  background: linear-gradient(90deg, #5fe6bd 0%, #69b9ff 54%, #ffc6dc 100%);
}

.bargain-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px 10px;
}

.bargain-card-head span {
  width: fit-content;
  border-radius: 999px;
  border: 1px solid rgba(21, 157, 125, 0.16);
  background: rgba(233, 255, 248, 0.9);
  color: #159d7d;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 900;
  flex: 0 0 auto;
}

.bargain-card-head strong {
  min-width: 0;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bargain-card-body {
  margin: 0 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.bargain-info-row {
  min-width: 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 18px;
  padding: 9px 0;
}

.bargain-info-row + .bargain-info-row {
  border-top: 1px dashed rgba(148, 163, 184, 0.22);
}

.bargain-card-body small {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.bargain-card-body b {
  color: #172033;
  font-size: 13px;
  font-weight: 900;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bargain-info-row.product-row {
  align-items: flex-start;
}

.bargain-info-row.product-row b {
  color: #334155;
  font-size: 14px;
}

.bargain-info-row.price-row b {
  color: #f05a7e;
  font-size: 18px;
  letter-spacing: 0;
}

.trade-note {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.bargain-card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 14px 14px;
}

.bargain-card-actions .el-button {
  border-radius: 8px;
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

.message-media {
  display: block;
  max-width: min(360px, 100%);
  border-radius: 10px;
  overflow: hidden;
}

.message-image {
  width: min(300px, 100%);
  max-height: 320px;
}

.message-video {
  width: min(360px, 100%);
  max-height: 320px;
  background: #0f172a;
}

.message-caption {
  margin-top: 8px;
  color: #334155;
  white-space: pre-wrap;
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

.media-input {
  display: none;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  gap: 12px;
}

.composer-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.composer-tip {
  color: #718096;
  font-size: 12px;
}

@media (max-width: 960px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .bargain-card-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .bargain-info-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .bargain-card-body b {
    text-align: left;
  }

  .message-bubble {
    max-width: 88%;
  }
}
</style>

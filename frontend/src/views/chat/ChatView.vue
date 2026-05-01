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
            <strong>{{ item.other?.nickname || '未知用户' }}</strong>
            <span>{{ formatTime(item.lastMessageTime) }}</span>
          </div>
          <div class="conversation-source">{{ formatSource(item) }}</div>
          <div class="conversation-bottom">
            <span class="conversation-preview">{{ item.lastMessageContent || '点击开始聊天' }}</span>
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
            <h3>{{ activeConversation.other?.nickname || '未知用户' }}</h3>
            <p>{{ formatSource(activeConversation) }}</p>
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
              <div class="message-author">{{ message.sender?.nickname || '用户' }}</div>

              <div v-if="parseTradeCard(message.content)?.type === 'BARGAIN_APPLY'" class="trade-card">
                <div class="trade-card-title">议价申请</div>
                <div class="trade-card-line">商品：{{ parseTradeCard(message.content)?.productName || '二手商品' }}</div>
                <div class="trade-card-line">买家出价：￥{{ parseTradeCard(message.content)?.proposedPrice }}</div>
                <el-space v-if="canHandleBargainApply(message)" wrap>
                  <el-button type="warning" size="small" @click="handleConfirmBargainFromCard(parseTradeCard(message.content))">
                    确认小刀价
                  </el-button>
                  <el-button type="danger" plain size="small" @click="handleRejectBargainFromCard(parseTradeCard(message.content))">
                    驳回小刀价
                  </el-button>
                </el-space>
              </div>

              <div v-else-if="parseTradeCard(message.content)?.type === 'BARGAIN_CONFIRM'" class="trade-card trade-card-confirmed">
                <div class="trade-card-title">议价已确认</div>
                <div class="trade-card-line">商品：{{ parseTradeCard(message.content)?.productName || '二手商品' }}</div>
                <div class="trade-card-line">成交小刀价：￥{{ parseTradeCard(message.content)?.confirmedPrice }}</div>
                <div class="trade-card-line">有效期至：{{ formatTime(parseTradeCard(message.content)?.effectiveUntil, true) }}</div>
                <el-button
                  v-if="canBuyConfirmedBargain(parseTradeCard(message.content))"
                  type="success"
                  size="small"
                  @click="goBuyBargainProduct(parseTradeCard(message.content))"
                >
                  去购买
                </el-button>
              </div>

              <div v-else-if="parseTradeCard(message.content)?.type === 'BARGAIN_REJECT'" class="trade-card trade-card-rejected">
                <div class="trade-card-title">议价已驳回</div>
                <div class="trade-card-line">商品：{{ parseTradeCard(message.content)?.productName || '二手商品' }}</div>
                <div class="trade-card-line">买家出价：￥{{ parseTradeCard(message.content)?.proposedPrice }}</div>
              </div>

              <div v-else class="message-content">{{ message.content }}</div>
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
            placeholder="输入消息，按 Ctrl/Cmd + Enter 发送"
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
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  createChatConversationApi,
  listChatConversationsApi,
  listChatMessagesApi,
  sendChatMessageApi,
} from '@/api/chat';
import { confirmBargainApi, rejectBargainApi } from '@/api/secondhand';
import { onRealtimeEvent, sendRealtimeMessage, startRealtimeClient } from '@/realtime/realtimeClient';
import { useUserStore } from '@/stores/user';
import {
  MSG_TYPE_AUCTION_BID_ACCEPTED,
  MSG_TYPE_AUCTION_OUTBID,
  MSG_TYPE_AUCTION_SETTLED,
  MSG_TYPE_BARGAIN_APPLY,
  MSG_TYPE_BARGAIN_CONFIRM,
} from '@/realtime/messageTypes';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const conversations = ref([]);
const messages = ref([]);
const activeConversationId = ref(null);
const draft = ref('');
const sendStatus = ref('正在连接实时通道...');

const currentUserId = computed(() => userStore.userInfo?.id);
const chatRoutePath = computed(() => (route.path.startsWith('/merchant') ? '/merchant/messages' : '/messages'));
const activeConversation = computed(() =>
  conversations.value.find((item) => Number(item.id) === Number(activeConversationId.value)) || null
);

let unsubscribeRealtime = null;

onMounted(async () => {
  startRealtimeClient();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
  await bootstrap();
});

onUnmounted(() => {
  if (typeof unsubscribeRealtime === 'function') unsubscribeRealtime();
});

watch(
  () => route.query.conversationId,
  async (conversationId) => {
    if (!conversationId) return;
    const found = conversations.value.find((item) => Number(item.id) === Number(conversationId));
    if (found && Number(activeConversationId.value) !== Number(found.id)) {
      await selectConversation(found);
    }
  }
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
  if (route.query.participantId) {
    await ensureConversation();
    return;
  }
  if (conversations.value.length) await selectConversation(conversations.value[0]);
}

async function refreshConversations() {
  loadingConversations.value = true;
  try {
    const result = await listChatConversationsApi();
    conversations.value = result.data || [];
  } finally {
    loadingConversations.value = false;
  }
}

async function ensureConversation() {
  const participantId = Number(route.query.participantId);
  if (!participantId) return;
  const result = await createChatConversationApi({
    targetUserId: participantId,
    sourceType: route.query.sourceType || 'DIRECT',
    sourceId: route.query.sourceId ? Number(route.query.sourceId) : null,
  });
  const conversation = result.data;
  const existingIndex = conversations.value.findIndex((item) => Number(item.id) === Number(conversation.id));
  if (existingIndex >= 0) conversations.value.splice(existingIndex, 1, conversation);
  else conversations.value.unshift(conversation);
  await router.replace({ path: chatRoutePath.value, query: { conversationId: conversation.id } });
  await selectConversation(conversation);
}

async function selectConversation(conversation) {
  if (!conversation?.id) return;
  activeConversationId.value = conversation.id;
  await loadMessages(conversation.id);
  if (String(route.query.conversationId || '') !== String(conversation.id)) {
    router.replace({ path: chatRoutePath.value, query: { conversationId: conversation.id } });
  }
}

async function loadMessages(conversationId) {
  loadingMessages.value = true;
  try {
    const result = await listChatMessagesApi(conversationId);
    messages.value = result.data || [];
    clearConversationUnread(conversationId);
    await scrollToBottom();
  } finally {
    loadingMessages.value = false;
  }
}

async function sendMessage() {
  const content = draft.value.trim();
  if (!content || !activeConversation.value) return;
  sending.value = true;
  try {
    const sentByRealtime = sendRealtimeMessage({
      eventType: 'CHAT_SEND',
      payload: { conversationId: activeConversation.value.id, content },
    });
    if (!sentByRealtime) {
      sendStatus.value = '实时通道未连上，已切换为普通发送';
      const result = await sendChatMessageApi(activeConversation.value.id, { content });
      appendIncomingMessage(result.data);
    }
    draft.value = '';
    await scrollToBottom();
  } finally {
    sending.value = false;
  }
}

function handleComposerKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault();
    sendMessage();
  }
}

function handleRealtimeEvent(event) {
  const detail = event?.detail;
  if (!detail) return;
  if (detail.eventType === 'CONNECTED' || detail.eventType === 'PONG') {
    sendStatus.value = '实时通道已连接';
    return;
  }
  if (detail.eventType === 'CHAT_MESSAGE' && detail.payload) {
    sendStatus.value = '实时通道已连接';
    appendIncomingMessage(detail.payload);
    return;
  }
  if (detail.eventType === 'CHAT_ERROR') {
    ElMessage.error(detail.payload?.message || '消息发送失败');
    return;
  }
  if (detail.eventType === MSG_TYPE_BARGAIN_APPLY) ElMessage.info('收到新的议价申请');
  if (detail.eventType === MSG_TYPE_BARGAIN_CONFIRM) ElMessage.success('议价状态已更新');
  if (detail.eventType === MSG_TYPE_AUCTION_BID_ACCEPTED) ElMessage.success('拍卖出价成功');
  if (detail.eventType === MSG_TYPE_AUCTION_OUTBID) ElMessage.warning('您已被他人超价，预扣金额已退回');
  if (detail.eventType === MSG_TYPE_AUCTION_SETTLED) ElMessage.info('拍卖已完成结算');
}

function parseTradeCard(content) {
  if (!content || typeof content !== 'string') return null;
  if (content.startsWith('[BARGAIN_APPLY]')) return parseCardPayload('BARGAIN_APPLY', content.slice('[BARGAIN_APPLY]'.length));
  if (content.startsWith('[BARGAIN_CONFIRM]')) return parseCardPayload('BARGAIN_CONFIRM', content.slice('[BARGAIN_CONFIRM]'.length));
  if (content.startsWith('[BARGAIN_REJECT]')) return parseCardPayload('BARGAIN_REJECT', content.slice('[BARGAIN_REJECT]'.length));
  return null;
}

function parseCardPayload(type, text) {
  try {
    const data = JSON.parse(text || '{}');
    return { type, ...data };
  } catch {
    return null;
  }
}

function canHandleBargainApply(message) {
  const card = parseTradeCard(message?.content);
  if (!card || card.type !== 'BARGAIN_APPLY') return false;
  return Number(message?.senderUserId) !== Number(currentUserId.value);
}

function canBuyConfirmedBargain(card) {
  if (!card || card.type !== 'BARGAIN_CONFIRM') return false;
  return Number(card.buyerUserId) === Number(currentUserId.value);
}

async function handleConfirmBargainFromCard(card) {
  if (!card?.negotiationId) return;
  const defaultPrice = Number(card.proposedPrice || 0);
  const finalPrice = Number.isFinite(defaultPrice) && defaultPrice > 0 ? defaultPrice : 0.01;
  try {
    await confirmBargainApi({
      negotiationId: Number(card.negotiationId),
      confirmedPrice: finalPrice.toFixed(2),
    });
    ElMessage.success('已确认小刀价');
    if (activeConversationId.value) await loadMessages(activeConversationId.value);
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '确认议价失败');
  }
}

async function handleRejectBargainFromCard(card) {
  if (!card?.negotiationId) return;
  try {
    await ElMessageBox.confirm('确认驳回该小刀价？', '驳回小刀价', { type: 'warning' });
    await rejectBargainApi(Number(card.negotiationId));
    ElMessage.success('已驳回小刀价');
    if (activeConversationId.value) await loadMessages(activeConversationId.value);
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '驳回议价失败');
  }
}

function goBuyBargainProduct(card) {
  if (!card?.productId || !canBuyConfirmedBargain(card)) return;
  router.push({ path: `/secondhand/${card.productId}`, query: { action: 'buy' } });
}

function appendIncomingMessage(message) {
  if (!message?.conversationId) return;
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
  if (target) target.unreadCount = Number(target.unreadCount || 0) + 1;
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
  if (conversation) conversation.unreadCount = 0;
}

function formatSource(conversation) {
  const type = String(conversation?.sourceType || 'DIRECT').toUpperCase();
  if (type === 'PRODUCT') return `商品咨询：${conversation?.sourceTitle || '商品'}`;
  if (type === 'SECONDHAND') return `二手商品咨询：${conversation?.sourceTitle || '商品'}`;
  return conversation?.sourceTitle || '站内私聊';
}

function formatTime(value, withTime = false) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return withTime
    ? date.toLocaleString('zh-CN', { hour12: false })
    : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false });
}

async function scrollToBottom() {
  await nextTick();
  const container = document.querySelector('.message-list');
  if (container) container.scrollTop = container.scrollHeight;
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
  border: 1px solid #e5ebf3;
  border-radius: 18px;
  overflow: hidden;
}

.panel-head,
.chat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid #eef2f7;
  background: linear-gradient(135deg, #f7fbff 0%, #f4f9f2 100%);
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
  border-bottom: 1px solid #eef2f7;
  background: transparent;
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  background: #f5f9ff;
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

.trade-card {
  border: 1px solid #f5d08a;
  background: #fffbeb;
  border-radius: 12px;
  padding: 10px;
}

.trade-card-confirmed {
  border-color: #86efac;
  background: #f0fdf4;
}

.trade-card-rejected {
  border-color: #fecaca;
  background: #fff1f2;
}

.trade-card-title {
  font-weight: 700;
  color: #92400e;
  margin-bottom: 6px;
}

.trade-card-line {
  color: #3f3f46;
  font-size: 13px;
  margin-bottom: 4px;
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

  .message-bubble {
    max-width: 88%;
  }
}
</style>

<template>
  <div class="page-card">
    <div class="page-head">
      <div>
        <h2 class="page-title">{{ pageTitle }}</h2>
        <p class="page-subtitle">{{ pageSubtitle }}</p>
      </div>
      <el-button :disabled="!hasUnread" @click="markAllRead">全部标为已读</el-button>
    </div>

    <div v-if="loading" class="loading-panel">
      <el-skeleton :rows="6" animated />
    </div>

    <el-empty v-else-if="!notifications.length" description="暂无通知" />

    <div v-else class="notification-list">
      <article
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: Number(item.isRead) === 0 }"
      >
        <div class="notification-top">
          <div class="notification-title-wrap">
            <strong>{{ item.title || "通知" }}</strong>
            <el-tag v-if="Number(item.isRead) === 0" size="small" type="danger">未读</el-tag>
          </div>
          <span class="notification-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <p class="notification-content">{{ item.content }}</p>
        <div class="notification-actions">
          <el-button
            v-if="Number(item.isRead) === 0"
            size="small"
            text
            type="primary"
            @click="markRead(item)"
          >
            标为已读
          </el-button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import {
  listNotificationsApi,
  markAllNotificationsReadApi,
  markNotificationReadApi,
} from "@/api/notification";
import { onRealtimeEvent, startRealtimeClient } from "@/realtime/realtimeClient";

const loading = ref(false);
const notifications = ref([]);
const route = useRoute();
let unsubscribeRealtime = null;

const notificationScope = computed(() => (route.path.startsWith("/merchant") ? "seller" : "buyer"));
const pageTitle = computed(() => (notificationScope.value === "seller" ? "卖家通知" : "通知"));
const pageSubtitle = computed(() =>
  notificationScope.value === "seller"
    ? "订单、发货、售后和店铺提醒会出现在这里"
    : "购买、物流、售后和账号提醒会出现在这里",
);
const hasUnread = computed(() =>
  notifications.value.some((item) => Number(item.isRead) === 0),
);

onMounted(async () => {
  startRealtimeClient();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
  await fetchNotifications();
});

onUnmounted(() => {
  if (typeof unsubscribeRealtime === "function") {
    unsubscribeRealtime();
  }
});

async function fetchNotifications() {
  loading.value = true;
  try {
    const result = await listNotificationsApi(notificationScope.value);
    notifications.value = result.data || [];
  } finally {
    loading.value = false;
  }
}

async function markRead(item) {
  await markNotificationReadApi(item.id);
  item.isRead = 1;
}

async function markAllRead() {
  await markAllNotificationsReadApi(notificationScope.value);
  notifications.value = notifications.value.map((item) => ({ ...item, isRead: 1 }));
  ElMessage.success("已全部标为已读");
}

function handleRealtimeEvent(event) {
  const detail = event?.detail;
  if (!detail) {
    return;
  }
  if (detail.eventType === "NOTIFICATION_CREATED" && detail.payload) {
    if (!isCurrentScopeNotification(detail.payload)) {
      return;
    }
    prependNotification(detail.payload);
    return;
  }
  if (
    detail.eventType === "ORDER_STATUS_UPDATED" ||
    detail.eventType === "AFTER_SALE_UPDATED" ||
    detail.eventType === "LOGISTICS_UPDATED" ||
    detail.eventType === "ORDER_REMIND_SHIP"
  ) {
    fetchNotifications();
  }
}

function isCurrentScopeNotification(item) {
  const itemScope = item?.scope || item?.notificationScope || item?.scene || inferNotificationScope(item);
  return itemScope === notificationScope.value;
}

function inferNotificationScope(item) {
  const text = `${item?.title || ""} ${item?.content || ""}`;
  const sellerKeywords = [
    "\u53d1\u8d27",
    "\u5356\u5bb6",
    "\u5e97\u94fa",
    "\u5de5\u4f5c\u53f0",
  ];
  if (
    sellerKeywords.some((keyword) => text.includes(keyword)) ||
    (text.includes("\u5165\u9a7b") && text.includes("\u901a\u8fc7"))
  ) {
    return "seller";
  }
  return "buyer";
}

function prependNotification(item) {
  const exists = notifications.value.some((current) => Number(current.id) === Number(item.id));
  if (exists) {
    return;
  }
  notifications.value.unshift(item);
}

function formatTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });
}
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.loading-panel {
  padding: 12px 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notification-item {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px 18px;
  background: #fff;
}

.notification-item.unread {
  border-color: #8bc34a;
  box-shadow: 0 10px 24px rgba(139, 195, 74, 0.1);
}

.notification-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.notification-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-time {
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.notification-content {
  margin: 12px 0 0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

.notification-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-head,
  .notification-top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

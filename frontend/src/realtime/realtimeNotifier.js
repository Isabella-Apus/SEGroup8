import { ElMessage } from "element-plus";
import { onRealtimeEvent } from "@/realtime/realtimeClient";
import { useUserStore } from "@/stores/user";

let unsubscribe = null;

export function startRealtimeNotifier(router) {
  if (unsubscribe) {
    return;
  }
  unsubscribe = onRealtimeEvent((event) => handleRealtimeEvent(event, router));
}

export function stopRealtimeNotifier() {
  if (typeof unsubscribe === "function") {
    unsubscribe();
  }
  unsubscribe = null;
}

function handleRealtimeEvent(event, router) {
  const detail = event?.detail;
  if (!detail) {
    return;
  }
  const userStore = useUserStore();
  const currentUserId = userStore.userInfo?.id;

  if (detail.eventType === "CHAT_MESSAGE" && detail.payload) {
    const message = detail.payload;
    if (Number(message.receiverUserId) !== Number(currentUserId)) {
      return;
    }
    const currentPath = router?.currentRoute?.value?.path || "";
    if (currentPath.includes("/messages")) {
      return;
    }
    ElMessage.info({
      message: "收到一条新的站内消息",
      duration: 3500,
    });
    return;
  }

  if (detail.eventType === "NOTIFICATION_CREATED" && detail.payload) {
    const scope = getNotificationScope(detail.payload);
    const currentPath = router?.currentRoute?.value?.path || "";
    if (
      (scope === "seller" && currentPath.startsWith("/merchant/notifications")) ||
      (scope === "buyer" && currentPath === "/notifications")
    ) {
      return;
    }
    ElMessage.info({
      message: scope === "seller" ? "卖家工作台收到一条新通知" : "商城收到一条新通知",
      duration: 3500,
    });
    return;
  }

  const eventMessage = detail.payload?.message;
  if (
    eventMessage &&
    [
      "ORDER_STATUS_UPDATED",
      "AFTER_SALE_UPDATED",
      "LOGISTICS_UPDATED",
      "ORDER_REMIND_SHIP",
      "MSG_TYPE_BARGAIN_APPLY",
      "MSG_TYPE_BARGAIN_CONFIRM",
      "MSG_TYPE_AUCTION_OUTBID",
      "MSG_TYPE_AUCTION_BID_ACCEPTED",
      "MSG_TYPE_AUCTION_SETTLED",
    ].includes(detail.eventType)
  ) {
    ElMessage.info({
      message: String(eventMessage),
      duration: 3500,
    });
  }
}

function getNotificationScope(item) {
  const explicit = item?.scope || item?.notificationScope || item?.scene;
  if (explicit === "seller" || explicit === "buyer") {
    return explicit;
  }
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

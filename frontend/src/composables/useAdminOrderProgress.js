import { computed } from "vue";
import { toApiAssetUrl } from "@/utils/url";

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

export function useAdminOrderProgress(drawerOrderRef) {
  const proofList = computed(() => {
    const raw = drawerOrderRef.value?.refundProofUrls;
    if (!raw) return [];
    return String(raw)
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
  });

  function toFullImageUrl(url) {
    if (!url) return "";
    if (url.startsWith("http://") || url.startsWith("https://")) return url;
    const normalized = url.startsWith("/") ? url : `/${url}`;
    return toApiAssetUrl(normalized);
}

  const orderTimeline = computed(() => {
    const o = drawerOrderRef.value;
    if (!o) return [];
    const orderStatus = o.orderStatus ?? 0;
    const refundStatus = o.refundStatus ?? 0;
    const isClosed = orderStatus === 9;
    const hasRefund = refundStatus > 0;
    const refundTerminal = !!o.refundDecisionTime && hasRefund && (refundStatus === 2 || refundStatus === 3);
    const decisionTime = o.refundDecisionTime ? formatTime(o.refundDecisionTime) : "";
    const steps = [
      { key: "created", label: "已下单", done: true, time: formatTime(o.createTime) },
      { key: "paid", label: "已付款", done: !!o.paidTime, time: o.paidTime ? formatTime(o.paidTime) : "-" },
      { key: "shipped", label: "已发货", done: !!o.shippedTime, time: o.shippedTime ? formatTime(o.shippedTime) : "-" },
      {
        key: "received",
        label: o.receivedTime ? "已收货" : refundTerminal ? (refundStatus === 2 ? "已退款" : "退款被拒绝") : "待收货",
        done: !!o.receivedTime || refundTerminal,
        time: o.receivedTime ? formatTime(o.receivedTime) : decisionTime || "-"
      },
      {
        key: "review",
        label: o.completedTime ? "已评价" : refundTerminal ? "退款影响评价" : "待评价",
        done: !!o.completedTime || refundTerminal,
        time: o.completedTime ? formatTime(o.completedTime) : decisionTime || "-"
      },
      {
        key: "completed",
        label: o.completedTime ? (isClosed && hasRefund ? "已完成(退款后关闭)" : "已完成") : refundTerminal ? "已完成(未完成)" : "已完成",
        done: !!o.completedTime,
        time: o.completedTime ? formatTime(o.completedTime) : "-"
      },
      { key: "closed", label: "已关闭", done: isClosed || !!o.closedTime, time: o.closedTime ? formatTime(o.closedTime) : "-" }
    ];
    let activeKey = "created";
    if (isClosed) activeKey = "closed";
    else if (refundTerminal) activeKey = o.completedTime ? "completed" : "review";
    else if (orderStatus === 4) activeKey = "completed";
    else if (orderStatus === 3) activeKey = "review";
    else if (orderStatus === 2) activeKey = "received";
    else if (orderStatus === 1) activeKey = "paid";
    return steps.map((s) => ({ ...s, status: s.done ? "done" : s.key === activeKey ? "active" : "todo" }));
  });

  const refundTimeline = computed(() => {
    const o = drawerOrderRef.value;
    if (!o) return [];
    const rs = o.refundStatus ?? 0;
    if (rs <= 0) return [];
    const decisionTime = o.refundDecisionTime ? formatTime(o.refundDecisionTime) : "";
    const steps = [
      { key: "apply", label: "买家申请退货", done: true, time: o.refundApplyTime ? formatTime(o.refundApplyTime) : "-" },
      { key: "processing", label: "退款中", done: rs >= 1, time: rs === 1 ? "-" : decisionTime || "-" },
      { key: "refunded", label: rs >= 2 ? `已退款（${o.refundDecisionRemark || "平台/卖家已同意"}）` : "已退款", done: rs >= 2, time: rs >= 2 ? decisionTime || "-" : "-" },
      { key: "rejected", label: rs >= 3 ? `退款被拒绝（${o.refundDecisionRemark || "平台/卖家已拒绝"}）` : "退款被拒绝", done: rs >= 3, time: rs >= 3 ? decisionTime || "-" : "-" }
    ];
    let activeKey = "apply";
    if (rs === 1) activeKey = "processing";
    else if (rs === 2) activeKey = "refunded";
    else if (rs === 3) activeKey = "rejected";
    return steps.map((s) => ({ ...s, status: s.done ? "done" : s.key === activeKey ? "active" : "todo" }));
  });

  const orderStageSummary = computed(() => {
    const o = drawerOrderRef.value;
    if (!o) return "-";
    if ((o.refundStatus ?? 0) === 1) return "售后处理中（等待审核）";
    if ((o.refundStatus ?? 0) === 2) return "售后已完成（已退款）";
    if ((o.refundStatus ?? 0) === 3) return "售后已拒绝";
    if (o.orderStatus === 0) return "待付款";
    if (o.orderStatus === 1) return "待发货";
    if (o.orderStatus === 2) return "待收货";
    if (o.orderStatus === 3) return "待评价";
    if (o.orderStatus === 4) return "已完成";
    if (o.orderStatus === 9) return "已关闭";
    return "状态未知";
  });

  const orderNextActionSummary = computed(() => {
    const o = drawerOrderRef.value;
    if (!o) return "-";
    if ((o.refundStatus ?? 0) === 1) return "请审核退款申请：同意退货并退款，或填写原因后拒绝。";
    if ((o.refundStatus ?? 0) === 2) return "退款流程已结束，建议核对售后日志与资金记录。";
    if ((o.refundStatus ?? 0) === 3) return "流程已拒绝，建议核对审核意见是否完整。";
    if (o.orderStatus === 0) return "待用户付款，暂无后台动作。";
    if (o.orderStatus === 1) return "可跟进发货履约，必要时提醒商家处理。";
    if (o.orderStatus === 2) return "待买家收货，关注物流与异常申诉。";
    if (o.orderStatus === 3) return "待买家评价，关注售后风险。";
    if (o.orderStatus === 4) return "订单已完成，建议归档。";
    if (o.orderStatus === 9) return "订单已关闭，建议核对关闭原因。";
    return "请刷新页面后重试。";
  });

  return {
    proofList,
    toFullImageUrl,
    orderTimeline,
    refundTimeline,
    orderStageSummary,
    orderNextActionSummary
  };
}


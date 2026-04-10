import { uiDialog, uiMessage } from "@/utils/uiFeedback";

const ERROR_HINT_MAP = {
  400: "当前订单状态已变化，请刷新后重试。",
  401: "登录状态已失效，请重新登录后操作。",
  403: "你没有当前订单操作权限，请确认账号身份。",
  404: "订单可能已被删除或关闭，请刷新列表确认。",
  409: "检测到重复提交，请勿连续点击，稍后查看结果。"
};

export function buildOrderErrorMessage(error, fallback = "操作失败，请稍后重试") {
  const code = error?.response?.data?.code;
  const reason =
    error?.response?.data?.message || error?.message || fallback;
  const hint = ERROR_HINT_MAP[code] || "请稍后重试；若持续失败请联系管理员。";
  return `${reason}（建议：${hint}）`;
}

export async function confirmOrderAction({
  title,
  message,
  confirmButtonText = "确认",
  cancelButtonText = "取消",
  type = "warning"
}) {
  await uiDialog.confirm(message, title, {
    type,
    confirmButtonText,
    cancelButtonText
  });
}

export function showOrderActionError(error, fallback) {
  const msg = buildOrderErrorMessage(error, fallback);
  uiMessage.error(msg);
}

export function showOrderActionSuccess(message) {
  uiMessage.success(message);
}


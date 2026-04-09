import http from "./http";

export function getAdminOrderListApi(params = {}) {
  return http.get("/admin/orders/list", { params });
}

export function getAdminOrderDetailApi(orderId) {
  return http.get(`/admin/orders/detail/${orderId}`);
}

export function getAdminOrderAfterSaleLogsApi(orderId) {
  return http.get(`/admin/orders/${orderId}/after-sale-logs`);
}

export function batchCloseAdminOrderApi(orderIds = []) {
  return http.post("/admin/orders/batch-close", { orderIds });
}

export function approveAdminRefundOrderApi(orderId, payload = {}) {
  return http.post(`/admin/orders/${orderId}/refund/approve`, payload);
}

export function rejectAdminRefundOrderApi(orderId, payload = {}) {
  return http.post(`/admin/orders/${orderId}/refund/reject`, payload);
}


import http from './http';

export function pageAdminOrdersApi(params = {}) {
    return http.get('/admin/orders/list', { params });
}

export function getAdminOrderDetailApi(orderId) {
    return http.get(`/admin/orders/detail/${orderId}`);
}

export function batchCloseAdminOrdersApi(orderIds) {
    return http.post('/admin/orders/batch-close', { orderIds });
}

export function approveAdminRefundApi(orderId, payload = {}) {
    return http.post(`/admin/orders/${orderId}/refund/approve`, payload);
}

export function rejectAdminRefundApi(orderId, payload = {}) {
    return http.post(`/admin/orders/${orderId}/refund/reject`, payload);
}

export function getAdminAfterSaleLogsApi(orderId) {
    return http.get(`/admin/orders/${orderId}/after-sale-logs`);
}
